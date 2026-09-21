package com.albertsilva.dev.dscatalog.validation.role.validator;

import java.util.Set;

import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.validation.role.annotation.ValidRoles;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link ValidRoles}, aplicado a {@code Set<Long>} com ids de
 * roles.
 *
 * <p>
 * <b>Comportamento:</b> {@code null} ou conjunto vazio ⇒ <b>válido</b>. Caso
 * contrário, chama {@code RoleRepository.existsById} para cada id (uma
 * consulta por id, interrompendo no primeiro inexistente); se algum não
 * existir, registra {@code {role.invalid}} sem nó de propriedade (fica no
 * campo anotado). Como o tipo é {@link Set}, ids duplicados não existem no
 * objeto (a desserialização os colapsa) e a ordem é irrelevante. Um elemento
 * {@code null} dentro do conjunto faria {@code existsById} lançar exceção (a
 * confirmar em testes).
 * </p>
 *
 * <p>
 * <b>Relação com {@code UserService}:</b> este validator aceita {@code null} e
 * vazio; o service trata {@code null} como "manter as roles" na atualização e
 * "sem roles" na criação, e conjunto vazio como "remover todas" na atualização
 * e "sem roles" na criação. O service ainda compara a quantidade de roles
 * encontradas com a de ids recebidos (validação repetida).
 * </p>
 */
public class ValidRolesValidator implements ConstraintValidator<ValidRoles, Set<Long>> {

  /** Repositório usado para verificar a existência de cada role. */
  private final RoleRepository repository;

  /**
   * @param repository repositório de roles, injetado pelo Spring
   */
  public ValidRolesValidator(RoleRepository repository) {
    this.repository = repository;
  }

  /**
   * Verifica se todos os ids informados existem como role.
   *
   * @param value   ids de roles (pode ser {@code null} ou vazio)
   * @param context contexto usado para registrar a violação {@code role.invalid}
   * @return {@code true} se for nulo/vazio ou se todos os ids existirem
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
