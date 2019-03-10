package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.loadbalancer.ReceiveDataCenterList
import com.cloudsimulator.entities.payload.VMPayload

import scala.collection.mutable.ListBuffer

class CISActor(id: Long) extends Actor with ActorLogging {

  var dcList: ListBuffer[Long] = ListBuffer()

  override def preStart(): Unit = {
    log.info("CISActor preStart()")
  }

  override def receive: Receive = {

    case DcRegistration(id: Long) => {
      log.info("DataCenterActor::CISActor:DcRegistration")
      registerDc(id)
    }

    case RequestDataCenterList(id, vmPayloads : List[VMPayload]) => {
      log.info(s"LoadBalancerActor::CISActor:RequestDataCenterList($id)")
      ReceiveDataCenterList(id, vmPayloads, dcList.toList)
    }

    case _ => log.info(s"CIS Actor created $id")

  }

  def registerDc(id: Long): ListBuffer[Long] = {
    dcList += id
  }

}


case class RequestDataCenterList(requestId : Long, vmPayloads : List[VMPayload])


case class DcRegistration(id: Long)
