package com.albertsilva.dev.dscatalog.dto.category.request;

/**
 * DTO utilizado para requisições de criação de categoria.
 *
 * <p>
 * Representa os dados que o cliente deve fornecer para cadastrar
 * uma nova categoria no sistema.
 * </p>
 *
 * <p>
 * <b>Observações:</b>
 * </p>
 * <ul>
 * <li>O campo {@code active} é opcional</li>
 * <li>Caso {@code active} não seja informado, será considerado
 * {@code false}</li>
 * </ul>
 *
 * @param name        nome da categoria
 * @param description descrição da categoria
 * @param active      indica se a categoria será criada como ativa
 */
public record CategoryCreateRequest(String name, String description) {
}