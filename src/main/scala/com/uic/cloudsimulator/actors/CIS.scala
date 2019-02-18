package com.uic.cloudsimulator.actors

import akka.actor.{Actor, ActorLogging}

class CIS(id : Long, dcList : List[Datacenter]) extends Actor with ActorLogging {

  override def receive: Receive = ???

}
