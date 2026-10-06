package com.albertsilva.dev.asjcatalog.dto.role.response;

/**
 * DTO de resposta para informações de Role.
 *
 * <p>
 * Este record representa os dados de uma Role que serão
 * retornados em respostas de API. Ele contém os seguintes campos:
 *
 * <ul>
 * <li>{@code id}: Identificador único da Role</li>
 * <li>{@code authority}: Nome da autoridade (ex: "ROLE_ADMIN")</li>
 * </ul>
 *
 * <p>
 * Exemplo de uso:
 *
 * <pre>{@code
 * RoleResponse role = new RoleResponse(1L, "ROLE_USER");
 * }</pre>
 *
 * @param id        Identificador único da Role
 * @param authority Nome da autoridade (ex: "ROLE_ADMIN")
 */
public record RoleResponse(
        Long id,
        String authority) {
}
