package com.albertsilva.dev.asjcatalog.dto.product.request;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.albertsilva.dev.asjcatalog.validation.product.annotation.ProductCreateValid;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
 * caracteres; letras, dígitos, espaços, hífen e parênteses), preço obrigatório
 * e {@code categoryIds} não vazio; os demais campos são opcionais, mas, quando
 * presentes, com formato exigido ({@code @Size}, {@code @Pattern} e
 * {@code @PastOrPresent} aceitam {@code null}). A unicidade do nome e a
 * existência das categorias são verificadas por {@link ProductCreateValid}.
 * </p>
 *
 * <p>
 * <b>Preço:</b> {@code @NotNull} (o Jackson converte {@code "price": ""} em
 * {@code null}, então a string vazia também é recusada), {@code @Positive} e
 * {@code @Digits(integer = 8, fraction = 2)}, o mesmo limite da coluna
 * {@code NUMERIC(10,2)}: no máximo 99.999.999,99. O {@code @Digits} conta a
 * escala do valor recebido, então zeros à direita além da segunda casa
 * ({@code 10.000}) também são recusados.
 * </p>
 *
 * @param name        nome do produto (obrigatório; 3 a 100 caracteres)
 * @param description descrição (opcional; se informada, 3 a 200 caracteres)
 * @param price       preço do produto (obrigatório; maior que zero; no máximo
 *                    8 dígitos inteiros e 2 casas decimais)
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

    @NotNull(message = "{product.price.notNull}")
    @Positive(message = "{product.price.positive}")
    @Digits(integer = 8, fraction = 2, message = "{product.price.digits}")
    BigDecimal price,

    @Pattern(regexp = "^(https?://).+$", message = "{product.imgUrl.pattern}") 
    String imgUrl,

    @PastOrPresent(message = "{product.date.pastOrPresent}") 
    Instant date,

    @NotEmpty(message = "{product.categoryIds.notEmpty}") 
    List<Long> categoryIds) {

}
