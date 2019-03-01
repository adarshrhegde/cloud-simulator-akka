package com.cloudsimulator.entities.policies

import akka.actor.{Actor, ActorLogging, Props}

class DataCenterSelectionPolicyActor(selectionPolicy : DataCenterSelectionPolicy)
  extends Actor with ActorLogging
{

  override def receive: Receive = {

    case FindDataCenterForVm(dcList, id) => {

      val dc : Option[Long] = selectionPolicy.selectDC(dcList)
    }
  }

}

object DataCenterSelectionPolicyActor {

  def props(selectionPolicy: DataCenterSelectionPolicy) : Props =
    Props(DataCenterSelectionPolicyActor.getClass, selectionPolicy)
}

case class FindDataCenterForVm(dcList : List[Long], id : Long)