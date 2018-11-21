package com.nucleus.transientmodel;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class DeleteRequest extends UpdateRequest {

  private Integer arraySize;

  public Integer getArraySize() {
    return arraySize;
  }

  public void setArraySize(Integer arraySize) {
    this.arraySize = arraySize;
  }

}
