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
 * Restrição de <b>campo</b> que aciona {@link UniqueEmailValidator}; aplicada ao
 * e-mail em {@code UserCreateRequest} e {@code UserRegisterRequest} (fluxos de
 * <b>criação</b>; não se aplica a atualização).
 *
 * <p>
 * <b>Regra efetiva:</b> o e-mail (após {@code trim} e minúsculas) não pode
 * existir em nenhum usuário, sem diferenciar maiúsculas de minúsculas
 * ({@code UserRepository.existsByEmailIgnoreCase}). {@code null} e valores em
 * branco são válidos. <b>Consulta o banco.</b>
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(FIELD)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {

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