package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.DataCenterActor

import scala.collection.mutable.ListBuffer

class CISActor(id: Long) extends Actor with ActorLogging {

  var dcList: ListBuffer[Long] = ListBuffer()

  final case class DcRegistration(id: Long)

  override def receive: Receive = {

    case DcRegistration(id: Long) => {
      log.info("CIS::DcRegistration")
      registerDc(id)
    }

    case _ => log.info(s"CIS Actor created $id")
  }

  def registerDc(id: Long): ListBuffer[Long] = {
    dcList += id
  }

}
