package com.albertsilva.dev.dscatalog.web.exception.response;

import java.io.Serializable;
import java.time.Instant;

import com.albertsilva.dev.dscatalog.web.exception.enums.ApiErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Classe que representa o padrão de resposta de erro da API.
 * Implementação simplificada de resposta de erro da API.
 * Não segue integralmente o padrão RFC 7807 (Problem Details).
 *
 * <p>
 * Todos os erros retornados pela aplicação seguem esse formato,
 * garantindo consistência e previsibilidade para quem consome a API.
 * </p>
 *
 * <p>
 * <b>Campos:</b>
 * </p>
 * <ul>
 * <li><b>timestamp</b> - Momento em que o erro ocorreu</li>
 * <li><b>status</b> - Código HTTP (ex: 404, 400)</li>
 * <li><b>error</b> - Tipo do erro (resumo)</li>
 * <li><b>message</b> - Mensagem detalhada</li>
 * <li><b>path</b> - Endpoint que gerou o erro</li>
 * </ul>
 *
 * <p>
 * <b>Exemplo de retorno:</b>
 * </p>
 * 
 * <pre>
 * {
 *   "timestamp": "2025-01-01T10:00:00Z",
 *   "status": 404,
 *   "error": "Resource not found",
 *   "message": "Entity not found id: 1",
 *   "path": "/categories/1"
 * }
 * </pre>
 */
@Schema(name = "ProblemDetails", description = "Resposta padrão de erro da API.")
public class ProblemDetails implements Serializable {
  private static final long serialVersionUID = 1L;

  @Schema(description = "Momento em que o erro ocorreu", example = "2026-07-14T00:00:00Z")
  private Instant timestamp;

  @Schema(description = "Status HTTP", example = "404")
  private Integer status;

  @Schema(implementation = ApiErrorCode.class, description = "Código estável do erro")
  private ApiErrorCode code;

  @Schema(description = "Título do erro traduzido", example = "Resource not found")
  private String error;

  @Schema(description = "Mensagem traduzida")
  private String message;

  @Schema(description = "Endpoint que originou o erro", example = "/api/v1/products/15")
  private String path;

  public ProblemDetails() {
  }

  public ProblemDetails(Instant timestamp, Integer status, String error, String message, String path) {
    this.timestamp = timestamp;
    this.status = status;
    this.error = error;
    this.message = message;
    this.path = path;
  }

  public ProblemDetails(Instant timestamp, Integer status, ApiErrorCode code, String error, String message,
      String path) {
    this.timestamp = timestamp;
    this.status = status;
    this.code = code;
    this.error = error;
    this.message = message;
    this.path = path;
  }

  /**
   * Retorna o timestamp do erro.
   */
  public Instant getTimestamp() {
    return timestamp;
  }

  /**
   * Define o timestamp do erro.
   */
  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * Retorna o status HTTP.
   */
  public Integer getStatus() {
    return status;
  }

  /**
   * Define o status HTTP.
   */
  public void setStatus(Integer status) {
    this.status = status;
  }

  /**
   * Retorna o código do erro.
   */
  public ApiErrorCode getCode() {
    return code;
  }

  /**
   * Define o código do erro.
   */
  public void setCode(ApiErrorCode code) {
    this.code = code;
  }

  /**
   * Retorna o tipo do erro.
   */
  public String getError() {
    return error;
  }

  /**
   * Define o tipo do erro.
   */
  public void setError(String error) {
    this.error = error;
  }

  /**
   * Retorna a mensagem detalhada do erro.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Define a mensagem detalhada do erro.
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Retorna o caminho da requisição que gerou o erro.
   */
  public String getPath() {
    return path;
  }

  /**
   * Define o endpoint que gerou o erro.
   */
  public void setPath(String path) {
    this.path = path;
  }

}
