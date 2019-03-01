package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}


import scala.collection.mutable.ListBuffer

final case class DcRegistration(id: Long)

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

    case _ => log.info(s"CIS Actor created $id")
  }

  def registerDc(id: Long): Unit = {
    dcList += id
  }

}
