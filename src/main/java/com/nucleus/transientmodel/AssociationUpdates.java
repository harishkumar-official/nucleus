package com.nucleus.transientmodel;

import java.util.List;

public class AssociationUpdates {

  private String refEntityName;
  private List<String> refEntityIds;

  public String getRefEntityName() {
    return refEntityName;
  }

  public void setRefEntityName(String refEntityName) {
    this.refEntityName = refEntityName;
  }

  public List<String> getRefEntityIds() {
    return refEntityIds;
  }

  public void setRefEntityIds(List<String> refEntityIds) {
    this.refEntityIds = refEntityIds;
  }

}
