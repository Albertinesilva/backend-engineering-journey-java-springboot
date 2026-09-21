package com.albertsilva.dev.dscatalog.dto.user.request;

import java.util.Set;

import com.albertsilva.dev.dscatalog.validation.role.annotation.ValidRoles;
import com.albertsilva.dev.dscatalog.validation.user.annotation.PasswordPersonalData;
import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmail;
import com.albertsilva.dev.dscatalog.validation.user.annotation.ValidEmail;
import com.albertsilva.dev.dscatalog.validation.user.contract.PasswordPersonalDataCandidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de <b>criação de usuário por administrador</b>
 * ({@code POST /api/v1/users}).
 *
 * <p>
 * Fluxo: {@code UserController.create} → {@code UserService.create} →
 * {@code UserMapper.toEntity(UserCreateRequest, Set<Role>)}. O service resolve
 * {@code roleIds} em entidades e as passa ao mapper (que <b>não lê</b>
 * {@code roleIds}); o mapper copia nome, e-mail e senha <b>sem normalizar e sem
 * codificar</b>, e o service codifica a senha em seguida e cria o usuário ativo.
 * </p>
 *
 * <p>
 * <b>Dado sensível:</b> {@code password} chega em texto e só existe neste
 * request; nenhuma resposta o devolve. Como todo {@code record}, o
 * {@code toString()} gerado inclui o valor (ver observações de logging da
 * documentação B-0).
 * </p>
 *
 * <p>
 * <b>Validação estrutural:</b> nomes de 2 a 80 caracteres; e-mail com
 * {@code @ValidEmail} (formato e consulta DNS) e {@code @UniqueEmail};
 * senha de 10 a 72 caracteres com {@code @StrongPassword}; {@code roleIds} com
 * {@code @ValidRoles} (aceita {@code null} e vazio; cada id informado deve
 * existir). A anotação de classe {@link PasswordPersonalData}, apoiada pelo
 * contrato {@link PasswordPersonalDataCandidate}, impede senha que contenha
 * dados pessoais.
 * </p>
 *
 * @param firstName primeiro nome (obrigatório; 2 a 80 caracteres)
 * @param lastName  sobrenome (obrigatório; 2 a 80 caracteres)
 * @param email     e-mail (obrigatório, válido e não cadastrado); vira o nome
 *                  de usuário
 * @param password  senha em texto (obrigatória; 10 a 72 caracteres; forte)
 * @param roleIds   ids das roles do usuário; {@code null} ou vazio cria usuário
 *                  sem roles
 */
@PasswordPersonalData
public record UserCreateRequest(

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
    String password,

    @ValidRoles 
    Set<Long> roleIds) implements PasswordPersonalDataCandidate {
}
