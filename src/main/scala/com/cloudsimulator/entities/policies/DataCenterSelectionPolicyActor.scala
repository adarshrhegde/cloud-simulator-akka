package com.cloudsimulator.entities.policies

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.entities.loadbalancer.ReceiveDataCenterForVm
import com.cloudsimulator.entities.payload.{Payload, VMPayload}

class DataCenterSelectionPolicyActor(selectionPolicy : DataCenterSelectionPolicy)
  extends Actor with ActorLogging
{

  override def receive: Receive = {

    case FindDataCenter(id, payloads : List[Payload], dcList) => {

      val dc : Option[Long] = selectionPolicy.selectDC(dcList)
      sender() ! ReceiveDataCenterForVm(id, payloads, dc)
    }
  }

}

object DataCenterSelectionPolicyActor {

  def props(selectionPolicy: DataCenterSelectionPolicy) : Props =
    Props(classOf[DataCenterSelectionPolicyActor], selectionPolicy)
}

case class FindDataCenter(requestId : Long, vmPayloads : List[Payload], dcList : List[Long])