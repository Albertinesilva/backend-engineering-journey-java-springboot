package com.albertsilva.dev.asjcatalog.domain.catalog;

import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NAME_FILTER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;

/**
 * Testes de {@code Category}. O {@code equals} e o {@code hashCode} ficam no
 * {@code EntityIdentityContractTest}.
 */
@DisplayName("Tests for Category")
class CategoryTest {

  private static final String DESCRIPTION = "Produtos eletrônicos, como TVs, smartphones e laptops.";

  @Nested
  @DisplayName("Constructor Operations")
  class ConstructorOperations {

    @Test
    @DisplayName("constructor should set name, description and status when values are given")
    void constructorShouldSetNameDescriptionAndStatusWhenValuesAreGiven() {

      // Act
      Category category = new Category(NAME_FILTER, DESCRIPTION, false);

      // Assert
      assertThat(category.getId()).isNull();
      assertThat(category.getName()).isEqualTo(NAME_FILTER);
      assertThat(category.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(category.isActive()).isFalse();
    }
  }

  /**
   * Testes de {@code Category.prePersist}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code updatedAt} na criação: o {@code prePersist} preenche só o
   * {@code createdAt}, e a categoria nasce sem {@code updatedAt} (#7).</li>
   * </ul>
   */
  @Nested
  @DisplayName("PrePersist Operations")
  class PrePersistOperations {

    @Test
    @DisplayName("prePersist should set createdAt with the current instant")
    void prePersistShouldSetCreatedAtWithTheCurrentInstant() {

      // Arrange
      Category category = CategoryFactory.createCategory();
      Instant before = Instant.now();

      // Act
      category.prePersist();
      Instant after = Instant.now();

      // Assert
      assertThat(category.getCreatedAt()).isBetween(before, after);
    }
  }

  @Nested
  @DisplayName("PreUpdate Operations")
  class PreUpdateOperations {

    @Test
    @DisplayName("preUpdate should set updatedAt with the current instant")
    void preUpdateShouldSetUpdatedAtWithTheCurrentInstant() {

      // Arrange
      Category category = CategoryFactory.createCategory();
      Instant before = Instant.now();

      // Act
      category.preUpdate();
      Instant after = Instant.now();

      // Assert
      assertThat(category.getUpdatedAt()).isBetween(before, after);
    }
  }
}
