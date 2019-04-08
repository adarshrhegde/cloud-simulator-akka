package com.cloudsimulator.entities.policies.vmscheduler

class SpaceSharedVmScheduler extends VmScheduler {
  override def scheduleVms(slice: Long, vmRequirementList: Seq[VmRequirement]): Seq[SliceAssignment] = ???
}
