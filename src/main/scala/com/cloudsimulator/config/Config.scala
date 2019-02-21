package com.cloudsimulator.config

import com.cloudsimulator.entities.{CIS, CISI}
import com.cloudsimulator.entities.datacenter.DataCenter
import com.cloudsimulator.entities.host.Host
import com.typesafe.config.ConfigFactory
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._

//case class Config(dataCenterList : List[DataCenter], hostList : List[Host], cis : CISI)

case class Config(config: String)
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
