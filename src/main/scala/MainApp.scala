import akka.actor.{ActorRef, ActorSystem, Props}
import com.cloudsimulator.{SimulationActor, Start}
import com.cloudsimulator.config.{CISConfig, Config}

import com.cloudsimulator.utils.ActorUtility
import pureconfig.generic.auto._

object MainApp extends App {
  val system = ActorSystem(ActorUtility.actorSystemName)

  val config = Config.loadConfig.get

  System.out.println(config)

  val simulationActor = system.actorOf(Props(new SimulationActor(1)), ActorUtility.simulationActor)

  simulationActor ! Start


}
