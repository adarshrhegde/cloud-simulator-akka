package com.cloudsimulator.entities.time

import java.util.Calendar

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.network.{NetworkPacket, NetworkPacketProperties}
import com.cloudsimulator.entities.{TimeActorRequestDataCenterList, TimeActorRequestRootSwitchList}
import com.cloudsimulator.utils.ActorUtility

class TimeActor(id: Long, timeSlice: Long) extends Actor with ActorLogging {

  var timeSliceId: Long = -1
  val seqOfSystemTime: Seq[TimeStartEnd] = Seq()
  val mapIdToDcCountRem: Map[Long, Long] = Map()
  var startExecTimeForTimeSlice: Long = Calendar.getInstance().getTimeInMillis

  var rootSwitchSet : Set[Long] = Set()
  var dcSet : Set[Long] = Set()

  override def preStart(): Unit = {

    // Register self with Edge switch
    self ! RequestRootSwitchList()
  }

  override def receive: Receive = {

    /**
      * Triggered by preStart()
      * To get the Root switches registered with the CIS.
      * Receiver: CIS
      */
    case RequestRootSwitchList() => {
      log.info("preStart::TimeActor:RequestRootSwitchList")
      context.actorSelection(ActorUtility.getActorRef("CIS")) ! TimeActorRequestRootSwitchList()

    }

    /**
      * Sender: CIS
      * List of Datacenters form the CIS.
      */
    case TimeActorReceiveDataCenterList(dcList: Seq[Long]) => {

      log.info("CISActor::TimeActor:TimeActorReceiveDataCenterList")
      dcSet = dcSet ++ dcList

      self ! SendTimeSliceInfo
    }


    /**
      * Sender : TimeActor
      * Time slice information is sent to all this DCs
      */
    case SendTimeSliceInfo => {

      log.info("TimeActor::TimeActor:SendTimeSliceInfo")

      timeSliceId+=1
      mapIdToDcCountRem + (timeSliceId -> dcSet.size)
      startExecTimeForTimeSlice = Calendar.getInstance().getTimeInMillis

      dcSet.foreach(dc => {

        val networkPacketProperties = new NetworkPacketProperties(self.path
          .toStringWithoutAddress, ActorUtility.getDcRefString() + s"$dc")

        log.info("Root switch size" + rootSwitchSet.size)
        log.info("DC size" + dcSet.size)
        rootSwitchSet.map(rs => context.actorSelection(ActorUtility
          .getRootSwitchRefString() + s"$rs"))
          .foreach(rootSwitchActor => {

            rootSwitchActor ! SendTimeSliceInfo(networkPacketProperties, TimeSliceInfo(timeSliceId, timeSlice, startExecTimeForTimeSlice))
          })

      })
      timeSliceId = timeSliceId + 1

    }

    /**
      * Sender : CIS
      * Receive a list of Root Switches in the system
      * Request DataCenter List
      */
    case TimeActorReceiveRootSwitchList(rootSwitchIds: Seq[Long]) => {
      log.info("CISActor::TimeActor:TimeActorReceiveRootSwitchList")
      rootSwitchSet = rootSwitchSet ++ rootSwitchIds

      context.actorSelection(ActorUtility.getActorRef("CIS")) ! TimeActorRequestDataCenterList()
    }

    /**
      * Sender: DataCenterActor
      * After it receives from all data centers, it repeats the flow.
      * Else it decrements the count of the remaining responses from the DCs.
      */
    case TimeSliceCompleted(sliceInfo:TimeSliceInfo)=>{
      log.info("DataCenterActor::HostActor:TimeSliceCompleted")

      val newCount:Option[Long]=mapIdToDcCountRem.get(sliceInfo.sliceId).map(_-1)
      newCount.filter(_==0).foreach(_ => {self ! TimeSliceCompleted(sliceInfo)
        seqOfSystemTime +: Seq(TimeStartEnd(sliceInfo.sliceStartSysTime,Calendar.getInstance().getTimeInMillis))
        self ! RequestRootSwitchList()
      })
      newCount.filter(_!=0).map(count => mapIdToDcCountRem+(sliceInfo.sliceId -> (count-1)))

    }

    case _ => {

    }
  }

}

case class RequestRootSwitchList()

case class TimeActorReceiveDataCenterList(dcList: Seq[Long])

case class SendTimeSliceInfo(override val networkPacketProperties : NetworkPacketProperties, sliceInfo: TimeSliceInfo) extends NetworkPacket

case class TimeSliceCompleted(timeSliceInfo:TimeSliceInfo)

case class TimeActorReceiveRootSwitchList(rootSwitchList: Seq[Long])