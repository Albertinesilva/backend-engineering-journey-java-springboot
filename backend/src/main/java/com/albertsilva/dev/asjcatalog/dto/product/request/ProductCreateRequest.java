package com.albertsilva.dev.dscatalog.dto.product.request;

import java.time.Instant;
import java.util.List;

/**
 * DTO utilizado para criação de produtos.
 *
 * <p>
 * <b>Importante sobre categorias:</b>
 * </p>
 * <ul>
 * <li>As categorias são enviadas apenas como IDs</li>
 * <li>Exemplo: {@code "categoryIds": [1, 2, 3]}</li>
 * <li>O sistema irá buscar essas categorias no banco</li>
 * </ul>
 *
 * <p>
 * <b>Exemplo de JSON:</b>
 * </p>
 * 
 * <pre>
 * {
 *   "name": "Notebook",
 *   "description": "Notebook gamer",
 *   "price": 4500.0,
 *   "imgUrl": "http://image.com",
 *   "date": "2025-01-01T10:00:00Z",
 *   "active": true,
 *   "categoryIds": [1, 2]
 * }
 * </pre>
 *
 * @param name        nome do produto
 * @param description descrição do produto
 * @param active      status do produto
 * @param price       preço
 * @param imgUrl      URL da imagem
 * @param date        data associada
 * @param categoryIds lista de IDs das categorias
 */
public record ProductCreateRequest(
        String name,
        String description,
        Double price,
        String imgUrl,
        Instant date,
        List<Long> categoryIds) {
}
