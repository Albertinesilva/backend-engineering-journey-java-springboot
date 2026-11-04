package com.albertsilva.dev.asjcatalog.validation.product.validator;

import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NAME_FILTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.asjcatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;
import com.albertsilva.dev.asjcatalog.repository.ProductRepository;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for ProductCreateValidator")
@ExtendWith(MockitoExtension.class)
class ProductCreateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{product.name.unique}";
  private static final String CATEGORY_MESSAGE = "{product.categoryIds.invalid}";
  private static final String NORMALIZED_NAME = NAME_FILTER.trim().toLowerCase();

  @InjectMocks
  private ProductCreateValidator validator;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @Mock
  private NodeBuilderCustomizableContext nodeBuilder;

  /** Request da fábrica com o nome e as categorias informados. */
  private static ProductCreateRequest request(String name, List<Long> categoryIds) {
    ProductCreateRequest base = ProductFactory.createProductCreateRequest();
    return new ProductCreateRequest(name, base.description(), base.price(), base.imgUrl(), base.date(), categoryIds);
  }

  /** Prepara o contexto para registrar uma violação com a mensagem e o campo informados. */
  private void givenViolation(String message, String field) {
    when(context.buildConstraintViolationWithTemplate(message)).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode(field)).thenReturn(nodeBuilder);
  }

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should accept request when name is unique and categories exist")
    void isValidShouldAcceptRequestWhenNameIsUniqueAndCategoriesExist() {

      // Arrange
      when(productRepository.existsByNameIgnoreCase(NORMALIZED_NAME)).thenReturn(false);
      when(categoryRepository.existsById(CategoryFactory.EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(request("  " + NAME_FILTER + "  ", List.of(CategoryFactory.EXISTING_ID)),
          context);

      // Assert
      assertThat(result).isTrue();
      verify(productRepository).existsByNameIgnoreCase(NORMALIZED_NAME);
      verifyNoInteractions(context);
    }

    @ParameterizedTest(name = "nome = [{0}]")
    @NullSource
    @ValueSource(strings = "   ")
    @DisplayName("isValid should skip name uniqueness when name is null or blank")
    void isValidShouldSkipNameUniquenessWhenNameIsNullOrBlank(String name) {

      // Arrange
      when(categoryRepository.existsById(CategoryFactory.EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(request(name, List.of(CategoryFactory.EXISTING_ID)), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(productRepository, context);
    }

    @Test
    @DisplayName("isValid should skip category lookup when categoryIds are null")
    void isValidShouldSkipCategoryLookupWhenCategoryIdsAreNull() {

      // Arrange
      when(productRepository.existsByNameIgnoreCase(NORMALIZED_NAME)).thenReturn(false);

      // Act
      boolean result = validator.isValid(request(NAME_FILTER, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(categoryRepository, context);
    }

    @Test
    @DisplayName("isValid should reject name and attach error to name when name already exists")
    void isValidShouldRejectNameAndAttachErrorToNameWhenNameAlreadyExists() {

      // Arrange
      givenViolation(UNIQUE_MESSAGE, "name");
      when(productRepository.existsByNameIgnoreCase(NORMALIZED_NAME)).thenReturn(true);

      // Act
      boolean result = validator.isValid(request(NAME_FILTER, List.of()), context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    @DisplayName("isValid should reject categories and attach error to categoryIds when any category does not exist")
    void isValidShouldRejectCategoriesAndAttachErrorToCategoryIdsWhenAnyCategoryDoesNotExist() {

      // Arrange
      givenViolation(CATEGORY_MESSAGE, "categoryIds");
      when(productRepository.existsByNameIgnoreCase(NORMALIZED_NAME)).thenReturn(false);
      when(categoryRepository.existsById(CategoryFactory.NON_EXISTING_ID)).thenReturn(false);

      // Act
      boolean result = validator.isValid(request(NAME_FILTER, List.of(CategoryFactory.NON_EXISTING_ID)), context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(nodeBuilder).addConstraintViolation();
    }
  }
}
