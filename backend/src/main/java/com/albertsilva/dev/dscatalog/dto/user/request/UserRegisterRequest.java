package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.PasswordPersonalData;
import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmail;
import com.albertsilva.dev.dscatalog.validation.user.annotation.ValidEmail;
import com.albertsilva.dev.dscatalog.validation.user.contract.PasswordPersonalDataCandidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de <b>registro público de conta</b>
 * ({@code POST /api/v1/accounts/register}).
 *
 * <p>
 * Fluxo: {@code AccountController.register} → {@code AccountService.register}
 * → {@code UserMapper.toEntity(UserRegisterRequest, Set<Role>)}. Tem os mesmos
 * campos e regras de {@link UserCreateRequest}, exceto {@code roleIds}: a role
 * é fixa ({@code ROLE_OPERATOR}, definida pelo service) e a conta nasce
 * <b>inativa</b>, até a confirmação por e-mail.
 * </p>
 *
 * <p>
 * <b>Dado sensível:</b> {@code password} é aceita em texto, apenas neste
 * request; o mapper a copia sem codificar e o service a codifica em seguida.
 * O e-mail é copiado como recebido (sem {@code trim} nem conversão de caixa).
 * </p>
 *
 * <p>
 * <b>Validação estrutural:</b> nomes de 2 a 80 caracteres; e-mail com
 * {@code @ValidEmail} (formato e DNS) e {@code @UniqueEmail}; senha de 10 a 72
 * caracteres com {@code @StrongPassword}; e {@link PasswordPersonalData} (a
 * senha não pode conter nome, sobrenome ou o prefixo do e-mail).
 * </p>
 *
 * @param firstName primeiro nome (obrigatório; 2 a 80 caracteres)
 * @param lastName  sobrenome (obrigatório; 2 a 80 caracteres)
 * @param email     e-mail (obrigatório, válido e não cadastrado)
 * @param password  senha em texto (obrigatória; 10 a 72 caracteres; forte)
 */
@PasswordPersonalData
public record UserRegisterRequest(
  
  @NotBlank(message = "{user.firstName.notBlank}") 
  @Size(min = 2, max = 80, message = "{user.firstName.size}") 
  String firstName,

  @NotBlank(message = "{user.lastName.notBlank}") 
  @Size(min = 2, max = 80, message = "{user.lastName.size}") 
  String lastName,

  @NotBlank(message = "{user.email.notBlank}") 
  @ValidEmail 
  @UniqueEmail 
  String email,

  @NotBlank(message = "{user.password.blank}")
  @Size(min = 10, max = 72, message = "{user.password.length}")
  @StrongPassword 
  String password) implements PasswordPersonalDataCandidate {
}
