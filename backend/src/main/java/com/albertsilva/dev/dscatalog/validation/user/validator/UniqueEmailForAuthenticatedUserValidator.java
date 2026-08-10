package com.albertsilva.dev.dscatalog.validation.user.validator;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.security.auth.AuthenticatedUserService;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmailForAuthenticatedUser;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela annotation
 * {@link UniqueEmailForAuthenticatedUser}.
 *
 * <p>
 * Este validator verifica se o endereço de email informado pode ser utilizado
 * pelo usuário autenticado durante a atualização do seu perfil.
 * </p>
 *
 * <p>
 * A validação considera válido quando:
 * </p>
 * <ul>
 * <li>o email ainda não está cadastrado;</li>
 * <li>o email pertence ao próprio usuário autenticado.</li>
 * </ul>
 *
 * <p>
 * A validação considera inválido quando o email pertence a outro usuário.
 * </p>
 */
public class UniqueEmailForAuthenticatedUserValidator
    implements ConstraintValidator<UniqueEmailForAuthenticatedUser, String> {

  /**
   * Repositório utilizado para consulta de usuários.
   */
  private final UserRepository repository;

  /**
   * Serviço responsável por obter o usuário autenticado.
   */
  private final AuthenticatedUserService authenticatedUserService;

  /**
   * Construtor.
   *
   * @param repository               repositório de usuários
   * @param authenticatedUserService serviço do usuário autenticado
   */
  public UniqueEmailForAuthenticatedUserValidator(UserRepository repository,
      AuthenticatedUserService authenticatedUserService) {
    this.repository = repository;
    this.authenticatedUserService = authenticatedUserService;
  }

  /**
   * Executa a validação de unicidade do email para atualização
   * do usuário autenticado.
   *
   * @param value   endereço de email informado
   * @param context contexto do Bean Validation
   * @return {@code true} quando o email é válido; {@code false} caso pertença
   *         a outro usuário
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    if (value == null || value.isBlank()) {
      return true;
    }

    String normalizedEmail = value.trim().toLowerCase();

    User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

    boolean emailAlreadyExists = repository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, authenticatedUser.getId());

    if (!emailAlreadyExists) {
      return true;
    }

    context.disableDefaultConstraintViolation();

    context.buildConstraintViolationWithTemplate("{user.email.unique}").addConstraintViolation();

    return false;
  }

}