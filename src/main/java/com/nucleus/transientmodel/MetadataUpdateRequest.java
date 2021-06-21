package com.nucleus.transientmodel;

import java.util.List;
import java.util.Map;

public class MetadataUpdateRequest {

  private List<String> ids;
  private Map<String, Object> updates;

  public List<String> getIds() {
    return ids;
  }

  public void setIds(List<String> ids) {
    this.ids = ids;
  }

  public Map<String, Object> getUpdates() {
    return updates;
  }

  public void setUpdates(Map<String, Object> updates) {
    this.updates = updates;
  }
}
