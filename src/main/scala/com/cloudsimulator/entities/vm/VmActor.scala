package com.cloudsimulator.entities.vm

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus
import com.cloudsimulator.entities.payload.cloudlet.{CloudletExecution, CloudletPayload}
import com.cloudsimulator.entities.policies.vmscheduler.VmRequirement
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceInfo}

class VmActor(id : Long, userId : Long, mips : Long,
              noOfPes : Int, ram : Long, bw : Double)
  extends Actor with ActorLogging {
  var timeSliceInfo:TimeSliceInfo=_
  var currentCloudletsSeq:Seq[CloudletPayload]=Seq()

  override def receive: Receive = {

    /**
      * Sender: HostActor
      * A compatible cloudlet which can be executed by this VM
      * Using the CloudletScheduler the cloudlet will be processed by the VM.
      */
    case ScheduleCloudlet(reqId, cloudlet) => {
      log.info(s"HostActor::VmActor:ScheduleCloudlet:$reqId:$cloudlet")
      currentCloudletsSeq = currentCloudletsSeq :+ cloudlet
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
      print(s"Received Time Slice $sendTimeSliceInfo")
      // TODO : execute
      // TODO : check if the cloudlet cannot be executed within the available resources
      // then update the status of cloudlet accordingly.
      timeSliceInfo = sendTimeSliceInfo.sliceInfo
      //TODO send the List CloudletExecution
      convertCloudletPayLoadToCloudletExecutionList(currentCloudletsSeq)
    }
  }

  def convertCloudletPayLoadToCloudletExecutionList(cloudletPayloads: Seq[CloudletPayload]): Seq[CloudletExecution] ={
    cloudletPayloads.map(cloudlet=>{
      CloudletExecution(cloudlet,Seq(),cloudlet.delay,cloudlet.dcId,cloudlet.hostId,-1,-1,CloudletPayloadStatus.ASSIGNED_TO_VM,cloudlet.workloadLength)
    })
  }
}


case class ScheduleCloudlet(reqId: Long, cloudlet: CloudletPayload)

case class SendVmRequirement()