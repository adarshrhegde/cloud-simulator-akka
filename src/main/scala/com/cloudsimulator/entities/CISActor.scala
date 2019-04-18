package com.cloudsimulator.entities

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.loadbalancer.ReceiveDataCenterList
import com.cloudsimulator.entities.payload.{Payload, VMPayload}
import com.cloudsimulator.entities.network.NetworkPacket
import com.cloudsimulator.entities.time.{TimeActorReceiveDataCenterList, TimeActorReceiveRootSwitchList}

import scala.collection.mutable.ListBuffer

class CISActor(id: Long) extends Actor with ActorLogging {

  val dcList: ListBuffer[Long] = ListBuffer()

  var rootSwitchList : Seq[Long] = Seq[Long]()

  override def preStart(): Unit = {
    log.info("CISActor preStart()")
  }

  override def receive: Receive = {

    /**
      * Sender : DataCenter
      * DataCenter registers itself with the CIS
      */
    case DcRegistration(id: Long) => {
      log.info("RootSwitchActor::CISActor:DcRegistration")
      registerDc(id)
    }

    /**
      * Sender : RootSwitchActor
      * Root Switch registers itself with the CIS
      */
    case RootSwitchRegistration(rootSwitchId : Long) => {

      log.info(s"RootSwitchActor::CISActor:RootSwitchRegistration:$rootSwitchId")
      rootSwitchList  = rootSwitchList :+ rootSwitchId

    }

    case RequestDataCenterList(id, vmPayloads: List[Payload]) => {
      log.info(s"LoadBalancer::CISActor:RequestDataCenterList:$id")
      sender() ! ReceiveDataCenterList(id, vmPayloads, dcList.toList)
    }

    /**
      * Sender: TimeActor
      * Send the registered DC list back to the TimeActor
      *
      */
    case TimeActorRequestDataCenterList() => {
      log.info("TimeActor::CISActor:TimeActorRequestDataCenterList")
      sender() ! TimeActorReceiveDataCenterList(dcList.toList)
    }

    /**
      * Sender : TimeActor
      * Request for the list of root switches in the system
      */
    case TimeActorRequestRootSwitchList() => {

      log.info(s"TimeActor::CISActor:TimeActorRequestRootSwitchList")
      sender() ! TimeActorReceiveRootSwitchList(rootSwitchList)
    }

    case _ => log.info(s"CIS Actor created $id")

  }

  def registerDc(id: Long): Unit = {
    dcList += id
    log.info("DataCenter list at CIS:: " + dcList)
  }

}


case class RequestDataCenterList(requestId: Long, vmPayloads: List[Payload]) extends NetworkPacket

case class TimeActorRequestDataCenterList() extends NetworkPacket

case class DcRegistration(id: Long) extends NetworkPacket

case class RootSwitchRegistration(id : Long) extends NetworkPacket

case class TimeActorRequestRootSwitchList()