package com.albertsilva.dev.asjcatalog.validation.product.validator;

import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NAME_FILTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.asjcatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;
import com.albertsilva.dev.asjcatalog.repository.ProductRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for ProductUpdateValidator")
@ExtendWith(MockitoExtension.class)
class ProductUpdateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{product.name.unique}";
  private static final String CATEGORY_MESSAGE = "{product.categoryIds.invalid}";
  private static final String NORMALIZED_NAME = NAME_FILTER.trim().toLowerCase();

  @InjectMocks
  private ProductUpdateValidator validator;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private CategoryRepository categoryRepository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @Mock
  private NodeBuilderCustomizableContext nodeBuilder;

  /** Request da fábrica com o nome e as categorias informados. */
  private static ProductUpdateRequest request(String name, List<Long> categoryIds) {
    ProductUpdateRequest base = ProductFactory.createProductUpdateRequest();
    return new ProductUpdateRequest(name, base.description(), base.price(), base.imgUrl(), categoryIds);
  }

  private void givenUriVariables(Map<String, String> variables) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(variables);
  }

  /** Prepara o contexto para registrar uma violação com a mensagem e o campo informados. */
  private void givenViolation(String message, String field) {
    when(context.buildConstraintViolationWithTemplate(message)).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode(field)).thenReturn(nodeBuilder);
  }

  /**
   * Testes de {@code ProductUpdateValidator.isValid}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Id não numérico na URI: na requisição real, o {@code @PathVariable Long id}
   * falha antes do validador e responde 500 em vez de 400 (#36).</li>
   * </ul>
   */
  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should accept request when name belongs only to the current product and categories exist")
    void isValidShouldAcceptRequestWhenNameBelongsOnlyToTheCurrentProductAndCategoriesExist() {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      when(productRepository.existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID)).thenReturn(false);
      when(categoryRepository.existsById(CategoryFactory.EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(request(" " + NAME_FILTER + " ", List.of(CategoryFactory.EXISTING_ID)),
          context);

      // Assert
      assertThat(result).isTrue();
      verify(productRepository).existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID);
      verifyNoInteractions(context);
    }

    @ParameterizedTest(name = "[nome = {0}]")
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
      verifyNoInteractions(productRepository, request, context);
    }

    @Test
    @DisplayName("isValid should skip name uniqueness when URI variables are missing")
    void isValidShouldSkipNameUniquenessWhenUriVariablesAreMissing() {

      // Arrange
      givenUriVariables(null);
      when(categoryRepository.existsById(CategoryFactory.EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(request(NAME_FILTER, List.of(CategoryFactory.EXISTING_ID)), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(productRepository, context);
    }

    @Test
    @DisplayName("isValid should skip name uniqueness when URI variables have no id")
    void isValidShouldSkipNameUniquenessWhenUriVariablesHaveNoId() {

      // Arrange
      givenUriVariables(Map.of("other", EXISTING_ID.toString()));
      when(categoryRepository.existsById(CategoryFactory.EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(request(NAME_FILTER, List.of(CategoryFactory.EXISTING_ID)), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(productRepository, context);
    }

    @ParameterizedTest(name = "[categoryIds = {0}]")
    @NullAndEmptySource
    @DisplayName("isValid should skip category lookup when categoryIds are null or empty")
    void isValidShouldSkipCategoryLookupWhenCategoryIdsAreNullOrEmpty(List<Long> categoryIds) {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      when(productRepository.existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID)).thenReturn(false);

      // Act
      boolean result = validator.isValid(request(NAME_FILTER, categoryIds), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(categoryRepository, context);
    }

    @Test
    @DisplayName("isValid should reject name and attach error to name when name is used by another product")
    void isValidShouldRejectNameAndAttachErrorToNameWhenNameIsUsedByAnotherProduct() {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      givenViolation(UNIQUE_MESSAGE, "name");
      when(productRepository.existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID)).thenReturn(true);

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
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      givenViolation(CATEGORY_MESSAGE, "categoryIds");
      when(productRepository.existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID)).thenReturn(false);
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
