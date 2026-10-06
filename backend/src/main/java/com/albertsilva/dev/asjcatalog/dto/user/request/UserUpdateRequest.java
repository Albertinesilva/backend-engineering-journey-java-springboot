package com.albertsilva.dev.asjcatalog.dto.user.request;

import java.util.Set;

import com.albertsilva.dev.asjcatalog.validation.role.annotation.ValidRoles;
import com.albertsilva.dev.asjcatalog.validation.user.annotation.StrongPassword;
import com.albertsilva.dev.asjcatalog.validation.user.annotation.UserUpdateValid;
import com.albertsilva.dev.asjcatalog.validation.user.annotation.ValidEmail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para requisições de atualização
 * de usuários.
 *
 * <p>
 * Representa os dados fornecidos pelo cliente
 * durante o processo de atualização de um usuário
 * no sistema.
 *
 * <p>
 * Este DTO utiliza a annotation
 * {@link UserUpdateValid} para executar
 * validações contextuais relacionadas ao processo
 * de atualização do usuário.
 *
 * <p>
 * As validações aplicadas garantem:
 * <ul>
 * <li>Obrigatoriedade do primeiro nome</li>
 * <li>Obrigatoriedade do sobrenome</li>
 * <li>Validação estrutural do email</li>
 * <li>Validação de segurança da senha</li>
 * <li>Validação das roles associadas</li>
 * <li>Validações contextuais de atualização</li>
 * </ul>
 *
 * <p>
 * As roles relacionadas ao usuário continuam
 * sendo enviadas apenas pelos seus identificadores.
 *
 * <p>
 * Exemplo:
 * 
 * <pre>{@code
 * "roleIds": [1, 2]
 * }</pre>
 *
 * <p>
 * As regras de validação utilizam Bean Validation
 * através das annotations presentes nos atributos
 * do record.
 *
 * @param firstName novo primeiro nome do usuário
 * @param lastName  novo sobrenome do usuário
 * @param email     novo email do usuário
 * @param password  nova senha do usuário
 * @param roleIds   lista contendo os identificadores
 *                  das roles associadas ao usuário
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

    @NotEmpty(message = "{user.roleIds.notEmpty}") 
    @ValidRoles 
    Set<Long> roleIds) {
}
