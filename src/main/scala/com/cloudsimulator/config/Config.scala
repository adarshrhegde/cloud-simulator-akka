package com.cloudsimulator.config

import com.cloudsimulator.entities.payload.cloudlet.CloudletPayload
import com.cloudsimulator.entities.payload.VMPayload
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

case class Config(rootSwitchList : List[SwitchConfig], dataCenterList : List[DataCenterConfig],
                  hostList : List[HostConfig], cis : CISConfig,
                  vmPayloadList : List[VMPayload], cloudletPayloadList: List[CloudletPayload])

object Config {

  def loadConfig : Option[Config] = {

    val config : Either[ConfigReaderFailures, Config] = pureconfig.loadConfig[Config]
    config.fold(
      l => {
        System.out.println("Error in loading configuration" + l.head.description)
        None
      },
      r => {
        Option(r)
      }
    )

  }
}


case class DataCenterConfig(id : Long, location : String, switchList : List[SwitchConfig])

/*case class VmConfig(id : Long, userId : Long, cloudletScheduler : String, mips : Long,
                   noOfPes : Int, ram : Int, bw : Double)*/

case class CISConfig(id : Long)

case class HostConfig(id : Long, dataCenterId : Long, hypervisor : String, bwProvisioner : String,
                     ramProvisioner : String, vmScheduler : String, availableNoOfPes : Int, mips: Long,
                      availableRam : Long, availableStorage : Long, availableBw : Double, edgeSwitch : String)


case class Connection(id : String, connectionType : String)
case class SwitchConfig(id : Long, switchType : String, upstreamConnections : List[Connection], downstreamConnections : List[Connection])