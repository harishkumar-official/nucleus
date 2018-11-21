package com.nucleus.transientmodel;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SetDefaultRequest {

  public String id;
  public boolean defaultDoc;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isDefaultDoc() {
    return defaultDoc;
  }

  public void setDefaultDoc(boolean defaultDoc) {
    this.defaultDoc = defaultDoc;
  }

}
