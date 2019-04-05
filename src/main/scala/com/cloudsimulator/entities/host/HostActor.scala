package com.cloudsimulator.entities.host

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus
import com.cloudsimulator.entities.datacenter.{CanAllocateVmTrue, HostCheckedForRequiredVms, VMPayloadTracker, VmAllocationSuccess}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.{CloudletPayload, VMPayload}
import com.cloudsimulator.entities.switch.RegisterHost
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceInfo}
import com.cloudsimulator.entities.vm.{ScheduleCloudlet, VmActor}

import scala.collection.mutable.ListBuffer


/**
  * Host Actor
  *
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
class HostActor(id: Long, dataCenterId: Long, hypervisor: String, bwProvisioner: String,
                ramProvisioner: String, vmScheduler: String, var availableNoOfPes: Int, nicCapacity: Double,
                var availableRam: Long, var availableStorage: Long, var availableBw: Double, edgeSwitchName: String)
  extends Actor with ActorLogging {

  private val vmIdToRefMap: Map[Long, String] = Map()

  val mapSliceIdToVmCountRem: Map[Long, Long] = Map()

  override def preStart(): Unit = {

    // Register self with Edge switch
    self ! RegisterWithSwitch
  }

  override def receive: Receive = {

    /**
      * Request from DataCenter asking host if Vm can be allocated
      */
    case CanAllocateVm(vmPayloadTracker: VMPayloadTracker) => {
      log.info(s"LoadBalancerActor::DataCenterActor:CanAllocateVm:$id")

      if (vmPayloadTracker.vmPayload.numberOfPes < availableNoOfPes &&
        vmPayloadTracker.vmPayload.ram < availableRam
        && vmPayloadTracker.vmPayload.storage < availableStorage &&
        vmPayloadTracker.vmPayload.bw < availableBw) {

        sender() ! CanAllocateVmTrue(vmPayloadTracker)
      }

    }

    case allocateVm: AllocateVm => {

      log.info(s"LoadBalancerActor::DataCenterActor:AllocateVm:$id")

      // update host resources with vm payload
      availableNoOfPes -= allocateVm.vmPayload.numberOfPes
      availableRam -= allocateVm.vmPayload.ram
      availableStorage -= allocateVm.vmPayload.storage
      availableBw -= allocateVm.vmPayload.bw

      // Create VM Actor
      val vmActor = context.actorOf(Props(new VmActor(allocateVm.vmPayload.payloadId,
        allocateVm.vmPayload.userId, allocateVm.vmPayload.mips,
        allocateVm.vmPayload.numberOfPes, allocateVm.vmPayload.ram,
        allocateVm.vmPayload.bw)))

      //add to vmList for the host
      vmIdToRefMap + (allocateVm.vmPayload.payloadId -> vmActor.path.toStringWithoutAddress)

      // send the allocation success in reverse direction
      val networkPacketProperties = new NetworkPacketProperties(
        allocateVm.networkPacketProperties.receiver, allocateVm.networkPacketProperties.sender)

      sender() ! VmAllocationSuccess(networkPacketProperties, allocateVm.vmPayload)
    }

    case RegisterWithSwitch => {
      log.info("HostActor::HostActor:RegisterWithSwitch")
      context.actorSelection(s"../$edgeSwitchName") ! RegisterHost(self.path.toStringWithoutAddress)
    }

    /**
      * Checks if the host is running a VM which is required by the cloudlet.
      * If it is then tag the cloudlet as processing and send to the VM.
      */
    case CheckHostforRequiredVMs(reqId, cloudletPayloads, cloudletVMList) => {

      //if vm required by cloudlet is present, then send the msg and update cloudlet's status
      val newCloudlets: List[CloudletPayload] = cloudletPayloads
        .map(cloudlet => {
          if (cloudlet.status.equals(CloudletPayloadStatus.SENT) &&
            vmIdToRefMap.contains(cloudlet.vmId)) {

            //TODO check the resources for cloudlet and VM are compatible

            cloudlet.status = CloudletPayloadStatus.ASSIGNED_TO_VM
            cloudlet.hostId = id
            cloudlet.dcId = dataCenterId
            context.actorSelection(vmIdToRefMap(cloudlet.vmId)) ! ScheduleCloudlet(reqId, cloudlet)
          }
          cloudlet
        })
      //send response(new cloudlets) back to the DC
      context.parent ! HostCheckedForRequiredVms(reqId, cloudletPayloads)
    }

    case SendTimeSliceInfo(sliceInfo: TimeSliceInfo) => {
      log.info("DataCenterActor::HostActor:SendTimeSliceInfo")
      mapSliceIdToVmCountRem + (sliceInfo.sliceId -> vmIdToRefMap.size)
    }
  }
}

case class CanAllocateVm(vmPayloadTracker: VMPayloadTracker)

case class AllocateVm(override val networkPacketProperties: NetworkPacketProperties, vmPayload: VMPayload) extends NetworkPacket

case class RequestHostResourceStatus(requestId: Long) extends NetworkPacket

case class HostResource(var availableNoOfPes: Int, var availableRam: Long,
                        var availableStorage: Long, var availableBw: Double) extends NetworkPacket

case class RegisterWithSwitch()

case class CheckHostforRequiredVMs(id: Long, cloudletPayloads: List[CloudletPayload], vmList: List[Long])