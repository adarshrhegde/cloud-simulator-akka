package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.VmAllocationSuccess
import com.cloudsimulator.entities.host.{AllocateVm, RequestHostResourceStatus}
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.policies.vmallocation.ReceiveHostResourceStatus

import scala.collection.mutable.ListBuffer

class EdgeSwitchActor(upstreamEntities : List[String], downstreamEntities : List[String]) extends Actor with ActorLogging with Switch {

  private var downlinkHostList : ListBuffer[String] = ListBuffer()
  private var uplinkEntityList : ListBuffer[String] = ListBuffer()

  override def receive: Receive = {

    // TODO Remove logic for hosts to register with edge switch
    case RegisterHost(hostPath) => {
      log.info(s"HostActor::EdgeSwitchActor:RegisterHost-$hostPath")
      downlinkHostList += hostPath
    }


    case allocateVm: AllocateVm => {
      log.info(s"DataCenterActor::EdgeSwitchActor:AllocateVm-${allocateVm.networkPacketProperties.receiver}")

      // forward to host if the host is connected to this switch
        processPacketDown(allocateVm.networkPacketProperties.receiver, allocateVm)
    }

    case requestHostResourceStatus: RequestHostResourceStatus => {

      log.info(s"VMAllocationPolicyActor::EdgeSwitchActor:RequestHostResourceStatus")

        processPacketDown(requestHostResourceStatus.networkPacketProperties
          .receiver,requestHostResourceStatus)

    }

    case receiveHostResourceStatus: ReceiveHostResourceStatus => {

      log.info(s"${sender().path.name}::EdgeSwitchActor:ReceiveHostResourceStatus" +
        s":${receiveHostResourceStatus.requestId}")

      processPacketUp(receiveHostResourceStatus.networkPacketProperties
        .receiver, receiveHostResourceStatus)

    }

    case vmAllocationSuccess: VmAllocationSuccess => {
      log.info(s"HostActor::EdgeSwitchActor:VmAllocationSuccess")

      processPacketUp(vmAllocationSuccess.networkPacketProperties.receiver, vmAllocationSuccess)
    }

  }

  override def processPacketDown(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = {

    // TODO Add delay

    // check if actor name is registered with downstreamConnections
    if(downstreamEntities.contains(destination.split("/").lastOption.get)){
      context.actorSelection(destination) ! cloudSimulatorMessage
    }

  }

  override def processPacketUp(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = {

    // TODO Add delay
    context.actorSelection(destination) ! cloudSimulatorMessage
  }
}


case class RegisterHost(hostPath : String)