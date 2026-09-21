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
 * Restrição de <b>campo</b> que aciona {@link ValidEmailValidator}; aplicada ao
 * e-mail em {@code UserCreateRequest}, {@code UserRegisterRequest},
 * {@code UserUpdateRequest} e {@code UserEmailRequest}.
 *
 * <p>
 * <b>Regra efetiva:</b> após {@code trim} e minúsculas, o e-mail deve casar
 * com uma expressão regular simples (apenas caracteres ASCII) <b>e</b> o
 * domínio deve ter ao menos um registro MX na consulta DNS. Qualquer falha na
 * consulta DNS resulta em e-mail <b>inválido</b>. {@code null} e valores em
 * branco são válidos (a obrigatoriedade é de {@code @NotBlank}). <b>Depende de
 * rede (DNS)</b>; não consulta o banco.
 * </p>
 *
 * <p>
 * A validação é feita sobre o valor normalizado, mas o DTO original não é
 * modificado.
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(FIELD)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = ValidEmailValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEmail {

    /**
     * Chave de mensagem padrão ({@code user.email.invalid}); é a mesma chave que o validator emite.
     *
     * @return chave de mensagem padrão da restrição
     */
    String message() default "{user.email.invalid}";

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