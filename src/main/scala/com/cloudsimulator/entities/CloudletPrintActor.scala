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
//  val writer = new PrintWriter("output.txt")

  override def receive: Receive = {

    case PrintCloudletsExectionStatus(cloudletsExecution) => {
      log.info(s"VmActor::CloudletPrintActor:PrintCloudletsExectionStatus")
      cloudletsExecution.foreach(cloudlet => {
//        log.info(s"Cloudlet-id \t Status \t DC-id \t Host-id \t Remaining Workload length \t Time Slice Info \t Delay(s) \t Cost(USD)")
//        log.info(s"${cloudlet.id} \t ${cloudlet.status} \t ${cloudlet.dcId} \t ${cloudlet.hostId} \t ${cloudlet.remWorkloadLength} \t ${cloudlet.timeSliceUsageInfo} \t ${cloudlet.delay} \t ${cloudlet.cost}")
        if(cloudlet.remWorkloadLength==0){
          finishedCloudlets=finishedCloudlets :+ cloudlet
        }
      })
      if(cloudletsExecution.count(_.remWorkloadLength>0)==0){
        log.info(s"Cloudlet-id \t Status \t DC-id \t Host-id \t Time Slice \t Delay(s) \t Cost(USD)")
//        writer.write(s"Cloudlet-id \t Status \t DC-id \t Host-id \t Time Slice \t Delay(s) \t Cost(USD)")
        finishedCloudlets.foreach(cloudlet=>{
          val timeSliceUsed=cloudlet.timeSliceUsageInfo.foldLeft(0.0)((sum,ts)=>{sum+ts.sliceLengthUsedInSeconds})
          log.info(s"${cloudlet.id} \t ${cloudlet.status} \t ${cloudlet.dcId} \t ${cloudlet.hostId} \t $timeSliceUsed \t ${cloudlet.delay} \t ${cloudlet.cost}")
//          writer.write(s"${cloudlet.id} \t ${cloudlet.status} \t ${cloudlet.dcId} \t ${cloudlet.hostId} \t $timeSliceUsed \t ${cloudlet.delay} \t ${cloudlet.cost}")
        })
        log.info(s"CloudPrintActor::SimulationCompleted")
        context.system.actorSelection(ActorUtility.timeActor) ! SimulationCompleted

        Await.result(context.system.terminate(),3 seconds)

      }

    }

    case _ => {

    }
  }
}

case class PrintCloudletsExectionStatus(cloudletsExecution: Seq[CloudletExecution])
