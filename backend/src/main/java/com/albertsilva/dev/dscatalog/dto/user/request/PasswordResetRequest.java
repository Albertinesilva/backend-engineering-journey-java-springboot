package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da requisição de <b>redefinição de senha por token</b> (fluxo de
 * "esqueci minha senha", {@code POST /api/v1/accounts/reset-password}).
 *
 * <p>
 * Fluxo: {@code AccountController.resetPassword} →
 * {@code AccountService.resetPassword(token, password)}: o controller extrai os
 * dois campos e o DTO não chega ao service nem a mapper. O {@code token} é o
 * valor do token de recuperação enviado por e-mail e é validado por
 * {@code TokenService}/{@code Token.validate}; não é o JWT.
 * </p>
 *
 * <p>
 * <b>Dados sensíveis:</b> {@code token} (credencial de uso único) e
 * {@code password} (texto) só existem em request. <b>Validação estrutural:</b>
 * ambos obrigatórios; a senha tem {@code @StrongPassword}, mas <b>não há
 * {@code @Size}</b> nem verificação de dados pessoais, e a nova senha não é
 * comparada com a atual.
 * </p>
 *
 * @param token    valor do token de recuperação recebido por e-mail
 *                 (obrigatório)
 * @param password nova senha em texto (obrigatória; forte)
 */
public record PasswordResetRequest(

    @NotBlank(message = "{user.password.resetToken.notBlank}") 
    String token,

    @NotBlank(message = "{user.password.newPassword.notBlank}") 
    @StrongPassword 
    String password) {

}
