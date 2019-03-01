package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.loadbalancer.RespondDataCenterList

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

    case RequestDataCenterList(id) => {
      log.info(s"LoadBalancerActor::CISActor:RequestDataCenterList($id)")
      RespondDataCenterList(dcList.toList, id)
    }

    case _ => log.info(s"CIS Actor created $id")

  }

  def registerDc(id: Long): ListBuffer[Long] = {
    dcList += id
  }

}


case class RequestDataCenterList(id : Long)


case class DcRegistration(id: Long)
