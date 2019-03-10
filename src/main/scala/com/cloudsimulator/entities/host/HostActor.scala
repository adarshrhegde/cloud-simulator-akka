package com.cloudsimulator.entities.host

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.{CanAllocateVmTrue, VMPayloadTracker, VmAllocationSuccess}
import com.cloudsimulator.entities.vm.VmActor


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
class HostActor(id : Long, dataCenterId : Long, hypervisor : String, vmList : List[VmActor], bwProvisioner : String,
                ramProvisioner : String, vmScheduler : String, availableNoOfPes : Int, nicCapacity: Double,
                availableRam : Long, availableStorage : Long, availableBw : Double)
  extends Actor with ActorLogging {

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

    case AllocateVm(vmPayloadTracker : VMPayloadTracker) => {
      log.info(s"LoadBalancerActor::DataCenterActor:AllocateVm:$id")

      // logic for allocation comes here
      sender() ! VmAllocationSuccess(vmPayloadTracker)
    }
  }
}

case class CanAllocateVm(vmPayloadTracker : VMPayloadTracker)

case class AllocateVm(vmPayloadTracker : VMPayloadTracker)