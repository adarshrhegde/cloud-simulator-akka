package com.cloudsimulator.entities.loadbalancer

import akka.actor.{Actor, ActorLogging, ActorSelection}
import com.cloudsimulator.cloudsimutils.RequestStatus
import com.cloudsimulator.entities.RequestDataCenterList
import com.cloudsimulator.entities.datacenter.{CheckDCForRequiredVMs, RequestCreateVms}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.cloudlet.CloudletPayload
import com.cloudsimulator.entities.payload.{ Payload, VMPayload}
import com.cloudsimulator.entities.policies.datacenterselection.FindDataCenter
import com.cloudsimulator.utils.ActorUtility

/**
  * LoadBalancer actor
  * Entry point into the cloud architecture
  * Accepts requests from the user
  */
class LoadBalancerActor(rootSwitchId: String) extends Actor with ActorLogging {

  var requestIdMap: Map[Long, RequestStatus.Value] = Map()

  // this map can be common for both VM and Cloudlet payload
  var requestIdToCheckedDcMap: Map[Long,Seq[Long]]=Map()

  override def receive: Receive = {

    /**
      * Cloudlets provided by the user
      * Sender: SimulationActor / LoadBalancerActor
      * Add to the requestMap and request for the DCList from the CIS
      */
    case CloudletRequest(id, cloudletPayloads: List[CloudletPayload]) => {
      requestIdMap=requestIdMap + (id -> RequestStatus("IN_PROGRESS"))

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
      requestIdMap = requestIdMap + (id -> RequestStatus("IN_PROGRESS"))

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
      selectionPolicy ! FindDataCenter(id, payloads, dcList, requestIdToCheckedDcMap.getOrElse(id, Seq[Long]()))
    }

    /**
      * Receive the selected DataCenter for creating the VM
      * Sender: DataCenterSelectionPolicy actor
      */
    case ReceiveDataCenterForVm(id, payloads: List[Payload], dc) => {

      log.info(s"DataCenterSelectionPolicyActor::LoadBalancerActor:ReceiveDataCenterForVm:$id")

      dc match {

        case None => log.info(s"No DataCenter can be selected for request ID $id")

        case Some(dcId) => {

          val rootSwitchActor = context.actorSelection(ActorUtility.getActorRef(rootSwitchId))

          val dcActor = context.actorSelection(ActorUtility.getActorRef(s"dc-${dcId}"))

          val networkPacketProperties = new NetworkPacketProperties(self.path.toStringWithoutAddress,
            dcActor.pathString)

          if (isVmPayload(payloads)) {
            // Request DataCenter to create VMs in its hosts
            // Send message to root switch to forward to DataCenter
            rootSwitchActor ! RequestCreateVms(networkPacketProperties, id, payloads.map(_.asInstanceOf[VMPayload]))
            //rootSwitchActor ! RequestCreateVms(networkPacketProperties, id, payloads.map(_[VMPayload]))
          }
          else {
            val cloudletPayload=payloads.map(_.asInstanceOf[CloudletPayload])
            val vmList:List[Long]=cloudletPayload.map(_.vmId)
            //send the cloudlet data to the dc and it takes the necessary steps
            //TODO change dcActor to rootSwitchActor
            //TODO vmList can be removed
            dcActor ! CheckDCForRequiredVMs(id,cloudletPayload,vmList )
          }

        }
      }



    }

    /**
      * FailedVmCreation : DataCenter actor returns the list of Vms that
      * could not be created. Loadbalancer will attempt to allocate the VM on
      * another DataCenter
      */
    case failedVmCreation: FailedVmCreation => {

      requestIdToCheckedDcMap = requestIdToCheckedDcMap + (failedVmCreation.dcId -> (requestIdToCheckedDcMap(failedVmCreation.dcId) ++ Seq(failedVmCreation.dcId)))

      val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))
      // Re-Start the allocation process for the failed Vms
      cis ! RequestDataCenterList(failedVmCreation.requestId, failedVmCreation.failedVmPayloads)

    }

    /**
      * Sender: DataCenterActor
      * The remaining cloudletPayload is returned to the LBActor.
      * For the current reqId the prevDcId is recorded and the DCSelectionPolicy selects
      * only from the remaining DC.
      */
    case ReceiveRemainingCloudletsFromDC(reqId, cloudletPayloads, prevDcId) => {

      requestIdToCheckedDcMap = requestIdToCheckedDcMap + (reqId -> (requestIdToCheckedDcMap(reqId) ++ Seq(prevDcId)))

      if(cloudletPayloads.nonEmpty){
        // Re-Start the allocation process for the failed cloudlets
        val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))
        cis ! RequestDataCenterList(reqId, cloudletPayloads)

      }
    }

  }

  def isVmPayload(x: List[Payload]): Boolean = x.head match {
    case vm: VMPayload => true
    case _ => false
  }
}

case class CloudletRequest(requestId: Long, cloudletPayloads: List[CloudletPayload]) extends NetworkPacket

case class VMRequest(requestId: Long, vmPayloads: List[VMPayload]) extends NetworkPacket

case class ReceiveDataCenterList(requestId: Long, payloads: List[Payload], dcList: List[Long]) extends NetworkPacket

case class FailedVmCreation(dcId : Long, requestId : Long, failedVmPayloads: List[VMPayload]) extends NetworkPacket

case class ReceiveDataCenterForVm(requestId: Long, payloads: List[Payload], dc: Option[Long]) extends NetworkPacket

case class ReceiveRemainingCloudletsFromDC(reqId:Long,cloudletPayload: List[CloudletPayload],prevDcId:Long)