package com.cloudsimulator

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.config.Config
import com.cloudsimulator.entities.CISActor
import com.cloudsimulator.entities.datacenter.{CreateHost, CreateSwitch, CreateVmAllocationPolicy, DataCenterActor}
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.entities.loadbalancer.{LoadBalancerActor, VMRequest}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.policies._
import com.cloudsimulator.entities.policies.vmallocation.{SimpleVmAllocationPolicy, StartAllocation}
import com.cloudsimulator.entities.policies.vmscheduler.{SpaceSharedVmScheduler, TimeSharedVmScheduler, VmScheduler}
import com.cloudsimulator.entities.switch.RootSwitchActor
import com.cloudsimulator.entities.time.TimeActor
import com.cloudsimulator.utils.ActorUtility

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration

class SimulationActor(id:Int) extends Actor with ActorLogging {

  // Import the execution context for message scheduling
  import context._

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
          host.bwProvisioner, host.ramProvisioner, getVmScheduler(host.vmScheduler), host.availableNoOfPes, host.mips,
          host.availableRam, host.availableStorage, host.availableBw, host.edgeSwitch)))
      })

      // Create DataCenter Selection policy actor
      context.actorOf(DataCenterSelectionPolicyActor.props(new SimpleDataCenterSelectionPolicy), "datacenter-selection-policy")

      // Create VMs and assign to hosts
      /*config.vmPayloadList.foreach(vm => {
        println(vm)
      })*/

      // Schedule message after 5s to ensure all actors are created
      context.system.scheduler.scheduleOnce(
        new FiniteDuration(5, TimeUnit.SECONDS), self, SendVMWorkload(config.vmPayloadList))

    }

    case sendVMWorkload: SendVMWorkload => {

      log.info("SimulatorActor::SimulatorActor:SendVMWorkload")

      context.child("loadBalancer").get ! VMRequest(1, sendVMWorkload.vmPayloadList)

      context.system.scheduler.scheduleOnce(
        new FiniteDuration(5, TimeUnit.SECONDS), self, StartTimeActor)
    }

    case StartTimeActor => {

      log.info("SimulationActor::SimulationActor:StartTimeActor")

      context.actorOf(Props(new TimeActor(99, 100)), "time-actor")

    }
  }

  def getVmScheduler(vmSchedulerType : String) : VmScheduler = {

    vmSchedulerType match {

      case "TimeSharedVmScheduler" => new TimeSharedVmScheduler()
      case "SpaceSharedVmScheduler" => new SpaceSharedVmScheduler()
    }
  }

}

case class SendVMWorkload(vmPayloadList : List[VMPayload])


final case class Start()

final case class StartTimeActor()



