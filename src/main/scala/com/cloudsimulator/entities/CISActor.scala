package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.DataCenterActor

class CISActor(id : Long, dcList : List[DataCenterActor]) extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ => println(s"CIS Actor created $id")
  }

}
