package com.albertsilva.dev.dscatalog.validation.user.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;

import com.albertsilva.dev.dscatalog.validation.user.validator.StrongPasswordValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida se a senha atende aos critérios mínimos
 * de segurança definidos pela aplicação.
 *
 * <p>
 * Esta annotation executa validações em nível
 * de campo ({@link ElementType#FIELD}) e deve ser
 * aplicada em atributos do tipo {@link String}.
 *
 * <p>
 * A lógica de validação é implementada por
 * {@link StrongPasswordValidator}.
 *
 * <p>
 * A senha deve conter:
 * <ul>
 * <li>No mínimo 10 caracteres</li>
 * <li>Ao menos uma letra maiúscula</li>
 * <li>Ao menos uma letra minúscula</li>
 * <li>Ao menos um número</li>
 * <li>Ao menos um caractere especial</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 * 
 * <pre>{@code
 * @StrongPassword
 * private String password;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

  /**
   * Mensagem padrão retornada quando a validação falha.
   *
   * @return mensagem padrão da validação
   */
  String message() default "{user.password.strong}";

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