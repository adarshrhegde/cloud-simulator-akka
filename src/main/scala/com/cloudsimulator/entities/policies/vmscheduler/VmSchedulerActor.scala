package com.cloudsimulator.entities.policies.vmscheduler

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.cloudsimulator.entities.host.HostResource
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceCompleted, TimeSliceInfo}
import com.cloudsimulator.entities.vm.SendVmRequirement


/**
  * VmSchedulerActor
  * Decides the scheduling of the VMs on the host
  * @param vmScheduler
  */
class VmSchedulerActor(vmScheduler: VmScheduler) extends Actor with ActorLogging {

  private var vmActorPaths : Seq[ActorRef] = Seq()

  private var vmRequirementList : Seq[VmRequirement] = Seq()

  private var slice : TimeSliceInfo = _

  private var hostResource : HostResource = _

  var mapSliceIdToVmCountRem: Map[Long, Long] = Map()

  override def receive: Receive = {


    /**
      * Sender : HostActor
      * Request to schedule the Vms on the host for the time slice
      */
    case scheduleVms: ScheduleVms => {

      log.info("HostActor::VmSchedulerActor:ScheduleVMs")

      vmActorPaths = vmActorPaths ++ scheduleVms.vmList

      slice = scheduleVms.slice
      hostResource = scheduleVms.hostResource

      //count for time-slices sent to VMs
      mapSliceIdToVmCountRem=mapSliceIdToVmCountRem + (slice.sliceId -> scheduleVms.vmList.size)

      scheduleVms.vmList.foreach(vm => {

        vm ! SendVmRequirement()
      })

    }

    /**
      * Check if all Vms have responded with the requirements (required to perform scheduling)
      * If true, perform VM scheduling
      */
    case CheckCanSchedule => {

      if(vmActorPaths.size == vmRequirementList.size) {

        // Invoke injected VM scheduling logic
        val assignment : Seq[SliceAssignment] = vmScheduler.scheduleVms(slice.slice, vmRequirementList, hostResource)

        // Assign the time slice info to each vm
        assignment.foreach(sliceAssigment => {

          sliceAssigment.vmRef ! SendTimeSliceInfo(new TimeSliceInfo(slice.sliceId, sliceAssigment.sliceLength , slice.sliceStartSysTime))
        })
      }
    }

    /**
      * Receive VM Requirement info from VM and maintain it
      */
    case vmRequirement: VmRequirement => {

      log.info(s"VmActor::VmSchedulerActor:VmRequirement:${vmRequirement.vmId}")

      vmRequirementList.count(vm => vm.vmId == vmRequirement.vmId) match {

        case 0 => {
          vmRequirementList = vmRequirementList :+ vmRequirement

          self ! CheckCanSchedule
        }
        case _ => {

          log.info(s"Already received requirement info from VM ${vmRequirement.vmId}")
        }

      }

    }
      //TODO call TimeSliceCompleted on the host when all timeslices are exhausted
    /**
      * Sender : VmActor
      * Time slice completed message received from the VmActor.
      */
    case timeSliceCompleted: TimeSliceCompleted=>{
      log.info(s"VmActor::VmSchedulerActor:TimeSliceCompleted:timeSlotId:${timeSliceCompleted.timeSliceInfo.sliceId}")


      val newCount:Option[Long]=mapSliceIdToVmCountRem.get(timeSliceCompleted.timeSliceInfo.sliceId).map(count=>{
        log.info(s"VmActor::VmSchedulerActor:TimeSliceCompleted: Count is ${count-1}")
        mapSliceIdToVmCountRem=
          mapSliceIdToVmCountRem +
            (timeSliceCompleted.timeSliceInfo.sliceId -> (count-1))
        count-1
      })

      newCount.filter(_==0).foreach(_ => {
        log.info(s"VmActor::VmSchedulerActor:TimeSliceCompleted: Count is 0")
        context.parent ! TimeSliceCompleted(timeSliceCompleted.timeSliceInfo)
      })

    }
  }
}

case class ScheduleVms(slice : TimeSliceInfo, vmList : Iterable[ActorRef], hostResource: HostResource) extends NetworkPacket

case class VmRequirement(vmId : Long, ref : ActorRef, mips : Long, noOfPes : Int)

case class CheckCanSchedule()