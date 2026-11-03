package com.albertsilva.dev.asjcatalog.service;

import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NAME_FILTER;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NON_EXISTING_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.domain.catalog.Product;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.mapper.product.ProductMapper;
import com.albertsilva.dev.asjcatalog.projection.ProductProjection;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;
import com.albertsilva.dev.asjcatalog.repository.ProductRepository;
import com.albertsilva.dev.asjcatalog.service.exception.DatabaseException;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

@DisplayName("Tests for ProductService")
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @InjectMocks
  private ProductService service;

  @Mock
  private ProductRepository repository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private ProductMapper productMapper;

  private Pageable pageable;
  private Page<ProductProjection> emptyProjections;

  @BeforeEach
  void setUp() {
    pageable = PageRequest.of(0, 10);
    emptyProjections = new PageImpl<>(List.of(), pageable, 0);
  }

  private static Product productWithId(Long id) {
    Product product = ProductFactory.createProduct();
    product.setId(id);
    return product;
  }

  private static Category categoryWithId(Long id) {
    Category category = CategoryFactory.createCategory();
    category.setId(id);
    return category;
  }

  /**
   * Testes de {@code ProductService.create}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code categoryIds} nulo ou vazio: o produto seria criado sem categoria
   * e não apareceria na listagem; hoje o {@code @NotEmpty} do DTO bloqueia o
   * caso na API (#4).</li>
   * </ul>
   */
  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("create should activate, link categories and save product when categories exist")
    void createShouldActivateLinkCategoriesAndSaveProductWhenCategoriesExist() {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();
      Product product = ProductFactory.createProduct();
      product.setActive(false);
      Category category = categoryWithId(CategoryFactory.EXISTING_ID);
      ProductResponse expectedResponse = ProductFactory.createProductResponse();

      when(productMapper.toEntity(request)).thenReturn(product);
      when(categoryRepository.findAllById(request.categoryIds())).thenReturn(List.of(category));
      when(repository.save(product)).thenReturn(product);
      when(productMapper.toResponse(product)).thenReturn(expectedResponse);

      // Act
      ProductResponse result = service.create(request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(product.isActive()).isTrue();
      assertThat(product.getCategories()).containsExactly(category);
      verify(repository).save(product);
    }

    @Test
    @DisplayName("create should throw ResourceNotFoundException when any category does not exist")
    void createShouldThrowResourceNotFoundExceptionWhenAnyCategoryDoesNotExist() {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();
      Product product = ProductFactory.createProduct();

      when(productMapper.toEntity(request)).thenReturn(product);
      when(categoryRepository.findAllById(request.categoryIds())).thenReturn(List.of());

      // Act & Assert
      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.product.categories.notFound");

      verify(repository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return product when id exists")
    void findByIdShouldReturnProductWhenIdExists() {

      // Arrange
      Product product = productWithId(EXISTING_ID);
      ProductDetailsResponse expectedResponse = ProductFactory.createProductDetailsResponse();

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(product));
      when(productMapper.toDetailsResponse(product)).thenReturn(expectedResponse);

      // Act
      ProductDetailsResponse result = service.findById(EXISTING_ID);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      verify(repository).findById(EXISTING_ID);
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
    void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.findById(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.product.notFound");

      verify(productMapper, never()).toDetailsResponse(any());
    }
  }

  /**
   * Testes de {@code ProductService.findAllPaged}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code categoryIds} não numérico (por exemplo, {@code "abc"} ou
   * {@code "1,,2"}): a conversão com {@code Long::parseLong} lança
   * {@code NumberFormatException}, que responde 500 em vez de 400 (#36).</li>
   * </ul>
   */
  @Nested
  @DisplayName("FindAllPaged Operations")
  class FindAllPagedOperations {

    @Test
    @DisplayName("findAllPaged should search without category filter when categoryId is 0")
    void findAllPagedShouldSearchWithoutCategoryFilterWhenCategoryIdIsZero() {

      // Arrange
      when(repository.searchProducts(List.of(), NAME_FILTER, pageable)).thenReturn(emptyProjections);
      when(repository.searchProductsWithCategories(List.of())).thenReturn(List.of());

      // Act
      Page<ProductResponse> result = service.findAllPaged(NAME_FILTER, "0", pageable);

      // Assert
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      assertThat(result.getPageable()).isEqualTo(pageable);
      verify(repository).searchProducts(List.of(), NAME_FILTER, pageable);
    }

    @Test
    @DisplayName("findAllPaged should trim name when name has surrounding spaces")
    void findAllPagedShouldTrimNameWhenNameHasSurroundingSpaces() {

      // Arrange
      when(repository.searchProducts(List.of(), NAME_FILTER, pageable)).thenReturn(emptyProjections);
      when(repository.searchProductsWithCategories(List.of())).thenReturn(List.of());

      // Act
      Page<ProductResponse> result = service.findAllPaged("  " + NAME_FILTER + "  ", "0", pageable);

      // Assert
      assertThat(result.getContent()).isEmpty();
      verify(repository).searchProducts(List.of(), NAME_FILTER, pageable);
    }

    @Test
    @DisplayName("findAllPaged should split categoryIds when categoryIds are comma separated")
    void findAllPagedShouldSplitCategoryIdsWhenCategoryIdsAreCommaSeparated() {

      // Arrange
      List<Long> categoryIds = List.of(CategoryFactory.EXISTING_ID, CategoryFactory.NON_DEPENDENT_ID);
      String categoryIdsParameter = CategoryFactory.EXISTING_ID + "," + CategoryFactory.NON_DEPENDENT_ID;

      when(repository.searchProducts(categoryIds, "", pageable)).thenReturn(emptyProjections);
      when(repository.searchProductsWithCategories(List.of())).thenReturn(List.of());

      // Act
      Page<ProductResponse> result = service.findAllPaged("", categoryIdsParameter, pageable);

      // Assert
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      verify(repository).searchProducts(categoryIds, "", pageable);
    }

    @Test
    @DisplayName("findAllPaged should keep page order and total when products come out of order")
    void findAllPagedShouldKeepPageOrderAndTotalWhenProductsComeOutOfOrder() {

      // Arrange
      // Os ids 3, 1 e 2 são a ordem da página devolvida pela consulta nativa; a
      // segunda consulta devolve os produtos em outra ordem (1, 2 e 3).
      Page<ProductProjection> projections = new PageImpl<>(
          List.of(projectionWithId(3L), projectionWithId(1L), projectionWithId(2L)), pageable, 30);

      when(repository.searchProducts(List.of(), "", pageable)).thenReturn(projections);
      when(repository.searchProductsWithCategories(List.of(3L, 1L, 2L)))
          .thenReturn(List.of(productWithId(1L), productWithId(2L), productWithId(3L)));
      when(productMapper.toResponse(any(Product.class))).thenAnswer(invocation -> {
        Product product = invocation.getArgument(0);
        return new ProductResponse(product.getId(), product.getName(), null, null, null, List.of());
      });

      // Act
      Page<ProductResponse> result = service.findAllPaged("", "0", pageable);

      // Assert
      assertThat(result.getContent()).extracting(ProductResponse::id).containsExactly(3L, 1L, 2L);
      assertThat(result.getTotalElements()).isEqualTo(30);
      assertThat(result.getPageable()).isEqualTo(pageable);
      verify(productMapper, times(3)).toResponse(any(Product.class));
    }

    private ProductProjection projectionWithId(Long id) {
      ProductProjection projection = mock(ProductProjection.class);
      when(projection.getId()).thenReturn(id);
      return projection;
    }
  }

  /**
   * Testes de {@code ProductService.update}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code categoryIds} vazio: as categorias seriam removidas e o produto
   * deixaria de aparecer na listagem; hoje o {@code @NotEmpty} do DTO bloqueia
   * o caso na API (#4).</li>
   * <li>{@code categoryIds} nulo: as categorias atuais são mantidas, o que é
   * semântica de atualização parcial num {@code PUT}; o padrão de atualização
   * ainda não foi decidido (#28).</li>
   * </ul>
   */
  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should apply request and replace categories when id exists")
    void updateShouldApplyRequestAndReplaceCategoriesWhenIdExists() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
      Product product = productWithId(EXISTING_ID);
      Category previousCategory = categoryWithId(CategoryFactory.NON_DEPENDENT_ID);
      product.getCategories().add(previousCategory);
      Category newCategory = categoryWithId(CategoryFactory.EXISTING_ID);
      ProductResponse expectedResponse = ProductFactory.createUpdatedProductResponse();

      when(repository.getReferenceById(EXISTING_ID)).thenReturn(product);
      when(categoryRepository.findAllById(request.categoryIds())).thenReturn(List.of(newCategory));
      when(repository.save(product)).thenReturn(product);
      when(productMapper.toResponse(product)).thenReturn(expectedResponse);

      // Act
      ProductResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(product.getCategories()).containsExactly(newCategory);
      verify(productMapper).updateEntity(request, product);
      verify(repository).save(product);
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when any category does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenAnyCategoryDoesNotExist() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
      Product product = productWithId(EXISTING_ID);

      when(repository.getReferenceById(EXISTING_ID)).thenReturn(product);
      when(categoryRepository.findAllById(request.categoryIds())).thenReturn(List.of());

      // Act & Assert
      assertThatThrownBy(() -> service.update(EXISTING_ID, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.product.categories.notFound");

      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
      Product reference = new Product();

      // getReferenceById não consulta o banco: o proxy só lança
      // EntityNotFoundException quando é acessado, aqui pelo mapper.
      when(repository.getReferenceById(NON_EXISTING_ID)).thenReturn(reference);
      doThrow(EntityNotFoundException.class).when(productMapper).updateEntity(request, reference);

      // Act & Assert
      assertThatThrownBy(() -> service.update(NON_EXISTING_ID, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.product.notFound");

      verify(repository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Activate Operations")
  class ActivateOperations {

    @Test
    @DisplayName("activate should set active to true when product is inactive")
    void activateShouldSetActiveToTrueWhenProductIsInactive() {

      // Arrange
      Product product = productWithId(EXISTING_ID);
      product.setActive(false);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(product));

      // Act
      service.activate(EXISTING_ID);

      // Assert
      assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("activate should not change product when product is already active")
    void activateShouldNotChangeProductWhenProductIsAlreadyActive() {

      // Arrange
      // O spy é necessário porque o estado final (ativo) é o mesmo nos dois
      // caminhos; só o verify prova que o método saiu cedo.
      Product product = spy(productWithId(EXISTING_ID));

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(product));

      // Act
      service.activate(EXISTING_ID);

      // Assert
      assertThat(product.isActive()).isTrue();
      verify(product, never()).setActive(anyBoolean());
    }

    @Test
    @DisplayName("activate should throw ResourceNotFoundException when id does not exist")
    void activateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.activate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.product.notFound");
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("deactivate should set active to false when product is active")
    void deactivateShouldSetActiveToFalseWhenProductIsActive() {

      // Arrange
      Product product = productWithId(EXISTING_ID);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(product));

      // Act
      service.deactivate(EXISTING_ID);

      // Assert
      assertThat(product.isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivate should not change product when product is already inactive")
    void deactivateShouldNotChangeProductWhenProductIsAlreadyInactive() {

      // Arrange
      // O spy é necessário porque o estado final (inativo) é o mesmo nos dois
      // caminhos; só o verify prova que o método saiu cedo. Ele é criado depois
      // do setActive(false), para que o verify não conte a chamada da preparação.
      Product inactive = productWithId(EXISTING_ID);
      inactive.setActive(false);
      Product product = spy(inactive);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(product));

      // Act
      service.deactivate(EXISTING_ID);

      // Assert
      assertThat(product.isActive()).isFalse();
      verify(product, never()).setActive(anyBoolean());
    }

    @Test
    @DisplayName("deactivate should throw ResourceNotFoundException when id does not exist")
    void deactivateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.deactivate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.product.notFound");
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove product when id exists")
    void deleteShouldRemoveProductWhenIdExists() {

      // Arrange
      Product product = productWithId(EXISTING_ID);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(product));

      // Act
      service.delete(EXISTING_ID);

      // Assert
      verify(repository).delete(product);
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.delete(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.product.notFound");

      verify(repository, never()).delete(any());
    }

    // #25, item 4: o catch de DataIntegrityViolationException talvez nunca dispare,
    // porque a exclusão só vai ao banco no commit. Mantido sem reforço até a decisão da issue.
    @Test
    @DisplayName("delete should throw DatabaseException when delete violates integrity")
    void deleteShouldThrowDatabaseExceptionWhenDeleteViolatesIntegrity() {

      // Arrange
      Product product = productWithId(EXISTING_ID);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(product));
      doThrow(DataIntegrityViolationException.class).when(repository).delete(product);

      // Act & Assert
      assertThatThrownBy(() -> service.delete(EXISTING_ID))
          .isInstanceOf(DatabaseException.class)
          .hasMessage("error.database.product.relatedEntities");
    }
  }
}
