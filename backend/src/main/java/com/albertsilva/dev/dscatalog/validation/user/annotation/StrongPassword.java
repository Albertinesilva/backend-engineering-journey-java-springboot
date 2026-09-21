package com.albertsilva.dev.dscatalog.validation.user.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;

import com.albertsilva.dev.dscatalog.validation.user.validator.StrongPasswordValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restrição de <b>campo</b> que aciona {@link StrongPasswordValidator}; aplicada
 * a senhas em {@code UserCreateRequest}, {@code UserRegisterRequest},
 * {@code UserUpdateRequest}, {@code PasswordUpdateRequest.newPassword} e
 * {@code PasswordResetRequest.password}.
 *
 * <p>
 * <b>Regras efetivas:</b> sem espaços em branco; ao menos uma letra
 * maiúscula (A-Z), uma minúscula (a-z), um dígito (0-9) e um caractere
 * "especial" (qualquer caractere que não seja letra ASCII, dígito ou espaço,
 * inclusive letras acentuadas); não ser uma das senhas comuns da lista interna;
 * e não conter sequência numérica crescente ou decrescente de 6 ou mais dígitos
 * (considerando todos os dígitos da senha). {@code null} é válido.
 * </p>
 *
 * <p>
 * <b>Não valida o tamanho</b>: mínimo e máximo são impostos, quando existem,
 * por {@code @Size} nos DTOs (não em {@code UserUpdateRequest.password} nem em
 * {@code PasswordResetRequest.password}). Não consulta o banco.
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(FIELD)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

  /**
   * Chave de mensagem padrão ({@code user.password.strong}). Essa chave <b>não existe</b> nos arquivos de mensagens e, na prática, nunca é emitida: o validator sempre substitui a violação padrão por chaves específicas ({@code user.password.uppercase}, etc.).
   *
   * @return chave de mensagem padrão da restrição
   */
  String message() default "{user.password.strong}";

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