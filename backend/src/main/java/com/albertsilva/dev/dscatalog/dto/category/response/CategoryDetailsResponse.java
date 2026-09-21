package com.albertsilva.dev.dscatalog.dto.category.response;

/**
 * Resposta <b>detalhada</b> de categoria.
 *
 * <p>
 * Produzida por {@code CategoryMapper.toDetailsResponse} para
 * {@code GET /api/v1/categories/{id}} e usada, aninhada, em
 * {@link com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse}
 * (construída diretamente por {@code ProductMapper}). Acrescenta a
 * {@link CategoryResponse} a descrição e o indicador {@code active}.
 * </p>
 *
 * <p>
 * <b>Não expõe</b> as datas de criação/atualização nem os produtos da
 * categoria.
 * </p>
 *
 * @param id          identificador da categoria
 * @param name        nome da categoria
 * @param description descrição (pode ser {@code null})
 * @param active      indica se a categoria está ativa
 */
public record CategoryDetailsResponse(Long id, String name, String description, boolean active) {

}
