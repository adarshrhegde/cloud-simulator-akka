package com.cloudsimulator.entities.loadbalancer

import akka.actor.{Actor, ActorLogging, ActorSelection}
import com.cloudsimulator.VMCreationConfirmation
import com.cloudsimulator.cloudsimutils.RequestStatus
import com.cloudsimulator.entities.RequestDataCenterList
import com.cloudsimulator.entities.datacenter.{CheckDCForRequiredVMs, RequestCreateVms}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.cloudlet.CloudletPayload
import com.cloudsimulator.entities.payload.{Payload, VMPayload}
import com.cloudsimulator.entities.policies.datacenterselection.FindDataCenter
import com.cloudsimulator.utils.ActorUtility

/**
  * LoadBalancer actor
  * Entry point into the cloud architecture
  * Accepts requests from the user
  */
class LoadBalancerActor(rootSwitchIds: List[String]) extends Actor with ActorLogging {

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
      log.info("SimulatorActor::LoadBalancerActor:CloudletRequest")

      requestIdMap=requestIdMap + (id -> RequestStatus("IN_PROGRESS"))

      val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef(ActorUtility.cis))

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

      val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef(ActorUtility.cis))

      // Request CIS to send DataCenter list
      cis ! RequestDataCenterList(id, vmPayloads)
    }

    /**
      * Receive list of DataCenters
      * Sender : CIS
      */
    case ReceiveDataCenterList(id, payloads: List[Payload], dcList) => {

      log.info(s"CISActor::LoadBalancerActor:ReceiveDataCenterList:$id")

      val selectionPolicy: ActorSelection = context.actorSelection(ActorUtility.getActorRef("datacenter-selection-policy"))

      // Request DataCenter selection policy to select DataCenter from provided list
      selectionPolicy ! FindDataCenter(id, payloads, dcList, requestIdToCheckedDcMap.getOrElse(id, Seq[Long]()))
    }

    /**
      * Receive the selected DataCenter for creating the VM
      * Sender: DataCenterSelectionPolicy actor
      * The payloads can be of two types : VmPayload/CloudletPayload
      * In case of VMPayload the selected datacenter is the used to allocate VMs
      * In case of CloudletPayload the selected datacenter is used to search for
      * the VM where the cloudlet will be deployed
      */
    case ReceiveDataCenterForVm(id, payloads: List[Payload], dc) => {

      log.info(s"DataCenterSelectionPolicyActor::LoadBalancerActor:ReceiveDataCenterForVm:$id")

      dc match {

        case None => {

          log.info(s"No DataCenter can be selected to complete request ID $id" +
            s", following payloads are un-allocated $payloads")

          val simActor: ActorSelection = context.actorSelection(ActorUtility.getSimulatorRefString())

          simActor ! VMCreationConfirmation(id)

        }

        case Some(dcId) => {

          /**
            * Send workload to datacenter via rootswitch. Since LB is not aware of architecture
            * message is sent to all root switches. The root switch connected to selected DC will
            * forward the packet down only
            */

          rootSwitchIds.map(rs => context.actorSelection(ActorUtility.getActorRef(rs)))
            .foreach(rootSwitchActor => {

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

                rootSwitchActor ! CheckDCForRequiredVMs(networkPacketProperties, id,cloudletPayload)
              }
            })


        }
      }



    }

    /**
      * FailedVmCreation : DataCenter actor returns the list of Vms that
      * could not be created. Loadbalancer will attempt to allocate the VM on
      * another DataCenter
      */
    case failedVmCreation: FailedVmCreation => {

      log.info(s"RootSwitchActor::LoadBalancerActor:FailedVmCreation:${failedVmCreation.requestId}")

      requestIdToCheckedDcMap = requestIdToCheckedDcMap + (failedVmCreation.requestId ->
        (requestIdToCheckedDcMap.getOrElse(failedVmCreation.requestId, Seq()) ++ Seq(failedVmCreation.dcId)))

      if(failedVmCreation.failedVmPayloads.size == 0){

        requestIdMap=requestIdMap + (failedVmCreation.requestId -> RequestStatus("COMPLETED"))
        val simActor: ActorSelection = context.actorSelection(ActorUtility.getSimulatorRefString())

        simActor ! VMCreationConfirmation(failedVmCreation.requestId)

      } else {

        val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))
        // Re-Start the allocation process for the failed Vms
        cis ! RequestDataCenterList(failedVmCreation.requestId, failedVmCreation.failedVmPayloads)

      }


    }

    /**
      * Sender: DataCenterActor
      * The remaining cloudletPayload is returned to the LBActor.
      * For the current reqId the prevDcId is recorded and the DCSelectionPolicy selects
      * only from the remaining DC.
      */
    case receiveRemainingCloudletsFromDC: ReceiveRemainingCloudletsFromDC => {

      log.info(s"${sender().path.toStringWithoutAddress}::LoadBalancerActor:ReceiveRemainingCloudletsFromDC:reqId")
      requestIdToCheckedDcMap = requestIdToCheckedDcMap + (receiveRemainingCloudletsFromDC.reqId ->
        (requestIdToCheckedDcMap.getOrElse(receiveRemainingCloudletsFromDC.reqId, Seq[Long]()) ++
          Seq(receiveRemainingCloudletsFromDC.prevDcId)))

      if(receiveRemainingCloudletsFromDC.cloudletPayload.nonEmpty){
        // Re-Start the allocation process for the failed cloudlets
        val cis: ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))
        cis ! RequestDataCenterList(receiveRemainingCloudletsFromDC.reqId, receiveRemainingCloudletsFromDC.cloudletPayload)

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

case class FailedVmCreation(override val networkPacketProperties : NetworkPacketProperties, dcId : Long, requestId : Long, failedVmPayloads: List[VMPayload]) extends NetworkPacket

case class ReceiveDataCenterForVm(requestId: Long, payloads: List[Payload], dc: Option[Long]) extends NetworkPacket

case class ReceiveRemainingCloudletsFromDC(override val networkPacketProperties: NetworkPacketProperties, reqId:Long,cloudletPayload: List[CloudletPayload],prevDcId:Long) extends NetworkPacket