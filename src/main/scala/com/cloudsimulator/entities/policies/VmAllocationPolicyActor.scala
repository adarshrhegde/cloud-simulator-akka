package com.cloudsimulator.entities.policies

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.ReceiveVmAllocation
import com.cloudsimulator.entities.host.{HostResource, RequestHostResourceStatus}
import com.cloudsimulator.entities.payload.VMPayload

import scala.concurrent.duration.FiniteDuration

class VmAllocationPolicyActor(vmAllocationPolicy: VmAllocationPolicy)
  extends Actor with ActorLogging {

    // Import the execution context for message scheduling
  import context._
  /**
    * This flag is used to make sure the VM Allocation Policy is only
    * working on one allocation request at a time
    */


  private var isProcessing : Boolean = false

  private var requestId : Long = _
  private var vmPayloads : List[VMPayload] = _
  private var hostResources : Map[String, HostResource] = _

  private val hostResponseWaitTime = 5 // seconds


  override def receive: Receive = {

    /**
      * The DataCenter asks the allocation policy actor to decide the VM to
      * Host mapping for the Vm creation request
      */
    case (requestVmAllocation : RequestVmAllocation) => {
      log.info("DataCenterActor::VmAllocationPolicyActor:RequestVmAllocation")

      if(isProcessing) {
        /**
          * Already processing a request.
          * Schedule incoming message to self after processing duration.
          */
        context.system.scheduler.scheduleOnce(
          new FiniteDuration(hostResponseWaitTime, TimeUnit.SECONDS), self, requestVmAllocation)
      }

      isProcessing = true

      log.info("Starting processing. Won't handle incoming requests" +
        " till processing is complete")

      this.vmPayloads = requestVmAllocation.vmPayloads
      this.requestId = requestVmAllocation.id
      hostResources = Map()

      // Send message to each host in host list of DataCenter to send resource status
      requestVmAllocation.hostList.map(host => context.actorSelection(host))
        .foreach(hostActor => {
          hostActor ! RequestHostResourceStatus(requestId)
        })

      /**
        * Schedule a message to self after a duration of $hostResponseWaitTime seconds
        * for all hosts to respond with resource status. The scheduled message triggers
        * allocation strategy
        */
      context.system.scheduler.scheduleOnce(
        new FiniteDuration(hostResponseWaitTime, TimeUnit.SECONDS), self, StartAllocation())
    }

    /**
      * Receive the Resource status from a host
      */
    case ReceiveHostResourceStatus(id, hostResource : HostResource) => {
      log.info("HostActor::VmAllocationPolicyActor:RequestVmAllocation")

      /**
        * If the host is responding to the same request as being processed
        * update the Host Resource Map with the sent information
        */
      if(id == requestId){
        hostResources += (sender().path.toStringWithoutAddress -> hostResource)
      }
    }

    /**
      * Start the +Vm allocation strategy
      * The injected Vm Allocation Policy instance is used to do this
      */
    case StartAllocation => {

      log.info("VmAllocationPolicy::VmAllocationPolicy:StartAllocation")

      val vmAllocationResult = vmAllocationPolicy.allocateVMs(vmPayloads, hostResources)

      context.parent ! ReceiveVmAllocation(requestId, vmAllocationResult)

    }

    case StopProcessing =>

      log.info("DataCenterActor::VmAllocationPolicy:StopProcessing")
      isProcessing = false
  }
}


case class RequestVmAllocation(id : Long, vmPayloads : List[VMPayload], hostList : List[String])

case class ReceiveHostResourceStatus(requestId : Long, hostResource: HostResource)

final case class StartAllocation()

case class VmAllocationResult(vmHostMap : Map[VMPayload, String], failedAllocationVms : List[VMPayload])

case class StopProcessing()