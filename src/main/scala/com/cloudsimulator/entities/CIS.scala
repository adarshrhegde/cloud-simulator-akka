package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.DataCenter

case class CISI(id : Long, dcList : List[String])

class CIS(id : Long, dcList : List[DataCenter]) extends Actor with ActorLogging {

  override def receive: Receive = ???

}
