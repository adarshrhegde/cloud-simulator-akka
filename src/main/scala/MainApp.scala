import akka.actor.{ActorRef, ActorSystem, Props}
import com.cloudsimulator.{SimulationActor, Start}
import com.cloudsimulator.config.{CISConfig, Config}
import com.cloudsimulator.entities.CISActor
import com.cloudsimulator.entities.datacenter.DataCenterActor
import com.cloudsimulator.entities.host.HostActor
import com.cloudsimulator.utils.ActorUtility
import pureconfig.generic.auto._

object MainApp extends App {
  val system = ActorSystem(ActorUtility.actorSystemName)

  val config = Config.loadConfig.get

  System.out.println(config)

  val simulationActor = system.actorOf(Props(new SimulationActor(1)), ActorUtility.simulationActor)

  simulationActor ! Start
  /*val cisActor = system.actorOf(Props(new CISActor(config.cis.id)), "CIS")

  config.dataCenterList.foreach(dc => {
    system.actorOf(Props(new DataCenterActor(dc.id, List(), List(), dc.location, Map())), "dc-" + dc.id)
  })*/

  //  config.hostList.foreach(host => {
  //
  //    system.actorOf(Props(new HostActor(host.id, host.dataCenterId, host.hypervisor,
  //      List(), host.bwProvisioner, host.ramProvisioner, host.vmScheduler, host.noOfPes, host.nicCapacity)))
  //  })

}
