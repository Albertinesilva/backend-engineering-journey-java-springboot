package com.albertsilva.dev.dscatalog.dto.category.response;

/**
 * Resposta <b>resumida</b> de categoria: apenas identificador e nome.
 *
 * <p>
 * Produzida por {@code CategoryMapper.toResponse} e devolvida na criação, na
 * atualização e na listagem paginada de categorias; também é usada, aninhada,
 * em {@link com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse}.
 * </p>
 *
 * <p>
 * <b>Não expõe</b> descrição, indicador {@code active}, datas nem a coleção de
 * produtos; esses dados só aparecem em {@link CategoryDetailsResponse}
 * (descrição e {@code active}).
 * </p>
 *
 * @param id   identificador da categoria
 * @param name nome da categoria
 */
public record CategoryResponse(Long id, String name) {
}