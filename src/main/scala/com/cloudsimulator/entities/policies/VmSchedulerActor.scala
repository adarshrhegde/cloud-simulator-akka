package com.cloudsimulator.entities.policies

import akka.actor.{Actor, ActorLogging}
import com.cloudsimulator.entities.network.NetworkPacket


/**
  * VmSchedulerActor
  * Decides the scheduling of the VMs on the host
  * @param vmScheduler
  */
class VmSchedulerActor(vmScheduler: VmScheduler) extends Actor with ActorLogging {

  private val vmActorPaths : Seq[String] = Seq()

  override def receive: Receive = {

    case ScheduleVms() => {}
  }
}

case class ScheduleVms() extends NetworkPacket