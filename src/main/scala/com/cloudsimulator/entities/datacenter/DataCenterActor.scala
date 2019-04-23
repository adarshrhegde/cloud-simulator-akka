package com.cloudsimulator.entities.datacenter

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorSelection, Props}
import com.cloudsimulator.cloudsimutils.VMPayloadStatus
import com.cloudsimulator.entities.DcRegistration
import com.cloudsimulator.entities.host.{AllocateVm, CanAllocateVm, CheckHostForRequiredVMs, HostActor}
import com.cloudsimulator.entities.loadbalancer.{FailedVmCreation, ReceiveRemainingCloudletsFromDC}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.payload.cloudlet.CloudletPayload
import com.cloudsimulator.entities.policies.vmallocation._
import com.cloudsimulator.entities.switch.{AggregateSwitchActor, EdgeSwitchActor, NetworkDevice, RootSwitchActor}
import com.cloudsimulator.entities.time.{SendTimeSliceInfo, TimeSliceCompleted, TimeSliceInfo}
import com.cloudsimulator.entities.vm.VmActor
import com.cloudsimulator.utils.ActorUtility

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration

/**
  * DataCenter Actor
  * @param id
  * @param hostList
  * @param vmList
  * @param location
  * @param vmToHostMap
  */
class DataCenterActor(id: Long,
                      location: String, rootSwitchId: String, var downStreamConnections : Seq[String])
  extends Actor with ActorLogging {

  import context._

  private val vmPayloadTrackerList : ListBuffer[VMPayloadTracker] = ListBuffer()


  private val hostList: ListBuffer[String] = ListBuffer()

  // this will be used to keep track of requests sent from LBs
  private var requestToLBMap : Map[Long, String] = Map()

//  private val vmList: ListBuffer[VmActor] = ListBuffer()


//  private val vmToHostMap: Map[Long, Long] = Map()

  var cloudletPayloadTrackerMap:Map[Long,List[CloudletPayload]]=Map()

  var cloudletReqCountFromHost: Map[Long, Long] = Map()

  var mapSliceIdToHostCountRem: Map[Long, Long] = Map()

  override def preStart(): Unit = {

    // Register self with CIS actor on startup
    //context.system.scheduler.scheduleOnce(new FiniteDuration(1, TimeUnit.SECONDS), self,RegisterWithCIS)
    self ! RegisterWithCIS

  }

  override def receive: Receive = {

    case CreateHost(hostId, hostProps : Props) => {
      log.info("SimulationActor::DataCenterActor:CreateHost")
      log.info(s"Creating host $hostId within DataCenter $id")
      val hostActor = context.actorOf(hostProps, s"${ActorUtility.host}-$hostId")
      hostList += hostActor.path.toStringWithoutAddress
    }

    case CreateVmAllocationPolicy(vmAllocationPolicy) => {
      log.info("SimulationActor::DataCenterActor:CreateVmAllocationPolicy")
      context.actorOf(Props(new VmAllocationPolicyActor(vmAllocationPolicy)), ActorUtility.vmAllocationPolicy)
    }

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
    case RequestCreateVms(networkPacketProperties, id, vmPayloads : List[VMPayload]) => {


      log.info(s"RootSwitchActor::DataCenterActor:RequestCreateVms:$id")
      log.info(s"Request to allocate Vms sent from ${networkPacketProperties.sender}")

      // update request-LB mapping to keep track of LB that sent the request
      requestToLBMap += (id -> networkPacketProperties.sender)

      val vmAllocationPolicyActor = context.child(ActorUtility.vmAllocationPolicy).get

      // ask Vm Allocation Policy actor to identify vm-host mapping
      vmAllocationPolicyActor ! RequestVmAllocation(id, vmPayloads, hostList.toList, downStreamConnections)

      // old logic to send each payload to each host
      // to be deleted shortly
      /*vmPayloads.foreach(vmPayload => {

        val payloadTracker = new VMPayloadTracker(id, vmPayload, VMPayloadStatus.NOT_ALLOCATED)
        vmPayloadTrackerList += payloadTracker

        log.debug("Received request to create VM " + vmPayload.toString())


        context.actorSelection("*").forward(CanAllocateVm(payloadTracker))
      })*/

    }



    /**
      * Receive Vm Allocation decision from Vm Allocation Policy Actor
      */
    case receiveVmAllocation: ReceiveVmAllocation => {

      log.info("VmAllocationPolicyActor::DataCenterActor:ReceiveVmAllocation")

      receiveVmAllocation.vmAllocationResult.vmHostMap.foreach(vmHost => {

        /**
          *  Forward allocateVM message to downlink switches.
          *  This message will be propagated to the right host
          *
          */
        // Need to optimize it such that message is not sent to all downlink switches
        var networkPacketProperties = new NetworkPacketProperties(
          self.path.toStringWithoutAddress, vmHost._2)

        downStreamConnections.foreach(switch => {
          context.actorSelection(switch) ! AllocateVm(networkPacketProperties, vmHost._1)
        })

      })

      // Ask Vm Allocation Policy Actor to change status and accept new requests
      sender() ! StopProcessing

      // handle failed vms
      if(receiveVmAllocation.vmAllocationResult.failedAllocationVms.size > 0){

        val networkPacketProperties = new NetworkPacketProperties(
          self.path.toStringWithoutAddress, requestToLBMap.get(receiveVmAllocation.requestId).get)

        // Send list of failed VM Payloads to Loadbalancer to allocate at different DC
        context.actorSelection(ActorUtility.getActorRef(rootSwitchId)) !
          FailedVmCreation(networkPacketProperties, id, receiveVmAllocation.requestId,
          receiveVmAllocation.vmAllocationResult.failedAllocationVms)

      }

    }

    case createSwitch : CreateSwitch => {


      createSwitch.switchType match {
        case "edge" => {

          val switchActor = context.actorOf(Props(new EdgeSwitchActor(
            createSwitch.upstreamConnections, createSwitch.downstreamConnections, createSwitch.switchDelay)),
            createSwitch.switchType + "-" + createSwitch.switchId)

          downStreamConnections = downStreamConnections :+ switchActor.path.toStringWithoutAddress
        }

        case "aggregate" => {

          val switchActor = context.actorOf(Props(new AggregateSwitchActor(
            createSwitch.upstreamConnections, createSwitch.downstreamConnections, createSwitch.switchDelay)),
            createSwitch.switchType + "-" + createSwitch.switchId)

          downStreamConnections = downStreamConnections :+ switchActor.path.toStringWithoutAddress
        }
      }
    }

    case vmAllocationSuccess : VmAllocationSuccess => {
      log.info("EdgeSwitchActor::DataCenterActor:VmAllocationSuccess")
      log.info(s"VM ${vmAllocationSuccess.vmPayload.payloadId} successfully created")
    }


    /**
      * Sender : LoadBalancerActor via RootSwitchActor
      *
      * Check if the DC has a host running a VM on which the the cloudlet needs to be executed.
      * vmList contains the VM ids required by the cloudlets
      * The cloudlet have a vmId present which tells us on which VM it can run.
      */
    case checkDCForRequiredVMs: CheckDCForRequiredVMs => {

      log.info(s"RootSwitchActor::DataCenterActor:CheckDCForRequiredVMs")
      // so we keep track of the cloudlet payload received on this DC.
      cloudletPayloadTrackerMap=cloudletPayloadTrackerMap + (checkDCForRequiredVMs.id -> checkDCForRequiredVMs.cloudletPayloads)

      // count kept to check if responses from all the hosts is received
      //We have to receive response from all the host since we have a vmId present in the cloudlet.
      cloudletReqCountFromHost=cloudletReqCountFromHost + (checkDCForRequiredVMs.id->hostList.size)

      //iterate over all host actor references and check for the required VMs
      hostList.foreach(host=>{

        val networkPacketProperties = new NetworkPacketProperties(
          self.path.toStringWithoutAddress, host)

        downStreamConnections.foreach(switch => {
          context.actorSelection(switch) ! CheckHostForRequiredVMs(networkPacketProperties, checkDCForRequiredVMs.id,checkDCForRequiredVMs.cloudletPayloads,checkDCForRequiredVMs.vmList)
        })
      })
    }

    /**
      * Sender: Host present in the DC
      *
      *
      * Decrement the host count for this CloudletPayload
      * Send all the remaining cloudlets to the LB.
      */
    case hostCheckedForRequiredVms: HostCheckedForRequiredVms => {
      log.info(s"SwitchActor::DataCenterActor:HostCheckedForRequiredVms:${hostCheckedForRequiredVms.reqId}")

//      val initialPayload: List[CloudletPayload] = cloudletPayloadTrackerMap(reqId)

      /*initialPayload.zip(cloudletPayloads).foreach(cloudlet => {
        if (cloudlet._1.payloadId == cloudlet._2.payloadId) {
//          cloudlet._1.status = cloudlet._2.status
          cloudlet._1.hostId = cloudlet._2.hostId
          cloudlet._1.dcId = cloudlet._2.dcId
        }
      })*/

      // reduce cloudletReqCountFromHost for the reqId
      cloudletReqCountFromHost=cloudletReqCountFromHost + (hostCheckedForRequiredVms.reqId ->
        (cloudletReqCountFromHost(hostCheckedForRequiredVms.reqId) - 1))

      // iterate over all cloudlets and check if any for assigned status,
      // send the remaining cloudlets to LB, so it can be sent to another DC.
      if (cloudletReqCountFromHost(hostCheckedForRequiredVms.reqId) == 0) {
        val remainingCloudlets: List[CloudletPayload] = cloudletPayloadTrackerMap(hostCheckedForRequiredVms.reqId)
          .filter(cloudlet=>{
            cloudlet.dcId != id
          })

        val networkPacketProperties = new NetworkPacketProperties(self.path.toStringWithoutAddress,
          ActorUtility.getActorRef("loadBalancer"))

        context.actorSelection(ActorUtility.getActorRef(rootSwitchId)) !
          ReceiveRemainingCloudletsFromDC(networkPacketProperties, hostCheckedForRequiredVms.reqId,
            remainingCloudlets,id)
      }
    }

    /**
      * Sender: TimeActor
      * TimeSliceInfo to be sent down the hierarchy till the VMSchedulerPolicy
      */
    case sendTimeSliceInfo: SendTimeSliceInfo => {

      if(mapSliceIdToHostCountRem.contains(sendTimeSliceInfo.sliceInfo.sliceId)){
        log.info(s"Time Slice ${sendTimeSliceInfo.sliceInfo.sliceId} already received by DC")
      }

      else {
        log.info("TimeActor::DataCenterActor:SendTimeSliceInfo")
        mapSliceIdToHostCountRem=mapSliceIdToHostCountRem + (sendTimeSliceInfo.sliceInfo.sliceId -> hostList.size)

          hostList.foreach(host => {

            context.actorSelection(host) ! SendTimeSliceInfo(sendTimeSliceInfo.sliceInfo)

          })
      }
    }

    /**
      * Sender: HostActor
      * After it receives from all hosts, it sends to the TimeActor to send the next time slice when available
      * Else it decrements the count of the remaining responses from the host.
      */
    case timeSliceCompleted: TimeSliceCompleted =>{
      log.info("HostActor::DataCenterActor:TimeSliceCompleted")
      val newCount:Option[Long]=mapSliceIdToHostCountRem.get(timeSliceCompleted.timeSliceInfo.sliceId).map(_-1)

      newCount.filter(_==0)
        .foreach(_ => context.actorSelection(ActorUtility.getActorRef("time-actor")) !
          TimeSliceCompleted(timeSliceCompleted.timeSliceInfo))

      newCount.filter(_!=0)
        .foreach(count => mapSliceIdToHostCountRem=mapSliceIdToHostCountRem +
          (timeSliceCompleted.timeSliceInfo.sliceId -> (count-1)))
    }


  }

}

