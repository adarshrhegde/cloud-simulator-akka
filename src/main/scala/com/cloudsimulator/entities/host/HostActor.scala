package com.cloudsimulator.entities.host

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.entities.datacenter.{CanAllocateVmTrue, VMPayloadTracker, VmAllocationSuccess}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.switch.RegisterHost
import com.cloudsimulator.entities.vm.VmActor

import scala.collection.mutable.ListBuffer


/**
  * Host Actor
  * @param id
  * @param dataCenterId
  * @param hypervisor
  * @param vmList
  * @param bwProvisioner
  * @param ramProvisioner
  * @param vmScheduler
  * @param availableNoOfPes
  * @param nicCapacity
  * @param availableRam
  * @param availableStorage
  * @param availableBw
  */
class HostActor(id : Long, dataCenterId : Long, hypervisor : String, bwProvisioner : String,
                ramProvisioner : String, vmScheduler : String,var availableNoOfPes : Int, nicCapacity: Double,
                var availableRam : Long, var availableStorage : Long, var availableBw : Double, edgeSwitchName : String)
  extends Actor with ActorLogging {

  private val vmList : ListBuffer[VmActor] = ListBuffer()

  override def preStart(): Unit = {

    // Register self with Edge switch
    self ! RegisterWithSwitch
  }

  override def receive: Receive = {

    /**
      * Request from DataCenter asking host if Vm can be allocated
      */
    case CanAllocateVm(vmPayloadTracker : VMPayloadTracker) => {
      log.info(s"LoadBalancerActor::DataCenterActor:CanAllocateVm:$id")

      if(vmPayloadTracker.vmPayload.numberOfPes < availableNoOfPes &&
        vmPayloadTracker.vmPayload.ram < availableRam
        && vmPayloadTracker.vmPayload.storage < availableStorage &&
        vmPayloadTracker.vmPayload.bw < availableBw){

        sender() ! CanAllocateVmTrue(vmPayloadTracker)
      }

    }

    case allocateVm : AllocateVm => {
      log.info(s"LoadBalancerActor::DataCenterActor:AllocateVm:$id")

      // update host resources with vm payload
      availableNoOfPes -= allocateVm.vmPayload.numberOfPes
      availableRam -= allocateVm.vmPayload.ram
      availableStorage -= allocateVm.vmPayload.storage
      availableBw -= allocateVm.vmPayload.bw

      // Create VM Actor
      context.actorOf(Props(new VmActor(allocateVm.vmPayload.payloadId,
        allocateVm.vmPayload.userId, allocateVm.vmPayload.mips,
        allocateVm.vmPayload.numberOfPes,allocateVm.vmPayload.ram,
        allocateVm.vmPayload.bw)))

      // send the allocation success in reverse direction
      val networkPacketProperties = new NetworkPacketProperties(
        allocateVm.networkPacketProperties.receiver, allocateVm.networkPacketProperties.sender)

      sender() ! VmAllocationSuccess(networkPacketProperties, allocateVm.vmPayload)
    }

    case RegisterWithSwitch => {
      log.info("HostActor::HostActor:RegisterWithSwitch")

      context.actorSelection(s"../$edgeSwitchName") ! RegisterHost(self.path.toStringWithoutAddress)

    }
  }
}

case class CanAllocateVm(vmPayloadTracker : VMPayloadTracker)

case class AllocateVm(override val networkPacketProperties: NetworkPacketProperties, vmPayload : VMPayload) extends NetworkPacket

case class RequestHostResourceStatus(requestId : Long)  extends NetworkPacket

case class HostResource(var availableNoOfPes : Int, var availableRam : Long,
                        var availableStorage : Long, var availableBw : Double)  extends NetworkPacket

case class RegisterWithSwitch()