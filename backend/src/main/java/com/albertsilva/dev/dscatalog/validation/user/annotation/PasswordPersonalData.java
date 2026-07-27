package com.albertsilva.dev.dscatalog.validation.user.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.user.validator.PasswordPersonalDataValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida se a senha informada não contém dados pessoais do usuário.
 *
 * <p>
 * Esta constraint atua em nível de classe ({@link ElementType#TYPE}),
 * pois necessita acessar simultaneamente múltiplos atributos do objeto,
 * como nome, sobrenome, email e senha.
 * </p>
 *
 * <p>
 * A validação impede que a senha contenha informações facilmente
 * associadas ao usuário, reduzindo o risco de utilização de senhas
 * previsíveis.
 * </p>
 *
 * <p>
 * Atualmente são verificadas:
 * </p>
 *
 * <ul>
 * <li>Primeiro nome</li>
 * <li>Sobrenome</li>
 * <li>Parte local do email (antes do "@")</li>
 * </ul>
 *
 * <p>
 * A implementação da validação é realizada por
 * {@link PasswordPersonalDataValidator}.
 * </p>
 */
@Documented
@Constraint(validatedBy = PasswordPersonalDataValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPersonalData {

  /**
   * Mensagem padrão.
   *
   * <p>
   * Normalmente não é utilizada, pois o validator registra
   * violações diretamente no campo {@code password}.
   * </p>
   *
   * @return mensagem padrão
   */
  String message() default "{user.password.personalData}";

  /**
   * Grupos de validação.
   *
   * @return grupos
   */
  Class<?>[] groups() default {};

  /**
   * Payload da constraint.
   *
   * @return payload
   */
  Class<? extends Payload>[] payload() default {};
}