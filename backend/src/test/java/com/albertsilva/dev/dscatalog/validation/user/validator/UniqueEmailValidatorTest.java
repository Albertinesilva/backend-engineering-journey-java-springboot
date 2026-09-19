package com.albertsilva.dev.dscatalog.validation.user.validator;

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

import com.albertsilva.dev.dscatalog.repository.UserRepository;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for UniqueEmailValidator")
@ExtendWith(MockitoExtension.class)
class UniqueEmailValidatorTest {

  private static final String UNIQUE_EMAIL_MESSAGE = "{user.email.unique}";

  @InjectMocks
  private UniqueEmailValidator validator;

  @Mock
  private UserRepository repository;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @BeforeEach
  void setUp() {
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Nested
  @DisplayName("valid values")
  class ValidValuesTests {

    @Test
    @DisplayName("isValid should return true when the email is available")
    void isValidShouldReturnTrueWhenEmailIsAvailable() {
      when(repository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

      boolean result = validator.isValid("new@example.com", context);

      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCase("new@example.com");
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true for null without consulting the repository")
    void isValidShouldReturnTrueForNull() {
      boolean result = validator.isValid(null, context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should return true for blank email without consulting the repository")
    void isValidShouldReturnTrueForBlankEmail() {
      boolean result = validator.isValid("   ", context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should trim and normalize the email before checking availability")
    void isValidShouldTrimAndNormalizeEmail() {
      when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);

      boolean result = validator.isValid("  USER@EXAMPLE.COM  ", context);

      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCase("user@example.com");
      verifyNoInteractions(context);
    }
  }

  @Nested
  @DisplayName("invalid values")
  class InvalidValuesTests {

    @Test
    @DisplayName("isValid should return false when the email is already used")
    void isValidShouldReturnFalseWhenEmailAlreadyExists() {
      when(repository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

      boolean result = validator.isValid("user@example.com", context);

      assertThat(result).isFalse();
      verify(repository).existsByEmailIgnoreCase("user@example.com");
      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate(UNIQUE_EMAIL_MESSAGE);
      verify(violationBuilder).addConstraintViolation();
    }
  }
}
