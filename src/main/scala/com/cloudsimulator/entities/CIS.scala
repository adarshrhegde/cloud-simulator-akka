package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.Datacenter

class CIS(id : Long, dcList : List[Datacenter]) extends Actor with ActorLogging {

  override def receive: Receive = ???

}
