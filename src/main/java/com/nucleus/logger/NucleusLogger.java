package com.nucleus.logger;

import org.apache.logging.log4j.LogManager;

public final class NucleusLogger {

  private static final String LOGGER_NAME = "TraceLogger";

  private NucleusLogger() {}

  private static String getMsg(Class callingClass, String msg) {
    return new StringBuilder(callingClass.getName()).append(": ").append(msg).toString();
  }

  public static void warn(String msg, Class callingClass) {
    LogManager.getLogger(LOGGER_NAME).warn(getMsg(callingClass, msg));
  }

  public static void info(String msg, Class callingClass) {
    LogManager.getLogger(LOGGER_NAME).info(getMsg(callingClass, msg));
  }

  public static void error(String msg, Class callingClass) {
    LogManager.getLogger(LOGGER_NAME).error(getMsg(callingClass, msg));
  }

  public static void debug(String msg, Class callingClass) {
    LogManager.getLogger(LOGGER_NAME).debug(getMsg(callingClass, msg));
  }

  public static void trace(String msg, Class callingClass) {
    LogManager.getLogger(LOGGER_NAME).trace(getMsg(callingClass, msg));
  }

  public static void fatal(String msg, Class callingClass) {
    LogManager.getLogger(LOGGER_NAME).fatal(getMsg(callingClass, msg));
  }
}
