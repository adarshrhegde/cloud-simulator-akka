package com.cloudsimulator.entities.policies.vmscheduler

import akka.actor.ActorRef
import com.cloudsimulator.entities.host.HostResource

/**
  * TimeSharedVmScheduler is an implementation on VmScheduler
  * Total available time is divided among all VMs
  */
class TimeSharedVmScheduler extends VmScheduler {


  override def scheduleVms(slice : Long, vmRequirementList: Seq[VmRequirement],hostResource: HostResource): Seq[SliceAssignment] = {

    val vmSliceLength : Long = slice / vmRequirementList.size

    vmRequirementList.map(vmReq => {

      new SliceAssignment(vmSliceLength, vmReq.ref)
    })

  }

}

case class SliceAssignment(sliceLength : Long, vmRef : ActorRef)
