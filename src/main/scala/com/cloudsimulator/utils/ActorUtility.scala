package com.cloudsimulator.utils


object ActorUtility {

  val actorSystemName = "Cloud-System"
  val simulationActor = "SimulationActor"
  val rootPath = "akka://CloudSimRM/user/"

  def getActorRef(actorPath: String): String = {
    s"akka://$actorSystemName/user/$simulationActor/$actorPath"
  }

}
