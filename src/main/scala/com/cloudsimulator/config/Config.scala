package com.cloudsimulator.config

import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

case class Config(rootSwitch : SwitchConfig, dataCenterList : List[DataCenterConfig], hostList : List[HostConfig], cis : CISConfig)

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

case class VmConfig(id : Long, userId : Long, cloudletScheduler : String, mips : Long,
                   noOfPes : Int, ram : Int, bw : Double)

case class CISConfig(id : Long)

case class HostConfig(id : Long, dataCenterId : Long, hypervisor : String, bwProvisioner : String,
                     ramProvisioner : String, vmScheduler : String, availableNoOfPes : Int, nicCapacity: Double,
                      availableRam : Long, availableStorage : Long, availableBw : Double, edgeSwitch : String)


case class SwitchConfig(id : Long, switchType : String)