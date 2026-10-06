package com.albertsilva.dev.dscatalog.mapper.category;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.entity.Category;

/**
 * Componente responsável pela conversão entre DTOs e entidade {@link Category}.
 *
 * <p>
 * Centraliza a lógica de transformação de dados entre as camadas da aplicação,
 * garantindo desacoplamento entre o modelo de domínio e os objetos expostos
 * pela API.
 * </p>
 *
 * <p>
 * <b>Responsabilidades:</b>
 * </p>
 * <ul>
 * <li>Converter requisições de criação em entidades</li>
 * <li>Atualizar entidades existentes com dados de requisição</li>
 * <li>Converter entidades em objetos de resposta</li>
 * <li>Mapear listas paginadas de entidades</li>
 * </ul>
 */
@Component
public class CategoryMapper {

  /**
   * Converte um {@link CategoryCreateRequest} em uma entidade {@link Category}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Cria uma nova instância de {@link Category}</li>
   * <li>Mapeia os campos básicos (nome e descrição)</li>
   * <li>Não define o ID (deve ser gerado pelo banco de dados)</li>
   *
   * @param request dados recebidos na requisição de criação
   * @return entidade pronta para persistência ou {@code null} se o request for
   *         nulo
   */
  public Category toEntity(CategoryCreateRequest request) {
    if (request == null) {
      return null;
    }

    Category entity = new Category();
    entity.setName(request.name());
    entity.setDescription(request.description());

    return entity;
  }

  /**
   * Atualiza uma entidade {@link Category} existente com base em um
   * {@link CategoryUpdateRequest}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Atualiza apenas os campos que foram informados (não nulos)</li>
   * <li>Campos nulos no request são ignorados</li>
   * <li>Permite atualização parcial da entidade (patch-like)</li>
   * </ul>
   *
   * @param request dados de atualização
   * @param entity  entidade a ser atualizada
   */
  public void updateEntity(CategoryUpdateRequest request, Category entity) {
    if (request == null || entity == null) {
      return;
    }

    if (request.name() != null) {
      entity.setName(request.name());
    }

    if (request.description() != null) {
      entity.setDescription(request.description());
    }

  }

  /**
   * Converte uma entidade {@link Category} em um {@link CategoryResponse}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Extrai apenas os dados necessários para resposta da API</li>
   * <li>Não expõe informações internas ou desnecessárias</li>
   * </ul>
   *
   * @param entity entidade de categoria
   * @return DTO de resposta ou {@code null} caso a entidade seja nula
   */
  public CategoryResponse toResponse(Category entity) {
    if (entity == null) {
      return null;
    }

    return new CategoryResponse(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.isActive());
  }

  /**
   * Converte uma página de entidades {@link Category} em uma página de
   * {@link CategoryResponse}.
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Utiliza a função {@link #toResponse(Category)} para mapear cada
   * elemento</li>
   * <li>Mantém as informações de paginação (total, páginas, etc.)</li>
   * </ul>
   *
   * @param entities página de entidades
   * @return página de DTOs de resposta
   */
  public Page<CategoryResponse> toResponsePage(Page<Category> entities) {
    return entities.map(this::toResponse);
  }
}