package com.albertsilva.dev.asjcatalog.integrations.service;

import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.COUNT_TOTAL_CATEGORIES;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.DEPENDENT_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NON_DEPENDENT_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;
import com.albertsilva.dev.asjcatalog.service.CategoryService;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

/**
 * Testes de integração do {@link CategoryService} com o banco H2 do perfil
 * {@code test}.
 *
 * <p>
 * Dependem do seed ({@code import.sql}): o total de categorias
 * ({@code COUNT_TOTAL_CATEGORIES}), os ids fixos de {@code CategoryFactory} e
 * as categorias com "book" no nome (#21).
 * </p>
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Integration tests for CategoryService")
class CategoryServiceIT {

  private static final String NOT_FOUND_KEY = "error.category.notFound";

  @Autowired
  private CategoryService service;

  @Autowired
  private CategoryRepository repository;

  private Pageable pageable;

  @BeforeEach
  void setUp() {
    pageable = PageRequest.of(0, 10);
  }

  @Nested
  @DisplayName("Search Operations")
  class SearchOperations {

    @Test
    @DisplayName("search should return first page of all categories when name is empty")
    void searchShouldReturnFirstPageOfAllCategoriesWhenNameIsEmpty() {

      // Act
      Page<CategoryResponse> result = service.search("", pageable);

      // Assert
      assertThat(result.getNumber()).isZero();
      assertThat(result.getSize()).isEqualTo(10);
      assertThat(result.getContent()).hasSize(10);
      assertThat(result.getTotalElements()).isEqualTo(COUNT_TOTAL_CATEGORIES);
    }

    @Test
    @DisplayName("search should return only matching categories when name has text")
    void searchShouldReturnOnlyMatchingCategoriesWhenNameHasText() {

      // Arrange
      Category created = repository.save(new Category("Bookshelf Test", null, true));

      // Act
      Page<CategoryResponse> result = service.search("book", pageable);

      // Assert
      assertThat(result.getContent())
          .extracting(CategoryResponse::name)
          .contains(created.getName())
          .allSatisfy(name -> assertThat(name).containsIgnoringCase("book"));
    }

    @Test
    @DisplayName("search should trim name when name has surrounding spaces")
    void searchShouldTrimNameWhenNameHasSurroundingSpaces() {

      // Arrange
      Page<CategoryResponse> trimmed = service.search("book", pageable);

      // Act
      Page<CategoryResponse> result = service.search("   book   ", pageable);

      // Assert
      assertThat(result.getTotalElements()).isPositive().isEqualTo(trimmed.getTotalElements());
      assertThat(result.getContent())
          .extracting(CategoryResponse::name)
          .allSatisfy(name -> assertThat(name).containsIgnoringCase("book"));
    }

    @Test
    @DisplayName("search should return all categories when name is blank")
    void searchShouldReturnAllCategoriesWhenNameIsBlank() {

      // Act
      Page<CategoryResponse> result = service.search("   ", pageable);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(COUNT_TOTAL_CATEGORIES);
    }

    @Test
    @DisplayName("search should return all categories when name is null")
    void searchShouldReturnAllCategoriesWhenNameIsNull() {

      // Act
      Page<CategoryResponse> result = service.search(null, pageable);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(COUNT_TOTAL_CATEGORIES);
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return category when id exists")
    void findByIdShouldReturnCategoryWhenIdExists() {

      // Arrange
      Category expected = repository.findById(EXISTING_ID).orElseThrow();

      // Act
      CategoryDetailsResponse result = service.findById(EXISTING_ID);

      // Assert
      assertThat(result.id()).isEqualTo(EXISTING_ID);
      assertThat(result.name()).isEqualTo(expected.getName());
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
    void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThatThrownBy(() -> service.findById(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }

  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("create should persist category when request is valid")
    void createShouldPersistCategoryWhenRequestIsValid() {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();
      long countBefore = repository.count();

      // Act
      CategoryResponse result = service.create(request);

      // Assert
      assertThat(result.id()).isNotNull();
      assertThat(result.name()).isEqualTo(request.name());
      assertThat(repository.count()).isEqualTo(countBefore + 1);
      assertThat(repository.findById(result.id()))
          .get()
          .extracting(Category::getName)
          .isEqualTo(request.name());
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should persist new name when id exists")
    void updateShouldPersistNewNameWhenIdExists() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      // Act
      CategoryResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertThat(result.id()).isEqualTo(EXISTING_ID);
      assertThat(result.name()).isEqualTo(request.name());
      assertThat(service.findById(EXISTING_ID).name()).isEqualTo(request.name());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      // Act + Assert
      assertThatThrownBy(() -> service.update(NON_EXISTING_ID, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove category when id exists and category has no dependencies")
    void deleteShouldRemoveCategoryWhenIdExistsAndCategoryHasNoDependencies() {

      // Act
      service.delete(NON_DEPENDENT_ID);

      // Assert
      assertThat(repository.count()).isEqualTo(COUNT_TOTAL_CATEGORIES - 1);
      assertThat(repository.existsById(NON_DEPENDENT_ID)).isFalse();
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThatThrownBy(() -> service.delete(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }

    // #5: a exclusão de categoria com produtos ainda não tem tratamento próprio; a
    // DataIntegrityViolationException sobe do flush e vira o 409 genérico no handler.
    @Test
    @DisplayName("delete should throw DataIntegrityViolationException when category has associated products")
    void deleteShouldThrowDataIntegrityViolationExceptionWhenCategoryHasAssociatedProducts() {

      // Act + Assert
      assertThatThrownBy(() -> {
        service.delete(DEPENDENT_ID);
        repository.flush();
      }).isInstanceOf(DataIntegrityViolationException.class);
    }
  }
}
