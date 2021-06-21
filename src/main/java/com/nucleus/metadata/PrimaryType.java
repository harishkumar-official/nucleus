package com.nucleus.metadata;

import java.util.HashSet;
import java.util.Set;

public class PrimaryType {

  public static final String FILE = "file";
  public static final String STRING = "string";
  public static final String FLOAT = "float";
  public static final String DOUBLE = "double";
  public static final String INTEGER = "integer";
  public static final String LONG = "long";
  public static final String DATE = "date";
  public static final String BOOLEAN = "boolean";
  public static final String ENUM = "enum";
  public static final String ARRAY = "array";
  public static final String OBJECT = "object";

  public static final Set<String> SET = new HashSet<String>();

  static {
    SET.add(PrimaryType.FILE);
    SET.add(PrimaryType.STRING);
    SET.add(PrimaryType.FLOAT);
    SET.add(PrimaryType.DOUBLE);
    SET.add(PrimaryType.INTEGER);
    SET.add(PrimaryType.LONG);
    SET.add(PrimaryType.DATE);
    SET.add(PrimaryType.BOOLEAN);
    SET.add(PrimaryType.ENUM);
    SET.add(PrimaryType.ARRAY);
    SET.add(PrimaryType.OBJECT);
  }
}
