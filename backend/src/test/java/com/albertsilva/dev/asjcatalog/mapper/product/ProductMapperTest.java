package com.albertsilva.dev.asjcatalog.mapper.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.domain.catalog.Product;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;

/**
 * Testes de {@code ProductMapper}.
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>{@code toResponsePage}: não é usado em produção (a listagem monta a página
 * a partir da consulta nativa); a decisão de remover ou manter é da #25.</li>
 * </ul>
 */
@DisplayName("Tests for ProductMapper")
class ProductMapperTest {

  private static final Instant CREATED_AT = Instant.parse("2026-01-10T10:00:00Z");
  private static final Instant UPDATED_AT = Instant.parse("2026-01-11T15:30:00Z");

  private final ProductMapper mapper = new ProductMapper();

  /** Produto com id, uma categoria e datas de auditoria (sem setter na entidade). */
  private static Product productWithCategory() {
    Product product = ProductFactory.createProduct();
    product.setId(ProductFactory.EXISTING_ID);
    Category category = CategoryFactory.createCategory();
    category.setId(CategoryFactory.EXISTING_ID);
    product.getCategories().add(category);
    ReflectionTestUtils.setField(product, "createdAt", CREATED_AT);
    ReflectionTestUtils.setField(product, "updatedAt", UPDATED_AT);
    return product;
  }

  /**
   * Testes de {@code ProductMapper.toEntity}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code date} do request: é descartado pelo mapper (#25, item 2).</li>
   * </ul>
   */
  @Nested
  @DisplayName("ToEntity Operations")
  class ToEntityOperations {

    @Test
    @DisplayName("toEntity should copy name, description, price and image when request is given")
    void toEntityShouldCopyNameDescriptionPriceAndImageWhenRequestIsGiven() {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();

      // Act
      Product result = mapper.toEntity(request);

      // Assert
      assertThat(result.getId()).isNull();
      assertThat(result.getName()).isEqualTo(request.name());
      assertThat(result.getDescription()).isEqualTo(request.description());
      assertThat(result.getPrice()).isEqualTo(request.price());
      assertThat(result.getImgUrl()).isEqualTo(request.imgUrl());
      assertThat(result.getCategories()).isEmpty();
    }

    @Test
    @DisplayName("toEntity should return null when request is null")
    void toEntityShouldReturnNullWhenRequestIsNull() {

      // Act
      Product result = mapper.toEntity(null);

      // Assert
      assertThat(result).isNull();
    }
  }

  /**
   * Testes de {@code ProductMapper.updateEntity}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Campos nulos ({@code name}, {@code description}, {@code price} ou
   * {@code imgUrl}): o valor atual é mantido, como numa atualização parcial num
   * {@code PUT}, e o padrão de atualização ainda não foi decidido (#28). Pela API,
   * só {@code description} e {@code imgUrl} chegam nulos, porque {@code name} e
   * {@code price} são obrigatórios no DTO.</li>
   * </ul>
   */
  @Nested
  @DisplayName("UpdateEntity Operations")
  class UpdateEntityOperations {

    @Test
    @DisplayName("updateEntity should replace all fields when request has all fields")
    void updateEntityShouldReplaceAllFieldsWhenRequestHasAllFields() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
      Product product = productWithCategory();

      // Act
      mapper.updateEntity(request, product);

      // Assert
      assertThat(product.getId()).isEqualTo(ProductFactory.EXISTING_ID);
      assertThat(product.getName()).isEqualTo(request.name());
      assertThat(product.getDescription()).isEqualTo(request.description());
      assertThat(product.getPrice()).isEqualTo(request.price());
      assertThat(product.getImgUrl()).isEqualTo(request.imgUrl());
    }

    @Test
    @DisplayName("updateEntity should keep entity unchanged when request is null")
    void updateEntityShouldKeepEntityUnchangedWhenRequestIsNull() {

      // Arrange
      Product product = productWithCategory();
      Product original = productWithCategory();

      // Act
      mapper.updateEntity(null, product);

      // Assert
      assertThat(product.getName()).isEqualTo(original.getName());
      assertThat(product.getDescription()).isEqualTo(original.getDescription());
      assertThat(product.getPrice()).isEqualTo(original.getPrice());
      assertThat(product.getImgUrl()).isEqualTo(original.getImgUrl());
    }

    @Test
    @DisplayName("updateEntity should ignore call when entity is null")
    void updateEntityShouldIgnoreCallWhenEntityIsNull() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

      // Act & Assert
      assertThatCode(() -> mapper.updateEntity(request, null)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("ToResponse Operations")
  class ToResponseOperations {

    @Test
    @DisplayName("toResponse should map fields and categories when entity is given")
    void toResponseShouldMapFieldsAndCategoriesWhenEntityIsGiven() {

      // Arrange
      Product product = productWithCategory();
      Category category = product.getCategories().iterator().next();

      // Act
      ProductResponse result = mapper.toResponse(product);

      // Assert
      assertThat(result).isEqualTo(new ProductResponse(product.getId(), product.getName(), product.getDescription(),
          product.getPrice(), product.getImgUrl(), List.of(
              new CategoryResponse(category.getId(), category.getName()))));
    }

    @Test
    @DisplayName("toResponse should return null when entity is null")
    void toResponseShouldReturnNullWhenEntityIsNull() {

      // Act
      ProductResponse result = mapper.toResponse(null);

      // Assert
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("ToDetailsResponse Operations")
  class ToDetailsResponseOperations {

    @Test
    @DisplayName("toDetailsResponse should map fields, dates, status and category details when entity is given")
    void toDetailsResponseShouldMapFieldsDatesStatusAndCategoryDetailsWhenEntityIsGiven() {

      // Arrange
      Product product = productWithCategory();
      product.setActive(false);
      Category category = product.getCategories().iterator().next();

      // Act
      ProductDetailsResponse result = mapper.toDetailsResponse(product);

      // Assert
      assertThat(result).isEqualTo(new ProductDetailsResponse(product.getId(), product.getName(),
          product.getDescription(), product.getPrice(), product.getImgUrl(), CREATED_AT, UPDATED_AT, false,
          List.of(new CategoryDetailsResponse(category.getId(), category.getName(),
              category.getDescription(), category.isActive()))));
    }

    @Test
    @DisplayName("toDetailsResponse should return null when entity is null")
    void toDetailsResponseShouldReturnNullWhenEntityIsNull() {

      // Act
      ProductDetailsResponse result = mapper.toDetailsResponse(null);

      // Assert
      assertThat(result).isNull();
    }
  }
}
