import akka.actor.{ActorSystem, Props}
import com.cloudsimulator.config.Config
import com.cloudsimulator.utils.ActorUtility
import com.cloudsimulator.{SimulationActor, Start}
import pureconfig.generic.auto._

object MainApp extends App {

  val system = ActorSystem(ActorUtility.actorSystemName)

  val config = Config.loadConfig.get

  val simulationActor = system.actorOf(
    Props(new SimulationActor(ActorUtility.simActorId)),
    ActorUtility.simulationActor)

  simulationActor ! Start

}
