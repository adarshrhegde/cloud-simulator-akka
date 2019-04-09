package com.cloudsimulator.entities.payload

import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus.CloudletPayloadStatus

trait Payload {
  val payloadId: Long
  val userId: Long
}

//TODO keep track of DC & host
case class CloudletPayload(payloadId: Long, userId: Long, /*cloudletId: Long,*/
                           var dcId: Long, var hostId: Long, vmId: Long,
                           numberOfPes: Int, var execStartTime: Double,
                           workloadLength: Long,
                           var status: CloudletPayloadStatus) extends Payload

case class VMPayload(payloadId: Long, userId: Long, numberOfPes: Int,
                     bw: Double, mips: Long, ram: Long, storage: Long, guestOs: String, cloudletScheduler: String) extends Payload

// TODO : CloudletScheduler, Host, inMigration in VM Actor