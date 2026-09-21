package com.albertsilva.dev.dscatalog.mapper.category;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.albertsilva.dev.dscatalog.domain.catalog.Category;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;

/**
 * Conversor manual (sem MapStruct) entre os DTOs de categoria e a entidade
 * {@link Category}. Componente sem estado, usado apenas por
 * {@code CategoryService}.
 *
 * <p>
 * <b>Direções:</b> {@link CategoryCreateRequest} → entidade
 * ({@link #toEntity}); {@link CategoryUpdateRequest} → entidade existente
 * ({@link #updateEntity}); entidade → {@link CategoryResponse} /
 * {@link CategoryDetailsResponse}; e página de entidades → página de
 * {@link CategoryResponse}.
 * </p>
 *
 * <p>
 * <b>Características:</b> nunca lê nem grava {@code active} nem as datas; não
 * acessa {@code Category.products}; os métodos de conversão devolvem
 * {@code null} para entrada {@code null}; não normaliza valores (nome e
 * descrição são copiados como recebidos).
 * </p>
 */
@Component
public class CategoryMapper {

  /**
   * Cria uma nova {@link Category} (ainda sem id) a partir do request de criação.
   *
   * <p>
   * Copia apenas {@code name} e {@code description}. O indicador {@code active}
   * <b>não é definido aqui</b>: a instância fica com o valor padrão do campo
   * ({@code false}) até que {@code CategoryService.create} o defina como
   * {@code true}. As datas são preenchidas depois pelos callbacks JPA.
   * </p>
   *
   * @param request dados de criação
   * @return nova entidade (não persistida) ou {@code null} se {@code request} for
   *         {@code null}
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
   * Aplica o request de atualização a uma categoria existente, <b>somente nos
   * campos diferentes de {@code null}</b>.
   *
   * <p>
   * {@code name} e {@code description} são tratados de forma independente: cada
   * um só é sobrescrito se o valor recebido não for {@code null}. Portanto,
   * {@code null} significa "manter o valor atual" (não é possível limpar a
   * descrição com {@code null}), enquanto {@code ""} substitui a descrição por
   * texto vazio. Se {@code request} ou {@code entity} for {@code null}, nada é
   * feito. {@code active} e as datas não são alteradas.
   * </p>
   *
   * <p>
   * Ao acessar os setters, força a inicialização de uma referência preguiçosa
   * ({@code getReferenceById}), como faz {@code CategoryService.update}.
   * </p>
   *
   * @param request dados de atualização
   * @param entity  categoria a ser modificada
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
   * Converte a entidade em resposta <b>resumida</b>: somente {@code id} e
   * {@code name}. Descrição, {@code active}, datas e produtos não são expostos.
   *
   * @param entity categoria
   * @return resposta ou {@code null} se {@code entity} for {@code null}
   */
  public CategoryResponse toResponse(Category entity) {
    if (entity == null) {
      return null;
    }

    return new CategoryResponse(entity.getId(), entity.getName());
  }

  /**
   * Converte a entidade em resposta <b>detalhada</b>: {@code id}, {@code name},
   * {@code description} e {@code active}. Datas e produtos não são expostos.
   *
   * @param entity categoria
   * @return resposta ou {@code null} se {@code entity} for {@code null}
   */
  public CategoryDetailsResponse toDetailsResponse(Category entity) {
    if (entity == null) {
      return null;
    }

    return new CategoryDetailsResponse(entity.getId(), entity.getName(), entity.getDescription(), entity.isActive());
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