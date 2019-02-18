package com.cloudsimulator.entities.host

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.vm.Vm

class Host(id : Long, hypervisor : String, vmList : List[Vm], bwProvisioner : String,
           ramProvisioner : String, vmScheduler : String, noOfPes : Int, nicCapacity: Double)
  extends Actor with ActorLogging {

  override def receive: Receive = ???
}
