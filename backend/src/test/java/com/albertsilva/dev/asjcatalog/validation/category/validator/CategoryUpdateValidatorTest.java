package com.albertsilva.dev.asjcatalog.validation.category.validator;

import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NAME_FILTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for CategoryUpdateValidator")
@ExtendWith(MockitoExtension.class)
class CategoryUpdateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{category.name.unique}";
  private static final String NORMALIZED_NAME = NAME_FILTER.trim().toLowerCase();

  @InjectMocks
  private CategoryUpdateValidator validator;

  @Mock
  private CategoryRepository repository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @Mock
  private NodeBuilderCustomizableContext nodeBuilder;

  private void givenUriVariables(Map<String, String> variables) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(variables);
  }

  /** Prepara o contexto para registrar uma violação no campo {@code name}. */
  private void givenNameViolation() {
    when(context.buildConstraintViolationWithTemplate(UNIQUE_MESSAGE)).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode("name")).thenReturn(nodeBuilder);
  }

  /**
   * Testes de {@code CategoryUpdateValidator.isValid}.
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
    @DisplayName("isValid should accept name when it belongs only to the current category")
    void isValidShouldAcceptNameWhenItBelongsOnlyToTheCurrentCategory() {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      when(repository.existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID)).thenReturn(false);

      // Act
      boolean result = validator.isValid(new CategoryUpdateRequest(" " + NAME_FILTER + " ", null), context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should skip repository when URI variables are missing")
    void isValidShouldSkipRepositoryWhenUriVariablesAreMissing() {

      // Arrange
      givenUriVariables(null);

      // Act
      boolean result = validator.isValid(new CategoryUpdateRequest(NAME_FILTER, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip repository when URI variables have no id")
    void isValidShouldSkipRepositoryWhenUriVariablesHaveNoId() {

      // Arrange
      givenUriVariables(Map.of("other", EXISTING_ID.toString()));

      // Act
      boolean result = validator.isValid(new CategoryUpdateRequest(NAME_FILTER, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip repository when name is blank")
    void isValidShouldSkipRepositoryWhenNameIsBlank() {

      // Act
      boolean result = validator.isValid(new CategoryUpdateRequest("   ", null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, request, context);
    }

    @Test
    @DisplayName("isValid should skip repository when name is null")
    void isValidShouldSkipRepositoryWhenNameIsNull() {

      // Act
      boolean result = validator.isValid(new CategoryUpdateRequest(null, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, request, context);
    }

    @Test
    @DisplayName("isValid should reject name and attach error to name when name is used by another category")
    void isValidShouldRejectNameAndAttachErrorToNameWhenNameIsUsedByAnotherCategory() {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      givenNameViolation();
      when(repository.existsByNameIgnoreCaseAndIdNot(NORMALIZED_NAME, EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(new CategoryUpdateRequest(NAME_FILTER, null), context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(nodeBuilder).addConstraintViolation();
    }
  }
}
