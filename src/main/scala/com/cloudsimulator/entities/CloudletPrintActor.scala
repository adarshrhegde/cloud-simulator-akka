package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.payload.cloudlet.CloudletExecution

class CloudletPrintActor extends Actor with ActorLogging {

  override def receive: Receive = {

    case PrintCloudletsExectionStatus(cloudletsExecution) => {
      log.info(s"VmActor::CloudletPrintActor:PrintCloudletsExectionStatus")
      cloudletsExecution.foreach(cloudlet => {
        log.info(s"Cloudlet-id \t Status \t DC-id \t Host-id \t Remaining Workload length \t Time Slice Info \t Delay(s) \t Cost(USD)")
        log.info(s"${cloudlet.id} \t ${cloudlet.status} \t ${cloudlet.dcId} \t ${cloudlet.hostId} \t ${cloudlet.remWorkloadLength} \t ${cloudlet.timeSliceUsageInfo} \t ${cloudlet.delay} \t ${cloudlet.cost}")
      })
    }

    case _ => {

    }
  }
}

case class PrintCloudletsExectionStatus(cloudletsExecution: Seq[CloudletExecution])
