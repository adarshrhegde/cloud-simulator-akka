package com.cloudsimulator.entities.payload.cloudlet

import com.cloudsimulator.entities.payload.Payload


//Pureconfig requires case classes
case class CloudletPayload(payloadId: Long, userId: Long,
                           vmId: Long,
                           var delay: Long,
                           var dcId: Long,
                           var hostId: Long,
                           numberOfPes: Int,
                           ram: Int,
                           storage: Int,
                           workloadLength: Long,
                           var status: Long,
                           var cost: Double) extends Payload

//Companion object used for creation of a new CloudletPayload from the old one with new values of host-id and dc-id
object CloudletPayload {
  def apply(oldCloudletPayload: CloudletPayload, dcId: Long, hostId: Long):CloudletPayload = CloudletPayload(
    oldCloudletPayload.payloadId,
    oldCloudletPayload.userId,
    oldCloudletPayload.vmId,
    oldCloudletPayload.delay,
    dcId, hostId, oldCloudletPayload.numberOfPes,
    oldCloudletPayload.ram,
    oldCloudletPayload.storage,
    oldCloudletPayload.workloadLength, oldCloudletPayload.status, oldCloudletPayload.cost)
}