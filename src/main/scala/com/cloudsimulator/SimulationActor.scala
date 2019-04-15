package com.cloudsimulator

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus
import com.cloudsimulator.config.Config
import com.cloudsimulator.entities.CISActor
import com.cloudsimulator.entities.payload.cloudlet.CloudletPayload
import com.cloudsimulator.entities.datacenter.{CreateHost, CreateSwitch, CreateVmAllocationPolicy, DataCenterActor}
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.entities.loadbalancer.{CloudletRequest, LoadBalancerActor, VMRequest}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.policies._
import com.cloudsimulator.entities.policies.datacenterselection.{DataCenterSelectionPolicyActor, SimpleDataCenterSelectionPolicy}
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

  val config = Config.loadConfig.get

  val actorCount : ActorCount = new ActorCount

  var deviceToSwitchMapping : Map[String, String] = Map()


  override def preStart(): Unit = {
    log.info(s"Starting the cloud simulation")
  }

  override def postStop(): Unit = {
    log.info(s"Stopping the cloud simulation")
  }

  override def receive: Receive = {

    case Start => {

      log.debug(s"Picked up configuration $config")

      /**
        * Create root switch, CIS, DataCenters, hosts and policy actors
        */

      val rootSwitchNames : List[String] = config.rootSwitchList.map(rootSwitchConfig => {

        val rootSwitchName : String = rootSwitchConfig.switchType + "-" + rootSwitchConfig.id.toString

        val dcConnections : List[String] = rootSwitchConfig.downstreamConnections
          .filter(connection => {
            connection.connectionType.equals("DataCenter")
          })
          .map(connection => {

          deviceToSwitchMapping = deviceToSwitchMapping + (connection.id -> rootSwitchName)
          connection.id
        })

        context.actorOf(Props(new RootSwitchActor(rootSwitchConfig.id, dcConnections)), rootSwitchName)

        rootSwitchName
      })


      val cisActor = context.actorOf(Props(new CISActor(config.cis.id)), "CIS")

      context.actorOf(Props(new LoadBalancerActor(rootSwitchNames)), "loadBalancer")

      var dcList: ListBuffer[String] = ListBuffer()

      /**
        * Each DataCenter actor will be parent/grandparent actor to all computing
        * resources within the DataCenter. This includes Hosts, VMs, VmAllocationPolicy etc
        */
      // Create DC actors and their respective Vm Allocation policy, Switch children
      config.dataCenterList.foreach(dc => {


        val dcActor = context.actorOf(Props(new DataCenterActor(dc.id, dc.location, deviceToSwitchMapping(s"dc-${dc.id}"), Seq())), "dc-" + dc.id)
        dcList += dcActor.path.toStringWithoutAddress

        val vmAllocationPolicy = new SimpleVmAllocationPolicy()

        dcActor ! CreateVmAllocationPolicy(vmAllocationPolicy)

        dc.switchList.foreach(switch => {
            dcActor ! CreateSwitch(switch.switchType, switch.id,
              switch.upstreamConnections.map(c => c.id), switch.downstreamConnections.map(c => c.id))
          })

      })

      log.info("DC List:: " + dcList.toList)

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
        new FiniteDuration(5, TimeUnit.SECONDS), self, SendCloudletPayload(config.cloudletPayloadList))
//      context.system.scheduler.scheduleOnce(
//        new FiniteDuration(5, TimeUnit.SECONDS), self, StartTimeActor)
    }

    //TODO should be sent when all the VMs are created and not after 5 seconds.
    case sendCloudletPayload: SendCloudletPayload => {

      log.info("SimulatorActor::SimulatorActor:SendCloudletPayload")
      val cloudletStatusUpdated: List[CloudletPayload] = sendCloudletPayload.cloudletPayloadList
        .map(cloudlet => {
          cloudlet.status = CloudletPayloadStatus.SENT
          cloudlet
        })
      context.child("loadBalancer").foreach(lb => {
        lb ! CloudletRequest(2, sendCloudletPayload.cloudletPayloadList)
      })

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

case class SendCloudletPayload(cloudletPayloadList:List[CloudletPayload])


final case class Start()

final case class StartTimeActor()

// Used for maintaining count of actors, to avoid using delays
class ActorCount {

  var actorCount : Int = 0
  var actorCreatedCount : Int = 0

  var vmActorCount : Int = 0
  var vmActorCreatedCount : Int = 0
}



