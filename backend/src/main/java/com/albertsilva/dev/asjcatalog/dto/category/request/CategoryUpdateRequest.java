package com.albertsilva.dev.dscatalog.dto.category.request;

/**
 * DTO utilizado para requisições de atualização de categoria.
 *
 * <p>
 * Permite atualização parcial dos dados da categoria, onde apenas
 * os campos informados (não nulos) serão alterados.
 * </p>
 *
 * <p>
 * <b>Comportamento:</b>
 * </p>
 * <ul>
 * <li>Campos nulos são ignorados</li>
 * <li>Suporta atualização parcial (similar a PATCH)</li>
 * </ul>
 *
 * @param name        novo nome da categoria
 * @param description nova descrição da categoria
 * @param active      novo status da categoria
 */
public record CategoryUpdateRequest(String name, String description) {
}
