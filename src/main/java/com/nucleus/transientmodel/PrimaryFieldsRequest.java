package com.nucleus.transientmodel;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties("ids")
public class PrimaryFieldsRequest extends UpdateRequest {

  private String id;
  private Map<String, Object> globalFields;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Map<String, Object> getGlobalFields() {
    return globalFields;
  }

  public void setGlobalFields(Map<String, Object> globalFields) {
    this.globalFields = globalFields;
  }

}
