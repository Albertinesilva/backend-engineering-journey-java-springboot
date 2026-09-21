package com.albertsilva.dev.dscatalog.validation.user.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.user.validator.UserUpdateValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restrição de <b>classe</b> que aciona {@link UserUpdateValidator}; usada em
 * {@code UserUpdateRequest}.
 *
 * <p>
 * <b>Regras efetivas:</b>
 * </p>
 * <ul>
 * <li>o e-mail (após {@code trim} e minúsculas) não pode pertencer a
 * <b>outro</b> usuário; o id do usuário atual vem da variável {@code id} do
 * caminho da URL e, se ela estiver ausente ou não for numérica, a verificação
 * é ignorada — violação no campo {@code email};</li>
 * <li>se a senha for informada e não estiver em branco, ela não pode conter o
 * primeiro nome, o sobrenome nem a parte local do e-mail (tokens de ao menos 3
 * caracteres) — violação no campo {@code password}. Repete a regra de
 * {@link PasswordPersonalData}, mas para o DTO de atualização.</li>
 * </ul>
 *
 * <p>
 * <b>Consulta o banco e depende do contexto HTTP.</b> Metadados:
 * {@code @Target(TYPE)}, {@code @Retention(RUNTIME)}, {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = UserUpdateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserUpdateValid {

  /**
   * Chave de mensagem padrão ({@code user.update.validation}). Essa chave <b>não existe</b> nos arquivos de mensagens e, na prática, nunca é emitida: o validator sempre substitui a violação padrão por chaves específicas.
   *
   * @return chave de mensagem padrão da restrição
   */
  String message() default "{user.update.validation}";

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
