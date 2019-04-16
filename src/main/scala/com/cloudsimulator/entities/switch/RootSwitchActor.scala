package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.datacenter.{RegisterWithCIS, RequestCreateVms}
import com.cloudsimulator.entities.{DcRegistration, RootSwitchRegistration}
import com.cloudsimulator.entities.loadbalancer.FailedVmCreation
import com.cloudsimulator.utils.ActorUtility
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceCompleted}

/**
  * A Root Switch is connected to the external network on the upstream
  * It is connected to DataCenters on the downstream
  * @param downStreamEntities
  */
class RootSwitchActor(id : Long, downStreamEntities: List[String]) extends Actor with ActorLogging with Switch {

  override def preStart(): Unit = {
    self ! RegisterWithCIS
  }

  override def receive: Receive = {

    case RegisterWithCIS => {
      log.info("RootSwitchActor::RootSwitchActor:RegisterWithCIS")
      processPacketUp(ActorUtility.getActorRef("CIS"), RootSwitchRegistration(id))
    }

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

    case sendTimeSliceInfo: SendTimeSliceInfo => {
      log.info(s"TimeActor::RootSwitchActor:SendTimeSliceInfo::SliceId:${sendTimeSliceInfo.sliceInfo.sliceId}")
      processPacketDown(sendTimeSliceInfo.networkPacketProperties.receiver, sendTimeSliceInfo)
    }

    case timeSliceCompleted: TimeSliceCompleted => {
      log.info(s"DataCenterActor::RootSwitchActor:TimeSliceCompleted::SliceId:${timeSliceCompleted.timeSliceInfo.sliceId}")

      processPacketUp(timeSliceCompleted.networkPacketProperties.receiver, timeSliceCompleted)
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
