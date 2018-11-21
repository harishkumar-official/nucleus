package com.nucleus.transientmodel;

import java.util.Map;

public class MetadataCreateRequest {

  public String localization;
  public Map<String, Object> metadata;

  public String getLocalization() {
    return localization;
  }

  public void setLocalization(String localization) {
    this.localization = localization;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

}
