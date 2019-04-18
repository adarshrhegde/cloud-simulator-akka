package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.VmAllocationSuccess
import com.cloudsimulator.entities.host.{AllocateVm, RequestHostResourceStatus}
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.policies.vmallocation.ReceiveHostResourceStatus

class AggregateSwitchActor(upstreamEntities : List[String], downstreamEntities : List[String]) extends Actor with ActorLogging with Switch {

  var parentActorPath : String = _
  override def preStart(): Unit = {

    parentActorPath = self.path.toStringWithoutAddress
      .split("/").lastOption.get.dropRight(1).mkString("")

    log.info(s"Self Actor name ${self.path.toStringWithoutAddress}")
    log.info(s"Parent Actor name $parentActorPath")
  }

  override def receive: Receive = {

    case allocateVm: AllocateVm => {
      log.info(s"DataCenterActor::AggregateSwitchActor:AllocateVm-${allocateVm.networkPacketProperties.receiver}")

      // forward to host if the host is connected to this switch
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

  }

  override def processPacketDown(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = {

    // TODO Add delay

    // check if actor name is registered with downstreamConnections
    if(downstreamEntities.contains(destination.split("/").lastOption.get)){

      context.actorSelection(destination) ! cloudSimulatorMessage

    } else {
      // forward to all connections in downstream

    }

  }

  override def processPacketUp(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = {

    // TODO Add delay
    context.actorSelection(destination) ! cloudSimulatorMessage
  }
}
