package com.cloudsimulator.entities.policies.vmscheduler

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceInfo}
import com.cloudsimulator.entities.vm.SendVmRequirement


/**
  * VmSchedulerActor
  * Decides the scheduling of the VMs on the host
  * @param vmScheduler
  */
class VmSchedulerActor(vmScheduler: VmScheduler) extends Actor with ActorLogging {

  private val vmActorPaths : Seq[ActorRef] = Seq()

  private val vmRequirementList : Seq[VmRequirement] = Seq()

  private var slice : TimeSliceInfo = _

  override def receive: Receive = {

    case (scheduleVms: ScheduleVms) => {



      log.info("HostActor::VmSchedulerActor:ScheduleVMs")

      vmActorPaths ++ scheduleVms.vmList
      slice = scheduleVms.slice
      scheduleVms.vmList.foreach(vm => {

        vm ! SendVmRequirement()
      })

    }

    case CheckCanSchedule => {

      if(vmActorPaths.size == vmRequirementList.size) {

        val assignment : Seq[SliceAssignment] = vmScheduler.scheduleVms(slice.slice, vmRequirementList)

        assignment.foreach(sliceAssigment => {

          sliceAssigment.vmRef ! SendTimeSliceInfo(new TimeSliceInfo(sliceAssigment.sliceLength , slice.sliceId, slice.sliceStartSysTime))
        })
      }
    }

    case vmRequirement: VmRequirement => {

      log.info("VmActor::VmSchedulerActor:VmRequirement")

      // TODO check if vm already has already sent the requirement
      vmRequirementList :+ vmRequirement

      self ! CheckCanSchedule
    }
  }
}

case class ScheduleVms(slice : TimeSliceInfo, vmList : Iterable[ActorRef]) extends NetworkPacket

case class VmRequirement(vmId : Long, ref : ActorRef, mips : Long, noOfPes : Int)

case class CheckCanSchedule()