package com.cloudsimulator.entities.policies


trait DataCenterSelectionPolicy {

  /**
    * Selects the datacenter to handle the workload
    * @param dcList - The list of datacenter actor ids
    * @return
    */
  def selectDC(dcList : List[Long], excludeDcList: Seq[Long]) : Option[Long]


}