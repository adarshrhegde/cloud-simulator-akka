package com.cloudsimulator.entities.switch

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.{ActorType, SendCreationConfirmation}
import com.cloudsimulator.entities.datacenter.{CheckDCForRequiredVMs, RegisterWithCIS, RequestCreateVms}
import com.cloudsimulator.entities.{DcRegistration, RootSwitchRegistration}
import com.cloudsimulator.entities.loadbalancer.{FailedVmCreation, ReceiveRemainingCloudletsFromDC}
import com.cloudsimulator.utils.ActorUtility
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceCompleted}

/**
  * A Root Switch is connected to the external network on the upstream
  * It is connected to DataCenters on the downstream
  * @param downStreamEntities
  */
class RootSwitchActor(id : Long, downStreamEntities: List[String], switchDelay : Int) extends Actor with ActorLogging with Switch {

  override def preStart(): Unit = {

    self ! SendCreationConfirmation(ActorType("SWITCH"))

    self ! RegisterWithCIS
  }

  override def receive: Receive = {

    case sendCreationConfirmation: SendCreationConfirmation => {
      log.info("RootSwitchActor::RootSwitchActor:SendCreationConfirmation")

      context.actorSelection(ActorUtility.getSimulatorRefString) ! sendCreationConfirmation
    }

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

    case checkDCForRequiredVMs: CheckDCForRequiredVMs => {
      log.info(s"LoadBalancerActor::RootSwitchActor:CheckDCForRequiredVMs:${checkDCForRequiredVMs.id}")

      checkDCForRequiredVMs.cloudletPayloads.foreach(payload => payload.delay += switchDelay)
      processPacketDown(checkDCForRequiredVMs.networkPacketProperties.receiver, checkDCForRequiredVMs)
    }

    case receiveRemainingCloudletsFromDC: ReceiveRemainingCloudletsFromDC => {
      log.info(s"DataCenterActor::LoadBalancerActor:ReceiveRemainingCloudletsFromDC:${receiveRemainingCloudletsFromDC.reqId}")

      receiveRemainingCloudletsFromDC.cloudletPayload.foreach(payload => payload.delay += switchDelay)
      processPacketUp(receiveRemainingCloudletsFromDC.networkPacketProperties.receiver, receiveRemainingCloudletsFromDC)

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
