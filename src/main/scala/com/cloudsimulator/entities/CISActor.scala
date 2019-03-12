package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.loadbalancer.ReceiveDataCenterList
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.switch.{NetworkPacket}

import scala.collection.mutable.ListBuffer

class CISActor(id: Long) extends Actor with ActorLogging {

  var dcList: ListBuffer[Long] = ListBuffer()

  override def preStart(): Unit = {
    log.info("CISActor preStart()")
  }

  override def receive: Receive = {

    case DcRegistration(id: Long) => {
      log.info("RootSwitch::CISActor:DcRegistration")
      registerDc(id)
    }

    case RequestDataCenterList(id, vmPayloads : List[VMPayload]) => {
      log.info(s"RootSwitchActor::CISActor:RequestDataCenterList($id)")
      sender() ! ReceiveDataCenterList(id, vmPayloads, dcList.toList)
    }

    case _ => log.info(s"CIS Actor created $id")

  }

  def registerDc(id: Long): Unit = {
    dcList += id
    log.info("DataCenter list at CIS:: " + dcList)
  }

}


case class RequestDataCenterList(requestId : Long, vmPayloads : List[VMPayload]) extends NetworkPacket


case class DcRegistration(id: Long) extends NetworkPacket
