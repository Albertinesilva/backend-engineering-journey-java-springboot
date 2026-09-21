package com.albertsilva.dev.dscatalog.web.exception.response;

import java.io.Serializable;
import java.time.Instant;

import com.albertsilva.dev.dscatalog.web.exception.enums.ApiErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo padrão das respostas de erro da API. É uma classe <b>própria</b> do
 * projeto: não é {@code org.springframework.http.ProblemDetail} e não segue o
 * RFC 7807 (não há {@code type}, {@code title}, {@code detail} nem
 * {@code instance}).
 *
 * <p>
 * <b>Campos (JSON):</b>
 * </p>
 * <ul>
 * <li>{@code timestamp}: instante da resposta ({@code Instant});</li>
 * <li>{@code status}: código HTTP numérico;</li>
 * <li>{@code code}: {@link ApiErrorCode}, estável entre idiomas (pode ser
 * {@code null} se o construtor sem {@code code} for usado);</li>
 * <li>{@code error}: título traduzido;</li>
 * <li>{@code message}: detalhe traduzido;</li>
 * <li>{@code path}: URI da requisição ({@code getRequestURI()}).</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 * </p>
 *
 * <pre>
 * {
 *   "timestamp": "2026-01-01T10:00:00Z",
 *   "status": 404,
 *   "code": "RESOURCE_NOT_FOUND",
 *   "error": "Recurso não encontrado",
 *   "message": "Usuário não encontrado",
 *   "path": "/api/v1/users/99"
 * }
 * </pre>
 *
 * <p>
 * Respostas de erro geradas <em>antes</em> do controller pela cadeia de
 * segurança (por exemplo, token ausente ou inválido) não usam este formato.
 * </p>
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

  /**
   * Construtor sem argumentos (todos os campos nulos).
   */
  public ProblemDetails() {
  }

  /**
   * Cria o corpo <b>sem</b> o {@code code}.
   */
  public ProblemDetails(Instant timestamp, Integer status, String error, String message, String path) {
    this.timestamp = timestamp;
    this.status = status;
    this.error = error;
    this.message = message;
    this.path = path;
  }

  /**
   * Cria o corpo completo (usado por {@code ControllerExceptionHandler}).
   */
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
