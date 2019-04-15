package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.RequestCreateVms
import com.cloudsimulator.entities.DcRegistration
import com.cloudsimulator.entities.loadbalancer.FailedVmCreation
import com.cloudsimulator.utils.ActorUtility
import com.cloudsimulator.entities.network.NetworkPacket

/**
  * A Root Switch is connected to the external network on the upstream
  * It is connected to DataCenters on the downstream
  * @param downStreamEntities
  */
class RootSwitchActor(downStreamEntities: List[String]) extends Actor with ActorLogging with Switch {

  override def receive: Receive = {

    case (dcRegistration: DcRegistration) => {
      log.info(s"DataCenterActor::RootSwitchActor:DcRegistration:${dcRegistration.id}")
      processPacketUp(ActorUtility.getActorRef("CIS"), dcRegistration)
    }

    case (requestCreateVms : RequestCreateVms) => {
      log.info(s"LoadBalancerActor::RootSwitchActor:RequestCreateVms:${requestCreateVms.requestId}")
      processPacketDown(requestCreateVms.networkPacketProperties.receiver, requestCreateVms)
    }

    case failedVmCreation: FailedVmCreation => {
      log.info(s"DataCenterActor::RootSwitchActor:FailedVmCreation:${failedVmCreation.requestId}" +
        s"::DataCenter:${failedVmCreation.dcId}")
      processPacketUp(failedVmCreation.networkPacketProperties.receiver, failedVmCreation)
    }

  }

  override def processPacketDown(destination : String, networkPacket: NetworkPacket): Unit = {

    log.info(s"Forwarding message to DataCenter $destination")
    if(downStreamEntities.contains(destination.split("/").lastOption.get)){

      context.actorSelection(destination) ! networkPacket
    } else {
      log.info(s"DataCenter $destination not connected to root switch " + self.path.name)
    }

  }

  override def processPacketUp(destination : String, networkPacket: NetworkPacket): Unit = {

    log.info("Forwarding the message upstream")
    context.actorSelection(destination) ! networkPacket

  }

}
