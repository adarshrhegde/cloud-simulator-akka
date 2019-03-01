package com.cloudsimulator

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.config.Config
import com.cloudsimulator.entities.CISActor
import com.cloudsimulator.entities.datacenter.DataCenterActor
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.entities.policies.{DataCenterSelectionPolicyActor, SimpleDataCenterSelectionPolicy}

class SimulationActor(id:Int) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.info(s"Starting the cloud simulation")
  }

  override def postStop(): Unit = {
    log.info(s"Stopping the cloud simulation")
  }

  override def receive: Receive = {
    /*case CisAvailable =>{
      log.info("DataCenterActor::DataCenterActor:RegisterWithCIS")
      sender() ! CisUp
    }

    case _ => {
      log.info("Default Simulation Actor")
    }
*/ case "Start" => {


      val config = Config.loadConfig.get

      log.debug(s"Picked up configuration $config")

      // create CIS, Datacenters, hosts and policy actors

      val cisActor = context.actorOf(Props(new CISActor(config.cis.id)), "CIS")

      config.dataCenterList.foreach(dc => {

        context.actorOf(Props(new DataCenterActor(dc.id, List(), List(), dc.location, Map())), "dc-" + dc.id)
      })

      config.hostList.foreach(host => {

        context.actorOf(Props(new HostActor(host.id, host.dataCenterId, host.hypervisor,
          List(), host.bwProvisioner, host.ramProvisioner, host.vmScheduler, host.noOfPes, host.nicCapacity)))
      })

      context.actorOf(DataCenterSelectionPolicyActor.props(new SimpleDataCenterSelectionPolicy), "datacenter-selection-policy")


      self ! "SendWorkload"
    }

    case "SendWorkload" => {

    }
  }

}