package com.cloudsimulator.entities

import java.io.PrintWriter

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.payload.cloudlet.CloudletExecution
import com.cloudsimulator.entities.time.SimulationCompleted
import com.cloudsimulator.utils.ActorUtility
import scala.concurrent.duration._

import scala.concurrent.Await

class CloudletPrintActor extends Actor with ActorLogging {

  var finishedCloudlets: Seq[CloudletExecution]=Seq()

  override def receive: Receive = {

    case PrintCloudletsExectionStatus(cloudletsExecution) => {
      log.info(s"VmActor::CloudletPrintActor:PrintCloudletsExectionStatus")
      cloudletsExecution.foreach(cloudlet => {
//        log.info(s"Cloudlet-id \t Status \t DC-id \t Host-id \t Remaining Workload length \t Time Slice Info \t Delay(s) \t Cost(USD)")
//        log.info(s"${cloudlet.id} \t ${cloudlet.status} \t ${cloudlet.dcId} \t ${cloudlet.hostId} \t ${cloudlet.remWorkloadLength} \t ${cloudlet.timeSliceUsageInfo} \t ${cloudlet.delay} \t ${cloudlet.cost}")
        if(cloudlet.remWorkloadLength<=0){
          finishedCloudlets=finishedCloudlets :+ cloudlet
        }
      })
//      if(cloudletsExecution.count(_.remWorkloadLength>0)==0){

        //send to TimeActor that all cloudlets have completed their workload
        //once TimeActor responds, then we print the cloudlets and shutdown the system.

//        context.actorSelection(ActorUtility.getActorRef(ActorUtility.timeActor)) ! AllCloudletsExecutionCompleted()
//      }

    }

    case PrintAllCloudletsAfterTimeSliceCompleted =>{

      log.info(s"Cloudlet-id \t Status \t DC-id \t Host-id \t Time Slice \t Delay(s) \t Actual Workload \t Cost(USD)")
      finishedCloudlets.foreach(cloudlet=>{
        val timeSliceUsed=cloudlet.timeSliceUsageInfo.foldLeft(0.0)((sum,ts)=>{sum+ts.sliceLengthUsedInSeconds})
        log.info(s"${cloudlet.id} \t ${cloudlet.status} \t ${cloudlet.dcId} \t ${cloudlet.hostId} \t $timeSliceUsed \t ${cloudlet.delay} \t ${cloudlet.actualWorkloadLength} ${cloudlet.cost}")
      })
      log.info(s"CloudPrintActor::SimulationCompleted")
//      context.system.actorSelection(ActorUtility.timeActor) ! SimulationCompleted

      Await.result(context.system.terminate(),3 seconds)


    }

  }
}

case class PrintCloudletsExectionStatus(cloudletsExecution: Seq[CloudletExecution])

case class PrintAllCloudletsAfterTimeSliceCompleted()

case class AllCloudletsExecutionCompleted()