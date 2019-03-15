package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.VmAllocationSuccess
import com.cloudsimulator.entities.host.AllocateVm
import com.cloudsimulator.entities.network.NetworkPacket

import scala.collection.mutable.ListBuffer

class EdgeSwitchActor extends Actor with ActorLogging with Switch {

  private var downlinkHostList : ListBuffer[String] = ListBuffer()
  private var uplinkEntityList : ListBuffer[String] = ListBuffer()

  override def receive: Receive = {

    case RegisterHost(hostPath) => {
      log.info(s"HostActor::EdgeSwitchActor:RegisterHost-$hostPath")
      downlinkHostList += hostPath
    }
    case allocateVm: AllocateVm => {
      log.info(s"DataCenterActor::EdgeSwitchActor:AllocateVm-${allocateVm.networkPacketProperties.receiver}")

      // forward to host if the host is connected to this switch
      if(downlinkHostList.contains(allocateVm.networkPacketProperties.receiver)){
        context.actorSelection(allocateVm.networkPacketProperties.receiver) ! allocateVm
      }
    }

    case vmAllocationSuccess: VmAllocationSuccess => {
      log.info(s"HostActor::EdgeSwitchActor:VmAllocationSuccess")

      context.actorSelection(vmAllocationSuccess.networkPacketProperties.receiver) ! vmAllocationSuccess
    }

  }

  override def processPacketDown(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = ???

  override def processPacketUp(destination: String, cloudSimulatorMessage: NetworkPacket): Unit = ???
}


case class RegisterHost(hostPath : String)