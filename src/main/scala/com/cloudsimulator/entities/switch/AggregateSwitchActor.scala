package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.network.NetworkPacket

class AggregateSwitchActor(upstreamEntities : List[String], downstreamEntities : List[String]) extends Actor with ActorLogging with Switch {
  override def receive: Receive = {
    case _ => ""
  }

  override def processPacketDown(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = ???

  override def processPacketUp(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = ???
}
