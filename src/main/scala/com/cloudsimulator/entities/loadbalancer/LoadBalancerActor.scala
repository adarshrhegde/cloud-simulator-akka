package com.cloudsimulator.entities.loadbalancer

import akka.actor.{Actor, ActorLogging, ActorSelection}
import com.cloudsimulator.cloudsimutils.RequestStatus
import com.cloudsimulator.entities.RequestDataCenterList
import com.cloudsimulator.entities.payload.{CloudletPayload, Payload, VMPayload}
import com.cloudsimulator.entities.policies.{ FindDataCenter}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.utils.ActorUtility
import com.cloudsimulator.entities.datacenter.{ProcessCloudletsOnVm, RequestCreateVms}

/**
  * LoadBalancer actor
  * Entry point into the cloud architecture
  * Accepts requests from the user
  */
class LoadBalancerActor(rootSwitchId: String) extends Actor with ActorLogging {

  private val requestIdMap: Map[Long, RequestStatus.Value] = Map()

  override def receive: Receive = {

    case CloudletRequest(id, cloudletPayloads: List[CloudletPayload]) => {
      requestIdMap + (id -> RequestStatus("IN_PROGRESS"))

      val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))

      //      val rootSwitchActor = context.actorSelection(ActorUtility.getActorRef(rootSwitchId))
      // Request CIS to send DataCenter list
      cis ! RequestDataCenterList(id, cloudletPayloads)
    }

    /**
      * User request to create VMs
      */
    case VMRequest(id, vmPayloads: List[VMPayload]) => {

      log.info(s"User::LoadBalancerActor:VMRequest:$id")
      requestIdMap + (id -> RequestStatus("IN_PROGRESS"))

      val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))

      // Request CIS to send DataCenter list
      cis ! RequestDataCenterList(id, vmPayloads)
    }

    /**
      * Receive list of DataCenters
      * Sender : CIS
      */
    case ReceiveDataCenterList(id, payloads: List[Payload], dcList) => {

      log.info(s"RootSwitchActor::LoadBalancerActor:ReceiveDataCenterList:$id")

      val selectionPolicy: ActorSelection = context.actorSelection(ActorUtility.getActorRef("datacenter-selection-policy"))

      // Request DataCenter selection policy to select DataCenter from provided list
      selectionPolicy ! FindDataCenter(id, payloads, dcList)
    }

    /**
      * Receive the selected DataCenter for creating the VM
      * Sender: DataCenterSelectionPolicy actor
      */
    case ReceiveDataCenterForVm(id, payloads: List[Payload], dc) => {

      log.info(s"DataCenterSelectionPolicyActor::LoadBalancerActor:ReceiveDataCenterForVm:$id")

      val rootSwitchActor = context.actorSelection(ActorUtility.getActorRef(rootSwitchId))

      val dcActor = context.actorSelection(ActorUtility.getActorRef(s"dc-$dc"))

      val networkPacketProperties = new NetworkPacketProperties(self.path.toStringWithoutAddress,
        dcActor.pathString)

      if (isVmPayload(payloads)) {
        // Request DataCenter to create VMs in its hosts
        // Send message to root switch to forward to DataCenter
        rootSwitchActor ! RequestCreateVms(networkPacketProperties, id, payloads.map(_[VMPayload]))
      }
      else {
        //send the cloudlet data to the dc and it takes the necessary steps
        dcActor ! ProcessCloudletsOnVm(id, payloads.map(_[CloudletPayload]))
      }
    }

      def isVmPayload(x: List[Payload]): Boolean = x match {
        case vm: List[VMPayload] => true
        case _ => false
      }
  }
}

case class CloudletRequest(requestId: Long, cloudletPayloads: List[CloudletPayload]) extends NetworkPacket

case class VMRequest(requestId: Long, vmPayloads: List[VMPayload]) extends NetworkPacket

case class ReceiveDataCenterList(requestId: Long, payloads: List[Payload], dcList: List[Long]) extends NetworkPacket

case class ReceiveDataCenterForVm(requestId: Long, payloads: List[Payload], dc: Option[Long]) extends NetworkPacket