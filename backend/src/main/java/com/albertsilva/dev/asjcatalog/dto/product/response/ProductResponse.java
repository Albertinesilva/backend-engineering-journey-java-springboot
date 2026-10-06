package com.albertsilva.dev.dscatalog.dto.product.response;

import java.time.Instant;
import java.util.List;

import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;

/**
 * DTO de resposta simplificada para produto.
 *
 * <p>
 * Utilizado em listagens para evitar retorno de dados excessivos.
 * </p>
 *
 * <p>
 * <b>Observação:</b>
 * </p>
 * <ul>
 * <li>O campo {@code categories} pode vir vazio propositalmente</li>
 * </ul>
 *
 * @param id          identificador
 * @param name        nome
 * @param description descrição
 * @param price       preço
 * @param imgUrl      imagem
 * @param date        data
 * @param categories  lista de categorias (geralmente vazia neste DTO)
 */
public record ProductResponse(
    Long id,
    String name,
    String description,
    Double price,
    String imgUrl,
    Instant date,
    List<CategoryResponse> categories) {
}