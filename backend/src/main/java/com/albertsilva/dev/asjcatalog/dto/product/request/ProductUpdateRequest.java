package com.albertsilva.dev.asjcatalog.dto.product.request;

import java.time.Instant;
import java.util.List;

import com.albertsilva.dev.asjcatalog.validation.product.annotation.ProductUpdateValid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO utilizado para requisições de atualização
 * de produtos.
 *
 * <p>
 * Representa os dados fornecidos pelo cliente
 * durante o processo de atualização de um produto.
 *
 * <p>
 * Este DTO utiliza a annotation
 * {@link ProductUpdateValid} para executar
 * validações contextuais relacionadas ao processo
 * de atualização do produto.
 *
 * <p>
 * As validações aplicadas garantem:
 * <ul>
 * <li>Obrigatoriedade do nome do produto</li>
 * <li>Tamanho mínimo e máximo permitido</li>
 * <li>Validação de caracteres permitidos</li>
 * <li>Validação do preço</li>
 * <li>Validação da URL da imagem</li>
 * <li>Validação da data</li>
 * <li>Validação das categorias associadas</li>
 * <li>Validações contextuais de atualização</li>
 * </ul>
 *
 * <p>
 * As categorias relacionadas ao produto continuam
 * sendo enviadas apenas pelos seus identificadores.
 *
 * <p>
 * Exemplo:
 * 
 * <pre>{@code
 * "categoryIds": [1, 2, 3]
 * }</pre>
 *
 * <p>
 * As regras de validação utilizam Bean Validation
 * através das annotations presentes nos atributos
 * do record.
 *
 * @param name        novo nome do produto
 * @param description nova descrição do produto
 * @param price       novo preço do produto
 * @param imgUrl      nova URL da imagem do produto
 * @param date        nova data associada ao produto
 * @param categoryIds lista contendo os identificadores
 *                    das categorias do produto
 */
@ProductUpdateValid
public record ProductUpdateRequest(

    @NotBlank(message = "{product.name.notBlank}") 
    @Size(min = 3, max = 100, message = "{product.name.size}") 
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s\\-()]+$", message = "{product.name.pattern}") 
    String name,

    @Size(min = 3, max = 200, message = "{product.description.size}") 
    String description,

    @Positive(message = "{product.price.positive}") 
    Double price,

    @Pattern(regexp = "^(https?://).+$", message = "{product.imgUrl.pattern}") 
    String imgUrl,

    @PastOrPresent(message = "{product.date.pastOrPresent}") 
    Instant date,

    @NotEmpty(message = "{product.categoryIds.notEmpty}") 
    List<Long> categoryIds) {
}
