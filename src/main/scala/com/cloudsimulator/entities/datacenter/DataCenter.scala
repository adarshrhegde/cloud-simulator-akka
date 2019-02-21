package com.cloudsimulator.entities.datacenter

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.host.Host
import com.cloudsimulator.entities.vm.Vm

class DataCenter(id : Long, hostList : List[Host], vmList : List[Vm],
                 location : String, vmToHostMap : Map[Long, Long])
  extends Actor with ActorLogging {

  override def receive: Receive = ???


}