package com.albertsilva.dev.asjcatalog.dto.product.response;

import java.math.BigDecimal;
import java.util.List;

import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse;

/**
 * Resposta <b>resumida</b> de produto, com as categorias em forma resumida.
 *
 * <p>
 * Produzida por {@code ProductMapper.toResponse} e devolvida na listagem
 * ({@code GET /api/v1/products}), na criação e na atualização. O mapper
 * percorre {@code Product.getCategories()} e cria um
 * {@link com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse}
 * (id e nome) para cada categoria; a lista reflete as categorias reais do
 * produto (não é vazia "por desenho").
 * </p>
 *
 * <p>
 * <b>Não expõe</b> {@code createdAt}, {@code updatedAt} nem {@code active}
 * (presentes em {@link ProductDetailsResponse}).
 * </p>
 *
 * @param id          identificador do produto
 * @param name        nome
 * @param description descrição (pode ser {@code null})
 * @param price       preço do produto, representado por {@link BigDecimal}
 * @param imgUrl      URL da imagem (pode ser {@code null})
 * @param categories  categorias do produto, com id e nome
 */
public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String imgUrl,
    List<CategoryResponse> categories) {
}