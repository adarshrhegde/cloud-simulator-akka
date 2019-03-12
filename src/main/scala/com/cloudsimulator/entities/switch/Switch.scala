package com.cloudsimulator.entities.switch

trait Switch {

  def processPacketDown(destination : String, cloudSimulatorMessage: NetworkPacket) : Unit
  def processPacketUp(destination : String, cloudSimulatorMessage : NetworkPacket) : Unit
}


trait NetworkPacket {

  var sender:String = _
  var receiver :String = _
}

//case class NetworkPacket(cloudSimulatorMessage : CloudSimulatorMessage)