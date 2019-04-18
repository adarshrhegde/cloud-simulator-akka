package com.cloudsimulator.entities.switch

import com.cloudsimulator.entities.network.NetworkPacket

trait NetworkDevice

trait Switch extends NetworkDevice {

  def processPacketDown(destination : String, cloudSimulatorMessage: NetworkPacket) : Unit
  def processPacketUp(destination : String, cloudSimulatorMessage : NetworkPacket) : Unit
}

