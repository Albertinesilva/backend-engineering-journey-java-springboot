package com.albertsilva.dev.dscatalog.dto.product.request;

import java.time.Instant;
import java.util.List;

/**
 * DTO utilizado para atualização de produtos.
 *
 * <p>
 * Permite atualização parcial (PATCH-like).
 * </p>
 *
 * <p>
 * <b>Importante:</b>
 * </p>
 * <ul>
 * <li>Campos nulos são ignorados</li>
 * <li>Categorias continuam sendo enviadas por IDs</li>
 * </ul>
 *
 * @param name        novo nome
 * @param description nova descrição
 * @param active      novo status
 * @param price       novo preço
 * @param imgUrl      nova imagem
 * @param date        nova data
 * @param categoryIds novos IDs de categorias
 */
public record ProductUpdateRequest(
    String name,
    String description,
    Double price,
    String imgUrl,
    Instant date,
    List<Long> categoryIds) {
}
