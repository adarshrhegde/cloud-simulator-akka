package com.cloudsimulator.entities.policies

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.payload.CloudletPayload

class CloudletAllocationPolicy extends Actor with ActorLogging{

  private val vmToHostMap:Map[Long,Long]=Map()

  override def receive: Receive = {

    case CheckAssignmentOfCloudlets(id,cloudletPayloads,hostList) =>{

    }

  }

}

case class CheckAssignmentOfCloudlets(id:Long,cloudletPayloads:List[CloudletPayload],hostList:List[String])
