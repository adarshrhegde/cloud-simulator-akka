package com.cloudsimulator.entities.host

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.vm.VmActor

class HostActor(id : Long, dataCenterId : Long, hypervisor : String, vmList : List[VmActor], bwProvisioner : String,
                ramProvisioner : String, vmScheduler : String, noOfPes : Int, nicCapacity: Double)
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ => println(s"Host Actor created $id")
  }
}
