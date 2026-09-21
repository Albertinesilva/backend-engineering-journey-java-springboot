package com.albertsilva.dev.dscatalog.dto.product.request;

import java.time.Instant;
import java.util.List;

import com.albertsilva.dev.dscatalog.validation.product.annotation.ProductCreateValid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de <b>criação de produto</b>
 * ({@code POST /api/v1/products}).
 *
 * <p>
 * Fluxo: {@code ProductController.create} → {@code ProductService.create} →
 * {@code ProductMapper.toEntity} (que copia nome, descrição, preço e URL da
 * imagem) e {@code ProductService.syncCategories} (que resolve
 * {@code categoryIds}). O produto é criado ativo pelo service.
 * </p>
 *
 * <p>
 * <b>Campos que não chegam à entidade:</b> {@code date} é validado
 * ({@code @PastOrPresent}), mas {@code ProductMapper.toEntity} não o copia e
 * nenhum outro código o utiliza: o valor enviado é descartado.
 * {@code categoryIds} não é copiado pelo mapper; é resolvido no service.
 * </p>
 *
 * <p>
 * <b>Validação estrutural (Bean Validation):</b> nome obrigatório (3 a 100
 * caracteres; letras, dígitos, espaços, hífen e parênteses); demais campos
 * opcionais, mas, quando presentes, com formato exigido. Note que
 * {@code @Positive}, {@code @Size}, {@code @Pattern} e {@code @PastOrPresent}
 * aceitam {@code null}: <b>o preço não é obrigatório neste DTO</b>. A
 * unicidade do nome e a existência das categorias são verificadas por
 * {@link ProductCreateValid}.
 * </p>
 *
 * @param name        nome do produto (obrigatório; 3 a 100 caracteres)
 * @param description descrição (opcional; se informada, 3 a 200 caracteres)
 * @param price       preço (opcional; se informado, deve ser maior que zero)
 * @param imgUrl      URL da imagem (opcional; se informada, deve começar com
 *                    {@code http://} ou {@code https://})
 * @param date        data (opcional; {@code null} ou passada/presente).
 *                    <b>Não é persistida</b>
 * @param categoryIds identificadores das categorias (obrigatório, não vazio)
 */
@ProductCreateValid
public record ProductCreateRequest(

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
