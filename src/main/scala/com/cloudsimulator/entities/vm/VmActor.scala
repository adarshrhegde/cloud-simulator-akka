package com.cloudsimulator.entities.vm

import akka.actor.{Actor, ActorLogging}

class VmActor(id : Long, userId : Long, mips : Long,
              noOfPes : Int, ram : Long, bw : Double)
  extends Actor with ActorLogging {

  override def receive: Receive = ???
}
