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
 * Restrição de <b>classe</b> que aciona {@link CategoryUpdateValidator}; usada
 * em {@code CategoryUpdateRequest}.
 *
 * <p>
 * <b>Regra efetiva:</b> o nome informado não pode pertencer a <b>outra</b>
 * categoria (comparação sem diferenciar maiúsculas de minúsculas, após
 * {@code trim} e minúsculas). O identificador da categoria atual é lido da
 * <b>variável {@code id} do caminho da URL</b> (atributo do Spring MVC na
 * requisição HTTP); se ela não estiver disponível, a verificação é ignorada.
 * A violação é associada ao campo {@code name}. <b>Consulta o banco e depende
 * do contexto HTTP.</b>
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(TYPE)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = CategoryUpdateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CategoryUpdateValid {

    /**
     * Chave de mensagem padrão ({@code error.validation.message}, definida nos três idiomas). Na prática não é emitida: o validator desabilita a violação padrão e registra chaves específicas por campo.
     *
     * @return chave de mensagem padrão da restrição
     */
    String message() default "{error.validation.message}";

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