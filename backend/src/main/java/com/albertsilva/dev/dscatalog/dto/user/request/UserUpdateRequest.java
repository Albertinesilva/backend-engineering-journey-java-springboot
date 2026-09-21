package com.albertsilva.dev.dscatalog.dto.user.request;

import java.util.Set;

import com.albertsilva.dev.dscatalog.validation.role.annotation.ValidRoles;
import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UserUpdateValid;
import com.albertsilva.dev.dscatalog.validation.user.annotation.ValidEmail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de <b>atualização de usuário</b>
 * ({@code PUT /api/v1/users/{id}}).
 *
 * <p>
 * Fluxo: {@code UserController.update} → {@code UserService.update}. O
 * {@code UserMapper.updateEntity} copia apenas {@code firstName},
 * {@code lastName} e {@code email}, <b>sem verificar {@code null} e sem
 * normalizar</b> (o DTO já os exige não vazios). {@code password} e
 * {@code roleIds} são tratados pelo próprio service:
 * </p>
 * <ul>
 * <li>{@code password}: {@code null} mantém a senha atual; caso contrário é
 * codificada e gravada</li>
 * <li>{@code roleIds}: {@code null} mantém as roles; um conjunto (mesmo vazio)
 * substitui as atuais, e um conjunto vazio remove todas</li>
 * </ul>
 * <p>
 * O indicador {@code active} não faz parte deste request.
 * </p>
 *
 * <p>
 * <b>Validação estrutural:</b> nomes de 2 a 80 caracteres; e-mail com
 * {@code @ValidEmail} (sem {@code @UniqueEmail}: a unicidade, excluindo o
 * próprio usuário, e a regra de dados pessoais na senha são verificadas por
 * {@link UserUpdateValid}); {@code password} opcional com
 * {@code @StrongPassword}, <b>sem {@code @Size}</b> (o limite de 10 a 72
 * caracteres de {@link UserCreateRequest} não se aplica aqui); {@code roleIds}
 * com {@code @ValidRoles}.
 * </p>
 *
 * <p>
 * <b>Dado sensível:</b> {@code password} só existe em request e seu
 * {@code toString()} gerado a inclui.
 * </p>
 *
 * @param firstName novo primeiro nome (obrigatório; 2 a 80 caracteres)
 * @param lastName  novo sobrenome (obrigatório; 2 a 80 caracteres)
 * @param email     novo e-mail (obrigatório e válido)
 * @param password  nova senha em texto (opcional; {@code null} mantém a atual)
 * @param roleIds   ids das roles (opcional; {@code null} mantém as atuais)
 */
@UserUpdateValid
public record UserUpdateRequest(

    @NotBlank(message = "{user.firstName.notBlank}") 
    @Size(min = 2, max = 80, message = "{user.firstName.size}") 
    String firstName,

    @NotBlank(message = "{user.lastName.notBlank}") 
    @Size(min = 2, max = 80, message = "{user.lastName.size}") 
    String lastName,

    @NotBlank(message = "{user.email.notBlank}") 
    @ValidEmail 
    String email,

    @StrongPassword 
    String password,

    @ValidRoles 
    Set<Long> roleIds) {
}
