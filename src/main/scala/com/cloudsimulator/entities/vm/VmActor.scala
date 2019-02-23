package com.cloudsimulator.entities.vm

import akka.actor.{Actor, ActorLogging}

class VmActor(id : Long, userId : Long, cloudletScheduler : String, mips : Long,
              noOfPes : Int, ram : Int, bw : Double)
  extends Actor with ActorLogging {

  override def receive: Receive = ???
}
