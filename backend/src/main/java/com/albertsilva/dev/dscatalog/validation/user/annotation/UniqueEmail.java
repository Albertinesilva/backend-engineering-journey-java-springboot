package com.albertsilva.dev.dscatalog.validation.user.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

import com.albertsilva.dev.dscatalog.validation.user.validator.UniqueEmailValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida se o endereço de email é único no banco de dados
 * e não está registrado por outro usuário.
 *
 * <p>
 * Esta annotation executa validações em nível de campo
 * ({@link ElementType#FIELD}) e deve ser aplicada em atributos
 * do tipo {@link String} que representam endereços de email.
 *
 * <p>
 * A lógica de validação é implementada por
 * {@link UniqueEmailValidator}, que consulta o banco de dados
 * para verificar a unicidade do email.
 *
 * <p>
 * A validação:
 * <ul>
 * <li>Normaliza o email para minúsculas</li>
 * <li>Remove espaços em branco</li>
 * <li>Verifica se não existe outro usuário com o mesmo email</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 *
 * <pre>{@code
 * @UniqueEmail
 * private String email;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {

    /**
     * Mensagem padrão retornada quando a validação falha.
     *
     * @return mensagem padrão da validação
     */
    String message() default "{user.email.unique}";

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