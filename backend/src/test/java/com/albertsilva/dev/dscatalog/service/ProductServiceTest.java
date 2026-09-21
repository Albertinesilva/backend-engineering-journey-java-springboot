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

import com.albertsilva.dev.dscatalog.domain.catalog.Product;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.dscatalog.factory.ProductFactory;
import com.albertsilva.dev.dscatalog.mapper.product.ProductMapper;
import com.albertsilva.dev.dscatalog.projection.ProductProjection;
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
  @DisplayName("Create Operations")
  class CreateOperations {

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
      Assertions.assertEquals("error.product.notFound", exception.getMessage());

      // Verify
      Mockito.verify(repository).findById(nonExistingId);
      Mockito.verify(productMapper, Mockito.never()).toDetailsResponse(Mockito.any());
    }
  }

  @Nested
  @DisplayName("Search Operations")
  class SearchOperations {

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
  @DisplayName("FindAllPaged Operations")
  class FindAllPagedOperations {

    @Test
    @DisplayName("findAllPaged should search without category filter when categoryId is 0")
    void findAllPagedShouldSearchWithoutCategoryFilterWhenCategoryIdIsZero() {

      // Arrange
      Page<ProductProjection> projections = new PageImpl<>(List.of(), pageable, 0);

      Mockito.when(repository.searchProducts(List.of(), "pc", pageable)).thenReturn(projections);
      Mockito.when(repository.searchProductsWithCategories(List.of())).thenReturn(List.of());

      // Act
      Page<ProductResponse> result = service.findAllPaged("pc", "0", pageable);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertTrue(result.isEmpty());
      Assertions.assertEquals(0, result.getTotalElements());

      // Verify
      Mockito.verify(repository).searchProducts(List.of(), "pc", pageable);
      Mockito.verify(repository).searchProductsWithCategories(List.of());
    }

    @Test
    @DisplayName("findAllPaged should split comma separated categoryIds before searching")
    void findAllPagedShouldSplitCommaSeparatedCategoryIdsBeforeSearching() {

      // Arrange
      Page<ProductProjection> projections = new PageImpl<>(List.of(), pageable, 0);

      Mockito.when(repository.searchProducts(List.of(1L, 3L), "", pageable)).thenReturn(projections);
      Mockito.when(repository.searchProductsWithCategories(List.of())).thenReturn(List.of());

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "1,3", pageable);

      // Assert
      Assertions.assertNotNull(result);

      // Verify
      Mockito.verify(repository).searchProducts(List.of(1L, 3L), "", pageable);
    }

    @Test
    @DisplayName("findAllPaged should keep the page order and the total of the native query")
    void findAllPagedShouldKeepPageOrderAndTotalOfNativeQuery() {

      // Arrange
      ProductProjection first = projectionWithId(3L);
      ProductProjection second = projectionWithId(1L);
      ProductProjection third = projectionWithId(2L);
      Page<ProductProjection> projections = new PageImpl<>(List.of(first, second, third), pageable, 30);

      Product product1 = productWithId(1L);
      Product product2 = productWithId(2L);
      Product product3 = productWithId(3L);

      // O repositório não garante ordem: devolve fora da ordem da página
      Mockito.when(repository.searchProducts(List.of(), "", pageable)).thenReturn(projections);
      Mockito.when(repository.searchProductsWithCategories(List.of(3L, 1L, 2L)))
          .thenReturn(List.of(product1, product2, product3));
      Mockito.when(productMapper.toResponse(Mockito.any(Product.class))).thenAnswer(invocation -> {
        Product product = invocation.getArgument(0);
        return new ProductResponse(product.getId(), product.getName(), null, null, null, List.of());
      });

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "0", pageable);

      // Assert
      Assertions.assertEquals(List.of(3L, 1L, 2L), result.getContent().stream().map(ProductResponse::id).toList());
      Assertions.assertEquals(30, result.getTotalElements());
      Assertions.assertEquals(pageable, result.getPageable());

      // Verify
      Mockito.verify(repository).searchProductsWithCategories(List.of(3L, 1L, 2L));
      Mockito.verify(productMapper, Mockito.times(3)).toResponse(Mockito.any(Product.class));
    }

    private ProductProjection projectionWithId(Long id) {
      ProductProjection projection = Mockito.mock(ProductProjection.class);
      Mockito.when(projection.getId()).thenReturn(id);
      return projection;
    }

    private Product productWithId(Long id) {
      Product product = ProductFactory.createProduct();
      product.setId(id);
      return product;
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
      Assertions.assertEquals("error.product.notFound", exception.getMessage());

      // Verify
      Mockito.verify(repository).getReferenceById(nonExistingId);
      Mockito.verify(productMapper, Mockito.never()).updateEntity(Mockito.any(), Mockito.any());
      Mockito.verify(repository, Mockito.never()).save(Mockito.any());
    }
  }

  @Nested
  @DisplayName("Activate Operations")
  class ActivateOperations {

    @Test
    @DisplayName("activate should set active to true when id exists")
    void activateShouldSetActiveToTrueWhenIdExists() {

      // Arrange
      Product product = ProductFactory.createProduct();
      product.setId(existingId);
      product.setActive(false);

      Mockito.when(repository.findById(existingId)).thenReturn(Optional.of(product));

      // Act
      Assertions.assertDoesNotThrow(() -> service.activate(existingId));

      // Assert
      Assertions.assertTrue(product.isActive());

      // Verify
      Mockito.verify(repository).findById(existingId);
    }

    @Test
    @DisplayName("activate should throw ResourceNotFoundException when id does not exist")
    void activateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.activate(nonExistingId));

      // Assert
      Assertions.assertEquals("error.product.notFound", exception.getMessage());

      // Verify
      Mockito.verify(repository).findById(nonExistingId);
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("deactivate should set active to false when id exists")
    void deactivateShouldSetActiveToFalseWhenIdExists() {

      // Arrange
      Product product = ProductFactory.createProduct();
      product.setId(existingId);
      product.setActive(true);

      Mockito.when(repository.findById(existingId)).thenReturn(Optional.of(product));

      // Act
      Assertions.assertDoesNotThrow(() -> service.deactivate(existingId));

      // Assert
      Assertions.assertFalse(product.isActive());

      // Verify
      Mockito.verify(repository).findById(existingId);
    }

    @Test
    @DisplayName("deactivate should throw ResourceNotFoundException when id does not exist")
    void deactivateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      Mockito.when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.deactivate(nonExistingId));

      // Assert
      Assertions.assertEquals("error.product.notFound", exception.getMessage());

      // Verify
      Mockito.verify(repository).findById(nonExistingId);
    }
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
      Assertions.assertEquals("error.product.notFound", exception.getMessage());

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
      Assertions.assertEquals("error.database.product.relatedEntities", exception.getMessage());

      // Verify
      Mockito.verify(repository).findById(dependentId);
      Mockito.verify(repository).delete(product);
    }
  }

}