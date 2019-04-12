package com.cloudsimulator.entities.policies.cloudletscheduler

import java.util.Calendar

import com.cloudsimulator.cloudsimutils.CloudletPayloadStatus
import com.cloudsimulator.entities.payload.cloudlet.{CloudletExecution, CloudletPayload}
import com.cloudsimulator.entities.policies.vmscheduler.VmRequirement
import com.cloudsimulator.entities.time.{TimeSliceInfo, TimeSliceUsage}

class TimeSharedCloudletScheduler extends CloudletScheduler {

  override def scheduleCloudlets(timeSliceInfo: TimeSliceInfo, vmResources: VmRequirement,
                                 cloudlets: Seq[CloudletExecution]): Seq[CloudletExecution] = {

    val timeSliceForEachCloudlet: Double = timeSliceInfo.slice.toDouble / cloudlets.size

    //TODO only run on the one's not completed
    // put this check on the VMActor

    cloudlets.map(cloudlet => {
      val newTimeSliceUsageInfo = cloudlet.timeSliceUsageInfo :+
        TimeSliceUsage(timeSliceInfo.sliceId, timeSliceForEachCloudlet)

      val newExecStartTime: Double = Option(cloudlet.execStartTime) match {
        case None => Calendar.getInstance().getTimeInMillis
      }

      val newExecEndTime: Double = Option(cloudlet.execEndTime) match {
        case None => {
          if (cloudlet.remWorkloadLength == 0) {
            Calendar.getInstance().getTimeInMillis
          }
          -1
        }
      }

      //TODO remaining workload length left

      CloudletExecution(cloudlet.cloudletPayload,
        newTimeSliceUsageInfo,
        cloudlet.dcId, cloudlet.delay,
        cloudlet.hostId, newExecStartTime, newExecEndTime,
        CloudletPayloadStatus.RUNNING,
        cloudlet.remWorkloadLength)
    })
  }
}
