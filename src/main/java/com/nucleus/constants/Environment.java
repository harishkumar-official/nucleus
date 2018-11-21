package com.nucleus.constants;

import java.util.ArrayList;
import java.util.List;

public enum Environment {

  development, stage, production;

  private static List<String> values;

  static {
    values = new ArrayList<String>();
    for (final Environment env : Environment.values()) {
      values.add(env.name());
    }
  }
  
  public static List<String> allValues() {
    return values;
  }
}
