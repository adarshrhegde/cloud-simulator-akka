package com.cloudsimulator.entities.network


trait NetworkPacket {

  def networkPacketProperties : NetworkPacketProperties = new NetworkPacketProperties("","")
}

class NetworkPacketProperties(var sender: String, var receiver: String){


  override def toString: String = s"Sender-$sender,Receiver-$receiver"

  def swapSenderReceiver():NetworkPacketProperties = new NetworkPacketProperties(receiver, sender)


}
/*

class NetworkPacketProperties2(var sender: String, var receiver: String){

  var delay :Int = _

  def apply(addedDelay : Int) = delay += addedDelay

  override def toString: String = s"Sender-$sender,Receiver-$receiver"

  def addDelay(delayTime : Int) = {

    delay += delayTime
  }
}


case class MyPacket2(val networkPacketProperties2: NetworkPacketProperties2)
object Test extends App {
  var properties : NetworkPacketProperties2 = new NetworkPacketProperties2("","")
  properties.sender = "d"
  val packet = MyPacket2(properties)

  var props = packet.networkPacketProperties2
  print(packet.networkPacketProperties2.delay)

  packet.networkPacketProperties2(10)
  props.sender = "c"
  props.receiver = "d"
  //packet.properties = props
  print(packet.networkPacketProperties2)
  print(packet.networkPacketProperties2.delay)
}

*/
