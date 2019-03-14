package com.cloudsimulator.cloudsimutils

/**
  * Status of the request received by loadbalancer
  */
object RequestStatus extends Enumeration {

  val IN_PROGRESS, COMPLETED = Value

  def apply(statusString : String): Value = {

    RequestStatus.withName(statusString)
  }

}
