package com.albertsilva.dev.asjcatalog.web.exception.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ValidationError extends ProblemDetails {

  private final List<FieldMessage> fieldErrors = new ArrayList<>();

  public ValidationError() {
  }

  public ValidationError(Instant timestamp, Integer status, String error, String message, String path) {
    super(timestamp, status, error, message, path);
  }

  public List<FieldMessage> getFieldErrors() {
    return fieldErrors;
  }

  public void addError(String fieldName, String message) {
    fieldErrors.add(new FieldMessage(fieldName, message));
  }

}
