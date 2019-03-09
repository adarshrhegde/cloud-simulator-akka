package com.cloudsimulator.entities.payload

import com.cloudsimulator.entities.policies.UtilizationModel

trait Payload {

  val payloadId : Long
  val userId : Long
}

case class CloudletPayload(payloadId : Long, userId : Long, cloudletId : Long, numberOfPes : Int, execStartTime : Double, workloadLength : Long,
                           utilizationModelRam : UtilizationModel, utilizationModelCPU: UtilizationModel,
                           utilizationModelBw : UtilizationModel) extends Payload

case class VMPayload(payloadId : Long, userId : Long, numberOfPes : Int,
                     bw : Double, mips : Long, ram : Long, storage : Long, guestOS : String) extends Payload

// TODO : CloudletScheduler, Host, inMigration in VM Actor