package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.RequestCreateVms
import com.cloudsimulator.entities.{DcRegistration}
import com.cloudsimulator.utils.ActorUtility
import com.cloudsimulator.entities.network.NetworkPacket

class RootSwitchActor(dataCenterlist: List[String]) extends Actor with ActorLogging with Switch {

  override def receive: Receive = {

    case (dcRegistration: DcRegistration) => {
      log.info("DataCenterActor::RootSwitchActor:DcRegistration")
      processPacketUp("CIS", dcRegistration)
    }

    case (requestCreateVms : RequestCreateVms) => {
      log.info("LoadBalancerActor::RootSwitchActor:RequestCreateVms")
      processPacketDown(requestCreateVms.networkPacketProperties.receiver, requestCreateVms)
    }

  }

  override def processPacketDown(destination : String, cloudSimulatorMessage: NetworkPacket): Unit = {

    log.info(s"Forwarding message to DataCenter $destination")
    if(dataCenterlist.contains(destination)){

      context.actorSelection(destination) ! cloudSimulatorMessage
    } else {
      log.info(s"DataCenter $destination not connected to root switch " + self.path.name)
    }

  }

  override def processPacketUp(destination : String, cloudSimulatorMessage: NetworkPacket): Unit = {

    destination match {

      case "CIS" => context.actorSelection(ActorUtility.getActorRef("CIS")) ! cloudSimulatorMessage
    }

  }

}
