package com.cloudsimulator.entities.policies.vmscheduler

import com.cloudsimulator.entities.host.HostResource

/**
  * SpaceSharedVmScheduler
  * The Pes at the Host are shared by all Vms
  * All Vms get same amount of time
  */
class SpaceSharedVmScheduler extends VmScheduler {
  override def scheduleVms(slice: Long, vmRequirementList: Seq[VmRequirement],
                           hostResource: HostResource): Seq[SliceAssignment] = {

    var sliceAssignmentList : Seq[SliceAssignment] = Seq()

    vmRequirementList.foreach(vmRequirement => {

      if(hostResource.availableNoOfPes > vmRequirement.noOfPes){

        // Update the number of Pes at the host
        hostResource.availableNoOfPes -= vmRequirement.noOfPes
        sliceAssignmentList = sliceAssignmentList :+ SliceAssignment(slice, vmRequirement.ref)
      }

    })

    sliceAssignmentList
  }
}
