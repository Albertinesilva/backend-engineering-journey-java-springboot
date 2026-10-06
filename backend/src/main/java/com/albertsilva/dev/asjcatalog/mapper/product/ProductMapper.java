package com.albertsilva.dev.dscatalog.mapper.product;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.dscatalog.entity.Product;

/**
 * Componente responsável pela conversão entre DTOs e entidade {@link Product}.
 *
 * <p>
 * Este mapper trata a transformação de dados entre a API e o modelo de domínio,
 * incluindo o tratamento de relacionamentos com categorias.
 * </p>
 *
 * <p>
 * <b>Importante para entendimento:</b>
 * </p>
 * <ul>
 * <li>Na ENTRADA (create/update), as categorias são representadas apenas por
 * IDs</li>
 * <li>Na SAÍDA (response), as categorias são retornadas como objetos
 * completos</li>
 * </ul>
 *
 * <p>
 * Esse padrão evita excesso de dados nas requisições e mantém respostas ricas
 * para o cliente.
 * </p>
 */
@Component
public class ProductMapper {

  /**
   * Converte um {@link ProductCreateRequest} em uma entidade {@link Product}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Mapeia os campos básicos do produto</li>
   * <li>Define {@code active = false} caso não seja informado</li>
   * <li><b>Não trata categorias diretamente</b> (apenas IDs são recebidos)</li>
   * </ul>
   *
   * <p>
   * <b>Importante:</b>
   * </p>
   * <ul>
   * <li>O relacionamento com categorias deve ser tratado na camada de
   * serviço</li>
   * <li>Isso ocorre porque é necessário buscar as categorias no banco</li>
   * </ul>
   *
   * @param request dados da requisição de criação
   * @return entidade pronta para persistência ou {@code null} se request for nulo
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
    entity.setDate(request.date());

    return entity;
  }

  /**
   * Atualiza uma entidade {@link Product} com base em um
   * {@link ProductUpdateRequest}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Atualiza apenas os campos não nulos</li>
   * <li>Permite atualização parcial (PATCH-like)</li>
   * <li><b>Não atualiza categorias diretamente</b></li>
   * </ul>
   *
   * <p>
   * <b>Importante:</b>
   * </p>
   * <ul>
   * <li>A atualização das categorias deve ser feita no service</li>
   * <li>Evita inconsistência e garante integridade relacional</li>
   * </ul>
   *
   * @param request dados de atualização
   * @param entity  entidade a ser atualizada
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

    if (request.date() != null) {
      entity.setDate(request.date());
    }
  }

  /**
   * Converte uma entidade {@link Product} em um {@link ProductResponse}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Retorna dados básicos do produto</li>
   * <li>Não inclui categorias (lista vazia)</li>
   * </ul>
   *
   * <p>
   * <b>Uso:</b>
   * </p>
   * <ul>
   * <li>Listagens simples (ex: GET /products)</li>
   * <li>Evita payload pesado</li>
   * </ul>
   *
   * @param entity entidade de produto
   * @return DTO simplificado
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
        entity.getDate(),
        List.of());
  }

  /**
   * Converte uma entidade {@link Product} em um {@link ProductDetailsResponse}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Retorna todos os dados do produto</li>
   * <li>Inclui categorias como objetos completos</li>
   * </ul>
   *
   * <p>
   * <b>Importante para o júnior:</b>
   * </p>
   * <ul>
   * <li>Na resposta, categorias vêm como objetos (id, name, etc.)</li>
   * <li>Isso é diferente da requisição, que usa apenas IDs</li>
   * </ul>
   *
   * @param entity entidade de produto
   * @return DTO detalhado
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
        entity.getDate(),
        entity.getCategories().stream().map(cat -> new CategoryResponse(
            cat.getId(),
            cat.getName(),
            cat.getDescription(),
            cat.isActive())).toList());
  }

  /**
   * Converte uma página de produtos em uma página de respostas simplificadas.
   *
   * @param entities página de entidades
   * @return página de DTOs
   */
  public Page<ProductResponse> toResponsePage(Page<Product> entities) {
    return entities.map(this::toResponse);
  }
}