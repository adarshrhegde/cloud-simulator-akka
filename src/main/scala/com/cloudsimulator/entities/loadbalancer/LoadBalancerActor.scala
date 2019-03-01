package com.cloudsimulator.entities.loadbalancer

import akka.actor.{Actor, ActorLogging, ActorSelection}
import com.cloudsimulator.entities.RequestDataCenterList
import com.cloudsimulator.entities.payload.WorkloadPayload
import com.cloudsimulator.entities.policies.{DataCenterSelectionPolicyActor, FindDataCenterForVm, SimpleDataCenterSelectionPolicy}
import com.cloudsimulator.utils.ActorUtility

class LoadBalancerActor extends Actor with ActorLogging {

  private val workloadMap : Map[Long, WorkloadPayload] = Map()
  private val userToVMMap : Map[Long, Long] = Map()

  override def receive: Receive = {

    case WorkloadRequest(workloadPayload) => {

      workloadMap + (workloadPayload.workloadId -> workloadPayload)

      val cis : ActorSelection = context.actorSelection(ActorUtility.getActorRef("CIS"))

      cis ! RequestDataCenterList(workloadPayload.workloadId)

    }

    case RespondDataCenterList(dcList, id) => {

      val selectionPolicy : ActorSelection = context.actorSelection(ActorUtility.getActorRef("datacenter-selection-policy"))

      selectionPolicy ! FindDataCenterForVm(dcList, id)
    }




  }
}

case class WorkloadRequest(workloadPayload: WorkloadPayload)

case class RespondDataCenterList(dcList : List[Long], id : Long)