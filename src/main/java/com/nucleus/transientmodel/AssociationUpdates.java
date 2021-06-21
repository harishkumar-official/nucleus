package com.nucleus.transientmodel;

import java.util.List;
import java.util.Map;

public class AssociationUpdates {

  private String parentEntityName;
  private String refEntityName;
  private List<Map<String, Object>> refEntityIdsMapList;

  public String getParentEntityName() {
    return parentEntityName;
  }

  public void setParentEntityName(String parentEntityName) {
    this.parentEntityName = parentEntityName;
  }

  public String getRefEntityName() {
    return refEntityName;
  }

  public void setRefEntityName(String refEntityName) {
    this.refEntityName = refEntityName;
  }

  public List<Map<String, Object>> getRefEntityIdsMapList() {
    return refEntityIdsMapList;
  }

  public void setRefEntityIdsMapList(List<Map<String, Object>> refEntityIdsMapList) {
    this.refEntityIdsMapList = refEntityIdsMapList;
  }
}
