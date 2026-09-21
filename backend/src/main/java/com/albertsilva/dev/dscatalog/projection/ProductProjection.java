package com.albertsilva.dev.dscatalog.projection;

import com.albertsilva.dev.dscatalog.domain.Identifiable;

/**
 * Projeção (interface) que representa uma linha do resultado da consulta nativa
 * {@code ProductRepository#searchProducts(List, String, Pageable)}: apenas o
 * identificador e o nome de um produto.
 *
 * <p>
 * É uma <em>interface projection</em> fechada do Spring Data: cada getter
 * corresponde a uma coluna do {@code SELECT} (sem alias, pois os nomes das
 * colunas {@code id} e {@code name} coincidem com os dos getters), e nenhuma
 * entidade {@code Product} é carregada. O método {@code getId()} vem do
 * contrato {@link Identifiable}, o que permite reordenar entidades a partir
 * desta projeção.
 * </p>
 *
 * <p>
 * <b>Não contém</b> preço, descrição, imagem, datas, indicador de ativo nem
 * categorias: esses dados só são obtidos depois, ao carregar as entidades
 * correspondentes com {@code ProductRepository#searchProductsWithCategories(List)}.
 * </p>
 *
 * <p>
 * <b>Uso atual:</b> {@code ProductService.findAllPaged} usa {@code getId()}
 * para coletar os ids da página e passa o conteúdo da página, como referência
 * de ordem, a {@code IdentifiableUtils.reorderByReference}.
 * </p>
 */
public interface ProductProjection extends Identifiable<Long> {

  /**
   * @return nome do produto (coluna {@code tb_product.name})
   */
  String getName();
}