final case class RegisterWithCIS()

case class RequestCreateVms(override val networkPacketProperties: NetworkPacketProperties,
                            requestId : Long, vmPayloads: List[VMPayload]) extends NetworkPacket

case class CanAllocateVmTrue(vmPayloadTracker : VMPayloadTracker)

case class VmAllocationSuccess(override val networkPacketProperties: NetworkPacketProperties, vmPayload : VMPayload) extends NetworkPacket

case class VMPayloadTracker(requestId : Long, vmPayload: VMPayload,
                            payloadStatus : VMPayloadStatus.Value)

case class CreateHost(hostId : Long, props : Props)

case class CreateVmAllocationPolicy(vmAllocationPolicy: VmAllocationPolicy) extends NetworkPacket

case class ReceiveVmAllocation(requestId : Long, vmAllocationResult: VmAllocationResult) extends NetworkPacket

case class CreateSwitch(switchType : String, switchId : Long, switchDelay : Int, upstreamConnections : List[String], downstreamConnections: List[String])

case class CheckDCForRequiredVMs(override val networkPacketProperties: NetworkPacketProperties, id: Long, cloudletPayloads: List[CloudletPayload], vmList:List[Long]) extends NetworkPacket

//case class CreateCloudletAllocationPolicyActor(id:Long,cloudletAssignmentPolicy:CloudletAssignmentPolicy)

case class HostCheckedForRequiredVms(override val networkPacketProperties: NetworkPacketProperties, reqId:Long, cloudletPayload: List[CloudletPayload]) extends NetworkPacket