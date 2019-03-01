/*
package com.cloudsimulator

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.common.Messages.{CisAvailable, CisUp}

class SimulationActor(id:Int) extends Actor with ActorLogging {
  override def receive: Receive = {

    case CisAvailable =>{
        log.info("DataCenterActor::DataCenterActor:RegisterWithCIS")
        sender() ! CisUp
    }

    case _ => {
      log.info("Default Simulation Actor")
    }
  }
}
*/
