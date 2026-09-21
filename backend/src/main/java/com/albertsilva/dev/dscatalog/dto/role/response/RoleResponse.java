package com.albertsilva.dev.dscatalog.dto.role.response;

/**
 * Representação de uma role dentro das respostas de usuário
 * ({@link com.albertsilva.dev.dscatalog.dto.user.response.UserResponse} e
 * {@link com.albertsilva.dev.dscatalog.dto.user.response.UserDetailsResponse}).
 *
 * <p>
 * Não é devolvida sozinha por nenhum endpoint; é criada por
 * {@code UserMapper} a partir de {@code Role.getId()} e
 * {@code Role.getAuthority()}.
 * </p>
 *
 * @param id        identificador da role
 * @param authority nome da autoridade, já com o prefixo {@code ROLE_} (por
 *                  exemplo, {@code ROLE_ADMIN})
 */
public record RoleResponse(
        Long id,
        String authority) {
}
