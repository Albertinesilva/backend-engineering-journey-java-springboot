package com.albertsilva.dev.dscatalog.validation.role.validator;

import java.util.Set;

import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.validation.role.annotation.ValidRoles;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela
 * annotation {@link ValidRoles}.
 *
 * <p>
 * Este validator verifica se todos os identificadores
 * de roles informados existem na base de dados.
 *
 * <p>
 * A validação é aplicada sobre coleções do tipo
 * {@link Set} contendo identificadores de roles.
 *
 * <p>
 * Caso uma ou mais roles informadas não existam,
 * uma mensagem de erro customizada é adicionada
 * ao contexto de validação.
 */
public class ValidRolesValidator implements ConstraintValidator<ValidRoles, Set<Long>> {

  /*
   * Repositório utilizado para verificar a existência
   * das roles na base de dados.
   */
  private final RoleRepository repository;

  /*
   * Construtor que recebe o repositório de roles
   * como dependência.
   */
  public ValidRolesValidator(RoleRepository repository) {
    this.repository = repository;
  }

  /**
   * Valida se todas as roles informadas existem
   * na base de dados.
   *
   * <p>
   * Caso a coleção seja {@code null} ou esteja vazia,
   * a validação será considerada válida.
   *
   * <p>
   * A verificação é realizada individualmente
   * para cada identificador informado.
   *
   * @param value   coleção contendo os identificadores
   *                das roles
   * @param context contexto utilizado pelo Bean Validation
   *                para registrar erros personalizados
   * @return {@code true} caso todas as roles existam;
   *         {@code false} caso uma ou mais roles
   *         sejam inválidas
   */
  @Override
  public boolean isValid(Set<Long> value, ConstraintValidatorContext context) {

    if (value == null || value.isEmpty()) {
      return true;
    }

    boolean allRolesExist = value.stream().allMatch(repository::existsById);

    if (allRolesExist) {
      return true;
    }

    context.disableDefaultConstraintViolation();

    context.buildConstraintViolationWithTemplate("{role.invalid}").addConstraintViolation();

    return false;
  }
}
