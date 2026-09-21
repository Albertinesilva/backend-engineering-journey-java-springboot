package com.albertsilva.dev.dscatalog.dto.category.request;

import com.albertsilva.dev.dscatalog.validation.category.annotation.CategoryCreateValid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de <b>criação de categoria</b>
 * ({@code POST /api/v1/categories}).
 *
 * <p>
 * Fluxo: {@code CategoryController.create} → {@code CategoryService.create} →
 * {@code CategoryMapper.toEntity}. Os dois campos correspondem a colunas de
 * {@code Category}; o indicador {@code active} não faz parte do request (o
 * service cria a categoria ativa).
 * </p>
 *
 * <p>
 * <b>Validação estrutural (Bean Validation):</b> nome obrigatório, de 3 a 80
 * caracteres, apenas letras (inclusive acentuadas), dígitos e espaços;
 * descrição opcional. A unicidade do nome, sem diferenciar maiúsculas de
 * minúsculas, é verificada pelo validator de classe
 * {@link CategoryCreateValid}; o banco também impõe {@code UNIQUE}.
 * </p>
 *
 * @param name        nome da categoria (obrigatório; 3 a 80 caracteres; letras,
 *                    dígitos e espaços)
 * @param description descrição (opcional). {@code null} e {@code ""} são
 *                    aceitos; se preenchida, deve ter de 3 a 255 caracteres e
 *                    não pode conter quebras de linha
 */
@CategoryCreateValid
public record CategoryCreateRequest(

    @NotBlank(message = "{category.name.notBlank}") 
    @Size(min = 3, max = 80, message = "{category.name.size}") 
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s]+$", message = "{category.name.pattern}") 
    String name,

    @Pattern(regexp = "^$|^.{3,255}$", message = "{category.description.size}") 
    String description) {
}
