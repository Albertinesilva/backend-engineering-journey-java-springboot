package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.ValidEmail;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de requisições que só precisam identificar uma conta pelo e-mail:
 * <b>reenvio do e-mail de ativação</b>
 * ({@code POST /api/v1/accounts/resend-activation}) e <b>solicitação de
 * recuperação de senha</b> ({@code POST /api/v1/accounts/password-recovery}).
 *
 * <p>
 * Fluxo: {@code AccountController} extrai {@code email()} e chama
 * {@code AccountService.resendActivationEmail} ou
 * {@code requestPasswordRecovery}; o DTO não chega a service nem a mapper. Os
 * dois serviços procuram o usuário por comparação exata do e-mail e respondem
 * da mesma forma quando ele não existe.
 * </p>
 *
 * <p>
 * <b>Validação estrutural:</b> e-mail obrigatório e {@code @ValidEmail}
 * (formato e consulta DNS). Não há {@code @UniqueEmail}: aqui o e-mail deve
 * pertencer a uma conta existente (ou o pedido é ignorado silenciosamente).
 * </p>
 *
 * @param email e-mail da conta (obrigatório e válido)
 */
public record UserEmailRequest(

  @NotBlank(message = "{user.email.notBlank}") 
  @ValidEmail 
  String email) {
}
