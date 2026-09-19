package com.albertsilva.dev.dscatalog.validation.product.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for ProductCreateValidator")
@ExtendWith(MockitoExtension.class)
class ProductCreateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{product.name.unique}";
  private static final String CATEGORY_MESSAGE = "{product.categoryIds.invalid}";

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

  @BeforeEach
  void setUp() {
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addPropertyNode("name")).thenReturn(nodeBuilder);
    lenient().when(violationBuilder.addPropertyNode("categoryIds")).thenReturn(nodeBuilder);
    lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Test
  @DisplayName("isValid should accept a unique name with existing categories")
  void isValidShouldAcceptUniqueNameWithExistingCategories() {
    when(productRepository.existsByNameIgnoreCase("smart tv")).thenReturn(false);
    when(categoryRepository.existsById(1L)).thenReturn(true);

    boolean result = validator.isValid(request("  Smart TV  ", List.of(1L)), context);

    assertThat(result).isTrue();
    verify(productRepository).existsByNameIgnoreCase("smart tv");
    verify(categoryRepository).existsById(1L);
    verifyNoInteractions(context);
  }

  @Test
  @DisplayName("isValid should reject a duplicated product name")
  void isValidShouldRejectDuplicatedProductName() {
    when(productRepository.existsByNameIgnoreCase("smart tv")).thenReturn(true);

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
    when(productRepository.existsByNameIgnoreCase("smart tv")).thenReturn(false);
    when(categoryRepository.existsById(999L)).thenReturn(false);

    boolean result = validator.isValid(request("Smart TV", List.of(999L)), context);

    assertThat(result).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(CATEGORY_MESSAGE);
    verify(violationBuilder).addPropertyNode("categoryIds");
    verify(nodeBuilder).addConstraintViolation();
  }

  @Test
  @DisplayName("isValid should skip category lookup when category IDs are null")
  void isValidShouldSkipNullCategoryIds() {
    when(productRepository.existsByNameIgnoreCase("smart tv")).thenReturn(false);

    boolean result = validator.isValid(request("Smart TV", null), context);

    assertThat(result).isTrue();
    verifyNoInteractions(categoryRepository, context);
  }

  private ProductCreateRequest request(String name, List<Long> categoryIds) {
    return new ProductCreateRequest(name, "Product description", 100.0, "https://example.com/image.png",
        Instant.now(), categoryIds);
  }
}
