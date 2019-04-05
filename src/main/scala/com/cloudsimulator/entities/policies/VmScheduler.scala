package com.cloudsimulator.entities.policies

trait VmScheduler {

  def scheduleVms(vmIds : List[Long])

}
