package com.cloudsimulator.entities.datacenter

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.entities.vm.VmActor

class DataCenterActor(id : Long, hostList : List[HostActor], vmList : List[VmActor],
                      location : String, vmToHostMap : Map[Long, Long])
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ => println(s"Datacenter Actor created $id")
  }


}