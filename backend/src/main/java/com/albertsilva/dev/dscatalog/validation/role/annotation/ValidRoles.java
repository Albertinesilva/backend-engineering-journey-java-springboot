package com.albertsilva.dev.dscatalog.validation.role.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.role.validator.ValidRolesValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restrição de <b>campo</b> que aciona {@link ValidRolesValidator}; aplicada a
 * atributos {@code Set<Long>} com ids de roles ({@code UserCreateRequest} e
 * {@code UserUpdateRequest}).
 *
 * <p>
 * <b>Regra efetiva:</b> conjunto {@code null} ou vazio é <b>válido</b>; caso
 * contrário, cada id deve existir em {@code tb_role} (uma consulta
 * {@code existsById} por id). Um id inexistente invalida o conjunto inteiro,
 * com a mensagem {@code role.invalid}. <b>Consulta o banco.</b>
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(FIELD)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}.
 * </p>
 */
@Documented
@Constraint(validatedBy = ValidRolesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRoles {

  /**
   * Chave de mensagem padrão ({@code role.invalid}); é a mesma chave que o validator emite.
   *
   * @return chave de mensagem padrão da restrição
   */
  String message() default "{role.invalid}";

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