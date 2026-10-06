package com.albertsilva.dev.dscatalog.web.exception.enums;

public enum ErrorType {

  RESOURCE_NOT_FOUND("Resource not found"),
  DATABASE_ERROR("Database error"),
  CONFLIT("Conflict"),
  INTERNAL_SERVER_ERROR("Internal server error"),
  UNEXPECTED_ERROR_OCCURRED("Unexpected error occurred");

  private final String message;

  ErrorType(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}
