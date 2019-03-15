package com.cloudsimulator

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.config.Config
import com.cloudsimulator.entities.CISActor
import com.cloudsimulator.entities.datacenter.{CreateHost, CreateSwitch, CreateVmAllocationPolicy, DataCenterActor}
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.entities.loadbalancer.LoadBalancerActor
import com.cloudsimulator.entities.policies._
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

    case Start => {

      val config = Config.loadConfig.get

      log.debug(s"Picked up configuration $config")

      /**
        * Create root switch, CIS, DataCenters, hosts and policy actors
        */

      val rootSwitchName : String = config.rootSwitch.switchType + "-" + config.rootSwitch.id.toString

      val cisActor = context.actorOf(Props(new CISActor(config.cis.id)), "CIS")

      context.actorOf(Props(new LoadBalancerActor(rootSwitchName)), "loadBalancer")

      var dcList: ListBuffer[String] = ListBuffer()

      /**
        * Each DataCenter actor will be parent/grandparent actor to all computing
        * resources within the DataCenter. This includes Hosts, VMs, VmAllocationPolicy etc
        */
      // Create DC actors and their respective Vm Allocation policy, Switch children
      config.dataCenterList.foreach(dc => {

        val dcActor = context.actorOf(Props(new DataCenterActor(dc.id, dc.location, rootSwitchName, "")), "dc-" + dc.id)
        dcList += dcActor.path.toStringWithoutAddress

        val vmAllocationPolicy = new SimpleVmAllocationPolicy()

        dcActor ! CreateVmAllocationPolicy(vmAllocationPolicy)

        dc.switchList.foreach(switch => {
            dcActor ! CreateSwitch(switch.switchType, switch.id)
          })

      })

      log.info("DC List:: " + dcList.toList)
      context.actorOf(Props(new RootSwitchActor(dcList.toList)), rootSwitchName)

      config.hostList.foreach(host => {

        val dcActor = context.actorSelection(ActorUtility
          .getActorRef("dc-"+host.dataCenterId))

        dcActor ! CreateHost(host.id, Props(new HostActor(host.id, host.dataCenterId, host.hypervisor,
          host.bwProvisioner, host.ramProvisioner, host.vmScheduler, host.availableNoOfPes, host.nicCapacity,
          host.availableRam, host.availableStorage, host.availableBw, host.edgeSwitch)))
      })

      context.actorOf(DataCenterSelectionPolicyActor.props(new SimpleDataCenterSelectionPolicy), "datacenter-selection-policy")


      self ! "SendWorkload"
    }

    case "SendWorkload" => {

    }
  }

}


final case class Start()



