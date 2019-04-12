package com.cloudsimulator.entities.policies.datacenterselection


class SimpleDataCenterSelectionPolicy extends DataCenterSelectionPolicy {
  /**
    * Selects the datacenter to handle the workload
    *
    * @param dcList - The list of datacenter actor ids
    * @return
    */

  override def selectDC(dcList: List[Long], excludeDcList: Seq[Long]): Option[Long] = {


    // Remove the DCs from the excluded list
    val filteredDCs = dcList
      .filterNot(dc =>
        excludeDcList.count(exDc => dc == exDc) > 0
      )

    if(filteredDCs.size > 0)
      Option(filteredDCs(0))
    else
      None
  }
}