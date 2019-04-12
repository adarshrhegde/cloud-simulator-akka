package com.cloudsimulator.entities.payload.cloudlet

import com.cloudsimulator.entities.payload.Payload


//Pureconfig requires case classes
case class CloudletPayload(payloadId: Long, userId: Long, /*cloudletId: Long,*/
                           vmId: Long,
                           var delay: Long,
                           var dcId: Long,
                           var hostId: Long,
                           numberOfPes: Int,
                           ram: Int,
                           storage: Int,
                           workloadLength: Long,
                           var status: Long
                           /*,var timeSliceUsageInfo: List[TimeSliceUsage]*/) extends Payload


// TODO : CloudletScheduler, Host, inMigration in VM Actor