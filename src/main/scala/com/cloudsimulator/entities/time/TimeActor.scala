package com.cloudsimulator.entities.time

import java.util.Calendar

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.TimeActorRequestDataCenterList
import com.cloudsimulator.utils.ActorUtility

class TimeActor(id: Long, timeSlice: Long) extends Actor with ActorLogging {

  var timeSliceId: Long = 0
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

      mapIdToDcCountRem + (timeSliceId -> dcList.size)
      startExecTimeForTimeSlice = Calendar.getInstance().getTimeInMillis
      dcList.foreach(dc => {
        context.actorSelection(ActorUtility.getActorRef(s"dc-$dc")) !
          SendTimeSliceInfo(TimeSliceInfo(timeSliceId, timeSlice, startExecTimeForTimeSlice))
      })
      timeSliceId = timeSliceId + 1
    }
    case _ => {

    }
  }

  case class RequestDataCenterList()

}

case class TimeActorReceiveDataCenterList(dcList: Seq[Long])

case class SendTimeSliceInfo(sliceInfo: TimeSliceInfo)