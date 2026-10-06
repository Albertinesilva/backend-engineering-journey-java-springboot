package com.albertsilva.dev.dscatalog.entity;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.albertsilva.dev.dscatalog.factory.CategoryFactory;
import com.albertsilva.dev.dscatalog.factory.ProductFactory;

@DisplayName("Tests for Product Entity")
public class ProductTest {

  @Test
  @DisplayName("Product should instantiate correctly")
  void productShouldInstantiateCorrectly() {

    // Arrange
    Product product = ProductFactory.createProduct();

    // Act
    // Apenas instanciação

    // Assert
    Assertions.assertNotNull(product);
    Assertions.assertNotNull(product.getName());
    Assertions.assertNotNull(product.getDescription());
    Assertions.assertNotNull(product.getPrice());
    Assertions.assertNotNull(product.getImgUrl());
    Assertions.assertNotNull(product.getDate());
  }

  @Test
  @DisplayName("Product should add categories correctly")
  void productShouldAddCategoriesCorrectly() {

    // Arrange
    Product product = ProductFactory.createProduct();
    Category category = CategoryFactory.createCategory();

    // Act
    product.getCategories().add(category);

    // Assert
    Assertions.assertFalse(product.getCategories().isEmpty());
    Assertions.assertTrue(product.getCategories().contains(category));
  }

  @Test
  @DisplayName("Product equals should return true for same id")
  void productEqualsShouldReturnTrueWhenIdsAreEqual() {

    // Arrange
    Product product1 = new Product();
    product1.setId(1L);

    Product product2 = new Product();
    product2.setId(1L);

    // Act
    boolean result = product1.equals(product2);

    // Assert
    Assertions.assertTrue(result);
  }

  @Test
  @DisplayName("Product equals should return false for different ids")
  void productEqualsShouldReturnFalseWhenIdsAreDifferent() {

    // Arrange
    Product product1 = new Product();
    product1.setId(1L);

    Product product2 = new Product();
    product2.setId(2L);

    // Act
    boolean result = product1.equals(product2);

    // Assert
    Assertions.assertFalse(result);
  }

  @Test
  @DisplayName("Product hashCode should be based on id")
  void productHashCodeShouldBeBasedOnId() {

    // Arrange
    Product product = new Product();
    product.setId(1L);

    int expected = 31 * 1 + Long.valueOf(1L).hashCode();

    // Act
    int hashCode = product.hashCode();

    // Assert
    Assertions.assertEquals(expected, hashCode);
  }

  @Test
  @DisplayName("Product active flag should be mutable")
  void productActiveFlagShouldBeMutable() {

    // Arrange
    Product product = ProductFactory.createProduct();

    // Act
    product.setActive(false);

    // Assert
    Assertions.assertFalse(product.isActive());
  }

  @Test
  @DisplayName("Product date should be mutable")
  void productDateShouldBeMutable() {

    // Arrange
    Product product = ProductFactory.createProduct();
    Instant newDate = Instant.now();

    // Act
    product.setDate(newDate);

    // Assert
    Assertions.assertEquals(newDate, product.getDate());
  }
}