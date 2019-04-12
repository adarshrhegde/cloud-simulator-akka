package com.cloudsimulator.entities.payload.cloudlet

import com.cloudsimulator.entities.time.TimeSliceUsage

case class CloudletExecution(cloudletPayload: CloudletPayload,
                             timeSliceUsageInfo: Seq[TimeSliceUsage],
                             delay: Long,
                             dcId: Long,
                             hostId: Long,
                             execStartTime: Double,
                             execEndTime: Double,
                             status: Int,
                             remWorkloadLength: Long
                            )

// TODO cost for RAM, storage, MIPS
