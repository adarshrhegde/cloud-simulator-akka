package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.{SendCreationConfirmation}
import com.cloudsimulator.entities.datacenter.{HostCheckedForRequiredVms, VmAllocationSuccess}
import com.cloudsimulator.entities.host.{AllocateVm, CheckHostForRequiredVMs, RequestHostResourceStatus}
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.policies.vmallocation.ReceiveHostResourceStatus
import com.cloudsimulator.utils.{ActorType, ActorUtility}

class AggregateSwitchActor(upstreamEntities : List[String], downstreamEntities : List[String],
                           switchDelay : Int) extends Actor with ActorLogging with Switch {

  var parentActorPath : String = _
  override def preStart(): Unit = {

    self ! SendCreationConfirmation(ActorType("SWITCH"))

    parentActorPath = self.path.toStringWithoutAddress
      .split("/").dropRight(1).mkString("/")

  }

  override def receive: Receive = {

    case sendCreationConfirmation: SendCreationConfirmation => {
      log.info("AggregateSwitchActor::AggregateSwitchActor:SendCreationConfirmation")

      context.actorSelection(ActorUtility.getSimulatorRefString) ! sendCreationConfirmation
    }

    case allocateVm: AllocateVm => {
      log.info(s"DataCenterActor::AggregateSwitchActor:AllocateVm-${allocateVm.networkPacketProperties.receiver}")

      processPacketDown(allocateVm.networkPacketProperties.receiver, allocateVm)
    }

    case requestHostResourceStatus: RequestHostResourceStatus => {

      log.info(s"VMAllocationPolicyActor::AggregateSwitchActor:RequestHostResourceStatus")

      processPacketDown(requestHostResourceStatus.networkPacketProperties
        .receiver,requestHostResourceStatus)

    }

    case receiveHostResourceStatus: ReceiveHostResourceStatus => {

      log.info(s"${sender().path.name}::AggregateSwitchActor:ReceiveHostResourceStatus" +
        s":${receiveHostResourceStatus.requestId}")

      processPacketUp(receiveHostResourceStatus.networkPacketProperties
        .receiver, receiveHostResourceStatus)

    }

    case vmAllocationSuccess: VmAllocationSuccess => {
      log.info(s"HostActor::AggregateSwitchActor:VmAllocationSuccess")

      processPacketUp(vmAllocationSuccess.networkPacketProperties.receiver, vmAllocationSuccess)
    }

    case checkHostForRequiredVMs: CheckHostForRequiredVMs => {
      log.info(s"DataCenter::AggregateSwitchActor:CheckHostForRequiredVMs")

      checkHostForRequiredVMs.cloudletPayloads.foreach(payload => payload.delay += switchDelay)
      processPacketDown(checkHostForRequiredVMs.networkPacketProperties.receiver, checkHostForRequiredVMs)
    }

    case hostCheckedForRequiredVms: HostCheckedForRequiredVms => {
      log.info(s"HostActor::AggregateSwitchActor:HostCheckedForRequiredVms")

      hostCheckedForRequiredVms.cloudletPayload.foreach(payload => payload.delay += switchDelay)
      processPacketUp(hostCheckedForRequiredVms.networkPacketProperties.receiver, hostCheckedForRequiredVms)
    }

  }

  override def processPacketDown(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = {


    // check if actor name is registered with downstreamConnections
    if(downstreamEntities.contains(destination.split("/").lastOption.get)){

      context.actorSelection(destination) ! cloudSimulatorMessage

    } else {

      // forward to all connections in downstream
      downstreamEntities.map(entity => context.actorSelection(parentActorPath + s"/$entity"))
        .foreach(entityActor => {

          entityActor ! cloudSimulatorMessage
      })

    }

  }

  override def processPacketUp(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = {


    if(upstreamEntities.contains(destination.split("/").lastOption.get)){

      context.actorSelection(destination) ! cloudSimulatorMessage

    } else {

      // forward to all connections in upstream
      upstreamEntities.map(entity => context.actorSelection(parentActorPath + s"/$entity"))
        .foreach(entityActor => {

          entityActor ! cloudSimulatorMessage
        })

    }
  }
}
