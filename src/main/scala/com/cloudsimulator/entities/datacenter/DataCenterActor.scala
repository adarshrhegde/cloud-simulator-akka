package com.cloudsimulator.entities.datacenter

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.DcRegistration
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.entities.vm.VmActor
import com.cloudsimulator.utils.ActorUtility

class DataCenterActor(id: Long, hostList: List[HostActor], vmList: List[VmActor],
                      location: String, vmToHostMap: Map[Long, Long])
  extends Actor with ActorLogging {

  final case class RegisterWithCIS()

  override def preStart(): Unit = {
    self ! RegisterWithCIS
  }

  override def receive: Receive = {

    /*case CisAvailable =>{
      log.info("DataCenterActor::DataCenterActor:preStart()")
      context.actorSelection(ActorUtility.getActorRef(ActorUtility.simulationActor)) ! CisAvailable
    }*/

    case RegisterWithCIS => {
      log.info("DataCenterActor::DataCenterActor:RegisterWithCIS")
      context.actorSelection(ActorUtility.getActorRef("CIS")) ! DcRegistration(id)
      log.info("DataCenterActor::DataCenterActor:RegisterWithCIS after")
    }
/*
    case CisUp =>{
      log.info("DataCenterActor::DataCenterActor:CisUp")
      self ! RegisterWithCIS
    }*/
    case _ => println(s"Datacenter Actor created $id")
  }


}