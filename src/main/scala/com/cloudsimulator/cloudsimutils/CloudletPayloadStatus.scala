package com.cloudsimulator.cloudsimutils

/**
  * CloudletPayload status
  * Maintains the status of the cloudletPayload
  */
object CloudletPayloadStatus {

  val NOT_INITIATED: Int = -1
  val SENT: Int = 0
  val ASSIGNED_TO_VM: Int = 1
  val RUNNING: Int = 2
  val PAUSED: Int = 3
  val COMPLETED: Int = 4

}
