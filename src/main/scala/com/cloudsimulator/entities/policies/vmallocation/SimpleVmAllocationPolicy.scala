package com.cloudsimulator.entities.policies.vmallocation

import com.cloudsimulator.entities.host.HostResource
import com.cloudsimulator.entities.payload.VMPayload

import scala.collection.mutable.ListBuffer

/**
  * This is a simple implementation if the VM Allocation Policy
  */
class SimpleVmAllocationPolicy extends VmAllocationPolicy {
  override def allocateVMs(vmPayloads: List[VMPayload],
                           hostResources: Map[String, HostResource]): VmAllocationResult = {

    var vmHostMap : Map[VMPayload, String] = Map()
    var allocationFailedVms : ListBuffer[VMPayload] = ListBuffer()

    // Obtain Vm to Host Mapping
    vmPayloads.foreach(vmPayload => {
      var isAllocated : Boolean = false

      hostResources.foreach( hostStatus => {
        val hostName = hostStatus._1
        val hostResource: HostResource = hostStatus._2


        // Check if host can handle vm resources
        if(vmPayload.numberOfPes < hostResource.availableNoOfPes &&
          vmPayload.ram < hostResource.availableRam
          && vmPayload.storage < hostResource.availableStorage &&
          vmPayload.bw < hostResource.availableBw){

          // reduce host resource status if vm can be allocated
          hostResource.availableNoOfPes -= vmPayload.numberOfPes
          hostResource.availableRam -= vmPayload.ram
          hostResource.availableStorage -= vmPayload.storage
          hostResource.availableBw -= vmPayload.bw

          // add to vm-host mapping
          vmHostMap += (vmPayload -> hostName)
          isAllocated = true

        }

      })

      // if no host can accomodate Vm add to failed allocation list
      if(!isAllocated)
        allocationFailedVms += vmPayload

    })

    VmAllocationResult(vmHostMap, allocationFailedVms.toList)
  }
}
