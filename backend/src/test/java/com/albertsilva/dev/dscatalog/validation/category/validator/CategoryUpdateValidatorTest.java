package com.albertsilva.dev.dscatalog.validation.category.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for CategoryUpdateValidator")
@ExtendWith(MockitoExtension.class)
class CategoryUpdateValidatorTest {

  private static final String UNIQUE_MESSAGE = "{category.name.unique}";

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
    @DisplayName("isValid should accept the current name when repository excludes the current ID")
    void isValidShouldAcceptCurrentName() {
      givenId("7");
      when(repository.existsByNameIgnoreCaseAndIdNot("electronics", 7L)).thenReturn(false);

      boolean result = validator.isValid(new CategoryUpdateRequest(" Electronics ", null), context);

      assertThat(result).isTrue();
      verify(repository).existsByNameIgnoreCaseAndIdNot("electronics", 7L);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should skip the repository when URI id is missing")
    void isValidShouldSkipWhenUriIdIsMissing() {
      when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(null);

      boolean result = validator.isValid(new CategoryUpdateRequest("Electronics", null), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip blank names")
    void isValidShouldSkipBlankNames() {
      boolean result = validator.isValid(new CategoryUpdateRequest("   ", null), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, request, context);
    }
  }

  @Test
  @DisplayName("isValid should reject a name used by another category")
  void isValidShouldRejectNameUsedByAnotherCategory() {
    givenId("7");
    when(repository.existsByNameIgnoreCaseAndIdNot("electronics", 7L)).thenReturn(true);

    boolean result = validator.isValid(new CategoryUpdateRequest("Electronics", null), context);

    assertThat(result).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(UNIQUE_MESSAGE);
    verify(violationBuilder).addPropertyNode("name");
    verify(nodeBuilder).addConstraintViolation();
  }

  private void givenId(String id) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(Map.of("id", id));
  }
}
