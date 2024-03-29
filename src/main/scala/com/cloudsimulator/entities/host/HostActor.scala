package com.cloudsimulator.entities.host

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSelection, Props}
import com.cloudsimulator.{ SendCreationConfirmation}
import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus
import com.cloudsimulator.entities.cost.Cost
import com.cloudsimulator.entities.payload.cloudlet.CloudletPayload
import com.cloudsimulator.entities.datacenter.{CanAllocateVmTrue, HostCheckedForRequiredVms, VMPayloadTracker, VmAllocationSuccess}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.policies.vmallocation.ReceiveHostResourceStatus
import com.cloudsimulator.entities.policies.vmscheduler.{ScheduleVms, VmScheduler, VmSchedulerActor}
import com.cloudsimulator.entities.switch.RegisterHost
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceCompleted, TimeSliceInfo}
import com.cloudsimulator.entities.vm.{ScheduleCloudlet, VmActor}
import com.cloudsimulator.utils.{ActorType, ActorUtility}

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
                ramProvisioner : String, vmScheduler : VmScheduler, var availableNoOfPes : Int, mips: Long,
                var availableRam : Long, var availableStorage : Long, var availableBw : Double, edgeSwitchName : String, cost : Cost)
  extends Actor with ActorLogging {

  private var vmIdToRefMap: Map[Long, String] = Map()

  private var vmRefList : Seq[ActorRef] = Seq()

  override def preStart(): Unit = {

    self ! SendCreationConfirmation(ActorType("HOST"))

    // Register self with Edge switch
    self ! CreateVmScheduler

  }

  override def receive: Receive = {

    case sendCreationConfirmation: SendCreationConfirmation => {
      log.info("HostActor::HostActor:SendCreationConfirmation")

      context.actorSelection(ActorUtility.getSimulatorRefString) ! sendCreationConfirmation
    }

    case CreateVmScheduler => {
      log.info(s"HostActor::HostActor:CreateVmScheduler:$id")

      context.actorOf(Props(new VmSchedulerActor(vmScheduler)),
        ActorUtility.vmScheduler)

    }

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

      log.info(s"DataCenterActor::HostActor:AllocateVm:$id")

      // update host resources with vm payload
      availableNoOfPes -= allocateVm.vmPayload.numberOfPes
      availableRam -= allocateVm.vmPayload.ram
      availableStorage -= allocateVm.vmPayload.storage
      availableBw -= allocateVm.vmPayload.bw

      // TODO - add vms to config and add vm id to vm actor

      // Create VM Actor
      val vmActor = context.actorOf(Props(new VmActor(allocateVm.vmPayload.payloadId,
        allocateVm.vmPayload.userId, allocateVm.vmPayload.mips,
        allocateVm.vmPayload.numberOfPes, allocateVm.vmPayload.ram,
        allocateVm.vmPayload.bw,cost)), s"vm-${allocateVm.vmPayload.payloadId}")

      vmRefList = vmRefList :+ vmActor

      //add to vmList for the host
      vmIdToRefMap = vmIdToRefMap + (allocateVm.vmPayload.payloadId -> vmActor.path.toStringWithoutAddress)

      // send the allocation success in reverse direction
      val networkPacketProperties = new NetworkPacketProperties(
        allocateVm.networkPacketProperties.receiver, allocateVm.networkPacketProperties.sender)

      log.info(s"DataCenterActor:AllocateVm: vm-${allocateVm.vmPayload.payloadId} allocated on host-$id")
      sender() ! VmAllocationSuccess(networkPacketProperties, allocateVm.vmPayload)
    }


    /**
      * Sender: DataCenterActor
      * Checks if the host is running a VM which is required by the cloudlet.
      * If it is then tag the cloudlet as processing and send to the VM.
      */
    case checkHostForRequiredVMs: CheckHostForRequiredVMs => {
      log.info(s"EdgeSwitchActor::HostActor:CheckHostForRequiredVMs")
      log.info(s"CheckHostForRequiredVMs:$vmIdToRefMap")
      log.debug(s"CheckHostForRequiredVMs:CloudletPayload:$checkHostForRequiredVMs.cloudletPayloads")
      //if vm required by cloudlet is present, then send the msg and update cloudlet's status
      val cloudlets: List[CloudletPayload] = checkHostForRequiredVMs.cloudletPayloads
        .map(cloudlet => {
          if (vmIdToRefMap.contains(cloudlet.vmId)) {
            cloudlet.hostId = id
            cloudlet.dcId = dataCenterId
            context.actorSelection(vmIdToRefMap(cloudlet.vmId)) ! ScheduleCloudlet(checkHostForRequiredVMs.id, CloudletPayload(cloudlet,dataCenterId,id))
          }
          cloudlet
        }).filter(cloudlet=>{vmIdToRefMap.contains(cloudlet.vmId)})

      //send response(new cloudlets) back to the DC
      val networkPacketProperties = checkHostForRequiredVMs.networkPacketProperties.swapSenderReceiver()

      sender() ! HostCheckedForRequiredVms(networkPacketProperties,
        checkHostForRequiredVMs.id, cloudlets)
    }

    /**
      * Sender : DataCenter
      */
    case sendTimeSliceInfo : SendTimeSliceInfo => {
      log.info("DataCenterActor::HostActor:SendTimeSliceInfo")


      if(vmRefList.size > 0) {
        context.child(ActorUtility.vmScheduler).get ! ScheduleVms(sendTimeSliceInfo.sliceInfo, vmRefList, HostResource(availableNoOfPes,
          availableRam, availableStorage, availableBw))

      } else {
        // no VMs in the host. Send completion message back
        sender() ! TimeSliceCompleted(sendTimeSliceInfo.sliceInfo,false)
      }

    }

    case requestHostResourceStatus: RequestHostResourceStatus => {
      log.info("EdgeSwitchActor::HostActor:RequestHostResourceStatus")

      val networkPacketProperties = new NetworkPacketProperties(self.path.toStringWithoutAddress,
        requestHostResourceStatus.networkPacketProperties.sender)

      sender() ! ReceiveHostResourceStatus(networkPacketProperties, requestHostResourceStatus.requestId,
        new HostResource(availableNoOfPes, availableRam,
      availableStorage, availableBw))

      /*context.child("vm-scheduler").foreach(vms =>{
        vms ! ScheduleVms(sliceInfo, vmPathList)
      })*/

    }

    /**
      * Sender: VmScheduler
      * Once the assigned time slice for VmScheduler is exhausted, it informs the datacenter
      * to provide next time slice when available.
      * Data center accumulates from all hosts and then informs the TimeActor.
      */
    case timeSliceCompleted: TimeSliceCompleted =>{
      log.info("VmSchedulerActor::HostActor:TimeSliceCompleted")
      context.parent ! TimeSliceCompleted(timeSliceCompleted.timeSliceInfo,timeSliceCompleted.continueSimulation)
    }
  }
}

case class CanAllocateVm(vmPayloadTracker : VMPayloadTracker)

case class AllocateVm(override val networkPacketProperties: NetworkPacketProperties, vmPayload : VMPayload) extends NetworkPacket

case class RequestHostResourceStatus(override val networkPacketProperties: NetworkPacketProperties, requestId : Long)  extends NetworkPacket

case class HostResource(var availableNoOfPes : Int, var availableRam : Long,
                        var availableStorage : Long, var availableBw : Double)  extends NetworkPacket

case class CheckHostForRequiredVMs(override val networkPacketProperties: NetworkPacketProperties, id: Long, cloudletPayloads: List[CloudletPayload]) extends NetworkPacket

case class CreateVmScheduler()