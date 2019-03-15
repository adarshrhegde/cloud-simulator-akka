package com.cloudsimulator.entities.policies

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.host.RequestHostResourceStatus
import com.cloudsimulator.entities.payload.CloudletPayload

class CloudletAssignmentPolicyActor(policy: CloudletAssignmentPolicy) extends Actor with ActorLogging {

  private val vmToHostMap: Map[Long, Long] = Map()
  private val payloadTracker: Map[Long, CloudletPayload] = Map()
  private var processingFlag: Boolean = false

  override def receive: Receive = {

    case CheckAssignmentOfCloudlets(id, cloudletPayloads, hostList) => {
      processingFlag=true
      //send messages to all hosts to ask about VM resources
      hostList.foreach(hostRef=>{
        context.actorSelection(hostRef) ! RequestHostResourceStatus(id)
      })
    }

  }

}

case class CheckAssignmentOfCloudlets(id: Long, cloudletPayloads: List[CloudletPayload], hostList: List[String])
