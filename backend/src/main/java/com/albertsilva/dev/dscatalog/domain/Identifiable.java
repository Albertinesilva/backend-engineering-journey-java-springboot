package com.albertsilva.dev.dscatalog.domain;

/**
 * Contrato mínimo para objetos que possuem um identificador.
 *
 * <p>
 * Permite tratar de forma uniforme estruturas diferentes, como uma entidade e
 * uma projeção de consulta, quando apenas o identificador importa (por exemplo,
 * para reordenar uma lista de entidades segundo a ordem de outra lista).
 * </p>
 *
 * <p>
 * Atualmente é implementada por {@link com.albertsilva.dev.dscatalog.domain.catalog.Product}
 * e estendida por {@code ProductProjection}. As demais entidades não implementam
 * este contrato.
 * </p>
 *
 * @param <ID> tipo do identificador (por exemplo, {@code Long})
 */
public interface Identifiable<ID> {

  /**
   * @return identificador do objeto (pode ser {@code null} em entidades ainda
   *         não persistidas)
   */
  ID getId();
}
