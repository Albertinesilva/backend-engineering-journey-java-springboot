package com.albertsilva.dev.dscatalog.integrations.service;

import static com.albertsilva.dev.dscatalog.factory.ProductFactory.COUNT_TOTAL_PRODUCTS;
import static com.albertsilva.dev.dscatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.ProductFactory.NON_EXISTING_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.dscatalog.factory.ProductFactory;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.albertsilva.dev.dscatalog.service.ProductService;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
@DisplayName("ProductService Integration Tests")
class ProductServiceIT {

  @Autowired
  private ProductService service;

  @Autowired
  private ProductRepository repository;

  @Nested
  @DisplayName("Read Operations")
  class ReadOperations {

    @Nested
    @DisplayName("FindAllPaged Operations")
    class FindAllPagedOperations {

      @Test
      @DisplayName("findAllPaged should return paged products when page 0 size 10")
      void findAllPagedShouldReturnPagedProductsWhenPage0Size10() {

        // Arrange
        String name = "";
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Act
        Page<ProductResponse> result = service.search(name, pageRequest);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(COUNT_TOTAL_PRODUCTS, result.getTotalElements());
      }

      @Test
      @DisplayName("findAllPaged should return empty page when page does not exist")
      void findAllPagedShouldReturnEmptyPageWhenPageDoesNotExist() {

        // Arrange
        String name = "";
        PageRequest pageRequest = PageRequest.of(50, 10);

        // Act
        Page<ProductResponse> result = service.search(name, pageRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(50, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(COUNT_TOTAL_PRODUCTS, result.getTotalElements());
      }

      @Test
      @DisplayName("findAllPaged should return ordered page when sorting by name")
      void findAllPagedShouldReturnOrderedPageWhenSortingByName() {

        // Arrange
        String name = "";
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("name"));

        // Act
        Page<ProductResponse> result = service.search(name, pageRequest);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Macbook Pro", result.getContent().get(0).name());
        assertEquals("PC Gamer", result.getContent().get(1).name());
        assertEquals("PC Gamer Alfa", result.getContent().get(2).name());
      }

      @Test
      @DisplayName("findAllPaged should return filtered products when name exists")
      void findAllPagedShouldReturnFilteredProductsWhenNameExists() {

        // Arrange
        String name = "pc";
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Act
        Page<ProductResponse> result = service.search(name, pageRequest);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());

        result.getContent().forEach(product -> assertTrue(product.name().toLowerCase().contains(name)));
      }
    }

    @Nested
    @DisplayName("FindById Operations")
    class FindByIdOperations {

      @Test
      @DisplayName("findById should return product details when id exists")
      void findByIdShouldReturnProductDetailsWhenIdExists() {

        // Act
        ProductDetailsResponse result = service.findById(EXISTING_ID);

        // Assert
        assertNotNull(result);
        assertEquals(EXISTING_ID, result.id());
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
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("insert should persist product when valid data")
    void insertShouldPersistProductWhenValidData() {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();

      // Act
      ProductResponse result = service.create(request);

      // Assert
      assertNotNull(result);
      assertNotNull(result.id());
      assertEquals(COUNT_TOTAL_PRODUCTS + 1, repository.count());
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should update product when id exists")
    void updateShouldUpdateProductWhenIdExists() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

      // Act
      ProductResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertNotNull(result);
      assertEquals(EXISTING_ID, result.id());
      assertEquals(request.name(), result.name());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.update(NON_EXISTING_ID, request));
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove product when id exists")
    void deleteShouldRemoveProductWhenIdExists() {

      // Act
      service.delete(EXISTING_ID);

      // Assert
      Assertions.assertEquals(COUNT_TOTAL_PRODUCTS - 1, repository.count());
      assertFalse(repository.existsById(EXISTING_ID));
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.delete(NON_EXISTING_ID));
    }
  }
}
