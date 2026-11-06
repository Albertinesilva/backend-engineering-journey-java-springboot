package com.albertsilva.dev.asjcatalog.service;

import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NAME_FILTER;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.mapper.category.CategoryMapper;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

@DisplayName("Tests for CategoryService")
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @InjectMocks
  private CategoryService service;

  @Mock
  private CategoryRepository repository;

  @Mock
  private CategoryMapper categoryMapper;

  private Pageable pageable;
  private Page<Category> categoryPage;
  private Page<CategoryResponse> responsePage;

  @BeforeEach
  void setUp() {
    pageable = PageRequest.of(0, 10);
    categoryPage = new PageImpl<>(List.of(CategoryFactory.createCategory()));
    responsePage = new PageImpl<>(List.of(CategoryFactory.createCategoryResponse()), pageable, 1);
  }

  private static Category categoryWithId(Long id) {
    Category category = CategoryFactory.createCategory();
    category.setId(id);
    return category;
  }

  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("create should activate and save category when request is valid")
    void createShouldActivateAndSaveCategoryWhenRequestIsValid() {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();
      Category category = CategoryFactory.createCategory();
      category.setActive(false);
      CategoryResponse expectedResponse = CategoryFactory.createCategoryResponse();

      when(categoryMapper.toEntity(request)).thenReturn(category);
      when(repository.save(category)).thenReturn(category);
      when(categoryMapper.toResponse(category)).thenReturn(expectedResponse);

      // Act
      CategoryResponse result = service.create(request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(category.isActive()).isTrue();
      verify(repository).save(category);
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return category when id exists")
    void findByIdShouldReturnCategoryWhenIdExists() {

      // Arrange
      Category category = categoryWithId(EXISTING_ID);
      CategoryDetailsResponse expectedResponse = CategoryFactory.createCategoryDetailsResponse();

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(category));
      when(categoryMapper.toDetailsResponse(category)).thenReturn(expectedResponse);

      // Act
      CategoryDetailsResponse result = service.findById(EXISTING_ID);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      verify(repository).findById(EXISTING_ID);
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
    void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.findById(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.category.notFound");

      verify(categoryMapper, never()).toDetailsResponse(any());
    }
  }

  @Nested
  @DisplayName("Search Operations")
  class SearchOperations {

    @Test
    @DisplayName("search should return all categories when name is empty")
    void searchShouldReturnAllCategoriesWhenNameIsEmpty() {

      // Arrange
      when(repository.findAll(pageable)).thenReturn(categoryPage);
      when(categoryMapper.toResponsePage(categoryPage)).thenReturn(responsePage);

      // Act
      Page<CategoryResponse> result = service.search("", pageable);

      // Assert
      assertThat(result).isSameAs(responsePage);
      verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("search should return all categories when name is null")
    void searchShouldReturnAllCategoriesWhenNameIsNull() {

      // Arrange
      when(repository.findAll(pageable)).thenReturn(categoryPage);
      when(categoryMapper.toResponsePage(categoryPage)).thenReturn(responsePage);

      // Act
      Page<CategoryResponse> result = service.search(null, pageable);

      // Assert
      assertThat(result).isSameAs(responsePage);
      verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("search should return all categories when name is blank")
    void searchShouldReturnAllCategoriesWhenNameIsBlank() {

      // Arrange
      when(repository.findAll(pageable)).thenReturn(categoryPage);
      when(categoryMapper.toResponsePage(categoryPage)).thenReturn(responsePage);

      // Act
      Page<CategoryResponse> result = service.search("   ", pageable);

      // Assert
      assertThat(result).isSameAs(responsePage);
      verify(repository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("search should return categories filtered by name when name has text")
    void searchShouldReturnCategoriesFilteredByNameWhenNameHasText() {

      // Arrange
      when(repository.findByNameContainingIgnoreCase(NAME_FILTER, pageable)).thenReturn(categoryPage);
      when(categoryMapper.toResponsePage(categoryPage)).thenReturn(responsePage);

      // Act
      Page<CategoryResponse> result = service.search(NAME_FILTER, pageable);

      // Assert
      assertThat(result).isSameAs(responsePage);
      verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("search should trim name when name has surrounding spaces")
    void searchShouldTrimNameWhenNameHasSurroundingSpaces() {

      // Arrange
      when(repository.findByNameContainingIgnoreCase(NAME_FILTER, pageable)).thenReturn(categoryPage);
      when(categoryMapper.toResponsePage(categoryPage)).thenReturn(responsePage);

      // Act
      Page<CategoryResponse> result = service.search("  " + NAME_FILTER + "  ", pageable);

      // Assert
      assertThat(result).isSameAs(responsePage);
      verify(repository).findByNameContainingIgnoreCase(NAME_FILTER, pageable);
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should apply request and save category when id exists")
    void updateShouldApplyRequestAndSaveCategoryWhenIdExists() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      Category category = categoryWithId(EXISTING_ID);
      CategoryResponse expectedResponse = CategoryFactory.createUpdatedCategoryResponse();

      when(repository.getReferenceById(EXISTING_ID)).thenReturn(category);
      when(repository.save(category)).thenReturn(category);
      when(categoryMapper.toResponse(category)).thenReturn(expectedResponse);

      // Act
      CategoryResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      verify(categoryMapper).updateEntity(request, category);
      verify(repository).save(category);
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      Category reference = new Category();

      // getReferenceById não consulta o banco: o proxy só lança
      // EntityNotFoundException quando é acessado, aqui pelo mapper.
      when(repository.getReferenceById(NON_EXISTING_ID)).thenReturn(reference);
      doThrow(EntityNotFoundException.class).when(categoryMapper).updateEntity(request, reference);

      // Act & Assert
      assertThatThrownBy(() -> service.update(NON_EXISTING_ID, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.category.notFound");

      verify(repository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Activate Operations")
  class ActivateOperations {

    @Test
    @DisplayName("activate should set active to true when category is inactive")
    void activateShouldSetActiveToTrueWhenCategoryIsInactive() {

      // Arrange
      Category category = categoryWithId(EXISTING_ID);
      category.setActive(false);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(category));

      // Act
      service.activate(EXISTING_ID);

      // Assert
      assertThat(category.isActive()).isTrue();
    }

    @Test
    @DisplayName("activate should not change category when category is already active")
    void activateShouldNotChangeCategoryWhenCategoryIsAlreadyActive() {

      // Arrange
      // O spy é necessário porque o estado final (ativa) é o mesmo nos dois
      // caminhos; só o verify prova que o método saiu cedo.
      Category category = spy(categoryWithId(EXISTING_ID));

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(category));

      // Act
      service.activate(EXISTING_ID);

      // Assert
      assertThat(category.isActive()).isTrue();
      verify(category, never()).setActive(anyBoolean());
    }

    @Test
    @DisplayName("activate should throw ResourceNotFoundException when id does not exist")
    void activateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.activate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.category.notFound");
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("deactivate should set active to false when category is active")
    void deactivateShouldSetActiveToFalseWhenCategoryIsActive() {

      // Arrange
      Category category = categoryWithId(EXISTING_ID);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(category));

      // Act
      service.deactivate(EXISTING_ID);

      // Assert
      assertThat(category.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivate should not change category when category is already inactive")
    void deactivateShouldNotChangeCategoryWhenCategoryIsAlreadyInactive() {

      // Arrange
      // O spy é necessário porque o estado final (inativa) é o mesmo nos dois
      // caminhos; só o verify prova que o método saiu cedo. Ele é criado depois
      // do setActive(false), para que o verify não conte a chamada da preparação.
      Category inactive = categoryWithId(EXISTING_ID);
      inactive.setActive(false);
      Category category = spy(inactive);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(category));

      // Act
      service.deactivate(EXISTING_ID);

      // Assert
      assertThat(category.isActive()).isFalse();
      verify(category, never()).setActive(anyBoolean());
    }

    @Test
    @DisplayName("deactivate should throw ResourceNotFoundException when id does not exist")
    void deactivateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.deactivate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.category.notFound");
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove category when id exists")
    void deleteShouldRemoveCategoryWhenIdExists() {

      // Arrange
      Category category = categoryWithId(EXISTING_ID);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(category));

      // Act
      service.delete(EXISTING_ID);

      // Assert
      verify(repository).delete(category);
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.delete(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.category.notFound");

      verify(repository, never()).delete(any());
    }
  }
}
