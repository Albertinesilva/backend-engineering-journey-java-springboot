package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição em que o <b>usuário autenticado troca a própria
 * senha</b> ({@code PATCH /api/v1/accounts/me/password}).
 *
 * <p>
 * Fluxo: {@code AccountController.updatePassword} →
 * {@code AccountService.updatePassword}, sem passar por mapper. As verificações
 * cruzadas entre os campos são feitas no service, na ordem: nova senha igual à
 * confirmação; senha atual confere com o hash; nova senha diferente da atual.
 * </p>
 *
 * <p>
 * <b>Dados sensíveis:</b> os três campos são senhas em texto e só existem neste
 * request. <b>Validação estrutural:</b> {@code currentPassword} apenas não
 * vazia; {@code newPassword} de 10 a 72 caracteres com
 * {@code @StrongPassword}; {@code confirmPassword} de 10 a 72 caracteres (sem
 * {@code @StrongPassword}: a igualdade com {@code newPassword} é verificada no
 * service). Não há verificação de dados pessoais na senha.
 * </p>
 *
 * @param currentPassword senha atual em texto (obrigatória)
 * @param newPassword     nova senha (obrigatória; 10 a 72 caracteres; forte)
 * @param confirmPassword confirmação da nova senha (obrigatória; 10 a 72
 *                        caracteres)
 */
public record PasswordUpdateRequest(

    @NotBlank(message = "{user.password.blank}") 
    String currentPassword,

    @NotBlank(message = "{user.password.blank}") 
    @Size(min = 10, max = 72, message = "{user.password.length}")
    @StrongPassword 
    String newPassword,

    @NotBlank(message = "{user.password.blank}")
    @Size(min = 10, max = 72, message = "{user.password.length}") 
    String confirmPassword) {

}
