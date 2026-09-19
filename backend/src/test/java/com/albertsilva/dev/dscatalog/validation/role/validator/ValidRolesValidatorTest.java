package com.albertsilva.dev.dscatalog.validation.role.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.dscatalog.repository.RoleRepository;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for ValidRolesValidator")
@ExtendWith(MockitoExtension.class)
class ValidRolesValidatorTest {

  private static final String INVALID_ROLE_MESSAGE = "{role.invalid}";

  @InjectMocks
  private ValidRolesValidator validator;

  @Mock
  private RoleRepository repository;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @BeforeEach
  void setUp() {
    lenient().when(context.buildConstraintViolationWithTemplate(INVALID_ROLE_MESSAGE)).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Nested
  @DisplayName("valid values")
  class ValidValuesTests {

    @Test
    @DisplayName("isValid should return true when all role IDs exist")
    void isValidShouldReturnTrueWhenAllRoleIdsExist() {
      when(repository.existsById(1L)).thenReturn(true);
      when(repository.existsById(2L)).thenReturn(true);

      boolean result = validator.isValid(Set.of(1L, 2L), context);

      assertThat(result).isTrue();
      verify(repository).existsById(1L);
      verify(repository).existsById(2L);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true for null")
    void isValidShouldReturnTrueForNull() {
      boolean result = validator.isValid(null, context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should return true for an empty set")
    void isValidShouldReturnTrueForEmptySet() {
      boolean result = validator.isValid(Set.of(), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should validate a single existing role ID")
    void isValidShouldValidateSingleExistingRoleId() {
      when(repository.existsById(1L)).thenReturn(true);

      boolean result = validator.isValid(Set.of(1L), context);

      assertThat(result).isTrue();
      verify(repository).existsById(1L);
      verifyNoInteractions(context);
    }
  }

  @Nested
  @DisplayName("invalid values")
  class InvalidValuesTests {

    @Test
    @DisplayName("isValid should return false when a role ID does not exist")
    void isValidShouldReturnFalseWhenRoleIdDoesNotExist() {
      when(repository.existsById(999L)).thenReturn(false);

      boolean result = validator.isValid(Set.of(999L), context);

      assertThat(result).isFalse();
      verify(repository).existsById(999L);
      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate(INVALID_ROLE_MESSAGE);
      verify(violationBuilder).addConstraintViolation();
    }
  }
}
