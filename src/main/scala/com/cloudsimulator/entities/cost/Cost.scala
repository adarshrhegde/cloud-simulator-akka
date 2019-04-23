package com.cloudsimulator.entities.cost


/**
  * Cost is in USD ($)
  * Associated with the host.
  *
  * @param vmCostPerSec                Vm cost Helps in calculating cost at VM level (like AWS EC2)
  * @param cloudletExecutionCostPerSec Cloudlet cost Helps in calculating for SaaS.
  */
case class Cost(vmCostPerSec: Double,
                cloudletExecutionCostPerSec: Double)
