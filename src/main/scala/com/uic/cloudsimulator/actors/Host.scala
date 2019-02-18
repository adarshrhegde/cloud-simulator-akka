package com.uic.cloudsimulator.actors

import akka.actor.{Actor, ActorLogging}

class Host(id : Long, hypervisor : String, vmList : List[Vm], bwProvisioner : String,
           ramProvisioner : String, vmScheduler : String, noOfPes : Int, nicCapacity: Double)
  extends Actor with ActorLogging {

  override def receive: Receive = ???
}
