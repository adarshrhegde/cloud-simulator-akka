package com.cloudsimulator.entities.policies

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.host.SendResourceStatus
import com.cloudsimulator.entities.payload.VMPayload

class VmAllocationPolicyActor(vmAllocationPolicy: VmAllocationPolicy)
  extends Actor with ActorLogging {

  /**
    * This flag is used to make sure the VM Allocation Policy is only
    * working on one allocation request at a time
    */


  private var isProcessing : Boolean = false

  private var requestId : Long = _
  private var vmPayloads : List[VMPayload] = _

  override def receive: Receive = {

    /**
      * The DataCenter asks the allocation policy actor to decide the VM to
      * Host mapping for the Vm creation request
      */
    case RequestVmAllocation(requestId, vmPayloads, hostList) => {

      isProcessing = true
      this.vmPayloads = vmPayloads
      this.requestId = requestId
      hostList.map(host => context.actorSelection(host))
        .foreach(hostActor => {
          hostActor ! SendResourceStatus(requestId)

      })

    }
    case _ => ???

  }
}


case class RequestVmAllocation(id : Long, vmPayloads : List[VMPayload], hostList : List[String])
