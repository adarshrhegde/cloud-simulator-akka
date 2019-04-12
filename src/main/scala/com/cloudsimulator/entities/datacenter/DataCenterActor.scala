package com.cloudsimulator.entities.datacenter

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.cloudsimutils.VMPayloadStatus
import com.cloudsimulator.entities.DcRegistration
import com.cloudsimulator.entities.host.{AllocateVm, CanAllocateVm, CheckHostForRequiredVMs, HostActor}
import com.cloudsimulator.entities.loadbalancer.{FailedVmCreation, ReceiveRemainingCloudletsFromDC}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.payload.VMPayload
import com.cloudsimulator.entities.payload.cloudlet.CloudletPayload
import com.cloudsimulator.entities.policies.vmallocation._
import com.cloudsimulator.entities.switch.{AggregateSwitchActor, EdgeSwitchActor, RootSwitchActor}
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
                      location: String, rootSwitchId: String, cloudletAllocationPolicyId : String)
  extends Actor with ActorLogging {

  import context._

  private val vmPayloadTrackerList : ListBuffer[VMPayloadTracker] = ListBuffer()

  private var downlinkSwitches : ListBuffer[String] = ListBuffer()

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
    context.system.scheduler.scheduleOnce(new FiniteDuration(1, TimeUnit.SECONDS), self,RegisterWithCIS)

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
      requestToLBMap += (id -> sender().path.toStringWithoutAddress)

      val vmAllocationPolicyActor = context.child(ActorUtility.vmAllocationPolicy).get

      // ask Vm Allocation Policy actor to identify vm-host mapping
      vmAllocationPolicyActor ! RequestVmAllocation(id, vmPayloads, hostList.toList)

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

        downlinkSwitches.foreach(switch => {
          context.actorSelection(switch) ! AllocateVm(networkPacketProperties, vmHost._1)
        })

      })

      // Ask Vm Allocation Policy Actor to change status and accept new requests
      sender() ! StopProcessing

      // handle failed vms
      if(receiveVmAllocation.vmAllocationResult.failedAllocationVms.size > 0){

        val loadBalancerActor = context.actorSelection(
          requestToLBMap.get(receiveVmAllocation.requestId).get)

        // Send list of failed VM Payloads to Loadbalancer to allocate at different DC
        loadBalancerActor ! FailedVmCreation(id, receiveVmAllocation.requestId,
          receiveVmAllocation.vmAllocationResult.failedAllocationVms)

      }

    }

    case createSwitch : CreateSwitch => {

      downlinkSwitches += (createSwitch.switchType + "-" + createSwitch.switchId)

      createSwitch.switchType match {
        case "edge" => context.actorOf(Props(new EdgeSwitchActor),
          createSwitch.switchType + "-" + createSwitch.switchId)

        case "aggregate" => context.actorOf(Props(new AggregateSwitchActor),
          createSwitch.switchType + "-" + createSwitch.switchId)
      }
    }

    case vmAllocationSuccess : VmAllocationSuccess => {
      log.info("EdgeSwitchActor::DataCenterActor:VmAllocationSuccess")
      log.info(s"VM ${vmAllocationSuccess.vmPayload.payloadId} successfully created")
    }


    /**
      * Sender : LoadBalancerActor/RootSwitchActor
      *
      * Check if the DC has a host running a VM on which the the cloudlet needs to be executed.
      * vmList contains the VM ids required by the cloudlets
      * The cloudlet have a vmId present which tells us on which VM it can run.
      */
    case CheckDCForRequiredVMs(id,cloudletPayloads,vmList) => {
      //TODO change to RootSwitchActor
      log.info(s"LoadBalancerActor/RootSwitchActor::DataCenterActor:CheckDCForRequiredVMs")
      // so we can update all cloudlets when their execution status is received.
      cloudletPayloadTrackerMap=cloudletPayloadTrackerMap + (id -> cloudletPayloads)

      // count kept to check if responses from all the hosts is received
      //We have to receive response from all the host since we have a vmId present in the cloudlet.
      cloudletReqCountFromHost=cloudletReqCountFromHost + (id->hostList.size)

      //iterate over all host actor references and check for the required VMs
      hostList.foreach(host=>{
        context.actorSelection(host) ! CheckHostForRequiredVMs(id,cloudletPayloads,vmList)
      })
    }

    /**
      * Sender: Host present in the DC
      *
      * Check if any cloudlet was assigned to a VM on this host, if so,
      * update the cloudlet on the DC.
      * Decrement the host count for this CloudletPayload
      * Send all the remaining cloudlets to the LB.
      */
    case HostCheckedForRequiredVms(reqId, cloudletPayloads) => {
      log.info(s"HostActor::DataCenterActor:HostCheckedForRequiredVms:$reqId")

      val initialPayload: List[CloudletPayload] = cloudletPayloadTrackerMap(reqId)

      initialPayload.zip(cloudletPayloads).foreach(cloudlet => {
        if (cloudlet._1.payloadId == cloudlet._2.payloadId) {
//          cloudlet._1.status = cloudlet._2.status
          cloudlet._1.hostId = cloudlet._2.hostId
          cloudlet._1.dcId = cloudlet._2.dcId
        }
      })

      // reduce cloudletReqCountFromHost for the reqId
      cloudletReqCountFromHost=cloudletReqCountFromHost + (reqId -> (cloudletReqCountFromHost(reqId) - 1))

      // iterate over all cloudlets and check if any for assigned status,
      // send the remaining cloudlets to LB, so it can be sent to another DC.
      if (cloudletReqCountFromHost(reqId) == 0) {
        val remainingCloudlets: List[CloudletPayload] = cloudletPayloadTrackerMap(reqId)
          .filter(cloudlet=>{
            cloudlet.dcId != id
          })
        context.actorSelection(ActorUtility.getActorRef("loadBalancer")) ! ReceiveRemainingCloudletsFromDC(reqId,remainingCloudlets,id)
      }
    }

    /**
      * Sender: TimeActor
      * TimeSliceInfo to be sent down the hierarchy till the VMSchedulerPolicy
      */
    case SendTimeSliceInfo(sliceInfo:TimeSliceInfo) =>{

      log.info("TimeActor::DataCenterActor:SendTimeSliceInfo")
      mapSliceIdToHostCountRem=mapSliceIdToHostCountRem + (sliceInfo.sliceId -> hostList.size)

      hostList.foreach(host=>{
        context.actorSelection(host) ! SendTimeSliceInfo(sliceInfo)
      })
    }

    /**
      * Sender: HostActor
      * After it receives from all hosts, it sends to the TimeActor to send the next time slice when available
      * Else it decrements the count of the remaining responses from the host.
      */
    case TimeSliceCompleted(sliceInfo:TimeSliceInfo) =>{
      log.info("DataCenterActor::HostActor:TimeSliceCompleted")
      val newCount:Option[Long]=mapSliceIdToHostCountRem.get(sliceInfo.sliceId).map(_-1)
      newCount.filter(_==0).foreach(_ => context.actorSelection(ActorUtility.getActorRef("TimeActor")) ! TimeSliceCompleted(sliceInfo))
      newCount.filter(_!=0).foreach(count => mapSliceIdToHostCountRem=mapSliceIdToHostCountRem+(sliceInfo.sliceId -> (count-1)))
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

case class CreateSwitch(switchType : String, switchId : Long)

case class CheckDCForRequiredVMs(id: Long, cloudletPayloads: List[CloudletPayload],vmList:List[Long])

//case class CreateCloudletAllocationPolicyActor(id:Long,cloudletAssignmentPolicy:CloudletAssignmentPolicy)

case class HostCheckedForRequiredVms(reqId:Long, cloudletPayload: List[CloudletPayload])