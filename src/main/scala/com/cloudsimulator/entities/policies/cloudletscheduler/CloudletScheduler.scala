package com.cloudsimulator.entities.policies.cloudletscheduler

import com.cloudsimulator.entities.cost.Cost
import com.cloudsimulator.entities.payload.cloudlet.CloudletExecution
import com.cloudsimulator.entities.time.TimeSliceInfo

trait CloudletScheduler {
  // 1. require the timeSliceInfo
  // 2. vm's current resources
  // 3. info of all cloudlets on this VM.
  // 4. pick the required cloudlet based on the policy and return the cloudets
  //    with time remaining
  //5. calculate the cost for execution of the cloudlet and append to its previous cost.
  def scheduleCloudlets(timeSliceInfo: TimeSliceInfo,
                        mips : Long,
                        noOfPes : Int,
                        cloudlets:Seq[CloudletExecution],cost: Cost): Seq[CloudletExecution]
}
