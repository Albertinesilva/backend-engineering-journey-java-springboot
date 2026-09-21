package com.albertsilva.dev.dscatalog.validation.product.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.albertsilva.dev.dscatalog.validation.product.validator.ProductUpdateValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restrição de <b>classe</b> que aciona {@link ProductUpdateValidator}; usada
 * em {@code ProductUpdateRequest}.
 *
 * <p>
 * <b>Regras efetivas (ambas consultam o banco):</b>
 * </p>
 * <ul>
 * <li>o nome não pode pertencer a <b>outro</b> produto (sem diferenciar
 * maiúsculas de minúsculas); o id do produto atual vem da variável {@code id}
 * do caminho da URL e, se ela não estiver disponível, a verificação é
 * ignorada — violação no campo {@code name};</li>
 * <li>todos os ids de {@code categoryIds} devem existir como categoria —
 * violação no campo {@code categoryIds}.</li>
 * </ul>
 *
 * <p>
 * <b>Metadados:</b> {@code @Target(TYPE)}, {@code @Retention(RUNTIME)},
 * {@code @Documented}. <b>Depende do contexto HTTP.</b>
 * </p>
 */
@Documented
@Constraint(validatedBy = ProductUpdateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProductUpdateValid {

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