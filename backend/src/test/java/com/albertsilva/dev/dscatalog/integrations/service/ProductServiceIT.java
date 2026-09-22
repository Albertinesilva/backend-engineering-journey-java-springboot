package com.albertsilva.dev.dscatalog.integrations.service;

import static com.albertsilva.dev.dscatalog.factory.ProductFactory.COUNT_TOTAL_PRODUCTS;
import static com.albertsilva.dev.dscatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.ProductFactory.NON_EXISTING_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
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
        assertEquals("Air Fryer 5L Digital", result.getContent().get(0).name());
        // Nome lido do repositório (id 155) em vez de literal com "ô": o compilador deste ambiente
        // corrompe literais Java com "ô"/"ê" mesmo com o projeto configurado para UTF-8; o valor
        // persistido é lido corretamente via JDBC, então a comparação permanece exata.
        assertEquals(repository.findById(155L).orElseThrow().getName(), result.getContent().get(1).name());
        assertEquals(repository.findById(162L).orElseThrow().getName(), result.getContent().get(2).name());
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
    @DisplayName("FindAllPaged (real flow) Operations")
    class FindAllPagedRealFlowOperations {

      @Test
      @DisplayName("findAllPaged should return products with their categories when there is no filter")
      void findAllPagedShouldReturnProductsWithCategoriesWhenThereIsNoFilter() {

        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("name"));

        // Act
        Page<ProductResponse> result = service.findAllPaged("", "0", pageRequest);

        // Assert
        assertEquals(COUNT_TOTAL_PRODUCTS, result.getTotalElements());
        assertEquals(10, result.getContent().size());
        assertEquals("Air Fryer 5L Digital", result.getContent().get(0).name());
        result.getContent().forEach(product -> assertFalse(product.categories().isEmpty()));

        Set<String> categoryNames = result.getContent().get(0).categories().stream().map(CategoryResponse::name)
            .collect(Collectors.toSet());
        assertEquals(Set.of("Home Appliances"), categoryNames);
      }

      @Test
      @DisplayName("findAllPaged should return only products of the informed category")
      void findAllPagedShouldReturnOnlyProductsOfTheInformedCategory() {

        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("name"));

        // Act
        Page<ProductResponse> result = service.findAllPaged("", "2", pageRequest);

        // Assert
        assertEquals(27, result.getTotalElements());
        assertEquals(List.of("Clean Architecture", "Clean Code", "Continuous Delivery",
            "Design Patterns: Elements of Reusable Object-Oriented Software", "Designing Data-Intensive Applications",
            "Domain-Driven Design: Tackling Complexity in the Heart of Software", "Effective Java", "Eloquent JavaScript",
            "Enterprise Integration Patterns", "Growing Object-Oriented Software, Guided by Tests"),
            result.getContent().stream().map(ProductResponse::name).toList());
      }

      @Test
      @DisplayName("findAllPaged should not repeat a product that belongs to more than one informed category")
      void findAllPagedShouldNotRepeatProductThatBelongsToMoreThanOneInformedCategory() {

        // Arrange: "Carregador Veicular Inteligente" pertence simultaneamente às categorias 1 (Electronics)
        // e 12 (Automotive); a combinação não pode devolvê-lo duas vezes.
        PageRequest pageRequest = PageRequest.of(0, 30, Sort.by("name"));

        // Act
        Page<ProductResponse> result = service.findAllPaged("", "1,12", pageRequest);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(1, result.getContent().stream()
            .filter(product -> "Carregador Veicular Inteligente".equals(product.name())).count());
      }

      @Test
      @DisplayName("findAllPaged should combine name and category filters")
      void findAllPagedShouldCombineNameAndCategoryFilters() {

        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Act
        Page<ProductResponse> result = service.findAllPaged("RAILS", "2", pageRequest);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Ruby on Rails For Dummies", result.getContent().get(0).name());
      }

      @Test
      @DisplayName("findAllPaged should keep the requested descending order")
      void findAllPagedShouldKeepTheRequestedDescendingOrder() {

        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "name"));

        // Act
        Page<ProductResponse> result = service.findAllPaged("", "0", pageRequest);

        // Assert
        // 3º nome lido do repositório (id 121) pelo mesmo motivo do teste de ordenação ascendente:
        // literal Java com "ê" é corrompido pelo compilador deste ambiente mesmo com UTF-8 configurado.
        String thirdName = repository.findById(121L).orElseThrow().getName();
        assertEquals(List.of("xUnit Test Patterns", "Webcam Full HD Stream", thirdName),
            result.getContent().stream().map(ProductResponse::name).toList());
      }

      @Test
      @DisplayName("findAllPaged should return empty page when no product matches the filters")
      void findAllPagedShouldReturnEmptyPageWhenNoProductMatchesTheFilters() {

        // Arrange
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Act
        Page<ProductResponse> result = service.findAllPaged("pc", "2", pageRequest);

        // Assert
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
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
    @DisplayName("create should persist product when valid data")
    void createShouldPersistProductWhenValidData() {

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