package com.albertsilva.dev.dscatalog.validation.category.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for CategoryCreateValidator")
@ExtendWith(MockitoExtension.class)
class CategoryCreateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{category.name.unique}";

  @InjectMocks
  private CategoryCreateValidator validator;

  @Mock
  private CategoryRepository repository;

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
    lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Nested
  @DisplayName("valid values")
  class ValidValuesTests {

    @Test
    @DisplayName("isValid should accept an available normalized name")
    void isValidShouldAcceptAvailableNormalizedName() {
      when(repository.existsByNameIgnoreCase("electronics")).thenReturn(false);

      boolean result = validator.isValid(new CategoryCreateRequest("  Electronics  ", null), context);

      assertThat(result).isTrue();
      verify(repository).existsByNameIgnoreCase("electronics");
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should skip blank names")
    void isValidShouldSkipBlankNames() {
      boolean result = validator.isValid(new CategoryCreateRequest("   ", null), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }
  }

  @Test
  @DisplayName("isValid should reject an existing name and attach the error to name")
  void isValidShouldRejectExistingName() {
    when(repository.existsByNameIgnoreCase("electronics")).thenReturn(true);

    boolean result = validator.isValid(new CategoryCreateRequest("Electronics", null), context);

    assertThat(result).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(UNIQUE_MESSAGE);
    verify(violationBuilder).addPropertyNode("name");
    verify(nodeBuilder).addConstraintViolation();
  }
}
