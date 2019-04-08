package com.cloudsimulator.entities.policies.vmallocation

import com.cloudsimulator.entities.host.HostResource
import com.cloudsimulator.entities.payload.VMPayload

/**
  * Vm Allocation Policy
  * This is the trait that declares the behavior used to allocate VMs to hosts
  * by a DataCenter. Each DataCenter will have an instance of this type
  */
trait VmAllocationPolicy {

  def allocateVMs(vmPayloads : List[VMPayload], hostResources : Map[String, HostResource]) : VmAllocationResult
}
