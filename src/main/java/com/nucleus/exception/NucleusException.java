package com.nucleus.exception;

public class NucleusException extends RuntimeException {

  private static final long serialVersionUID = 7860562657250001553L;

  public NucleusException(String message) {
    super(message);
  }
  
  public NucleusException(Exception ex) {
    super(ex);
  }
  
  public NucleusException(String message, Exception ex) {
    super(message, ex);
  }
}
