package com.albertsilva.dev.dscatalog.dto.category.request;

import com.albertsilva.dev.dscatalog.validation.category.annotation.CategoryUpdateValid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de <b>atualização de categoria</b>
 * ({@code PATCH /api/v1/categories/{id}}).
 *
 * <p>
 * Fluxo: {@code CategoryController.update} → {@code CategoryService.update} →
 * {@code CategoryMapper.updateEntity}. As regras estruturais são as mesmas de
 * {@link CategoryCreateRequest}; a unicidade do nome, excluindo a própria
 * categoria (o {@code id} vem da URL), é verificada por
 * {@link CategoryUpdateValid}.
 * </p>
 *
 * <p>
 * <b>Semântica de atualização:</b> o mapper só sobrescreve um campo quando ele
 * é diferente de {@code null}. Como o nome é obrigatório, ele sempre é
 * atualizado; {@code description} {@code null} mantém a descrição atual, e
 * {@code ""} a substitui por texto vazio. Não há como limpar a descrição
 * enviando {@code null}. O indicador {@code active} não é alterado por este
 * request.
 * </p>
 *
 * @param name        novo nome (obrigatório; 3 a 80 caracteres; letras, dígitos
 *                    e espaços)
 * @param description nova descrição (opcional; {@code null} mantém a atual)
 */
@CategoryUpdateValid
public record CategoryUpdateRequest(

    @NotBlank(message = "{category.name.notBlank}") 
    @Size(min = 3, max = 80, message = "{category.name.size}") 
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s]+$", message = "{category.name.pattern}") 
    String name,

    @Pattern(regexp = "^$|^.{3,255}$", message = "{category.description.size}") 
    String description) {
}
