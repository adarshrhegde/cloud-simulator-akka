package com.cloudsimulator

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.config.Config
import com.cloudsimulator.entities.CISActor
import com.cloudsimulator.entities.datacenter.{CreateHost, DataCenterActor}
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.entities.loadbalancer.LoadBalancerActor
import com.cloudsimulator.entities.policies.{DataCenterSelectionPolicyActor, SimpleDataCenterSelectionPolicy}
import com.cloudsimulator.entities.switch.RootSwitchActor
import com.cloudsimulator.utils.ActorUtility

import scala.collection.mutable.ListBuffer

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
*/ case Start => {

      val config = Config.loadConfig.get

      log.debug(s"Picked up configuration $config")

      // create root switch, CIS, Datacenters, hosts and policy actors
      val cisActor = context.actorOf(Props(new CISActor(config.cis.id)), "CIS")

      context.actorOf(Props(new LoadBalancerActor(config.rootSwitchId)), "loadBalancer")

      var dcList: ListBuffer[String] = ListBuffer()
      config.dataCenterList.foreach(dc => {

        val dcActor = context.actorOf(Props(new DataCenterActor(dc.id, dc.location, config.rootSwitchId)), "dc-" + dc.id)
        dcList += dcActor.path.toStringWithoutAddress

      })

      log.info("DC List:: " + dcList.toList)
      context.actorOf(Props(new RootSwitchActor(dcList.toList)), config.rootSwitchId)

      config.hostList.foreach(host => {

        val dcActor = context.actorSelection(ActorUtility
          .getActorRef("dc-"+host.dataCenterId))

        dcActor ! CreateHost(host.id, Props(new HostActor(host.id, host.dataCenterId, host.hypervisor,
          host.bwProvisioner, host.ramProvisioner, host.vmScheduler, host.availableNoOfPes, host.nicCapacity,
          host.availableRam, host.availableStorage, host.availableBw)))
      })

      context.actorOf(DataCenterSelectionPolicyActor.props(new SimpleDataCenterSelectionPolicy), "datacenter-selection-policy")


      self ! "SendWorkload"
    }

    case "SendWorkload" => {

    }
  }

}


final case class Start()