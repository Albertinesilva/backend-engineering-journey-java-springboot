package com.albertsilva.dev.dscatalog.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.dscatalog.entity.Product;
import com.albertsilva.dev.dscatalog.factory.ProductFactory;
import com.albertsilva.dev.dscatalog.mapper.product.ProductMapper;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.albertsilva.dev.dscatalog.service.exception.DatabaseException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

// Meus testes unitários validam fluxo, estado, comportamento e tratamento de exceções utilizando JUnit e Mockito.
// Nos testes unitários, mocko exceções técnicas da camada inferior para validar se a camada de serviço faz corretamente a tradução para exceções de negócio.
@DisplayName("Tests for ProductService")
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @InjectMocks
  private ProductService service;

  @Mock
  private ProductRepository repository;

  @Mock
  private ProductMapper productMapper;

  private Long existingId;
  private Long nonExistingId;
  private Long dependentId;
  private Pageable pageable;
  private PageImpl<Product> page;

  @BeforeEach
  void setUp() {
    existingId = 1L;
    nonExistingId = 1000L;
    dependentId = 4L;
    pageable = PageRequest.of(0, 10);
    page = new PageImpl<>(List.of(ProductFactory.createProduct()));
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove product when id exists")
    void deleteShouldRemoveProductWhenIdExists() {

      // Arrange
      Product product = ProductFactory.createProduct();
      product.setId(existingId);

      Mockito.when(repository.findById(existingId)).thenReturn(Optional.of(product));

      // Act + Assert
      Assertions.assertDoesNotThrow(() -> service.delete(existingId));

      // Verify
      Mockito.verify(repository).findById(existingId);
      Mockito.verify(repository).delete(product);
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.delete(nonExistingId));

      // Assert
      Assertions.assertEquals("Entity not found id: " + nonExistingId, exception.getMessage());

      // Verify
      Mockito.verify(repository).findById(nonExistingId);
      Mockito.verify(repository, Mockito.never()).delete(Mockito.any());
    }

    @Test
    @DisplayName("delete should throw DatabaseException when integrity violation occurs")
    void deleteShouldThrowDatabaseExceptionWhenDependentId() {

      // Arrange
      Product product = ProductFactory.createProduct();
      product.setId(dependentId);

      Mockito.when(repository.findById(dependentId)).thenReturn(Optional.of(product));

      Mockito.doThrow(DataIntegrityViolationException.class).when(repository).delete(product);

      // Act
      DatabaseException exception = Assertions.assertThrows(DatabaseException.class, () -> service.delete(dependentId));

      // Assert
      Assertions.assertEquals("Integrity violation: cannot delete product with related entities",
          exception.getMessage());

      // Verify
      Mockito.verify(repository).findById(dependentId);
      Mockito.verify(repository).delete(product);
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return product when id exists")
    void findByIdShouldReturnProductWhenIdExists() {

      // Arrange
      Product product = ProductFactory.createProduct();
      product.setId(existingId);

      ProductDetailsResponse expectedResponse = Mockito.mock(ProductDetailsResponse.class);

      Mockito.when(repository.findById(existingId)).thenReturn(Optional.of(product));
      Mockito.when(productMapper.toDetailsResponse(product)).thenReturn(expectedResponse);

      // Act
      ProductDetailsResponse result = service.findById(existingId);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertEquals(expectedResponse, result);

      // Verify
      Mockito.verify(repository).findById(existingId);
      Mockito.verify(productMapper).toDetailsResponse(product);
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
    void findByIdShouldThrowExceptionWhenIdDoesNotExist() {

      // Arrange
      Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.findById(nonExistingId));

      // Assert
      Assertions.assertEquals("Entity not found id: " + nonExistingId, exception.getMessage());

      // Verify
      Mockito.verify(repository).findById(nonExistingId);
      Mockito.verify(productMapper, Mockito.never()).toDetailsResponse(Mockito.any());
    }
  }

  @Nested
  @DisplayName("FindAllPaged Operations")
  class FindAllPagedOperations {

    @Test
    @DisplayName("search should return page when name is empty")
    void searchShouldReturnPageWhenNameIsEmpty() {

      // Arrange
      String name = "";
      Page<ProductResponse> expectedPage = new PageImpl<>(List.of());

      Mockito.when(repository.findAll(pageable)).thenReturn(page);
      Mockito.when(productMapper.toResponsePage(page)).thenReturn(expectedPage);

      // Act
      Page<ProductResponse> result = service.search(name, pageable);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertEquals(expectedPage, result);

      Mockito.verify(repository).findAll(pageable);
      Mockito.verify(productMapper).toResponsePage(page);
    }

    @Test
    @DisplayName("search should return filtered page when name exists")
    void searchShouldReturnFilteredPageWhenNameExists() {

      // Arrange
      String name = "pc";
      Page<ProductResponse> expectedPage = new PageImpl<>(List.of());

      Mockito.when(repository.findByNameContainingIgnoreCase(name, pageable)).thenReturn(page);

      Mockito.when(productMapper.toResponsePage(page)).thenReturn(expectedPage);

      // Act
      Page<ProductResponse> result = service.search(name, pageable);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertEquals(expectedPage, result);

      Mockito.verify(repository).findByNameContainingIgnoreCase(name, pageable);

      Mockito.verify(productMapper).toResponsePage(page);
    }

    @Test
    @DisplayName("search should trim name before filtering")
    void searchShouldTrimNameBeforeFiltering() {

      // Arrange
      String name = "  pc  ";
      Page<ProductResponse> expectedPage = new PageImpl<>(List.of());

      Mockito.when(repository.findByNameContainingIgnoreCase("pc", pageable)).thenReturn(page);

      Mockito.when(productMapper.toResponsePage(page)).thenReturn(expectedPage);

      // Act
      Page<ProductResponse> result = service.search(name, pageable);

      // Assert
      Assertions.assertNotNull(result);

      Mockito.verify(repository).findByNameContainingIgnoreCase("pc", pageable);
    }
  }

  @Nested
  @DisplayName("Insert Operations")
  class InsertOperations {

    @Test
    @DisplayName("create should save product successfully")
    void createShouldSaveProduct() {

      // Arrange
      ProductCreateRequest request = Mockito.mock(ProductCreateRequest.class);

      Product product = ProductFactory.createProduct();
      product.setId(existingId);

      ProductResponse expectedResponse = Mockito.mock(ProductResponse.class);

      Mockito.when(productMapper.toEntity(request)).thenReturn(product);
      Mockito.when(request.categoryIds()).thenReturn(List.of());
      Mockito.when(repository.save(product)).thenReturn(product);
      Mockito.when(productMapper.toResponse(product)).thenReturn(expectedResponse);

      // Act
      ProductResponse result = service.create(request);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertEquals(expectedResponse, result);

      // Verify
      Mockito.verify(productMapper).toEntity(request);
      Mockito.verify(repository).save(product);
      Mockito.verify(productMapper).toResponse(product);
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should update product when id exists")
    void updateShouldUpdateProductWhenIdExists() {

      // Arrange
      Product product = ProductFactory.createProduct();
      product.setId(existingId);

      ProductUpdateRequest request = Mockito.mock(ProductUpdateRequest.class);

      ProductResponse expectedResponse = Mockito.mock(ProductResponse.class);

      Mockito.when(request.categoryIds()).thenReturn(null);
      Mockito.when(repository.getReferenceById(existingId)).thenReturn(product);
      Mockito.when(repository.save(product)).thenReturn(product);
      Mockito.when(productMapper.toResponse(product)).thenReturn(expectedResponse);

      // Act
      ProductResponse result = service.update(existingId, request);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertEquals(expectedResponse, result);

      // Verify
      Mockito.verify(repository).getReferenceById(existingId);
      Mockito.verify(productMapper).updateEntity(request, product);
      Mockito.verify(repository).save(product);
      Mockito.verify(productMapper).toResponse(product);
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowExceptionWhenIdDoesNotExist() {

      // Arrange
      ProductUpdateRequest request = Mockito.mock(ProductUpdateRequest.class);

      Mockito.when(repository.getReferenceById(nonExistingId)).thenThrow(EntityNotFoundException.class);

      // Act
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.update(nonExistingId, request));

      // Assert
      Assertions.assertEquals("Entity not found id: " + nonExistingId, exception.getMessage());

      // Verify
      Mockito.verify(repository).getReferenceById(nonExistingId);
      Mockito.verify(productMapper, Mockito.never()).updateEntity(Mockito.any(), Mockito.any());
      Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }
  }
}