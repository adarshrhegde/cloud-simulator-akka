package com.cloudsimulator.cloudsimutils

/**
  * PayloadStatus enum
  * Maintains the status of a vm payload at datacenter
  */
object VMPayloadStatus extends Enumeration {

  var NOT_ALLOCATED, ALLOCATED = Value

  def apply(payloadStatusString: String): VMPayloadStatus.Value =
    VMPayloadStatus.withName(payloadStatusString)
}
