package com.albertsilva.dev.dscatalog.dto.user.response;

import java.util.Set;

import com.albertsilva.dev.dscatalog.dto.role.response.RoleResponse;

/**
 * Resposta de <b>detalhe de usuário</b> ({@code GET /api/v1/users/{id}}).
 *
 * <p>
 * Produzida por {@code UserMapper.toDetailsResponse}. Tem os campos de
 * {@link UserResponse} e acrescenta o indicador {@code active}. Continua sem
 * expor senha ou hash e tokens.
 * </p>
 *
 * @param id        identificador do usuário
 * @param firstName primeiro nome
 * @param lastName  sobrenome
 * @param email     e-mail (também usado como nome de usuário)
 * @param roles     roles do usuário (id e nome da autoridade)
 * @param active    indica se a conta está ativa
 */
public record UserDetailsResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    Set<RoleResponse> roles,
    boolean active) {
}