package com.albertsilva.dev.dscatalog.web.exception.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Códigos estáveis de erro devolvidos no campo {@code code} de
 * {@link com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails}.
 * Não variam com o idioma; cada um é produzido por um único tipo de
 * exceção em {@code ControllerExceptionHandler} (exceto
 * {@link #RESOURCE_NOT_FOUND}, usado por duas).
 */
@Schema(name = "ApiErrorCode", description = "Código estável do erro retornado pela API. Este código nunca muda de acordo com o idioma. O frontend deve utilizar este campo para tomada de decisão, enquanto a mensagem apresentada ao usuário deve utilizar o campo message.")
public enum ApiErrorCode {

  /** {@code MethodArgumentNotValidException} (422). */
  @Schema(description = "Erro de validação dos dados enviados")
  VALIDATION_ERROR,

  /** {@code PasswordUpdateException} (422). */
  @Schema(description = "Erro ao atualizar a senha do usuário")
  PASSWORD_UPDATE_ERROR,

  /** {@code ResourceNotFoundException} e {@code NoResourceFoundException} (404). */
  @Schema(description = "Recurso não encontrado")
  RESOURCE_NOT_FOUND,

  /** {@code DatabaseException} (400). */
  @Schema(description = "Erro relacionado ao banco de dados")
  DATABASE_ERROR,

  /** {@code DataIntegrityViolationException} (409). */
  @Schema(description = "Conflito de dados")
  CONFLICT,

  /** {@code InvalidTokenException}: token de ativação ou recuperação (400). */
  @Schema(description = "Token inválido ou expirado")
  INVALID_TOKEN,

  /** {@code DisabledException} (403); provavelmente inalcançável hoje. */
  @Schema(description = "Conta desativada")
  ACCESS_DISABLED,

  /** {@code AccessDeniedException} de {@code @PreAuthorize} (403). */
  @Schema(description = "Usuário sem permissão")
  ACCESS_DENIED,

  /** {@code AuthenticatedUserNotFoundException} (401). */
  @Schema(description = "Usuário não autenticado")
  AUTHENTICATION_REQUIRED,

  /** Qualquer {@code Exception} sem handler específico (500). */
  @Schema(description = "Erro interno inesperado")
  INTERNAL_SERVER_ERROR
}