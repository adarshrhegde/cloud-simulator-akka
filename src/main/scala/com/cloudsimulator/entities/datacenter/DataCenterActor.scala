package com.cloudsimulator.entities.datacenter

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.cloudsimutils.VMPayloadStatus
import com.cloudsimulator.entities.DcRegistration
import com.cloudsimulator.entities.host.{AllocateVm, CanAllocateVm, HostActor}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.policies._
import com.cloudsimulator.entities.vm.VmActor
import com.cloudsimulator.utils.ActorUtility

import scala.collection.mutable.ListBuffer

/**
  * DataCenter Actor
  * @param id
  * @param hostList
  * @param vmList
  * @param location
  * @param vmToHostMap
  */
class DataCenterActor(id: Long,
                      location: String, rootSwitchId : String)
  extends Actor with ActorLogging {

  private val vmPayloadTrackerList : ListBuffer[VMPayloadTracker] = ListBuffer()

  private val hostList: ListBuffer[String] = ListBuffer()

  private val vmList: ListBuffer[VmActor] = ListBuffer()

  private val vmToHostMap: Map[Long, Long] = Map()

  override def preStart(): Unit = {

    // Register self with CIS actor on startup
    self ! RegisterWithCIS
  }

  override def receive: Receive = {

    case CreateHost(hostId, hostProps : Props) => {
      log.info("SimulationActor::DataCenterActor:CreateHost")
      log.info(s"Creating host $hostId within DataCenter $id")
      val hostActor = context.actorOf(hostProps, s"host-$hostId")
      hostList += hostActor.path.toStringWithoutAddress
    }

    case CreateVmAllocationPolicy(vmAllocationPolicy) => {
      log.info("SimulationActor::DataCenterActor:CreateVmAllocationPolicy")
      context.actorOf(Props(new VmAllocationPolicyActor(vmAllocationPolicy)), "vm-allocation-policy")
    }

    /*case CisAvailable =>{
      log.info("DataCenterActor::DataCenterActor:preStart()")
      context.actorSelection(ActorUtility.getActorRef(ActorUtility.simulationActor)) ! CisAvailable
    }*/

    /**
      * Register with CIS
      */
    case RegisterWithCIS => {
      log.info("DataCenterActor::DataCenterActor:RegisterWithCIS")
      //context.actorSelection(ActorUtility.getActorRef("CIS")) ! DcRegistration(id)

      context.actorSelection(ActorUtility.getActorRef(rootSwitchId)) ! DcRegistration(id)
    }

    /**
      * Request to create VMs sent by LoadBalancer
      */
    case RequestCreateVms(networkPacketProperties, id, vmPayloads : List[VMPayload]) => {


      log.info(s"RootSwitchActor::DataCenterActor:RequestCreateVms:$id")
      log.info(s"Request to allocate Vms sent from ${networkPacketProperties.sender}")

      val vmAllocationPolicyActor = context.child("vm-allocation-policy").get

      vmAllocationPolicyActor ! RequestVmAllocation(id, vmPayloads, hostList.toList)

      // old logic to send each payload to each host
      // to be deleted shortly
      /*vmPayloads.foreach(vmPayload => {

        val payloadTracker = new VMPayloadTracker(id, vmPayload, VMPayloadStatus.NOT_ALLOCATED)
        vmPayloadTrackerList += payloadTracker

        log.debug("Received request to create VM " + vmPayload.toString())


        context.actorSelection("*").forward(CanAllocateVm(payloadTracker))
      })*/

    }



    /**
      * Receive Vm Allocation decision from Vm Allocation Policy Actor
      */
    case receiveVmAllocation: ReceiveVmAllocation => {

      log.info("VmAllocationPolicyActor::DataCenterActor:ReceiveVmAllocation")

      receiveVmAllocation.vmAllocationResult.vmHostMap.foreach(vmHost => {

        // Ask host to allocate vm
        context.actorSelection(vmHost._2) ! AllocateVm(vmHost._1)
      })

      // Ask Vm Allocation Policy Actor to change status and accept new requests
      sender() ! StopProcessing

      // handle failed vms

    }

      /*case CanAllocateVmTrue(vmPayloadTracker) => {
      log.info(s"HostActor::DataCenterActor:CanAllocateVmTrue:$vmPayloadTracker")

      vmPayloadTrackerList.filter(tracker => tracker.requestId == vmPayloadTracker.requestId
        && tracker.vmPayload.payloadId == vmPayloadTracker.vmPayload.payloadId
        && tracker.payloadStatus == VMPayloadStatus.NOT_ALLOCATED).foreach(tracker => {

        if(tracker.payloadStatus == VMPayloadStatus.NOT_ALLOCATED)

          log.info(s"Sending allocation message to Host ")
          sender() ! AllocateVm(vmPayloadTracker)

      })

    }
    case VmAllocationSuccess(vmPayloadTracker) => {

      log.info(s"HostActor::DataCenterActor:VmAllocationSuccess:$vmPayloadTracker")

      vmPayloadTrackerList.filter(tracker => tracker.requestId == vmPayloadTracker.requestId
      && tracker.vmPayload.payloadId == vmPayloadTracker.vmPayload.payloadId
      && tracker.payloadStatus == VMPayloadStatus.NOT_ALLOCATED).remove(0)

      vmPayloadTrackerList += new VMPayloadTracker(vmPayloadTracker.requestId, vmPayloadTracker.vmPayload,
        VMPayloadStatus.ALLOCATED)

    }*/

    /*
    case CisUp =>{
      log.info("DataCenterActor::DataCenterActor:CisUp")
      self ! RegisterWithCIS
    }*/
    case _ => println(s"DataCenter Actor created $id")
  }

}

final case class RegisterWithCIS()

case class RequestCreateVms(override val networkPacketProperties: NetworkPacketProperties,
                            requestId : Long, vmPayloads: List[VMPayload]) extends NetworkPacket

case class CanAllocateVmTrue(vmPayloadTracker : VMPayloadTracker)

case class VmAllocationSuccess(vmPayload : VMPayload)

case class VMPayloadTracker(requestId : Long, vmPayload: VMPayload,
                            payloadStatus : VMPayloadStatus.Value)

case class CreateHost(hostId : Long, props : Props)

case class CreateVmAllocationPolicy(vmAllocationPolicy: VmAllocationPolicy) extends NetworkPacket

case class ReceiveVmAllocation(requestId : Long, vmAllocationResult: VmAllocationResult) extends NetworkPacket