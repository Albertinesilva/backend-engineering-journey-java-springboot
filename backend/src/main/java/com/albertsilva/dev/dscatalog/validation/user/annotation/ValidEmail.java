package com.albertsilva.dev.dscatalog.validation.user.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.user.validator.ValidEmailValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida se o endereço de email possui um formato válido
 * e possui registros MX válidos no servidor DNS.
 *
 * <p>
 * Esta annotation executa validações em nível de campo
 * ({@link ElementType#FIELD}) e deve ser aplicada em atributos
 * do tipo {@link String} que representam endereços de email.
 *
 * <p>
 * A lógica de validação é implementada por
 * {@link ValidEmailValidator}, que realiza validações de
 * formato e verifica a existência de registros MX.
 *
 * <p>
 * As validações incluem:
 * <ul>
 * <li>Verifica o formato do email através de expressão regular</li>
 * <li>Valida se o domínio possui registros MX (Mail Exchange) válidos</li>
 * <li>Normaliza o email para minúsculas e remove espaços em branco</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 *
 * <pre>{@code
 * @ValidEmail
 * private String email;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = ValidEmailValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    /**
     * Mensagem padrão retornada quando a validação falha.
     *
     * @return mensagem padrão da validação
     */
    String message() default "{user.email.invalid}";

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