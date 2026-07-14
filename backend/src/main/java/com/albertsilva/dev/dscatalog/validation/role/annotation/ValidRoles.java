package com.albertsilva.dev.dscatalog.validation.role.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.role.validator.ValidRolesValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida se os identificadores de roles informados
 * existem na base de dados.
 *
 * <p>
 * Esta annotation executa validações em nível
 * de campo ({@link ElementType#FIELD}) e pode ser
 * aplicada em coleções contendo identificadores
 * de roles.
 *
 * <p>
 * A lógica de validação é implementada por
 * {@link ValidRolesValidator}.
 *
 * <p>
 * Exemplo:
 * 
 * <pre>{@code
 * @ValidRoles
 * private Set<Long> roleIds;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = ValidRolesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoles {

  /**
   * Mensagem padrão retornada quando a validação falha.
   *
   * @return mensagem padrão da validação
   */
  String message() default "{role.invalid}";

  /**
   * Define grupos de validação associados
   * à constraint.
   *
   * @return grupos de validação
   */
  Class<?>[] groups() default {};

  /**
   * Permite associar metadados adicionais
   * à validação.
   *
   * @return payloads associados à constraint
   */
  Class<? extends Payload>[] payload() default {};
}