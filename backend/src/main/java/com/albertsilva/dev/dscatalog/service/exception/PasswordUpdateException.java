package com.albertsilva.dev.dscatalog.service.exception;

public class PasswordUpdateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PasswordUpdateException(String message) {
    super(message);
  }
}