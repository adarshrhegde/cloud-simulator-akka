package com.cloudsimulator.entities.switch

import com.cloudsimulator.entities.network.NetworkPacket

trait Switch {

  def processPacketDown(destination : String, cloudSimulatorMessage: NetworkPacket) : Unit
  def processPacketUp(destination : String, cloudSimulatorMessage : NetworkPacket) : Unit
}



//case class NetworkPacket(cloudSimulatorMessage : CloudSimulatorMessage)