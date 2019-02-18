package com.uic.cloudsimulator.actors

import akka.actor.{Actor, ActorLogging}

class Vm(id : Long, userId : Long, cloudletScheduler : String, mips : Long,
        noOfPes : Int, ram : Int, bw : Double)
  extends Actor with ActorLogging {

  override def receive: Receive = ???
}
