package com.albertsilva.dev.dscatalog.validation.category.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.category.validator.CategoryUpdateValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida regras contextuais do processo de atualização
 * de categorias.
 *
 * <p>
 * Esta annotation executa validações em nível
 * de classe ({@link ElementType#TYPE}), permitindo
 * validar regras que dependem de múltiplos atributos
 * do objeto validado.
 *
 * <p>
 * A lógica de validação é implementada por
 * {@link CategoryUpdateValidator}.
 *
 * <p>
 * Exemplo:
 * 
 * <pre>{@code
 * @CategoryUpdateValid
 * public record CategoryUpdateRequest(
 *         String name) {
 * }
 * }</pre>
 */
@Documented
@Constraint(validatedBy = CategoryUpdateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CategoryUpdateValid {

    /**
     * Mensagem padrão retornada quando a validação falha.
     *
     * @return mensagem padrão da validação
     */
    String message() default "{error.validation.message}";

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