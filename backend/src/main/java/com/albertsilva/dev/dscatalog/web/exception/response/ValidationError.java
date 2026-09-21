package com.albertsilva.dev.dscatalog.web.exception.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.dscatalog.web.exception.enums.ApiErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo de resposta do {@code 422} de validação: os campos de
 * {@link ProblemDetails} mais {@code fieldErrors}, uma lista de
 * {@link FieldMessage} (nome do campo e mensagem traduzida).
 *
 * <pre>
 * {
 *   "timestamp": "...", "status": 422, "code": "VALIDATION_ERROR",
 *   "error": "...", "message": "...", "path": "/api/v1/users",
 *   "fieldErrors": [ { "fieldName": "email", "message": "Email já cadastrado" } ]
 * }
 * </pre>
 *
 * <p>
 * Só erros de campo entram na lista; um mesmo campo pode aparecer várias vezes
 * (uma entrada por violação, por exemplo, as regras de senha). Não há valor
 * rejeitado na resposta.
 * </p>
 */
@Schema(description = "Resposta utilizada para erros de validação.")
public class ValidationError extends ProblemDetails {

  @Schema(description = "Lista de campos inválidos.")
  private final List<FieldMessage> fieldErrors = new ArrayList<>();

  /**
   * Construtor sem argumentos (lista de erros vazia).
   */
  public ValidationError() {
  }

  /**
   * Cria o corpo com a lista de erros vazia.
   */
  public ValidationError(Instant timestamp, Integer status, ApiErrorCode code, String error, String message,
      String path) {
    super(timestamp, status, code, error, message, path);
  }

  /**
   * @return lista (mutável) de erros de campo
   */
  public List<FieldMessage> getFieldErrors() {
    return fieldErrors;
  }

  /**
   * Acrescenta um erro de campo.
   *
   * @param fieldName nome do campo inválido
   * @param message   mensagem traduzida
   */
  public void addError(String fieldName, String message) {
    fieldErrors.add(new FieldMessage(fieldName, message));
  }

}
