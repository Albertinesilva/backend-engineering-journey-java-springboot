package com.albertsilva.dev.asjcatalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.domain.catalog.Product;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests for CategoryRepository")
class CategoryRepositoryTest {

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private ProductRepository productRepository;

  @Nested
  @DisplayName("Save Operations")
  class SaveOperations {

    @Test
    @DisplayName("save should generate id when category is new")
    void saveShouldGenerateIdWhenCategoryIsNew() {

      // Arrange
      long countBefore = categoryRepository.count();

      Category category = CategoryFactory.createCategory();
      category.setId(null);

      // Act
      Category savedCategory = categoryRepository.save(category);

      // Assert
      assertThat(savedCategory.getId())
          .as("Category id should be auto-generated")
          .isNotNull()
          .isPositive();

      assertThat(categoryRepository.count()).as("Repository count should increase by one").isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("save should update category when category already exists")
    void saveShouldUpdateCategoryWhenCategoryAlreadyExists() {

      // Arrange
      Category category = categoryRepository.save(CategoryFactory.createCategory());
      String updatedName = "Updated Category";

      // Act
      category.setName(updatedName);
      Category updatedCategory = categoryRepository.save(category);

      // Assert
      assertThat(updatedCategory.getName()).as("Category name should be updated").isEqualTo(updatedName);
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return category when id exists")
    void findByIdShouldReturnCategoryWhenIdExists() {

      // Arrange
      Category category = categoryRepository.saveAndFlush(CategoryFactory.createCategory());

      // Act
      Optional<Category> result = categoryRepository.findById(category.getId());

      // Assert
      assertThat(result).as("Category should be found").isPresent().contains(category);
    }

    @Test
    @DisplayName("findById should return empty when id does not exist")
    void findByIdShouldReturnEmptyWhenIdDoesNotExist() {

      // Arrange
      Long nonExistingId = 999999L;

      // Act
      Optional<Category> result = categoryRepository.findById(nonExistingId);

      // Assert
      assertThat(result).as("Category should not be found").isEmpty();
    }
  }

  @Nested
  @DisplayName("Search Operations")
  class SearchOperations {

    @Test
    @DisplayName("findByNameContainingIgnoreCase should return matching categories when case differs")
    void findByNameContainingIgnoreCaseShouldReturnMatchingCategoriesWhenCaseDiffers() {

      // Arrange
      Category electronics = CategoryFactory.createCategory("Electronics-Test");

      Category books = CategoryFactory.createCategory("Books-Test");

      categoryRepository.saveAndFlush(electronics);
      categoryRepository.saveAndFlush(books);

      Pageable pageable = PageRequest.of(0, 10);

      // Act
      Page<Category> result = categoryRepository.findByNameContainingIgnoreCase("elec", pageable);

      // Assert
      List<String> names = result.getContent().stream().map(Category::getName).toList();

      assertThat(names)
          .as("Should return categories containing 'elec' ignoring case")
          .contains("Electronics-Test")
          .doesNotContain("Books-Test");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase should return empty page when no category matches")
    void findByNameContainingIgnoreCaseShouldReturnEmptyPageWhenNoCategoryMatches() {

      // Arrange
      categoryRepository.saveAndFlush(CategoryFactory.createCategory());

      Pageable pageable = PageRequest.of(0, 10);

      // Act
      Page<Category> result = categoryRepository.findByNameContainingIgnoreCase("NonExistingCategory", pageable);

      // Assert
      assertThat(result.getContent()).as("Should return empty result").isEmpty();
    }

    @Test
    @DisplayName("findAll should return categories in name order when sort is by name")
    void findAllShouldReturnCategoriesInNameOrderWhenSortIsByName() {

      // Arrange
      Category c1 = CategoryFactory.createCategory("Zebra-Test");
      Category c2 = CategoryFactory.createCategory("Apple-Test");
      Category c3 = CategoryFactory.createCategory("Books-Test");

      categoryRepository.saveAndFlush(c1);
      categoryRepository.saveAndFlush(c2);
      categoryRepository.saveAndFlush(c3);

      // Act
      List<Category> result = categoryRepository.findAll(Sort.by("name"));

      // Assert
      // O seed também tem categorias: confere a ordem relativa das três criadas aqui.
      assertThat(result).extracting(Category::getName)
          .as("Categories should be sorted by name")
          .containsSubsequence("Apple-Test", "Books-Test", "Zebra-Test");
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("deleteById should remove category when id exists")
    void deleteByIdShouldRemoveCategoryWhenIdExists() {

      // Arrange
      Category category = categoryRepository.saveAndFlush(CategoryFactory.createCategory());
      Long existingId = category.getId();

      // Act
      categoryRepository.deleteById(existingId);
      categoryRepository.flush();

      // Assert
      assertThat(categoryRepository.existsById(existingId)).as("Category should be deleted").isFalse();
    }


    @Test
    @DisplayName("deleteById should keep products when category is dissociated from them first")
    void deleteByIdShouldKeepProductsWhenCategoryIsDissociatedFromThemFirst() {

      // Arrange
      Category category = categoryRepository.saveAndFlush(CategoryFactory.createCategory());

      Product product = ProductFactory.createProduct();
      product.getCategories().add(category);
      product = productRepository.saveAndFlush(product);

      Long categoryId = category.getId();
      Long productId = product.getId();

      product.getCategories().remove(category);
      productRepository.saveAndFlush(product);

      // Act
      categoryRepository.deleteById(categoryId);
      categoryRepository.flush();

      // Assert
      assertThat(categoryRepository.existsById(categoryId)).as("Category should be deleted").isFalse();

      assertThat(productRepository.existsById(productId)).as("Associated product should remain").isTrue();
    }
  }
}