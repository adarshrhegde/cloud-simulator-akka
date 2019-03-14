package com.cloudsimulator.entities.network


trait NetworkPacket {

  def networkPacketProperties : NetworkPacketProperties = new NetworkPacketProperties("","")
}

class NetworkPacketProperties(var sender: String, var receiver: String){


  override def toString: String = s"Sender-$sender,Receiver-$receiver"
}

/*

case class MyPacket2(override val networkPacketProperties: NetworkPacketProperties) extends NetworkPacket
object Test extends App {
  var properties : NetworkPacketProperties = new NetworkPacketProperties("","")
  properties.sender = "d"
  val packet = MyPacket2(properties)

  var props = packet.networkPacketProperties
  print(packet.networkPacketProperties.toString)

  props.sender = "c"
  props.receiver = "d"
  //packet.properties = props
  print(packet.networkPacketProperties)
}

*/
