package com.cloudsimulator.utils


object ActorUtility {

  //  val rootPath = "akka://CloudSimRM/user/"
  val actorSystemName = "Cloud-System"
  val simulationActor = "SimulationActor"
  val timeActor = "time-actor"
  val cis = "CIS"
  val switch = "switch-"
  val dc = "dc-"
  val host = "host"
  val vm = "vm-"
  val rootSwitch = "root-"
  val vmAllocationPolicy = "vm-allocation-policy"
  val vmScheduler = "vm-scheduler"
  val cloudletScheduler = "cloudlet-scheduler"
  val edgeSwitch = "edge-"
  val aggregateSwitch = "aggregate-"
  val cloudletPrintActor ="cloudlet-print-actor"

  val simActorId=1


  def getActorRef(actorPath: String): String = {
    s"akka://$actorSystemName/user/$simulationActor/$actorPath"
  }

  def getDcRefString(): String = {
    s"akka://$actorSystemName/user/$simulationActor/$dc"
  }

  def getRootSwitchRefString() : String = {
    s"akka://$actorSystemName/user/$simulationActor/$rootSwitch"
  }

  def getEdgeSwitchRefString(dcName : String) : String = {
    s"akka://$actorSystemName/user/$simulationActor/$dcName/$edgeSwitch"
  }

  def getAggregateSwitchRefString(dcName : String) : String = {
    s"akka://$actorSystemName/user/$simulationActor/$dcName/$aggregateSwitch"
  }

  def getHostRefString(dcId: String): String = {
    s"akka://$actorSystemName/user/$simulationActor/$dc-$dcId/$host-"
  }

  def getSimulatorRefString() : String = {
    s"akka://$actorSystemName/user/$simulationActor"
  }

}
