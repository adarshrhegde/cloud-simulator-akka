package com.cloudsimulator.entities.time

import java.util.Calendar

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.TimeActorRequestDataCenterList
import com.cloudsimulator.utils.ActorUtility

class TimeActor(id: Long, timeSlice: Long) extends Actor with ActorLogging {

  var timeSliceId: Long = -1
  val seqOfSystemTime: Seq[TimeStartEnd] = Seq()
  val mapIdToDcCountRem: Map[Long, Long] = Map()
  var startExecTimeForTimeSlice: Long = Calendar.getInstance().getTimeInMillis

  override def preStart(): Unit = {

    // Register self with Edge switch
    self ! RequestDataCenterList()
  }

  override def receive: Receive = {

    /**
      * Triggered by preStart()
      * To get the Datacenters registered with the CIS.
      * Receiver: CIS
      */
    case RequestDataCenterList() => {
      log.info("preStart::TimeActor:RequestDataCenterList")
      context.actorSelection(ActorUtility.getActorRef("CIS")) ! TimeActorRequestDataCenterList()
    }

    /**
      * Sender: CIS
      * List of Datacenters form the CIS.
      * Time slice information is sent to all this DCs
      */
    case TimeActorReceiveDataCenterList(dcList: Seq[Long]) => {

      log.info("CIS::TimeActor:TimeActorReceiveDataCenterList")
      timeSliceId+=1
      mapIdToDcCountRem + (timeSliceId -> dcList.size)
      startExecTimeForTimeSlice = Calendar.getInstance().getTimeInMillis
      dcList.foreach(dc => {
        context.actorSelection(ActorUtility.getActorRef(s"dc-$dc")) !
          SendTimeSliceInfo(TimeSliceInfo(timeSliceId, timeSlice, startExecTimeForTimeSlice))
      })
      timeSliceId = timeSliceId + 1
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
        self ! RequestDataCenterList()
      })
      newCount.filter(_!=0).map(count => mapIdToDcCountRem+(sliceInfo.sliceId -> (count-1)))

    }

    case _ => {

    }
  }

  case class RequestDataCenterList()

}

case class TimeActorReceiveDataCenterList(dcList: Seq[Long])

case class SendTimeSliceInfo(sliceInfo: TimeSliceInfo)

case class TimeSliceCompleted(timeSliceInfo:TimeSliceInfo)