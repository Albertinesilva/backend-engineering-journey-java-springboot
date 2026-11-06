package com.albertsilva.dev.asjcatalog.mapper.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;

@DisplayName("Tests for CategoryMapper")
class CategoryMapperTest {

  private final CategoryMapper mapper = new CategoryMapper();

  private static Category categoryWithId(Long id) {
    Category category = CategoryFactory.createCategory();
    category.setId(id);
    return category;
  }

  @Nested
  @DisplayName("ToEntity Operations")
  class ToEntityOperations {

    @Test
    @DisplayName("toEntity should copy name and description when request is given")
    void toEntityShouldCopyNameAndDescriptionWhenRequestIsGiven() {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();

      // Act
      Category result = mapper.toEntity(request);

      // Assert
      assertThat(result.getId()).isNull();
      assertThat(result.getName()).isEqualTo(request.name());
      assertThat(result.getDescription()).isEqualTo(request.description());
    }

    @Test
    @DisplayName("toEntity should return null when request is null")
    void toEntityShouldReturnNullWhenRequestIsNull() {

      // Act
      Category result = mapper.toEntity(null);

      // Assert
      assertThat(result).isNull();
    }
  }

  /**
   * Testes de {@code CategoryMapper.updateEntity}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code name} ou {@code description} nulo: o valor atual é mantido, como
   * numa atualização parcial; o padrão de atualização ainda não foi decidido
   * (#28).</li>
   * </ul>
   */
  @Nested
  @DisplayName("UpdateEntity Operations")
  class UpdateEntityOperations {

    @Test
    @DisplayName("updateEntity should replace name and description when request has both")
    void updateEntityShouldReplaceNameAndDescriptionWhenRequestHasBoth() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      Category category = categoryWithId(CategoryFactory.EXISTING_ID);

      // Act
      mapper.updateEntity(request, category);

      // Assert
      assertThat(category.getId()).isEqualTo(CategoryFactory.EXISTING_ID);
      assertThat(category.getName()).isEqualTo(request.name());
      assertThat(category.getDescription()).isEqualTo(request.description());
    }

    @Test
    @DisplayName("updateEntity should keep entity unchanged when request is null")
    void updateEntityShouldKeepEntityUnchangedWhenRequestIsNull() {

      // Arrange
      Category category = categoryWithId(CategoryFactory.EXISTING_ID);
      Category original = categoryWithId(CategoryFactory.EXISTING_ID);

      // Act
      mapper.updateEntity(null, category);

      // Assert
      assertThat(category.getName()).isEqualTo(original.getName());
      assertThat(category.getDescription()).isEqualTo(original.getDescription());
    }

    @Test
    @DisplayName("updateEntity should ignore call when entity is null")
    void updateEntityShouldIgnoreCallWhenEntityIsNull() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      // Act & Assert
      assertThatCode(() -> mapper.updateEntity(request, null)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("ToResponse Operations")
  class ToResponseOperations {

    @Test
    @DisplayName("toResponse should map id and name when entity is given")
    void toResponseShouldMapIdAndNameWhenEntityIsGiven() {

      // Arrange
      Category category = categoryWithId(CategoryFactory.EXISTING_ID);

      // Act
      CategoryResponse result = mapper.toResponse(category);

      // Assert
      assertThat(result).isEqualTo(new CategoryResponse(category.getId(), category.getName()));
    }

    @Test
    @DisplayName("toResponse should return null when entity is null")
    void toResponseShouldReturnNullWhenEntityIsNull() {

      // Act
      CategoryResponse result = mapper.toResponse(null);

      // Assert
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("ToDetailsResponse Operations")
  class ToDetailsResponseOperations {

    @Test
    @DisplayName("toDetailsResponse should map all fields when entity is given")
    void toDetailsResponseShouldMapAllFieldsWhenEntityIsGiven() {

      // Arrange
      Category category = categoryWithId(CategoryFactory.EXISTING_ID);
      category.setActive(false);

      // Act
      CategoryDetailsResponse result = mapper.toDetailsResponse(category);

      // Assert
      assertThat(result).isEqualTo(new CategoryDetailsResponse(category.getId(), category.getName(),
          category.getDescription(), false));
    }

    @Test
    @DisplayName("toDetailsResponse should return null when entity is null")
    void toDetailsResponseShouldReturnNullWhenEntityIsNull() {

      // Act
      CategoryDetailsResponse result = mapper.toDetailsResponse(null);

      // Assert
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("ToResponsePage Operations")
  class ToResponsePageOperations {

    @Test
    @DisplayName("toResponsePage should map every category and keep pagination when page is given")
    void toResponsePageShouldMapEveryCategoryAndKeepPaginationWhenPageIsGiven() {

      // Arrange
      Category first = categoryWithId(CategoryFactory.EXISTING_ID);
      Category second = categoryWithId(CategoryFactory.OTHER_ID);
      PageRequest pageable = PageRequest.of(1, 2);
      Page<Category> page = new PageImpl<>(List.of(first, second), pageable, 5);

      // Act
      Page<CategoryResponse> result = mapper.toResponsePage(page);

      // Assert
      assertThat(result.getContent()).containsExactly(
          new CategoryResponse(first.getId(), first.getName()),
          new CategoryResponse(second.getId(), second.getName()));
      assertThat(result.getPageable()).isEqualTo(pageable);
      assertThat(result.getTotalElements()).isEqualTo(5);
    }
  }
}
