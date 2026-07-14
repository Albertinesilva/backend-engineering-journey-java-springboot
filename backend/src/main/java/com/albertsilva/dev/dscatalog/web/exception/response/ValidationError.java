package com.albertsilva.dev.dscatalog.web.exception.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.dscatalog.web.exception.enums.ApiErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta utilizada para erros de validação.")
public class ValidationError extends ProblemDetails {

  @Schema(description = "Lista de campos inválidos.")
  private final List<FieldMessage> fieldErrors = new ArrayList<>();

  public ValidationError() {
  }

  public ValidationError(Instant timestamp, Integer status, ApiErrorCode code, String error, String message,
      String path) {
    super(timestamp, status, code, error, message, path);
  }

  public List<FieldMessage> getFieldErrors() {
    return fieldErrors;
  }

  public void addError(String fieldName, String message) {
    fieldErrors.add(new FieldMessage(fieldName, message));
  }

}
