package com.nucleus.transientmodel;

import java.util.Map;

public class JsonRequest {

  public String environment;
  public String localization;
  public Map<String, Object> doc;

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getLocalization() {
    return localization;
  }

  public void setLocalization(String localization) {
    this.localization = localization;
  }

  public Map<String, Object> getDoc() {
    return doc;
  }

  public void setDoc(Map<String, Object> doc) {
    this.doc = doc;
  }

}
