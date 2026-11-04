package com.albertsilva.dev.asjcatalog.validation.user.validator;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_EMAIL;
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

import com.albertsilva.dev.asjcatalog.repository.UserRepository;

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

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should return true when email is available")
    void isValidShouldReturnTrueWhenEmailIsAvailable() {

      // Arrange
      when(repository.existsByEmailIgnoreCase(NON_EXISTING_EMAIL)).thenReturn(false);

      // Act
      boolean result = validator.isValid(NON_EXISTING_EMAIL, context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCase(NON_EXISTING_EMAIL);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when email is null")
    void isValidShouldReturnTrueWhenEmailIsNull() {

      // Act
      boolean result = validator.isValid(null, context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should return true when email is blank")
    void isValidShouldReturnTrueWhenEmailIsBlank() {

      // Act
      boolean result = validator.isValid("   ", context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should check normalized email when email has spaces and uppercase letters")
    void isValidShouldCheckNormalizedEmailWhenEmailHasSpacesAndUppercaseLetters() {

      // Arrange
      when(repository.existsByEmailIgnoreCase(NON_EXISTING_EMAIL)).thenReturn(false);

      // Act
      boolean result = validator.isValid("  " + NON_EXISTING_EMAIL.toUpperCase() + "  ", context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCase(NON_EXISTING_EMAIL);
    }

    @Test
    @DisplayName("isValid should return false when email is already used")
    void isValidShouldReturnFalseWhenEmailIsAlreadyUsed() {

      // Arrange
      when(context.buildConstraintViolationWithTemplate(UNIQUE_EMAIL_MESSAGE)).thenReturn(violationBuilder);
      when(repository.existsByEmailIgnoreCase(EXISTING_EMAIL)).thenReturn(true);

      // Act
      boolean result = validator.isValid(EXISTING_EMAIL, context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(violationBuilder).addConstraintViolation();
    }
  }
}
