package com.albertsilva.dev.dscatalog.dto.product.request;

import java.util.List;

import com.albertsilva.dev.dscatalog.validation.product.annotation.ProductUpdateValid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de <b>atualização de produto</b>
 * ({@code PUT /api/v1/products/{id}}).
 *
 * <p>
 * Fluxo: {@code ProductController.update} → {@code ProductService.update} →
 * {@code ProductMapper.updateEntity} (nome, descrição, preço e URL da imagem) e
 * {@code ProductService.syncCategories} (categorias). Não possui o campo
 * {@code date} de {@link ProductCreateRequest}.
 * </p>
 *
 * <p>
 * <b>Semântica de atualização:</b> o mapper só sobrescreve nome, descrição,
 * preço e URL da imagem quando o valor recebido é diferente de {@code null};
 * portanto, omitir um campo opcional mantém o valor atual e não é possível
 * limpá-lo enviando {@code null}. O nome e {@code categoryIds} são obrigatórios
 * neste DTO, então sempre chegam ao service; {@code categoryIds} substitui
 * integralmente as categorias do produto. O indicador {@code active} não é
 * alterado por este request.
 * </p>
 *
 * <p>
 * <b>Validação estrutural:</b> mesmas regras de {@link ProductCreateRequest}
 * (preço, descrição e URL aceitam {@code null}). A unicidade do nome,
 * excluindo o próprio produto, e a existência das categorias são verificadas
 * por {@link ProductUpdateValid}.
 * </p>
 *
 * @param name        novo nome (obrigatório; 3 a 100 caracteres)
 * @param description nova descrição (opcional; se informada, 3 a 200 caracteres)
 * @param price       novo preço (opcional; se informado, maior que zero)
 * @param imgUrl      nova URL da imagem (opcional; {@code http://} ou
 *                    {@code https://})
 * @param categoryIds identificadores das categorias que substituirão as atuais
 *                    (obrigatório, não vazio)
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

    @NotEmpty(message = "{product.categoryIds.notEmpty}") 
    List<Long> categoryIds) {
}
