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
 * Valida se o endereço de email informado é único para o usuário autenticado.
 *
 * <p>
 * Diferentemente da annotation {@link UniqueEmail}, esta validação permite que
 * o usuário mantenha o próprio endereço de email, impedindo apenas a utilização
 * de um email pertencente a outro usuário.
 * </p>
 */
@Documented
@Constraint(validatedBy = UniqueEmailForAuthenticatedUserValidator.class)
@Target({ FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
public @interface UniqueEmailForAuthenticatedUser {

  String message() default "{user.email.unique}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}