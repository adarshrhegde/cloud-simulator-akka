package com.cloudsimulator.entities.loadbalancer

import akka.actor.{Actor, ActorLogging, ActorSelection}
import com.cloudsimulator.entities.RequestDataCenterList
import com.cloudsimulator.entities.payload.{CloudletPayload, Payload, VMPayload, WorkloadPayload}
import com.cloudsimulator.entities.policies.{DataCenterSelectionPolicyActor, FindDataCenterForVm, SimpleDataCenterSelectionPolicy}
import com.cloudsimulator.utils.ActorUtility

class LoadBalancerActor extends Actor with ActorLogging {

  private val requestIdMap : Map[Long, String] = Map()

  override def receive: Receive = {

    case CloudletRequest(id, cloudletPayloads : List[CloudletPayload]) => {

      cloudletPayloads.foreach( cloudletPayload => {

      })


    }

    case VMRequest(id, vmPayloads : List[VMPayload]) => {

      requestIdMap + (id -> "INPROGRESS")

      val cis : ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))

      cis ! RequestDataCenterList(vmPayloads)

    }

    case ReceiveDataCenterList(vmPayloads : List[VMPayload], dcList) => {

      val selectionPolicy : ActorSelection = context.actorSelection(ActorUtility.getActorRef("datacenter-selection-policy"))

      selectionPolicy ! FindDataCenterForVm(vmPayloads, dcList)
    }


  }
}

case class CloudletRequest(requestId : Long, cloudletPayloads: List[CloudletPayload])

case class VMRequest(requestId : Long,  vmPayloads : List[VMPayload])

case class ReceiveDataCenterList(vmPayloads : List[VMPayload], dcList : List[Long])