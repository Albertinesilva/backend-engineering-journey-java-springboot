package com.albertsilva.dev.dscatalog.mapper.product;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.albertsilva.dev.dscatalog.domain.catalog.Product;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;

/**
 * Conversor manual (sem MapStruct) entre os DTOs de produto e a entidade
 * {@link Product}. Componente sem estado, usado apenas por
 * {@code ProductService}.
 *
 * <p>
 * <b>Categorias:</b> na <b>entrada</b> (criação/atualização) este mapper
 * <b>não trata categorias</b>: {@code categoryIds} é resolvido e aplicado por
 * {@code ProductService.syncCategories}. Na <b>saída</b>, ele percorre
 * {@code Product.getCategories()} e as converte: em
 * {@link ProductResponse} como
 * {@link com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse}
 * (id e nome) e em {@link ProductDetailsResponse} como
 * {@link com.albertsilva.dev.dscatalog.dto.category.response.CategoryDetailsResponse}
 * (id, nome, descrição e {@code active}). Como a coleção é carregada sob
 * demanda, a conversão só evita consultas adicionais quando o produto foi
 * carregado com {@code JOIN FETCH} (caso de {@code findAllPaged}).
 * </p>
 *
 * <p>
 * Os métodos de conversão devolvem {@code null} para entrada {@code null}; o
 * mapper não normaliza valores.
 * </p>
 */
@Component
public class ProductMapper {

  /**
   * Cria um novo {@link Product} (ainda sem id) a partir do request de criação.
   *
   * <p>
   * Copia {@code name}, {@code description}, {@code price} e {@code imgUrl}.
   * <b>Não copia</b> {@code date} (o campo do request é descartado) nem
   * {@code categoryIds}, e <b>não define</b> {@code active}: a instância fica
   * com o valor padrão do campo ({@code false}) até que
   * {@code ProductService.create} o defina como {@code true}. As datas são
   * preenchidas pelos callbacks JPA.
   * </p>
   *
   * @param request dados de criação
   * @return nova entidade (não persistida) ou {@code null} se {@code request} for
   *         {@code null}
   */
  public Product toEntity(ProductCreateRequest request) {
    if (request == null) {
      return null;
    }

    Product entity = new Product();
    entity.setName(request.name());
    entity.setDescription(request.description());
    entity.setPrice(request.price());
    entity.setImgUrl(request.imgUrl());

    return entity;
  }

  /**
   * Aplica o request de atualização a um produto existente, <b>somente nos
   * campos diferentes de {@code null}</b>.
   *
   * <p>
   * {@code name}, {@code description}, {@code price} e {@code imgUrl} são
   * tratados de forma independente: cada um só é sobrescrito se o valor recebido
   * não for {@code null}; portanto, {@code null} mantém o valor atual e não é
   * possível limpar um campo por esta via. {@code categoryIds} não é tratado
   * aqui (ver {@code ProductService.syncCategories}), e {@code active} e as datas
   * não são alterados. Se {@code request} ou {@code entity} for {@code null},
   * nada é feito.
   * </p>
   *
   * @param request dados de atualização
   * @param entity  produto a ser modificado
   */
  public void updateEntity(ProductUpdateRequest request, Product entity) {
    if (request == null || entity == null) {
      return;
    }

    if (request.name() != null) {
      entity.setName(request.name());
    }

    if (request.description() != null) {
      entity.setDescription(request.description());
    }

    if (request.price() != null) {
      entity.setPrice(request.price());
    }

    if (request.imgUrl() != null) {
      entity.setImgUrl(request.imgUrl());
    }

  }

  /**
   * Converte o produto em resposta <b>resumida</b>: {@code id}, {@code name},
   * {@code description}, {@code price}, {@code imgUrl} e a lista de categorias
   * (cada uma como id e nome).
   *
   * <p>
   * Percorre {@code entity.getCategories()}; se a coleção ainda não estiver
   * inicializada, ela é carregada nesse momento (uma consulta adicional por
   * produto, padrão N+1). Datas e {@code active} não são expostos.
   * </p>
   *
   * @param entity produto
   * @return resposta ou {@code null} se {@code entity} for {@code null}
   */
  public ProductResponse toResponse(Product entity) {
    if (entity == null) {
      return null;
    }

    return new ProductResponse(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.getPrice(),
        entity.getImgUrl(),
        entity.getCategories().stream().map(cat -> new CategoryResponse(cat.getId(), cat.getName())).toList());
  }

  /**
   * Converte o produto em resposta <b>detalhada</b>: todos os campos de
   * {@link #toResponse(Product)} mais {@code createdAt}, {@code updatedAt} e
   * {@code active}, com as categorias em forma detalhada (id, nome, descrição e
   * {@code active}).
   *
   * <p>
   * Percorre {@code entity.getCategories()}, com o mesmo efeito de carregamento
   * sob demanda descrito em {@link #toResponse(Product)}.
   * </p>
   *
   * @param entity produto
   * @return resposta ou {@code null} se {@code entity} for {@code null}
   */
  public ProductDetailsResponse toDetailsResponse(Product entity) {
    if (entity == null)
      return null;

    return new ProductDetailsResponse(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.getPrice(),
        entity.getImgUrl(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.isActive(),
        entity.getCategories().stream().map(cat -> new CategoryDetailsResponse(
            cat.getId(),
            cat.getName(),
            cat.getDescription(),
            cat.isActive())).toList());
  }

  /**
   * Converte uma página de produtos em página de {@link ProductResponse}
   * aplicando {@link #toResponse(Product)} a cada elemento e preservando os
   * metadados de paginação.
   *
   * @param entities página de entidades
   * @return página de respostas resumidas
   */
  public Page<ProductResponse> toResponsePage(Page<Product> entities) {
    return entities.map(this::toResponse);
  }
}