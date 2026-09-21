package com.albertsilva.dev.dscatalog.dto.user.response;

import java.util.Set;

import com.albertsilva.dev.dscatalog.dto.role.response.RoleResponse;

/**
 * Resposta de <b>usuário</b> sem o indicador de ativação.
 *
 * <p>
 * Produzida por {@code UserMapper.toResponse} e devolvida na criação
 * ({@code POST /api/v1/users}), na listagem paginada e na atualização
 * ({@code PUT /api/v1/users/{id}}), no registro
 * ({@code POST /api/v1/accounts/register}) e nos endpoints do próprio perfil
 * ({@code GET} e {@code PUT /api/v1/accounts/me}).
 * </p>
 *
 * <p>
 * <b>Exposição de dados:</b> devolve identificador, nomes, e-mail e as roles
 * (aninhadas como {@link RoleResponse}). <b>Não devolve</b> a senha (nem o
 * hash), o indicador {@code active} (presente em {@link UserDetailsResponse}) nem
 * tokens ({@code User.tokens} não é mapeado). Como o mapper percorre
 * {@code User.getRoles()}, que é carregado sob demanda, listar usuários pode
 * disparar uma consulta adicional por usuário.
 * </p>
 *
 * @param id        identificador do usuário
 * @param firstName primeiro nome
 * @param lastName  sobrenome
 * @param email     e-mail (também usado como nome de usuário)
 * @param roles     roles do usuário (id e nome da autoridade)
 */
public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    Set<RoleResponse> roles) {

}
