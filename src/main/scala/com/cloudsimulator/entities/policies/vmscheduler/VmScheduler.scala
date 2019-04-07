package com.cloudsimulator.entities.policies.vmscheduler

trait VmScheduler {

  def scheduleVms(slice : Long, vmRequirementList: Seq[VmRequirement]) : Seq[SliceAssignment]

}
