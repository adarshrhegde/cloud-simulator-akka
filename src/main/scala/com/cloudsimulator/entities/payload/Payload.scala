package com.cloudsimulator.entities.payload

import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus.CloudletPayloadStatus
import com.cloudsimulator.entities.policies.UtilizationModel

trait Payload {
  val payloadId: Long
  val userId: Long
}

//TODO keep track of DC & host
case class CloudletPayload(payloadId: Long, userId: Long, cloudletId: Long, vmId: Long, numberOfPes: Int, execStartTime: Double, workloadLength: Long, var status: CloudletPayloadStatus,
                           utilizationModelRam: UtilizationModel, utilizationModelCPU: UtilizationModel,
                           utilizationModelBw: UtilizationModel) extends Payload

case class VMPayload(payloadId: Long, userId: Long, numberOfPes: Int,
                     bw: Double, mips: Long, ram: Long, storage: Long, guestOS: String) extends Payload

// TODO : CloudletScheduler, Host, inMigration in VM Actor