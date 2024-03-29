package com.cloudsimulator.entities.time

import java.util.Calendar

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.{AllCloudletsExecutionCompleted, PrintAllCloudletsAfterTimeSliceCompleted, TimeActorRequestDataCenterList}
import com.cloudsimulator.utils.ActorUtility

class TimeActor(id: Long, timeSlice: Long) extends Actor with ActorLogging {

  var timeSliceId: Long = -1
  var seqOfSystemTime: Seq[TimeStartEnd] = Seq()
  var mapIdToDcCountRem: Map[Long, Long] = Map()
  var startExecTimeForTimeSlice: Long = Calendar.getInstance().getTimeInMillis
//  var cloudletsExecutionCompleted: Boolean = false

  var continueSimulation: Boolean = false

  var dcSet: Set[Long] = Set()

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

//      if (!cloudletsExecutionCompleted) {
        self ! SendTimeSliceInfo
//      }
    }


    /**
      * Sender : TimeActor
      * Time slice information is sent to all this DCs
      */
    case SendTimeSliceInfo => {
      continueSimulation=false
      log.info("TimeActor::TimeActor:SendTimeSliceInfo")

      timeSliceId += 1
      mapIdToDcCountRem = mapIdToDcCountRem + (timeSliceId -> dcSet.size)
      startExecTimeForTimeSlice = Calendar.getInstance().getTimeInMillis

      dcSet.map(dc => context.actorSelection(ActorUtility
        .getDcRefString() + s"$dc")) foreach (dcActor => {


        dcActor ! SendTimeSliceInfo(TimeSliceInfo(timeSliceId, timeSlice, startExecTimeForTimeSlice))


      })
//      timeSliceId = timeSliceId + 1

    }

    /**
      * Sender: DataCenterActor
      * After it receives from all data centers, it repeats the flow.
      * Else it decrements the count of the remaining responses from the DCs.
      */
    case timeSliceCompleted: TimeSliceCompleted => {
      log.info(s"DataCenterActor::TimeActor:TimeSliceCompleted")

      if(timeSliceCompleted.continueSimulation){
        continueSimulation=timeSliceCompleted.continueSimulation
      }

      mapIdToDcCountRem.get(timeSliceCompleted.timeSliceInfo.sliceId)
        .foreach(count => mapIdToDcCountRem = mapIdToDcCountRem +
          (timeSliceCompleted.timeSliceInfo.sliceId -> (count - 1)))
//      log.info(s"TimeActor:TimeSliceCompleted:Outside")

      mapIdToDcCountRem.get(timeSliceCompleted.timeSliceInfo.sliceId).filter(_ == 0).foreach(value => {
        log.info(s"TimeActor:TimeSliceCompleted:Eq:$value")
        seqOfSystemTime = seqOfSystemTime :+ TimeStartEnd(timeSliceCompleted.timeSliceInfo
          .sliceStartSysTime, Calendar.getInstance().getTimeInMillis)

        if (continueSimulation) {
          log.info(s"TimeActor:TimeSliceCompleted:ReqDCList")
          self ! RequestDataCenterList()
        }else{
          log.info(s"TimeActor:TimeSliceCompleted:StopSimulation:printCloudlets")
          context.actorSelection(ActorUtility.getActorRef(ActorUtility.cloudletPrintActor)) ! PrintAllCloudletsAfterTimeSliceCompleted
        }
      })


    }

    case AllCloudletsExecutionCompleted => {
      log.info(s"CloudPrintActor::TimeActor:AllCloudletsExecutionCompleted")
//      cloudletsExecutionCompleted = true

      context.actorSelection(ActorUtility.getActorRef(ActorUtility.cloudletPrintActor)) ! PrintAllCloudletsAfterTimeSliceCompleted

    }

    case SimulationCompleted => {
      log.info(s"CloudPrintActor::TimeActor:SimulationCompleted")
      context.system.terminate()
    }

    case _ => {

    }
  }

}

case class RequestDataCenterList()

case class TimeActorReceiveDataCenterList(dcList: Seq[Long])

case class SendTimeSliceInfo(sliceInfo: TimeSliceInfo)

case class TimeSliceCompleted(timeSliceInfo: TimeSliceInfo, continueSimulation:Boolean)

case class TimeActorReceiveRootSwitchList(rootSwitchList: Seq[Long])

case class SimulationCompleted()