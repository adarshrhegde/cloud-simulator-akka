package com.cloudsimulator.entities.policies.cloudletscheduler

import com.cloudsimulator.entities.payload.CloudletPayload
import com.cloudsimulator.entities.policies.vmscheduler.VmRequirement
import com.cloudsimulator.entities.time.TimeSliceInfo

class TimeSharedCloudletScheduler extends CloudletScheduler {

  override def scheduleCloudlets(timeSliceInfo: TimeSliceInfo, vmResources: VmRequirement,
                                 cloudlets: Seq[CloudletPayload]): Seq[CloudletPayload] = {

    val timeSliceForEachCloudlet:Long = timeSliceInfo.slice / cloudlets.size

    Seq()
  }
}
