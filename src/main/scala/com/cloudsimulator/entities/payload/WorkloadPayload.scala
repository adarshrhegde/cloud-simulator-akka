package com.cloudsimulator.entities.payload

import com.cloudsimulator.entities.policies.UtilizationModel


case class WorkloadPayload(workloadId : Long, numberOfPes : Int, execStartTime : Double, workloadLength : Long,
                           utilizationModelRam : UtilizationModel, utilizationModelCPU: UtilizationModel,
                           utilizationModelBw : UtilizationModel) extends Payload