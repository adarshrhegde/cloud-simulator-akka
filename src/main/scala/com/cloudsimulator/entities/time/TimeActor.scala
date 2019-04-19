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

  var dcSet : Set[Long] = Set()

  override def preStart(): Unit = {

    self ! RequestDataCenterList()
  }

  override def receive: Receive = {

    /**
      * Triggered by preStart()
      * To get the DataCenter registered with the CIS.
      * Receiver: CIS
      */
    case RequestDataCenterList() => {
      log.info("preStart::TimeActor:RequestDataCenterList")
      context.actorSelection(ActorUtility.getActorRef("CIS")) ! TimeActorRequestDataCenterList()

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

      dcSet.map(dc => context.actorSelection(ActorUtility
        .getDcRefString() + s"$dc"))foreach(dcActor => {

        dcActor ! SendTimeSliceInfo(TimeSliceInfo(timeSliceId, timeSlice, startExecTimeForTimeSlice))

      })
      timeSliceId = timeSliceId + 1

    }

    /**
      * Sender: DataCenterActor
      * After it receives from all data centers, it repeats the flow.
      * Else it decrements the count of the remaining responses from the DCs.
      */
    case timeSliceCompleted: TimeSliceCompleted=>{
      log.info("DataCenterActor::TimeActor:TimeSliceCompleted")

      val newCount:Option[Long]=mapIdToDcCountRem.get(timeSliceCompleted.timeSliceInfo.sliceId).map(_-1)

      newCount.filter(_==0).foreach(_ => {
//        self ! TimeSliceCompleted(
//        timeSliceCompleted.timeSliceInfo)

        seqOfSystemTime +: Seq(TimeStartEnd(timeSliceCompleted.timeSliceInfo
          .sliceStartSysTime,Calendar.getInstance().getTimeInMillis))

        self ! RequestDataCenterList()
      })
      newCount.filter(_!=0).map(count => mapIdToDcCountRem +
        (timeSliceCompleted.timeSliceInfo.sliceId -> (count-1)))

    }

    case _ => {

    }
  }

}

case class RequestDataCenterList()

case class TimeActorReceiveDataCenterList(dcList: Seq[Long])

case class SendTimeSliceInfo(sliceInfo: TimeSliceInfo)

case class TimeSliceCompleted(timeSliceInfo:TimeSliceInfo)

case class TimeActorReceiveRootSwitchList(rootSwitchList: Seq[Long])