package com.albertsilva.dev.asjcatalog.domain.catalog;

import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NAME_FILTER;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.albertsilva.dev.asjcatalog.factory.ProductFactory;

/**
 * Testes de {@code Product}. O {@code equals} e o {@code hashCode} ficam no
 * {@code EntityIdentityContractTest}; os callbacks de auditoria, que são
 * privados, são testados no {@code ProductRepositoryTest}.
 */
@DisplayName("Tests for Product")
class ProductTest {

  private static final String DESCRIPTION = "TV 50 polegadas";
  private static final BigDecimal PRICE = new BigDecimal("2190.00");
  private static final String IMG_URL = "https://img.com/tv.png";

  @Nested
  @DisplayName("Constructor Operations")
  class ConstructorOperations {

    @Test
    @DisplayName("constructor should set fields and empty categories when values are given")
    void constructorShouldSetFieldsAndEmptyCategoriesWhenValuesAreGiven() {

      // Act
      Product product = new Product(NAME_FILTER, DESCRIPTION, PRICE, IMG_URL, false);

      // Assert
      assertThat(product.getId()).isNull();
      assertThat(product.getName()).isEqualTo(NAME_FILTER);
      assertThat(product.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(product.getPrice()).isEqualTo(PRICE);
      assertThat(product.getImgUrl()).isEqualTo(IMG_URL);
      assertThat(product.isActive()).isFalse();
      assertThat(product.getCategories()).isEmpty();
    }

    @Test
    @DisplayName("constructor should leave audit dates null when product is not persisted")
    void constructorShouldLeaveAuditDatesNullWhenProductIsNotPersisted() {

      // Act
      Product product = ProductFactory.createProduct();

      // Assert
      assertThat(product.getCreatedAt()).isNull();
      assertThat(product.getUpdatedAt()).isNull();
    }
  }
}
