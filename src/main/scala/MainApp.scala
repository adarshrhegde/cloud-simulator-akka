import akka.actor.{ActorSystem, Props}
import com.cloudsimulator.config.Config
import com.cloudsimulator.entities.CIS
import com.cloudsimulator.entities.host.Host
import pureconfig.generic.auto._
object MainApp extends App {
  val system = ActorSystem("system1")

  //val config = Config.loadConfig

  System.out.println(cis)

}
