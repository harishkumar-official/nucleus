package com.nucleus.logger;

import org.apache.logging.log4j.LogManager;

@SuppressWarnings("rawtypes")
public class NucleusLogger {

  private NucleusLogger() {}

  private static String getMsg(Class callingClass, String msg) {
    return new StringBuilder(callingClass.getName()).append(": ").append(msg).toString();
  }

  public static void warn(String msg, Class callingClass) {
    LogManager.getLogger(callingClass).warn(getMsg(callingClass, msg));
  }

  public static void info(String msg, Class callingClass) {
    LogManager.getLogger(callingClass).info(getMsg(callingClass, msg));
  }

  public static void error(String msg, Class callingClass) {
    LogManager.getLogger(callingClass).error(getMsg(callingClass, msg));
  }

  public static void debug(String msg, Class callingClass) {
    LogManager.getLogger(callingClass).debug(getMsg(callingClass, msg));
  }

  public static void trace(String msg, Class callingClass) {
    LogManager.getLogger(callingClass).trace(getMsg(callingClass, msg));
  }

  public static void fatal(String msg, Class callingClass) {
    LogManager.getLogger(callingClass).fatal(getMsg(callingClass, msg));
  }

}
