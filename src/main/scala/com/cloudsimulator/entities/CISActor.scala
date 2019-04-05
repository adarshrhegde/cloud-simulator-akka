package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.loadbalancer.ReceiveDataCenterList
import com.cloudsimulator.entities.payload.{Payload, VMPayload}
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.time.TimeActorReceiveDataCenterList

import scala.collection.mutable.ListBuffer

class CISActor(id: Long) extends Actor with ActorLogging {

  var dcList: ListBuffer[Long] = ListBuffer()

  override def preStart(): Unit = {
    log.info("CISActor preStart()")
  }

  override def receive: Receive = {

    case DcRegistration(id: Long) => {
      log.info("LoadBalancer::CISActor:DcRegistration")
      registerDc(id)
    }

    case RequestDataCenterList(id, vmPayloads: List[Payload]) => {
      log.info(s"LoadBalancer::CISActor:RequestDataCenterList($id)")
      sender() ! ReceiveDataCenterList(id, vmPayloads, dcList.toList)
    }

    /**
      * Sender: TimeActor
      * Send the registered DC list back to the TimeActor
      *
      */
    case TimeActorRequestDataCenterList() => {
      log.info("TimeActor::CISActor:TimeActorRequestDataCenterList")
      sender() ! TimeActorReceiveDataCenterList(dcList.toList)
    }

    case _ => log.info(s"CIS Actor created $id")

  }

  def registerDc(id: Long): Unit = {
    dcList += id
    log.info("DataCenter list at CIS:: " + dcList)
  }

}


case class RequestDataCenterList(requestId: Long, vmPayloads: List[Payload]) extends NetworkPacket

case class TimeActorRequestDataCenterList() extends NetworkPacket

case class DcRegistration(id: Long) extends NetworkPacket
