package com.cloudsimulator.entities.policies

class SimpleDataCenterSelectionPolicy extends DataCenterSelectionPolicy {
  /**
    * Selects the datacenter to handle the workload
    *
    * @param dcList - The list of datacenter actor ids
    * @return
    */
  override def selectDC(dcList: List[Long], excludeDcList: Seq[Long]): Option[Long] = {

    Option(dcList
      .filterNot(dc =>
      excludeDcList.count(exDc => dc == exDc) > 0
    )(0))
  }
}