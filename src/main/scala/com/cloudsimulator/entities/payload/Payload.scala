package com.cloudsimulator.entities.payload

import com.cloudsimulator.entities.time.TimeSliceUsage


trait Payload {
  val payloadId: Long
  val userId: Long
}

case class CloudletPayload(payloadId: Long, userId: Long, /*cloudletId: Long,*/
                           var dcId: Long, var hostId: Long, vmId: Long,
                           numberOfPes: Int,
                           workloadLength: Long,
                           var status: Int, var execStartTime: Double, var execEndTime: Double/*,var timeSliceUsageInfo: List[TimeSliceUsage]*/) extends Payload

case class VMPayload(payloadId: Long, userId: Long, numberOfPes: Int,
                     bw: Double, mips: Long, ram: Long, storage: Long, guestOs: String, cloudletScheduler: String) extends Payload

// TODO : CloudletScheduler, Host, inMigration in VM Actor