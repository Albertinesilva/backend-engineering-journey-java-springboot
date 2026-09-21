package com.albertsilva.dev.dscatalog.validation.user.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.user.validator.UniqueEmailForAuthenticatedUserValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restrição de <b>campo</b> que aciona
 * {@link UniqueEmailForAuthenticatedUserValidator}; aplicada ao e-mail de
 * {@code AuthenticatedUserUpdateRequest} (edição do próprio perfil).
 *
 * <p>
 * <b>Regra efetiva:</b> o e-mail (após {@code trim} e minúsculas) não pode
 * pertencer a <b>outro</b> usuário; o usuário autenticado pode manter o
 * próprio e-mail. O usuário atual é obtido de {@code AuthenticatedUserService}
 * (claim {@code userId} do JWT no {@code SecurityContext}), <b>e não da URL</b>.
 * {@code null} e valores em branco são válidos. <b>Consulta o banco e depende
 * da autenticação.</b>
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target({FIELD, ANNOTATION_TYPE})} (também pode ser
 * usada como meta-anotação), {@code @Retention(RUNTIME)}, {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = UniqueEmailForAuthenticatedUserValidator.class)
@Target({ FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface UniqueEmailForAuthenticatedUser {

  /**
   * Chave de mensagem padrão ({@code user.email.unique}); é a mesma chave que o validator emite.
   *
   * @return chave de mensagem padrão da restrição
   */
  String message() default "{user.email.unique}";

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