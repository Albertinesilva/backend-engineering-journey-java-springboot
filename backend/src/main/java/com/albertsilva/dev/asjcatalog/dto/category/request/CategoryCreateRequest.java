package com.albertsilva.dev.asjcatalog.dto.category.request;

import com.albertsilva.dev.asjcatalog.validation.category.annotation.CategoryCreateValid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para requisições de criação de categorias.
 *
 * <p>
 * Representa os dados fornecidos pelo cliente
 * durante o processo de cadastro de uma nova
 * categoria no sistema.
 *
 * <p>
 * As validações aplicadas garantem:
 * <ul>
 * <li>Obrigatoriedade do nome da categoria</li>
 * <li>Tamanho mínimo e máximo permitido</li>
 * <li>Validação de caracteres permitidos</li>
 * <li>Validação do tamanho da descrição</li>
 * </ul>
 *
 * <p>
 * As regras de validação utilizam Bean Validation
 * através das annotations presentes nos atributos
 * do record.
 *
 * @param name        nome da categoria
 * @param description descrição da categoria
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
