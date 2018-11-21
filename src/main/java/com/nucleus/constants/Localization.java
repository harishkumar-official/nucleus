package com.nucleus.constants;

import java.util.ArrayList;
import java.util.List;

public enum Localization {

  en, fr, sp;

  private static List<String> values;

  static {
    values = new ArrayList<String>();
    for (final Localization env : Localization.values()) {
      values.add(env.name());
    }
  }
  
  public static List<String> allValues() {
    return values;
  }
}
