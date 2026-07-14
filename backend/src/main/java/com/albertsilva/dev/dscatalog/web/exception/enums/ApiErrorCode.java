// package com.albertsilva.dev.dscatalog.web.exception.enums;

// import io.swagger.v3.oas.annotations.media.Schema;

// @Schema(name = "ApiErrorCode", description = "Código estável do erro retornado pela API. Este código nunca muda de acordo com o idioma. O frontend deve utilizar este campo para tomada de decisão, enquanto a mensagem apresentada ao usuário deve utilizar o campo message.")
// public enum ApiErrorCode {

//   @Schema(description = "Erro de validação dos dados enviados")
//   VALIDATION_ERROR,

//   @Schema(description = "Recurso não encontrado")
//   RESOURCE_NOT_FOUND,

//   @Schema(description = "Erro relacionado ao banco de dados")
//   DATABASE_ERROR,

//   @Schema(description = "Conflito de dados")
//   CONFLICT,

//   @Schema(description = "Token inválido ou expirado")
//   INVALID_TOKEN,

//   @Schema(description = "Conta desativada")
//   ACCESS_DISABLED,

//   @Schema(description = "Usuário sem permissão")
//   ACCESS_DENIED,

//   @Schema(description = "Usuário não autenticado")
//   AUTHENTICATION_REQUIRED,

//   @Schema(description = "Erro interno inesperado")
//   INTERNAL_SERVER_ERROR
// }