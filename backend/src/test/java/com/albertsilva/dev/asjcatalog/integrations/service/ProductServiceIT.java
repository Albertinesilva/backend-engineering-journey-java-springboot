package com.albertsilva.dev.asjcatalog.integrations.service;

import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.COUNT_TOTAL_PRODUCTS;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NON_EXISTING_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.asjcatalog.domain.catalog.Product;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.repository.ProductRepository;
import com.albertsilva.dev.asjcatalog.service.ProductService;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

/**
 * Testes de integração do {@link ProductService} com o banco H2 do perfil
 * {@code test}.
 *
 * <p>
 * Dependem do seed ({@code import.sql}): o total de produtos
 * ({@code COUNT_TOTAL_PRODUCTS}), os ids fixos de produtos e de categorias, os
 * nomes esperados nas ordenações e os totais por categoria (#21).
 * </p>
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Integration tests for ProductService")
class ProductServiceIT {

  private static final String NOT_FOUND_KEY = "error.product.notFound";

  @Autowired
  private ProductService service;

  @Autowired
  private ProductRepository repository;

  @Nested
  @DisplayName("FindAllPaged Operations")
  class FindAllPagedOperations {

    @Test
    @DisplayName("findAllPaged should return empty page with the real total when page does not exist")
    void findAllPagedShouldReturnEmptyPageWithTheRealTotalWhenPageDoesNotExist() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(50, 10);

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "0", pageRequest);

      // Assert
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getNumber()).isEqualTo(50);
      assertThat(result.getSize()).isEqualTo(10);
      assertThat(result.getTotalElements()).isEqualTo(COUNT_TOTAL_PRODUCTS);
    }

    @Test
    @DisplayName("findAllPaged should return ordered page when sorting by name")
    void findAllPagedShouldReturnOrderedPageWhenSortingByName() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("name"));
      // Nomes lidos do repositório (ids 155 e 162) em vez de literais com "ô": o compilador deste
      // ambiente corrompe literais Java com "ô"/"ê" mesmo com o projeto configurado para UTF-8; o
      // valor persistido é lido corretamente via JDBC, então a comparação permanece exata.
      String secondName = repository.findById(155L).orElseThrow().getName();
      String thirdName = repository.findById(162L).orElseThrow().getName();

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "0", pageRequest);

      // Assert
      assertThat(result.getContent())
          .extracting(ProductResponse::name)
          .startsWith("Air Fryer 5L Digital", secondName, thirdName);
    }

    @Test
    @DisplayName("findAllPaged should return only products whose name contains the filter when only name is informed")
    void findAllPagedShouldReturnOnlyMatchingProductsWhenOnlyNameIsInformed() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10);

      // Act
      Page<ProductResponse> result = service.findAllPaged("pc", "0", pageRequest);

      // Assert
      assertThat(result.getContent())
          .extracting(ProductResponse::name)
          .isNotEmpty()
          .allSatisfy(name -> assertThat(name).containsIgnoringCase("pc"));
    }

    @Test
    @DisplayName("findAllPaged should trim name when name has surrounding spaces")
    void findAllPagedShouldTrimNameWhenNameHasSurroundingSpaces() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10);
      Page<ProductResponse> trimmed = service.findAllPaged("pc", "0", pageRequest);

      // Act
      Page<ProductResponse> result = service.findAllPaged("   pc   ", "0", pageRequest);

      // Assert
      assertThat(result.getTotalElements()).isPositive().isEqualTo(trimmed.getTotalElements());
      assertThat(result.getContent())
          .extracting(ProductResponse::name)
          .allSatisfy(name -> assertThat(name).containsIgnoringCase("pc"));
    }

    @Test
    @DisplayName("findAllPaged should return products with their categories when there is no filter")
    void findAllPagedShouldReturnProductsWithCategoriesWhenThereIsNoFilter() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("name"));

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "0", pageRequest);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(COUNT_TOTAL_PRODUCTS);
      assertThat(result.getContent()).hasSize(10)
          .allSatisfy(product -> assertThat(product.categories()).isNotEmpty());
      assertThat(result.getContent().get(0).name()).isEqualTo("Air Fryer 5L Digital");
      assertThat(result.getContent().get(0).categories())
          .extracting(CategoryResponse::name)
          .containsExactly("Home Appliances");
    }

    @Test
    @DisplayName("findAllPaged should return only products of the informed category when category is informed")
    void findAllPagedShouldReturnOnlyProductsOfTheInformedCategoryWhenCategoryIsInformed() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("name"));

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "2", pageRequest);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(27);
      assertThat(result.getContent())
          .extracting(ProductResponse::name)
          .containsExactly("Clean Architecture", "Clean Code", "Continuous Delivery",
              "Design Patterns: Elements of Reusable Object-Oriented Software", "Designing Data-Intensive Applications",
              "Domain-Driven Design: Tackling Complexity in the Heart of Software", "Effective Java",
              "Eloquent JavaScript", "Enterprise Integration Patterns",
              "Growing Object-Oriented Software, Guided by Tests");
    }

    @Test
    @DisplayName("findAllPaged should not repeat a product when it belongs to more than one informed category")
    void findAllPagedShouldNotRepeatProductWhenItBelongsToMoreThanOneInformedCategory() {

      // Arrange: o produto 87 pertence às categorias 29 (Cameras) e 33 (Smart Home); a combinação não
      // pode devolvê-lo duas vezes. Esperados: 74, 75, 76 (Cameras), 85, 86 (Smart Home), 87 (ambas) e
      // 155 (Smart Home).
      PageRequest pageRequest = PageRequest.of(0, 30, Sort.by("name"));

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "29,33", pageRequest);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(7);
      assertThat(result.getContent())
          .extracting(ProductResponse::id)
          .containsExactlyInAnyOrder(74L, 75L, 76L, 85L, 86L, 87L, 155L);
    }

    @Test
    @DisplayName("findAllPaged should apply both filters when name and category are informed")
    void findAllPagedShouldApplyBothFiltersWhenNameAndCategoryAreInformed() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10);

      // Act
      Page<ProductResponse> result = service.findAllPaged("RAILS", "2", pageRequest);

      // Assert
      assertThat(result.getContent())
          .extracting(ProductResponse::name)
          .containsExactly("Ruby on Rails For Dummies");
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAllPaged should return descending order when sorting by name descending")
    void findAllPagedShouldReturnDescendingOrderWhenSortingByNameDescending() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "name"));
      // 3º nome lido do repositório (id 121) pelo mesmo motivo do teste de ordenação ascendente.
      String thirdName = repository.findById(121L).orElseThrow().getName();

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "0", pageRequest);

      // Assert
      assertThat(result.getContent())
          .extracting(ProductResponse::name)
          .containsExactly("xUnit Test Patterns", "Webcam Full HD Stream", thirdName);
    }

    @Test
    @DisplayName("findAllPaged should return empty page when no product matches the filters")
    void findAllPagedShouldReturnEmptyPageWhenNoProductMatchesTheFilters() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10);

      // Act
      Page<ProductResponse> result = service.findAllPaged("pc", "2", pageRequest);

      // Assert
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return product details when id exists")
    void findByIdShouldReturnProductDetailsWhenIdExists() {

      // Arrange
      Product expected = repository.findById(EXISTING_ID).orElseThrow();

      // Act
      ProductDetailsResponse result = service.findById(EXISTING_ID);

      // Assert
      assertThat(result.id()).isEqualTo(EXISTING_ID);
      assertThat(result.name()).isEqualTo(expected.getName());
      assertThat(result.price()).isEqualByComparingTo(expected.getPrice());
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
    @DisplayName("create should persist product when request is valid")
    void createShouldPersistProductWhenRequestIsValid() {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();

      // Act
      ProductResponse result = service.create(request);

      // Assert
      assertThat(result.id()).isNotNull();
      assertThat(result.name()).isEqualTo(request.name());
      assertThat(result.price()).isEqualByComparingTo(request.price());
      assertThat(result.categories())
          .extracting(CategoryResponse::id)
          .containsExactlyInAnyOrderElementsOf(request.categoryIds());
      assertThat(repository.count()).isEqualTo(COUNT_TOTAL_PRODUCTS + 1);
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should persist new data when id exists")
    void updateShouldPersistNewDataWhenIdExists() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

      // Act
      ProductResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertThat(result.id()).isEqualTo(EXISTING_ID);
      assertThat(result.name()).isEqualTo(request.name());
      assertThat(service.findById(EXISTING_ID))
          .extracting(ProductDetailsResponse::name, ProductDetailsResponse::description, ProductDetailsResponse::price)
          .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
          .containsExactly(request.name(), request.description(), request.price());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

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
    @DisplayName("delete should remove product when id exists")
    void deleteShouldRemoveProductWhenIdExists() {

      // Act
      service.delete(EXISTING_ID);

      // Assert
      assertThat(repository.count()).isEqualTo(COUNT_TOTAL_PRODUCTS - 1);
      assertThat(repository.existsById(EXISTING_ID)).isFalse();
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThatThrownBy(() -> service.delete(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }
}
