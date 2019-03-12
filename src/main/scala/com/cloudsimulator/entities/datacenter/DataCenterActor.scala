package com.cloudsimulator.entities.datacenter

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.cloudsimutils.VMPayloadStatus
import com.cloudsimulator.entities.DcRegistration
import com.cloudsimulator.entities.host.{AllocateVm, CanAllocateVm, HostActor}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.vm.VmActor
import com.cloudsimulator.utils.ActorUtility

import scala.collection.mutable.ListBuffer

/**
  * DataCenter Actor
  * @param id
  * @param hostList
  * @param vmList
  * @param location
  * @param vmToHostMap
  */
class DataCenterActor(id: Long,
                      location: String, rootSwitchId : String)
  extends Actor with ActorLogging {

  private val vmPayloadTrackerList : ListBuffer[VMPayloadTracker] = ListBuffer()

  private val hostList: ListBuffer[String] = ListBuffer()

  private val vmList: ListBuffer[VmActor] = ListBuffer()

  private val vmToHostMap: Map[Long, Long] = Map()

  override def preStart(): Unit = {

    // Register self with CIS actor on startup
    self ! RegisterWithCIS
  }

  override def receive: Receive = {

    case CreateHost(hostId, hostProps : Props) => {
      log.info(s"Creating host $hostId within DataCenter $id")
      context.actorOf(hostProps, s"host-$hostId")
      hostList += s"host-$hostId"
    }
    /*case CisAvailable =>{
      log.info("DataCenterActor::DataCenterActor:preStart()")
      context.actorSelection(ActorUtility.getActorRef(ActorUtility.simulationActor)) ! CisAvailable
    }*/

    /**
      * Register with CIS
      */
    case RegisterWithCIS => {
      log.info("DataCenterActor::DataCenterActor:RegisterWithCIS")
      //context.actorSelection(ActorUtility.getActorRef("CIS")) ! DcRegistration(id)

      context.actorSelection(ActorUtility.getActorRef(rootSwitchId)) ! DcRegistration(id)
    }

    /**
      * Request to create VMs sent by LoadBalancer
      */
    case RequestCreateVms(id, vmPayloads : List[VMPayload]) => {


      log.info(s"LoadBalancerActor::DataCenterActor:RequestCreateVms:$id")

      vmPayloads.foreach(vmPayload => {

        val payloadTracker = new VMPayloadTracker(id, vmPayload, VMPayloadStatus.NOT_ALLOCATED)
        vmPayloadTrackerList += payloadTracker

        log.debug("Received request to create VM " + vmPayload.toString())
        context.actorSelection("*").forward(CanAllocateVm(payloadTracker))
      })

    }

    case CanAllocateVmTrue(vmPayloadTracker) => {
      log.info(s"HostActor::DataCenterActor:CanAllocateVmTrue:$vmPayloadTracker")

      vmPayloadTrackerList.filter(tracker => tracker.requestId == vmPayloadTracker.requestId
        && tracker.vmPayload.payloadId == vmPayloadTracker.vmPayload.payloadId
        && tracker.payloadStatus == VMPayloadStatus.NOT_ALLOCATED).foreach(tracker => {

        if(tracker.payloadStatus == VMPayloadStatus.NOT_ALLOCATED)

          log.info(s"Sending allocation message to Host ")
          sender() ! AllocateVm(vmPayloadTracker)

      })

    }
    case VmAllocationSuccess(vmPayloadTracker) => {

      log.info(s"HostActor::DataCenterActor:VmAllocationSuccess:$vmPayloadTracker")

      vmPayloadTrackerList.filter(tracker => tracker.requestId == vmPayloadTracker.requestId
      && tracker.vmPayload.payloadId == vmPayloadTracker.vmPayload.payloadId
      && tracker.payloadStatus == VMPayloadStatus.NOT_ALLOCATED).remove(0)

      vmPayloadTrackerList += new VMPayloadTracker(vmPayloadTracker.requestId, vmPayloadTracker.vmPayload,
        VMPayloadStatus.ALLOCATED)

    }
    /*
    case CisUp =>{
      log.info("DataCenterActor::DataCenterActor:CisUp")
      self ! RegisterWithCIS
    }*/
    case _ => println(s"DataCenter Actor created $id")
  }

}

final case class RegisterWithCIS()

case class RequestCreateVms(requestId : Long, vmPayloads: List[VMPayload])

case class CanAllocateVmTrue(vmPayloadTracker : VMPayloadTracker)

case class VmAllocationSuccess(vmPayloadTracker : VMPayloadTracker)

case class VMPayloadTracker(requestId : Long, vmPayload: VMPayload,
                            payloadStatus : VMPayloadStatus.Value)

case class CreateHost(hostId : Long, props : Props)