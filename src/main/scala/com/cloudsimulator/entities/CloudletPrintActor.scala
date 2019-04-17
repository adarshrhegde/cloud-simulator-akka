package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.payload.cloudlet.CloudletExecution

class CloudletPrintActor extends Actor with ActorLogging {

  override def receive: Receive = {

    case PrintCloudletsExectionStatus(cloudletsExecution) => {
      log.info(s"VmActor::CloudletPrintActor:PrintCloudletsExectionStatus")
      cloudletsExecution.foreach(cloudlet => {
        log.info(s"${cloudlet.id} \t ${cloudlet.status} \t ${cloudlet.dcId} \t ${cloudlet.hostId} \t ${cloudlet.remWorkloadLength} \t ${cloudlet.timeSliceUsageInfo}")
      })
    }

    case _ => {

    }
  }
}

case class PrintCloudletsExectionStatus(cloudletsExecution: Seq[CloudletExecution])
