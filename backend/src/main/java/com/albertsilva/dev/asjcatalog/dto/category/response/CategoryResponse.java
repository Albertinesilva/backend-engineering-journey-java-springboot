package com.albertsilva.dev.dscatalog.dto.category.response;

/**
 * DTO utilizado para retorno de dados de categoria na API.
 *
 * <p>
 * Contém apenas as informações que devem ser expostas ao cliente,
 * evitando o vazamento de detalhes internos da entidade.
 * </p>
 *
 * @param id          identificador da categoria
 * @param name        nome da categoria
 * @param description descrição da categoria
 * @param active      indica se a categoria está ativa
 */
public record CategoryResponse(Long id, String name, String description, boolean active) {
}