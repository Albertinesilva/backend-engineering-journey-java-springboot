package com.albertsilva.dev.dscatalog.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.albertsilva.dev.dscatalog.factory.CategoryFactory;

@DisplayName("Tests for Category Entity")
public class CategoryTest {

  @Test
  @DisplayName("Category should instantiate correctly")
  void categoryShouldInstantiateCorrectly() {

    // Arrange
    Category category = CategoryFactory.createCategory();

    // Act
    // Apenas instanciação

    // Assert
    Assertions.assertNotNull(category);
    Assertions.assertNotNull(category.getName());
    Assertions.assertNotNull(category.getDescription());
  }

  @Test
  @DisplayName("Category prePersist should set createdAt")
  void categoryPrePersistShouldSetCreatedAt() {

    // Arrange
    Category category = CategoryFactory.createCategory();

    // Act
    category.prePersist();

    // Assert
    Assertions.assertNotNull(category.getCreatedAt());
  }

  @Test
  @DisplayName("Category preUpdate should set updatedAt")
  void categoryPreUpdateShouldSetUpdatedAt() {

    // Arrange
    Category category = CategoryFactory.createCategory();

    // Act
    category.preUpdate();

    // Assert
    Assertions.assertNotNull(category.getUpdatedAt());
  }

  @Test
  @DisplayName("Category equals should return true for same id")
  void categoryEqualsShouldReturnTrueWhenIdsAreEqual() {

    // Arrange
    Category category1 = new Category();
    category1.setId(1L);

    Category category2 = new Category();
    category2.setId(1L);

    // Act
    boolean result = category1.equals(category2);

    // Assert
    Assertions.assertTrue(result);
  }

  @Test
  @DisplayName("Category equals should return false for different ids")
  void categoryEqualsShouldReturnFalseWhenIdsAreDifferent() {

    // Arrange
    Category category1 = new Category();
    category1.setId(1L);

    Category category2 = new Category();
    category2.setId(2L);

    // Act
    boolean result = category1.equals(category2);

    // Assert
    Assertions.assertFalse(result);
  }

  @Test
  @DisplayName("Category hashCode should be consistent with id")
  void categoryHashCodeShouldBeConsistentWithId() {

    // Arrange
    Category category = new Category();
    category.setId(1L);

    int expected = 31 * 1 + Long.valueOf(1L).hashCode();

    // Act
    int hashCode = category.hashCode();

    // Assert
    Assertions.assertEquals(expected, hashCode);
  }
}