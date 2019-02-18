package com.cloudsimulator.entities.cloudlet

import akka.actor.{Actor, ActorLogging}

class Cloudlet(id : Long, userId : Long, brokerId : Long, length : Long,
               noOfPes : Int, execStartTime : Double, execEndTime : Double)
  extends Actor with ActorLogging {


  override def receive: Receive = ???
}
