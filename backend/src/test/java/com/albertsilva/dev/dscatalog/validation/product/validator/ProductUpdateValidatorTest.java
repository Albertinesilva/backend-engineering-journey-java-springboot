package com.albertsilva.dev.dscatalog.validation.product.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for ProductUpdateValidator")
@ExtendWith(MockitoExtension.class)
class ProductUpdateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{product.name.unique}";
  private static final String CATEGORY_MESSAGE = "{product.categoryIds.invalid}";

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

  @BeforeEach
  void setUp() {
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addPropertyNode("name")).thenReturn(nodeBuilder);
    lenient().when(violationBuilder.addPropertyNode("categoryIds")).thenReturn(nodeBuilder);
    lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Nested
  @DisplayName("valid values")
  class ValidValuesTests {

    @Test
    @DisplayName("isValid should accept the current product name and existing categories")
    void isValidShouldAcceptCurrentNameAndExistingCategories() {
      givenId("7");
      when(productRepository.existsByNameIgnoreCaseAndIdNot("smart tv", 7L)).thenReturn(false);
      when(categoryRepository.existsById(1L)).thenReturn(true);

      boolean result = validator.isValid(request(" Smart TV ", List.of(1L)), context);

      assertThat(result).isTrue();
      verify(productRepository).existsByNameIgnoreCaseAndIdNot("smart tv", 7L);
      verify(categoryRepository).existsById(1L);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should skip name uniqueness when URI id is missing")
    void isValidShouldSkipNameUniquenessWhenUriIdIsMissing() {
      when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(null);
      when(categoryRepository.existsById(1L)).thenReturn(true);

      boolean result = validator.isValid(request("Smart TV", List.of(1L)), context);

      assertThat(result).isTrue();
      verifyNoInteractions(productRepository, context);
      verify(categoryRepository).existsById(1L);
    }
  }

  @Test
  @DisplayName("isValid should reject a name used by another product")
  void isValidShouldRejectNameUsedByAnotherProduct() {
    givenId("7");
    when(productRepository.existsByNameIgnoreCaseAndIdNot("smart tv", 7L)).thenReturn(true);

    boolean result = validator.isValid(request("Smart TV", List.of()), context);

    assertThat(result).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(UNIQUE_MESSAGE);
    verify(violationBuilder).addPropertyNode("name");
    verify(nodeBuilder).addConstraintViolation();
  }

  @Test
  @DisplayName("isValid should reject a missing category")
  void isValidShouldRejectMissingCategory() {
    givenId("7");
    when(productRepository.existsByNameIgnoreCaseAndIdNot("smart tv", 7L)).thenReturn(false);
    when(categoryRepository.existsById(999L)).thenReturn(false);

    boolean result = validator.isValid(request("Smart TV", List.of(999L)), context);

    assertThat(result).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(CATEGORY_MESSAGE);
    verify(violationBuilder).addPropertyNode("categoryIds");
    verify(nodeBuilder).addConstraintViolation();
  }

  private ProductUpdateRequest request(String name, List<Long> categoryIds) {
    return new ProductUpdateRequest(name, "Product description", 100.0, "https://example.com/image.png",
        categoryIds);
  }

  private void givenId(String id) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(Map.of("id", id));
  }
}
