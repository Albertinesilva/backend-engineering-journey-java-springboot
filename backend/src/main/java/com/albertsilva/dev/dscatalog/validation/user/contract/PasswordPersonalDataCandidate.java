package com.albertsilva.dev.dscatalog.validation.user.contract;

/**
 * Contrato utilizado por validações que necessitam
 * acessar os dados pessoais do usuário para validar
 * a senha.
 *
 * <p>
 * DTOs que representam operações de criação ou registro
 * de usuários podem implementar esta interface para
 * reutilizar a validação {@code @PasswordPersonalData}.
 * </p>
 */
public interface PasswordPersonalDataCandidate {

  String firstName();

  String lastName();

  String email();

  String password();
}