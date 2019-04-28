package com.cloudsimulator.entities.payload.cloudlet

import com.cloudsimulator.entities.time.TimeSliceUsage

case class CloudletExecution(id: Long,
                             cloudletPayload: CloudletPayload,
                             timeSliceUsageInfo: Seq[TimeSliceUsage],
                             delay: Long,
                             dcId: Long,
                             hostId: Long,
                             vmId: Long,
                             execStartTime: Long,
                             execEndTime: Long,
                             status: Int,
                             remWorkloadLength: Double,
                             actualWorkloadLength:Double,
                             cost: Double
                            )

