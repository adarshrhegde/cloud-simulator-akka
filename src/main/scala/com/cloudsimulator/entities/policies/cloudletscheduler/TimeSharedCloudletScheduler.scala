package com.cloudsimulator.entities.policies.cloudletscheduler

import java.util.Calendar

import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus
import com.cloudsimulator.entities.cost.Cost
import com.cloudsimulator.entities.payload.cloudlet.CloudletExecution
import com.cloudsimulator.entities.time.{TimeSliceInfo, TimeSliceUsage}

case class TimeSharedCloudletScheduler() extends CloudletScheduler {

  override def scheduleCloudlets(timeSliceInfo: TimeSliceInfo, mips: Long,
                                 noOfPes: Int,
                                 cloudlets: Seq[CloudletExecution], cost: Cost): Seq[CloudletExecution] = {

    val timeSliceForEachCloudlet: Double = timeSliceInfo.slice.toDouble / cloudlets.size

    //TODO only run on the one's not completed
    // put this check on the VMActor

    cloudlets.map(cloudlet => {
      val newTimeSliceUsageInfo = cloudlet.timeSliceUsageInfo :+
        TimeSliceUsage(timeSliceInfo.sliceId, timeSliceForEachCloudlet)

      val newExecStartTime: Long = Option(cloudlet.execStartTime) match {
        case Some(-1.0) => Calendar.getInstance().getTimeInMillis
        case Some(value) => value
      }

      //remaining workload length
      //      val tempMipProcessed: Double = vmResources.mips * vmResources.noOfPes * timeSliceForEachCloudlet
      //      if(cloudlet.remWorkloadLength<tempMipProcessed)
      //TODO exact usage of the time slice shouldn't go into negative
      val newRemWorkloadLength = cloudlet.remWorkloadLength -
        mips * noOfPes * timeSliceForEachCloudlet


      val newExecEndTime: Long = Option(cloudlet.execEndTime) match {
        case Some(-1.0) => {
          if (newRemWorkloadLength <= 0) {
            Calendar.getInstance().getTimeInMillis
          }
          -1
        }
        case Some(value) => value
      }

      //cost calculation, append to the previous cost
      val newCost = cloudlet.cost + timeSliceForEachCloudlet * cost.cloudletExecutionCostPerSec

      CloudletExecution(cloudlet.id, cloudlet.cloudletPayload,
        newTimeSliceUsageInfo,
        cloudlet.delay, cloudlet.dcId,
        cloudlet.hostId, cloudlet.vmId, newExecStartTime, newExecEndTime,
        CloudletPayloadStatus.RUNNING,
        newRemWorkloadLength, newCost)
    })
  }
}
