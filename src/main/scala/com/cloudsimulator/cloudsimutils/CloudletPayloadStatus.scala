package com.cloudsimulator.cloudsimutils

/**
  * Enum for CloudletPayload status
  * Maintains the status of the cloudletPayload
  */
object CloudletPayloadStatus extends Enumeration {
  type CloudletPayloadStatus = Value
  var NOT_INITIATED, SENT, ASSIGNED_TO_VM, RUNNING, PAUSED, COMPLETED = Value
  def apply(statusString: String): Value = {

    CloudletPayloadStatus.withName(statusString)
  }
}
