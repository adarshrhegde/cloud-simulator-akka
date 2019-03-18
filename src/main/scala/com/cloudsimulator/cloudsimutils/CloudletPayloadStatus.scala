package com.cloudsimulator.cloudsimutils

/**
  * Enum for CloudletPayload status
  * Maintains the status of the cloudletPayload
  */
object CloudletPayloadStatus extends Enumeration {
  type CloudletPayloadStatus = Value
  val NOT_INITIATED, SENT, ASSIGNED_TO_VM, RUNNING, PAUSED, COMPLETED = Value
}
