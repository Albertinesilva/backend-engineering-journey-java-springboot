package com.albertsilva.dev.asjcatalog.dto.user.response;

import java.util.Set;

import com.albertsilva.dev.asjcatalog.dto.role.response.RoleResponse;

/**
 * DTO de resposta para detalhes de um usuário.
 *
 * <p>
 * Este record representa os dados detalhados de um usuário que serão
 * retornados em respostas de API. Ele contém os seguintes campos:
 *
 * <ul>
 * <li>{@code id}: Identificador único do usuário</li>
 * <li>{@code firstName}: Primeiro nome do usuário</li>
 * <li>{@code lastName}: Sobrenome do usuário</li>
 * <li>{@code email}: Endereço de email do usuário</li>
 * <li>{@code roles}: Conjunto de roles associadas ao usuário</li>
 * </ul>
 *
 * <p>
 * Exemplo de uso:
 *
 * <pre>{@code
 * Set<RoleResponse> roles = Set.of(new RoleResponse(1L, "ROLE_USER"));
 * UserDetailsResponse user = new UserDetailsResponse(1L, "John", "Doe", "john.doe@example.com", roles);
 * }</pre>
 *
 * @param id        Identificador único do usuário
 * @param firstName Primeiro nome do usuário
 * @param lastName  Sobrenome do usuário
 * @param email     Endereço de email do usuário
 * @param roles     Conjunto de roles associadas ao usuário
 */
public record UserDetailsResponse(
                Long id,
                String firstName,
                String lastName,
                String email,
                Set<RoleResponse> roles,
                boolean active) {
}