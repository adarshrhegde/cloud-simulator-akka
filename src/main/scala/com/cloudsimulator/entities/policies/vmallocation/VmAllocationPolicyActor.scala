package com.cloudsimulator.entities.policies.vmallocation

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.ReceiveVmAllocation
import com.cloudsimulator.entities.host.{HostResource, RequestHostResourceStatus}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
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

  private var hostCount : Int = _

  private val waitTime = 5 // seconds


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
          new FiniteDuration(waitTime, TimeUnit.SECONDS), self, requestVmAllocation)
      }

      isProcessing = true

      log.info("Starting processing. Won't handle incoming requests" +
        " till processing is complete")

      this.vmPayloads = requestVmAllocation.vmPayloads
      this.requestId = requestVmAllocation.id

      // Initialize host resources before vm allocation starts
      hostResources = Map()

      // Host count at the dataCenter. This will be used to count responses from hosts
      hostCount = requestVmAllocation.hostList.size

      // Send message to each host in host list of DataCenter to send resource status
      requestVmAllocation.hostList.foreach(hostName => {

        val networkPacketProperties = new NetworkPacketProperties(self.path.toStringWithoutAddress,
          hostName)

        requestVmAllocation.downstreamSwitches.map(sw => context.actorSelection(sw))
        .foreach(switchActor => {

          switchActor ! RequestHostResourceStatus(networkPacketProperties, requestId)

        })

        })

      self ! StartAllocation
    }

    /**
      * Receive the Resource status from a host
      */
    case receiveHostResourceStatus: ReceiveHostResourceStatus => {
      log.info("HostActor::VmAllocationPolicyActor:ReceiveHostResourceStatus")

      /**
        * If the host is responding to the same request as being processed
        * update the Host Resource Map with the sent information
        */
      if(receiveHostResourceStatus.requestId == requestId){
        hostResources += (receiveHostResourceStatus.networkPacketProperties.sender -> receiveHostResourceStatus.hostResource)

        if(hostResources.size == hostCount)
          self ! StartAllocation
      }
    }

    /**
      * Start the +Vm allocation strategy
      * The injected Vm Allocation Policy instance is used to do this
      */
    case StartAllocation => {

      if(hostResources.size == hostCount){
        log.info("VmAllocationPolicy::VmAllocationPolicy:StartAllocation")

        val vmAllocationResult = vmAllocationPolicy.allocateVMs(vmPayloads, hostResources)

        log.info(s"VM allocation result -> ${vmAllocationResult.vmHostMap}" )
        context.parent ! ReceiveVmAllocation(requestId, vmAllocationResult)

      } else {

        log.info(s"Waiting for resource status responses from all hosts for DataCenter ${parent.path.name}")

      }
    }

    case StopProcessing =>

      log.info("DataCenterActor::VmAllocationPolicy:StopProcessing")
      isProcessing = false
  }
}


case class RequestVmAllocation(id : Long, vmPayloads : List[VMPayload], hostList : List[String], downstreamSwitches : Seq[String]) extends NetworkPacket

case class ReceiveHostResourceStatus(override val networkPacketProperties: NetworkPacketProperties, requestId : Long, hostResource: HostResource) extends NetworkPacket

final case class StartAllocation()

case class VmAllocationResult(vmHostMap : Map[VMPayload, String], failedAllocationVms : List[VMPayload])

case class StopProcessing()