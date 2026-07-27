package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.PasswordPersonalData;
import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmail;
import com.albertsilva.dev.dscatalog.validation.user.annotation.ValidEmail;
import com.albertsilva.dev.dscatalog.validation.user.contract.PasswordPersonalDataCandidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para requisições de registro
 * de usuários.
 *
 * <p>
 * Representa os dados fornecidos pelo cliente
 * durante o processo de registro de um novo usuário
 * no sistema.
 *
 * <p>
 * Este DTO utiliza a annotation
 * {@link UserCreateValid} para executar
 * validações contextuais relacionadas ao processo
 * de criação do usuário.
 *
 * <p>
 * As validações aplicadas garantem:
 * <ul>
 * <li>Obrigatoriedade do primeiro nome</li>
 * <li>Obrigatoriedade do sobrenome</li>
 * <li>Validação estrutural do email</li>
 * <li>Validação de unicidade do email</li>
 * <li>Validação de segurança da senha</li>
 * </ul>
 *
 * <p>
 * As regras de validação utilizam Bean Validation
 * através das annotations presentes nos atributos
 * do record.
 *
 * @param firstName primeiro nome do usuário
 * @param lastName  sobrenome do usuário
 * @param email     email do usuário
 * @param password  senha do usuário
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
