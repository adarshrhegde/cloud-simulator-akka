package com.uic.cloudsimulator.actors

import akka.actor.{Actor, ActorLogging}

class Datacenter(id : Long, hostList : List[Host], vmList : List[Vm],
                 location : String, vmToHostMap : Map[Long, Long])
  extends Actor with ActorLogging {

  override def receive: Receive = ???

}
