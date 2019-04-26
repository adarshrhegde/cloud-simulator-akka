package com.cloudsimulator.entities.payload

import com.cloudsimulator.entities.time.TimeSliceUsage


trait Payload {
  val payloadId: Long
  val userId: Long
}

case class VMPayload(payloadId: Long, userId: Long, numberOfPes: Int,
                     bw: Double, mips: Long, ram: Long, storage: Long, guestOs: String, cloudletScheduler: String) extends Payload
