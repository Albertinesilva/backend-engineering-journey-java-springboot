package com.albertsilva.dev.dscatalog.dto.product.response;

import java.time.Instant;
import java.util.List;

import com.albertsilva.dev.dscatalog.dto.category.response.CategoryDetailsResponse;

/**
 * Resposta <b>detalhada</b> de produto.
 *
 * <p>
 * Produzida por {@code ProductMapper.toDetailsResponse} para
 * {@code GET /api/v1/products/{id}}. Em relação a {@link ProductResponse},
 * acrescenta as datas, o indicador {@code active} e devolve cada categoria como
 * {@link com.albertsilva.dev.dscatalog.dto.category.response.CategoryDetailsResponse}
 * (id, nome, descrição e {@code active}).
 * </p>
 *
 * @param id          identificador do produto
 * @param name        nome
 * @param description descrição (pode ser {@code null})
 * @param price       preço (pode ser {@code null})
 * @param imgUrl      URL da imagem (pode ser {@code null})
 * @param createdAt   instante de criação (preenchido na persistência)
 * @param updatedAt   instante da última atualização
 * @param active      indica se o produto está ativo
 * @param categories  categorias do produto, em forma detalhada
 */
public record ProductDetailsResponse(
    Long id,
    String name,
    String description,
    Double price,
    String imgUrl,
    Instant createdAt,
    Instant updatedAt,
    boolean active,
    List<CategoryDetailsResponse> categories) {
}
