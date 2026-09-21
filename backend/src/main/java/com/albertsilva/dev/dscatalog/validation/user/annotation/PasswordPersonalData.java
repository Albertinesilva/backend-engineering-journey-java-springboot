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
 * Restrição de <b>classe</b> que aciona {@link PasswordPersonalDataValidator};
 * só se aplica a tipos que implementam
 * {@link com.albertsilva.dev.dscatalog.validation.user.contract.PasswordPersonalDataCandidate}
 * ({@code UserCreateRequest} e {@code UserRegisterRequest}).
 *
 * <p>
 * <b>Regra efetiva:</b> a senha (após {@code trim} e minúsculas) não pode
 * <em>conter</em> o primeiro nome, o sobrenome nem a parte do e-mail antes do
 * {@code @}, desde que cada um tenha ao menos 3 caracteres. No máximo uma
 * violação é registrada, no campo {@code password}. Não consulta o banco.
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(TYPE)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = PasswordPersonalDataValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordPersonalData {

  /**
   * Chave de mensagem padrão ({@code user.password.personalData}); é a mesma chave que o validator emite.
   *
   * @return chave de mensagem padrão da restrição
   */
  String message() default "{user.password.personalData}";

  /**
   * Grupos de validação da restrição (padrão do Bean Validation). Nenhuma
   * validação do projeto usa grupos.
   *
   * @return grupos de validação
   */
  Class<?>[] groups() default {};

  /**
   * Metadados associados à restrição (padrão do Bean Validation). Não são
   * usados pelo projeto.
   *
   * @return payloads da restrição
   */
  Class<? extends Payload>[] payload() default {};
}