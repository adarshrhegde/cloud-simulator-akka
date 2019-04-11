package com.cloudsimulator.entities.policies.cloudletscheduler

import akka.actor.{Actor, ActorLogging}

class CloudletSchedulerActor(cloudletScheduler: CloudletScheduler)
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ => {

    }
  }
}
