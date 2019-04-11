package com.cloudsimulator.utils


object ActorUtility {

  //  val rootPath = "akka://CloudSimRM/user/"
  val actorSystemName = "Cloud-System"
  val simulationActor = "SimulationActor"
  val cis = "cis"
  val switch = "switch-"
  val dc = "dc-"
  val host = "host"
  val vm = "vm-"
  val vmAllocationPolicy = "vm-allocation-policy"
  val vmScheduler = "vm-scheduler"
  val cloudletScheduler = "cloudlet-scheduler"


  def getActorRef(actorPath: String): String = {
    s"akka://$actorSystemName/user/$simulationActor/$actorPath"
  }

  def getDcRefString(): String = {
    s"akka://$actorSystemName/user/$simulationActor/$dc"
  }

  def getHostRefString(dcId: String): String = {
    s"akka://$actorSystemName/user/$simulationActor/$dc-$dcId/$host-"
  }

}
