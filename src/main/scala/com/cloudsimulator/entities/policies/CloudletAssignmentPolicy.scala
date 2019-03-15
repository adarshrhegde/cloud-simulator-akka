package com.cloudsimulator.entities.policies

import com.cloudsimulator.entities.payload.CloudletPayload

/**
  * The actual policies will implement this trait.
  *
  */
trait CloudletAssignmentPolicy {

  //TODO also add host list as parameter
  def cloudletAssignment(cloudlets:List[CloudletPayload])
}
