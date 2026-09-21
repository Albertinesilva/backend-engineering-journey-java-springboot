package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmailForAuthenticatedUser;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição em que o <b>usuário autenticado atualiza o próprio
 * perfil</b> ({@code PUT /api/v1/accounts/me}).
 *
 * <p>
 * Fluxo: {@code AccountController.updateAuthenticatedUser} →
 * {@code AccountService.updateAuthenticatedUser}, que identifica o usuário pelo
 * claim {@code userId} do JWT (o request não carrega id) e <b>não usa
 * {@code UserMapper}</b>: o próprio service grava os campos, aplicando
 * {@code trim} a nome e sobrenome e {@code trim} + minúsculas ao e-mail.
 * </p>
 *
 * <p>
 * <b>Validação estrutural:</b> nome e sobrenome obrigatórios, com até 100
 * caracteres (limite diferente dos 80 de {@link UserCreateRequest}); e-mail
 * obrigatório, com o {@code @Email} padrão do Jakarta Validation (menos rígido
 * que {@code @ValidEmail}: não consulta DNS), até 255 caracteres, e
 * {@link com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmailForAuthenticatedUser}
 * (não pode pertencer a outro usuário). Este request não tem senha, roles nem
 * o indicador {@code active}.
 * </p>
 *
 * @param firstName novo primeiro nome (obrigatório; até 100 caracteres)
 * @param lastName  novo sobrenome (obrigatório; até 100 caracteres)
 * @param email     novo e-mail (obrigatório, válido, até 255 caracteres, único)
 */
public record AuthenticatedUserUpdateRequest(

  @NotBlank(message = "{user.firstName.notBlank}")
  @Size(max = 100, message = "{user.firstName.size}")
  String firstName,

  @NotBlank(message = "{user.lastName.notBlank}")
  @Size(max = 100, message = "{user.lastName.size}")
  String lastName,

  @NotBlank(message = "{user.email.notBlank}")
  @Email(message = "{user.email.invalid}")
  @Size(max = 255, message = "{user.email.size}")
  @UniqueEmailForAuthenticatedUser
  String email) {
}
