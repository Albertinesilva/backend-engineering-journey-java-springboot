package com.albertsilva.dev.dscatalog.validation.user.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.user.validator.UserCreateValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida se os dados fornecidos para a criação de um novo usuário
 * atendem aos critérios de segurança definidos pela aplicação.
 *
 * <p>
 * Esta annotation executa validações em nível de classe
 * ({@link ElementType#TYPE}) e deve ser aplicada em classes
 * que representam requisições de criação de usuário, como
 * {@link UserCreateRequest}.
 *
 * <p>
 * A lógica de validação é implementada por
 * {@link UserCreateValidator}.
 *
 * <p>
 * As validações incluem:
 * <ul>
 * <li>Verifica se a senha não contém o primeiro nome do usuário</li>
 * <li>Verifica se a senha não contém o sobrenome do usuário</li>
 * <li>Verifica se a senha não contém a parte local do email (antes do @)</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 *
 * <pre>{@code
 * @UserCreateValid
 * public class UserCreateRequest {
 *   private String firstName;
 *   private String lastName;
 *   private String email;
 *   private String password;
 * }
 * }</pre>
 */
@Documented
@Constraint(validatedBy = UserCreateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserCreateValid {

  /**
   * Mensagem padrão retornada quando a validação falha.
   *
   * @return mensagem padrão da validação
   */
  String message() default "{user.create.validation}";

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