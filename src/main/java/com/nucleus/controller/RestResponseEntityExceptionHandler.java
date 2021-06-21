package com.nucleus.controller;

import com.nucleus.exception.NucleusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(value = {NucleusException.class})
  protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
    return handleException(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
  }

  private ResponseEntity<Object> handleException(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatus status,
      WebRequest request) {
    log.error(ex.getMessage(), ex);
    return handleExceptionInternal(ex, body, headers, status, request);
  }
}
