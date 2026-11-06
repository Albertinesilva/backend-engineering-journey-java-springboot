package com.albertsilva.dev.asjcatalog.validation.category.validator;

import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NAME_FILTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for CategoryCreateValidator")
@ExtendWith(MockitoExtension.class)
class CategoryCreateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{category.name.unique}";
  private static final String NORMALIZED_NAME = NAME_FILTER.trim().toLowerCase();

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

  /** Prepara o contexto para registrar uma violação no campo {@code name}. */
  private void givenNameViolation() {
    when(context.buildConstraintViolationWithTemplate(UNIQUE_MESSAGE)).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode("name")).thenReturn(nodeBuilder);
  }

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should accept name when normalized name is available")
    void isValidShouldAcceptNameWhenNormalizedNameIsAvailable() {

      // Arrange
      when(repository.existsByNameIgnoreCase(NORMALIZED_NAME)).thenReturn(false);

      // Act
      boolean result = validator.isValid(new CategoryCreateRequest("  " + NAME_FILTER + "  ", null), context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsByNameIgnoreCase(NORMALIZED_NAME);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should skip repository when name is blank")
    void isValidShouldSkipRepositoryWhenNameIsBlank() {

      // Act
      boolean result = validator.isValid(new CategoryCreateRequest("   ", null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip repository when name is null")
    void isValidShouldSkipRepositoryWhenNameIsNull() {

      // Act
      boolean result = validator.isValid(new CategoryCreateRequest(null, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should reject name and attach error to name when name already exists")
    void isValidShouldRejectNameAndAttachErrorToNameWhenNameAlreadyExists() {

      // Arrange
      givenNameViolation();
      when(repository.existsByNameIgnoreCase(NORMALIZED_NAME)).thenReturn(true);

      // Act
      boolean result = validator.isValid(new CategoryCreateRequest(NAME_FILTER, null), context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(nodeBuilder).addConstraintViolation();
    }
  }
}
