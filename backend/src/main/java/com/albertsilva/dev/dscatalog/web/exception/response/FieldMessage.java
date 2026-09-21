package com.albertsilva.dev.dscatalog.web.exception.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Par (nome do campo, mensagem) com dois usos: item de
 * {@code ValidationError.fieldErrors} na resposta {@code 422} e estrutura
 * temporária que os validators de classe ({@code Category*}, {@code Product*},
 * {@code UserUpdateValidator}, {@code PasswordPersonalDataValidator}) usam para
 * acumular erros antes de registrá-los no contexto do Bean Validation. Neste
 * segundo uso, {@code message} é uma chave {@code {...}} ainda não traduzida.
 *
 * @param fieldName nome do campo inválido
 * @param message   mensagem (traduzida na resposta; chave nos validators)
 */
@Schema(description = "Erro de validação de um campo.")
public record FieldMessage(

    @Schema(description = "Nome do campo inválido", example = "email") 
    String fieldName,

    @Schema(description = "Mensagem traduzida", example = "Email already exists") 
    String message

) {
}