package com.albertsilva.dev.dscatalog.integrations.service;

import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.COUNT_TOTAL_CATEGORIES;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.DEPENDENT_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_DEPENDENT_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.factory.CategoryFactory;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.service.CategoryService;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

@SpringBootTest
@Transactional
@DisplayName("CategoryService Integration Tests")
class CategoryServiceIT {

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
  @DisplayName("Read Operations")
  class ReadOperations {

    @Nested
    @DisplayName("FindAllPaged Operations")
    class FindAllPagedOperations {

      @Test
      @DisplayName("findAllPaged should return paged categories when name filter is empty")
      void findAllPagedShouldReturnPagedCategoriesWhenNameFilterIsEmpty() {

        // Arrange
        String name = "";

        // Act
        Page<CategoryResponse> result = service.search(name, pageable);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(COUNT_TOTAL_CATEGORIES, result.getTotalElements());
      }

      @Test
      @DisplayName("findAllPaged should return filtered categories when name exists")
      void findAllPagedShouldReturnFilteredCategoriesWhenNameExists() {

        // Arrange
        String name = "book";

        // Act
        Page<CategoryResponse> result = service.search(name, pageable);

        // Assert
        assertNotNull(result);

        result.getContent().forEach(category -> assertTrue(category.name().toLowerCase().contains(name.toLowerCase())));
      }

      @Test
      @DisplayName("findAllPaged should trim name before searching")
      void findAllPagedShouldTrimNameBeforeSearching() {

        // Arrange
        String nameWithSpaces = "   book   ";

        // Act
        Page<CategoryResponse> result = service.search(nameWithSpaces, pageable);

        // Assert
        assertNotNull(result);

        result.getContent().forEach(category -> assertTrue(category.name().toLowerCase().contains("book")));
      }

      @Test
      @DisplayName("findAllPaged should use findAll when name is blank")
      void findAllPagedShouldUseFindAllWhenNameIsBlank() {

        // Arrange
        String name = "   ";

        // Act
        Page<CategoryResponse> result = service.search(name, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(COUNT_TOTAL_CATEGORIES, result.getTotalElements());
      }

      @Test
      @DisplayName("findAllPaged should use findAll when name is null")
      void findAllPagedShouldUseFindAllWhenNameIsNull() {

        // Act
        Page<CategoryResponse> result = service.search(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(COUNT_TOTAL_CATEGORIES, result.getTotalElements());
      }
    }

    @Nested
    @DisplayName("FindById Operations")
    class FindByIdOperations {

      @Test
      @DisplayName("findById should return category when id exists")
      void findByIdShouldReturnCategoryWhenIdExists() {

        // Act
        CategoryResponse result = service.findById(EXISTING_ID);

        // Assert
        assertNotNull(result);
        assertEquals(EXISTING_ID, result.id());
        assertNotNull(result.name());
      }

      @Test
      @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
      void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> service.findById(NON_EXISTING_ID));
      }
    }
  }

  @Nested
  @DisplayName("Insert Operations")
  class InsertOperations {

    @Test
    @DisplayName("insert should create category successfully")
    void insertShouldCreateCategorySuccessfully() {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();

      long countBeforeInsert = repository.count();

      // Act
      CategoryResponse result = service.create(request);

      // Assert
      assertNotNull(result);
      assertNotNull(result.id());
      assertEquals(request.name(), result.name());

      assertEquals(countBeforeInsert + 1, repository.count());
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should update category when id exists")
    void updateShouldUpdateCategoryWhenIdExists() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      // Act
      CategoryResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertNotNull(result);
      assertEquals(EXISTING_ID, result.id());
      assertEquals(request.name(), result.name());

      CategoryResponse updatedCategory = service.findById(EXISTING_ID);

      assertEquals(request.name(), updatedCategory.name());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.update(NON_EXISTING_ID, request));
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
      assertEquals(COUNT_TOTAL_CATEGORIES - 1, repository.count());

      assertFalse(repository.existsById(NON_DEPENDENT_ID));
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.delete(NON_EXISTING_ID));
    }

    @Test
    @DisplayName("delete should throw DataIntegrityViolationException when category has associated products")
    void deleteShouldThrowDataIntegrityViolationExceptionWhenCategoryHasAssociatedProducts() {

      // Arrange
      assertTrue(repository.existsById(DEPENDENT_ID));

      // Act + Assert
      assertThrows(DataIntegrityViolationException.class, () -> {
        service.delete(DEPENDENT_ID);
        repository.flush();
      });
    }
  }
}