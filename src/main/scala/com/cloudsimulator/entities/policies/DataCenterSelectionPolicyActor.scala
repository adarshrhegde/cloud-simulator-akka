package com.cloudsimulator.entities.policies

import akka.actor.{Actor, ActorLogging, Props}
import com.cloudsimulator.entities.payload.VMPayload

class DataCenterSelectionPolicyActor(selectionPolicy : DataCenterSelectionPolicy)
  extends Actor with ActorLogging
{

  override def receive: Receive = {

    case FindDataCenterForVm(vmPayloads : List[VMPayload], dcList) => {

      val dc : Option[Long] = selectionPolicy.selectDC(dcList)
    }
  }

}

object DataCenterSelectionPolicyActor {

  def props(selectionPolicy: DataCenterSelectionPolicy) : Props =
    Props(DataCenterSelectionPolicyActor.getClass, selectionPolicy)
}

case class FindDataCenterForVm(vmPayloads : List[VMPayload], dcList : List[Long])