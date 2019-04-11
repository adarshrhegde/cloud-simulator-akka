package com.cloudsimulator.entities.policies.vmscheduler

import com.cloudsimulator.entities.host.HostResource

/**
  * The type VmScheduler
  * Instance of VmScheduler is injected to a HostActor
  */
trait VmScheduler {

  def scheduleVms(slice : Long, vmRequirementList: Seq[VmRequirement], hostResource: HostResource) : Seq[SliceAssignment]

}
