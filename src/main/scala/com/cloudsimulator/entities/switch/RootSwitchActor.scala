package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.loadbalancer.ReceiveDataCenterList
import com.cloudsimulator.entities.{DcRegistration, RequestDataCenterList}
import com.cloudsimulator.utils.ActorUtility

class RootSwitchActor(dataCenterlist: List[String]) extends Actor with ActorLogging with Switch {

  override def receive: Receive = {

    case (dcRegistration: DcRegistration) => {
      log.info("DataCenterActor::RootSwitchActor:DcRegistration")
      processPacketUp("CIS", dcRegistration)
    }

    case (requestDataCenterList: RequestDataCenterList) => {
      log.info("LoadBalancerActor::RootSwitchActor:RequestDataCenterList")
      processPacketUp("CIS", requestDataCenterList)
    }

    case (receiveDataCenterList : ReceiveDataCenterList) =>
      log.info("RootSwitchActor::LoadBalancerActor:ReceiveDataCenterList")
      processPacketDown("",receiveDataCenterList)

  }

  override def processPacketDown(destination : String, cloudSimulatorMessage: NetworkPacket): Unit = ???

  override def processPacketUp(destination : String, cloudSimulatorMessage: NetworkPacket): Unit = {

    destination match {
      case "CIS" => context.actorSelection(ActorUtility.getActorRef("CIS")) ! cloudSimulatorMessage
    }

  }

}
