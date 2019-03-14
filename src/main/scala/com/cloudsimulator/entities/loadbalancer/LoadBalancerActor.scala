package com.cloudsimulator.entities.loadbalancer

import akka.actor.{Actor, ActorLogging, ActorSelection}
import com.cloudsimulator.cloudsimutils.RequestStatus
import com.cloudsimulator.entities.RequestDataCenterList
import com.cloudsimulator.entities.datacenter.RequestCreateVms
import com.cloudsimulator.entities.payload.{CloudletPayload, Payload, VMPayload}
import com.cloudsimulator.entities.policies.{DataCenterSelectionPolicyActor, FindDataCenterForVm, SimpleDataCenterSelectionPolicy}
import com.cloudsimulator.utils.ActorUtility
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}

/**
  * LoadBalancer actor
  * Entry point into the cloud architecture
  * Accepts requests from the user
  */
class LoadBalancerActor(rootSwitchId : String) extends Actor with ActorLogging {

  private val requestIdMap : Map[Long, RequestStatus.Value] = Map()

  override def receive: Receive = {

    case CloudletRequest(id, cloudletPayloads : List[CloudletPayload]) => {

      cloudletPayloads.foreach( cloudletPayload => {

      })


    }

    /**
      * User request to create VMs
      */
    case VMRequest(id, vmPayloads : List[VMPayload]) => {

      log.info(s"User::LoadBalancerActor:VMRequest:$id")
      requestIdMap + (id -> RequestStatus("IN_PROGRESS"))

      val cis : ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))

      // Request CIS to send DataCenter list
      cis ! RequestDataCenterList(id, vmPayloads)

    }

    /**
      * Receive list of DataCenters
      * Sender : CIS
      */
    case ReceiveDataCenterList(id, vmPayloads : List[VMPayload], dcList) => {

      log.info(s"CIS::LoadBalancerActor:ReceiveDataCenterList:$id")

      val selectionPolicy : ActorSelection = context.actorSelection(ActorUtility.getActorRef("datacenter-selection-policy"))

      // Request DataCenter selection policy to select DataCenter from provided list
      selectionPolicy ! FindDataCenterForVm(id, vmPayloads, dcList)
    }

    /**
      * Receive the selected DataCenter for creating the VM
      * Sender: DataCenterSelectionPolicy actor
      */
    case ReceiveDataCenterForVm(id, vmPayloads : List[VMPayload], dc) => {

      log.info(s"DataCenterSelectionPolicyActor::LoadBalancerActor:ReceiveDataCenterForVm:$id")

      val rootSwitchActor = context.actorSelection(ActorUtility.getActorRef(rootSwitchId))

      val dcActor = context.actorSelection(ActorUtility.getActorRef(s"dc-$dc"))

      val networkPacketProperties = new NetworkPacketProperties(self.path.toStringWithoutAddress,
        dcActor.pathString)

      // Request DataCenter to create VMs in its hosts
      // Send message to root switch to forward to DataCenter
      rootSwitchActor ! RequestCreateVms(networkPacketProperties, id, vmPayloads)
    }

  }
}

case class CloudletRequest(requestId : Long, cloudletPayloads: List[CloudletPayload]) extends NetworkPacket

case class VMRequest(requestId : Long,  vmPayloads : List[VMPayload]) extends NetworkPacket

case class ReceiveDataCenterList(requestId : Long, vmPayloads : List[VMPayload], dcList : List[Long]) extends NetworkPacket

case class ReceiveDataCenterForVm(requestId : Long, vmPayloads : List[VMPayload], dc : Long) extends NetworkPacket