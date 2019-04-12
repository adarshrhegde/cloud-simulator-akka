package com.cloudsimulator.entities.policies.cloudletscheduler

import com.cloudsimulator.entities.payload.cloudlet.{CloudletExecution, CloudletPayload}
import com.cloudsimulator.entities.policies.vmscheduler.VmRequirement
import com.cloudsimulator.entities.time.TimeSliceInfo

trait CloudletScheduler {
  // 1. require the timeSliceInfo
  // 2. vm's current resources
  // 3. info of all cloudlets on this VM.
  // 4. pick the required cloudlet based on the policy and return the cloudets
  //    with time remaining
  def scheduleCloudlets(timeSliceInfo: TimeSliceInfo,
                        vmResources: VmRequirement,
                        cloudlets:Seq[CloudletExecution]): Seq[CloudletExecution]
}
