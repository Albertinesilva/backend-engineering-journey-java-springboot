package com.albertsilva.dev.dscatalog.web.exception.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erro de validação de um campo.")
public record FieldMessage(

    @Schema(description = "Nome do campo inválido", example = "email") 
    String fieldName,

    @Schema(description = "Mensagem traduzida", example = "Email already exists") 
    String message

) {
}