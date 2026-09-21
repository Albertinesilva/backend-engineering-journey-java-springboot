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
 * Restrição de <b>classe</b> que aciona {@link CategoryCreateValidator}; usada
 * em {@code CategoryCreateRequest}.
 *
 * <p>
 * <b>Regra efetiva:</b> o nome informado não pode já existir em categoria
 * cadastrada, sem diferenciar maiúsculas de minúsculas (o nome é comparado
 * após {@code trim} e conversão para minúsculas). Apesar de ser uma restrição
 * de classe, hoje valida um único atributo; a violação é associada ao campo
 * {@code name}. <b>Consulta o banco.</b>
 * </p>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(TYPE)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}. Não declara atributos além de {@code message},
 * {@code groups} e {@code payload}.
 * </p>
 */
@Documented
@Constraint(validatedBy = CategoryCreateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CategoryCreateValid {

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