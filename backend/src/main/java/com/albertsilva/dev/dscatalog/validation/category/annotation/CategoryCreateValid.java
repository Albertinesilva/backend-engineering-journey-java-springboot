package com.albertsilva.dev.dscatalog.validation.category.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;

import com.albertsilva.dev.dscatalog.validation.category.validator.CategoryCreateValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Valida regras contextuais do processo de criação
 * de categorias.
 *
 * <p>
 * Esta annotation executa validações em nível
 * de classe ({@link ElementType#TYPE}), permitindo
 * validar relações entre múltiplos atributos do DTO.
 *
 * <p>
 * A lógica de validação é implementada por
 * {@link CategoryCreateValidator}.
 *
 * <p>
 * Exemplo:
 * 
 * <pre>{@code
 * @CategoryCreateValid
 * public record CategoryCreateRequest(
 *     String name) {
 * }
 * }</pre>
 */
@Documented
@Constraint(validatedBy = CategoryCreateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CategoryCreateValid {

  /**
   * Mensagem padrão retornada quando a validação falha.
   *
   * @return mensagem padrão da validação
   */
  String message() default "{error.validation.message}";

  /**
   * Permite a criação de grupos de validação.
   *
   * @return grupos de validação associados
   */
  Class<?>[] groups() default {};

  /**
   * Permite associar metadados adicionais à validação.
   *
   * @return payloads associados à validação
   */
  Class<? extends Payload>[] payload() default {};
}