package com.albertsilva.dev.asjcatalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.domain.catalog.Product;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.projection.ProductProjection;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests for ProductRepository")
class ProductRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Nested
  @DisplayName("Save Operations")
  class SaveOperations {

    @Test
    @DisplayName("save should generate id when product is new")
    void saveShouldGenerateIdWhenProductIsNew() {

      // Arrange
      long countBefore = productRepository.count();

      Product product = ProductFactory.createProduct();
      product.setId(null);

      // Act
      Product savedProduct = productRepository.save(product);

      // Assert
      assertThat(savedProduct.getId()).as("Product id should be auto-generated").isNotNull().isPositive();

      assertThat(productRepository.count()).as("Repository count should increase by one").isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("save should update product when product already exists")
    void saveShouldUpdateProductWhenProductAlreadyExists() {

      // Arrange
      Product product = productRepository.saveAndFlush(ProductFactory.createProduct());
      String updatedName = "Updated Name";

      // Act
      product.setName(updatedName);
      Product updatedProduct = productRepository.save(product);

      // Assert
      assertThat(updatedProduct.getName()).as("Product name should be updated").isEqualTo(updatedName);
    }

    @Test
    @DisplayName("save should persist associated categories when product has categories")
    void saveShouldPersistAssociatedCategoriesWhenProductHasCategories() {

      // Arrange
      Category category = categoryRepository.saveAndFlush(CategoryFactory.createCategory());

      Product product = ProductFactory.createProduct();
      product.getCategories().add(category);

      // Act
      Product savedProduct = productRepository.saveAndFlush(product);

      // Assert
      assertThat(savedProduct.getCategories()).as("Product should contain the associated category")
          .containsExactly(category);
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return product when id exists")
    void findByIdShouldReturnProductWhenIdExists() {

      // Arrange
      Product product = productRepository.saveAndFlush(ProductFactory.createProduct());

      // Act
      Optional<Product> result = productRepository.findById(product.getId());

      // Assert
      assertThat(result).as("Product should be found").isPresent().contains(product);
    }

    @Test
    @DisplayName("findById should return empty when id does not exist")
    void findByIdShouldReturnEmptyWhenIdDoesNotExist() {

      // Arrange
      Long nonExistingId = 999999L;

      // Act
      Optional<Product> result = productRepository.findById(nonExistingId);

      // Assert
      assertThat(result).as("Product should not be found").isEmpty();
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("deleteById should remove product when id exists")
    void deleteByIdShouldRemoveProductWhenIdExists() {

      // Arrange
      Product product = productRepository.saveAndFlush(ProductFactory.createProduct());
      Long existingId = product.getId();

      // Act
      productRepository.deleteById(existingId);
      productRepository.flush();

      // Assert
      assertThat(productRepository.existsById(existingId)).as("Product should be deleted").isFalse();
    }


    @Test
    @DisplayName("deleteById should remove category links when product has categories")
    void deleteByIdShouldRemoveCategoryLinksWhenProductHasCategories() {

      // Arrange
      Category category = categoryRepository.saveAndFlush(CategoryFactory.createCategory());

      Product product = ProductFactory.createProduct();
      product.getCategories().add(category);
      product = productRepository.saveAndFlush(product);

      Long productId = product.getId();

      // Act
      productRepository.deleteById(productId);
      productRepository.flush();

      // Assert
      assertThat(productRepository.existsById(productId)).as("Product should be deleted").isFalse();

      assertThat(countProductCategoryAssociations(productId)).as("Join table associations should be removed").isZero();
    }

    @Test
    @DisplayName("deleteById should keep categories when product has categories")
    void deleteByIdShouldKeepCategoriesWhenProductHasCategories() {

      // Arrange
      Category category = categoryRepository.saveAndFlush(CategoryFactory.createCategory());

      Product product = ProductFactory.createProduct();
      product.getCategories().add(category);
      product = productRepository.saveAndFlush(product);

      Long productId = product.getId();
      Long categoryId = category.getId();

      // Act
      productRepository.deleteById(productId);
      productRepository.flush();

      // Assert
      assertThat(productRepository.existsById(productId)).as("Product should be deleted").isFalse();

      assertThat(categoryRepository.existsById(categoryId)).as("Category should remain").isTrue();
    }
  }

  @Nested
  @DisplayName("Audit Dates Operations")
  class AuditDatesOperations {

    @Test
    @DisplayName("save should fill createdAt and updatedAt with the same instant when product is new")
    void saveShouldFillCreatedAtAndUpdatedAtWithTheSameInstantWhenProductIsNew() {

      // Arrange
      Product product = ProductFactory.createProduct();

      // Act
      Product savedProduct = productRepository.saveAndFlush(product);

      // Assert
      assertThat(savedProduct.getCreatedAt()).as("createdAt should be filled on persist").isNotNull();
      assertThat(savedProduct.getUpdatedAt()).as("updatedAt should be filled on persist").isNotNull();
      assertThat(savedProduct.getUpdatedAt()).as("both dates should share the same instant on persist")
          .isEqualTo(savedProduct.getCreatedAt());
    }

    @Test
    @DisplayName("flush should refresh only updatedAt when persisted product is modified")
    void flushShouldRefreshOnlyUpdatedAtWhenPersistedProductIsModified() throws InterruptedException {

      // Arrange
      Product product = productRepository.saveAndFlush(ProductFactory.createProduct());
      Instant createdAt = product.getCreatedAt();
      Instant firstUpdatedAt = product.getUpdatedAt();

      // Exceção documentada no TESTING.md (seção 7.6): sem Clock injetável nem setUpdatedAt na
      // entidade, a pausa garante instantes diferentes entre a criação e a atualização.
      Thread.sleep(20);

      // Act
      product.setName("Updated Name");
      productRepository.flush();

      // Assert
      assertThat(product.getCreatedAt()).as("createdAt should not change on update").isEqualTo(createdAt);
      assertThat(product.getUpdatedAt()).as("updatedAt should be refreshed on update").isAfter(firstUpdatedAt);
    }
  }

  @Nested
  @DisplayName("SearchProducts Operations")
  class SearchProductsOperations {

    @Test
    @DisplayName("searchProducts should return only products of the category when one category is given")
    void searchProductsShouldReturnOnlyProductsOfTheCategoryWhenOneCategoryIsGiven() {

      // Arrange
      Category books = persistCategory("Qwzx Books");
      Category games = persistCategory("Qwzx Games");
      Product book = persistProduct("Qwzx Clean Book", books);
      persistProduct("Qwzx Board Game", games);

      // Act
      Page<ProductProjection> result = productRepository.searchProducts(List.of(books.getId()), "",
          PageRequest.of(0, 10));

      // Assert
      assertThat(result.getContent()).extracting(ProductProjection::getId).containsExactly(book.getId());
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchProducts should return each product once when several categories are given")
    void searchProductsShouldReturnEachProductOnceWhenSeveralCategoriesAreGiven() {

      // Arrange
      Category books = persistCategory("Qwzx Books");
      Category games = persistCategory("Qwzx Games");
      Product book = persistProduct("Qwzx Clean Book", books);
      Product game = persistProduct("Qwzx Board Game", games);
      Product shared = persistProduct("Qwzx Game Book", books, games);

      // Act
      Page<ProductProjection> result = productRepository.searchProducts(List.of(books.getId(), games.getId()), "",
          PageRequest.of(0, 10));

      // Assert
      assertThat(result.getContent()).extracting(ProductProjection::getId)
          .containsExactlyInAnyOrder(book.getId(), game.getId(), shared.getId());
      assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("searchProducts should filter by name ignoring case when no category is given")
    void searchProductsShouldFilterByNameIgnoringCaseWhenNoCategoryIsGiven() {

      // Arrange
      Category books = persistCategory("Qwzx Books");
      Product match = persistProduct("Qwzx Unique Title", books);
      persistProduct("Qwzx Other Title", books);

      // Act
      Page<ProductProjection> result = productRepository.searchProducts(List.of(), "QWZX UNIQUE",
          PageRequest.of(0, 10));

      // Assert
      assertThat(result.getContent()).extracting(ProductProjection::getId).containsExactly(match.getId());
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchProducts should combine filters when name and category are given")
    void searchProductsShouldCombineFiltersWhenNameAndCategoryAreGiven() {

      // Arrange
      Category books = persistCategory("Qwzx Books");
      Category games = persistCategory("Qwzx Games");
      Product match = persistProduct("Qwzx Chess Book", books);
      persistProduct("Qwzx Chess Game", games);
      persistProduct("Qwzx Poetry Book", books);

      // Act
      Page<ProductProjection> result = productRepository.searchProducts(List.of(books.getId()), "chess",
          PageRequest.of(0, 10));

      // Assert
      assertThat(result.getContent()).extracting(ProductProjection::getId).containsExactly(match.getId());
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("searchProducts should paginate, sort and report total when page is requested")
    void searchProductsShouldPaginateSortAndReportTotalWhenPageIsRequested() {

      // Arrange
      Category books = persistCategory("Qwzx Books");
      persistProduct("Qwzx C Book", books);
      persistProduct("Qwzx A Book", books);
      persistProduct("Qwzx B Book", books);

      // Act
      Page<ProductProjection> firstPage = productRepository.searchProducts(List.of(books.getId()), "",
          PageRequest.of(0, 2, Sort.by("name")));
      Page<ProductProjection> secondPage = productRepository.searchProducts(List.of(books.getId()), "",
          PageRequest.of(1, 2, Sort.by("name")));

      // Assert
      assertThat(firstPage.getContent()).extracting(ProductProjection::getName)
          .containsExactly("Qwzx A Book", "Qwzx B Book");
      assertThat(secondPage.getContent()).extracting(ProductProjection::getName).containsExactly("Qwzx C Book");
      assertThat(firstPage.getTotalElements()).isEqualTo(3);
      assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    private Category persistCategory(String name) {
      return categoryRepository.saveAndFlush(CategoryFactory.createCategory(name));
    }

    private Product persistProduct(String name, Category... categories) {
      Product product = new Product(name, "Description of " + name, new BigDecimal("10.00"), "https://img.com/qwzx.png", true);
      product.getCategories().addAll(List.of(categories));
      return productRepository.saveAndFlush(product);
    }
  }

  private long countProductCategoryAssociations(Long productId) {
    Number result = (Number) entityManager.getEntityManager()
        .createNativeQuery("SELECT COUNT(*) FROM tb_product_category WHERE product_id = :id")
        .setParameter("id", productId).getSingleResult();

    return result.longValue();
  }
}