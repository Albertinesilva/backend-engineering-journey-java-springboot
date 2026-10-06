package com.albertsilva.dev.dscatalog.dto.product.response;

import java.time.Instant;
import java.util.List;

import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;

/**
 * DTO de resposta detalhada para produto.
 *
 * <p>
 * Retorna o produto com suas categorias associadas.
 * </p>
 *
 * <p>
 * <b>Diferença chave:</b>
 * </p>
 * <ul>
 * <li>Aqui as categorias são objetos completos</li>
 * <li>Ideal para telas de detalhe (ex: GET /products/{id})</li>
 * </ul>
 *
 * <p>
 * <b>Exemplo de retorno:</b>
 * </p>
 * 
 * <pre>
 * {
 *   "id": 1,
 *   "name": "Notebook",
 *   "categories": [
 *     { "id": 1, "name": "Eletrônicos" }
 *   ]
 * }
 * </pre>
 *
 * @param id          identificador
 * @param name        nome
 * @param description descrição
 * @param price       preço
 * @param imgUrl      imagem
 * @param date        data
 * @param categories  categorias completas
 */
public record ProductDetailsResponse(
    Long id,
    String name,
    String description,
    Double price,
    String imgUrl,
    Instant date,
    List<CategoryResponse> categories) {
}
