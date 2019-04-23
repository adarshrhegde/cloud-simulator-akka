package com.cloudsimulator.entities.vm

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus
import com.cloudsimulator.entities.PrintCloudletsExectionStatus
import com.cloudsimulator.entities.cost.Cost
import com.cloudsimulator.entities.payload.cloudlet.{CloudletExecution, CloudletPayload}
import com.cloudsimulator.entities.policies.cloudletscheduler.{CloudletScheduler, TimeSharedCloudletScheduler}
import com.cloudsimulator.entities.policies.vmscheduler.VmRequirement
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceCompleted, TimeSliceInfo}
import com.cloudsimulator.utils.ActorUtility

class VmActor(id : Long, userId : Long, mips : Long,
              noOfPes : Int, ram : Long, bw : Double,cost : Cost)
  extends Actor with ActorLogging {
  var timeSliceInfo:TimeSliceInfo=_
  var currentCloudletsExecSeq:Seq[CloudletExecution]=Seq()
  val cloudletScheduler: CloudletScheduler = TimeSharedCloudletScheduler()

  override def receive: Receive = {

    /**
      * Sender: HostActor
      * A compatible cloudlet which can be executed by this VM
      * Using the CloudletScheduler the cloudlet will be processed by the VM.
      */
    case ScheduleCloudlet(reqId, cloudlet) => {
      log.info(s"HostActor::VmActor:ScheduleCloudlet:$reqId:$cloudlet")
      currentCloudletsExecSeq = currentCloudletsExecSeq :+ convertCloudletPayLoadToCloudletExecutionList(cloudlet)
    }

    case SendVmRequirement() => {

      sender() ! VmRequirement(id, self, mips, noOfPes)
    }

    /**
      * Sender: VmScheduler
      * TimeSliceInfo provided by the VM Scheduler.
      * The cloudlets details and the timeslice is provide to the CloudletScheduler
      * and execution of the cloudlets is triggered.
      */
    case sendTimeSliceInfo: SendTimeSliceInfo => {
      log.info(s"VmScheduler::VmActor:SendTimeSliceInfo:Received:$id")
      log.info(s"Received Time Slice $sendTimeSliceInfo")
      log.info(s"currentCloudletsSeq:$currentCloudletsExecSeq")

      // TODO : check if the cloudlet cannot be executed within the available resources

      // then update the status of cloudlet accordingly.
      timeSliceInfo = sendTimeSliceInfo.sliceInfo
      // send the List CloudletExecution for execution
      val cloudletsExecuted:Seq[CloudletExecution]=cloudletScheduler.scheduleCloudlets(
        timeSliceInfo,mips,noOfPes,
        currentCloudletsExecSeq,cost)

      log.info(s"VmScheduler::VmActor:SendTimeSliceInfo:Completed:$id")
      log.info(s"VmScheduler::VmActor:SendTimeSliceInfo:cloudletsExecuted:$cloudletsExecuted")

      context.system.actorSelection(ActorUtility.getActorRef(ActorUtility.cloudletPrintActor)) ! PrintCloudletsExectionStatus(cloudletsExecuted)
      sender() ! TimeSliceCompleted(sendTimeSliceInfo.sliceInfo)
    }
  }

  def convertCloudletPayLoadToCloudletExecutionList(cloudlet: CloudletPayload): CloudletExecution ={

      CloudletExecution(cloudlet.payloadId,cloudlet,Seq(),cloudlet.delay,cloudlet.dcId,cloudlet.hostId,cloudlet.vmId,-1,-1,CloudletPayloadStatus.ASSIGNED_TO_VM,cloudlet.workloadLength,cloudlet.cost)

  }
}


case class ScheduleCloudlet(reqId: Long, cloudlet: CloudletPayload)

case class SendVmRequirement()