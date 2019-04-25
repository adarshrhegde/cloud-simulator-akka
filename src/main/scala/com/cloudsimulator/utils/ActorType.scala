package com.cloudsimulator.utils


object ActorType extends Enumeration {

  type ActorType = Value
  val DATACENTER, HOST, SWITCH = Value

  def apply(actorType: String): ActorType.Value =
    ActorType.withName(actorType)
}

