package com.cloudsimulator.entities.policies.vmscheduler

import akka.actor.ActorRef

class TimeSharedVmScheduler extends VmScheduler {


  override def scheduleVms(slice : Long, vmRequirementList: Seq[VmRequirement]): Seq[SliceAssignment] = {

    val vmSliceLength : Long = slice / vmRequirementList.size

    vmRequirementList.map(vmReq => {

      new SliceAssignment(vmSliceLength, vmReq.ref)
    })

  }

}

case class SliceAssignment(sliceLength : Long, vmRef : ActorRef)
