package com.cloudsimulator.entities.vm

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.payload.CloudletPayload
import com.cloudsimulator.entities.policies.vmscheduler.VmRequirement
import com.cloudsimulator.entities.time.SendTimeSliceInfo

class VmActor(id : Long, userId : Long, mips : Long,
              noOfPes : Int, ram : Long, bw : Double)
  extends Actor with ActorLogging {

  override def receive: Receive = {

    /**
      * Sender: HostActor
      * A compatible cloudlet which can be executed by this VM
      * Using the CloudletScheduler the cloudlet will be processed by the VM.
      */
    case ScheduleCloudlet(reqId,cloudlet)=>{

    }

    case SendVmRequirement() => {

      sender() ! VmRequirement(id, self, mips, noOfPes)
    }

    case (sendTimeSliceInfo: SendTimeSliceInfo) => {
      print(s"Received Time Slice $sendTimeSliceInfo")
      // TODO : execute
    }
  }
}


case class ScheduleCloudlet(reqId:Long,cloudlet:CloudletPayload)

case class SendVmRequirement()