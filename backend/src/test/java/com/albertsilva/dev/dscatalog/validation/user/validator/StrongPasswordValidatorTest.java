package com.albertsilva.dev.dscatalog.validation.user.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for StrongPasswordValidator")
@ExtendWith(MockitoExtension.class)
class StrongPasswordValidatorTest {

  private StrongPasswordValidator validator;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @BeforeEach
  void setUp() {
    validator = new StrongPasswordValidator();
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
  }

  @Test
  @DisplayName("isValid should return true for a strong password without prohibited sequence")
  void isValidShouldReturnTrueForStrongPasswordWithoutProhibitedSequence() {
    boolean result = validator.isValid("ValidPass@135790", context);

    assertThat(result).isTrue();
    verifyNoInteractions(context);
  }

  @Test
  @DisplayName("isValid should return true for null because nullability is delegated to other constraints")
  void isValidShouldReturnTrueForNull() {
    boolean result = validator.isValid(null, context);

    assertThat(result).isTrue();
    verifyNoInteractions(context);
  }

  @Test
  @DisplayName("isValid should reject an empty password and report missing composition rules")
  void isValidShouldRejectEmptyPassword() {
    boolean result = validator.isValid("", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).containsExactlyInAnyOrder(
        "{user.password.uppercase}",
        "{user.password.lowercase}",
        "{user.password.number}",
        "{user.password.specialCharacter}");
  }

  @Test
  @DisplayName("isValid should reject whitespace")
  void isValidShouldRejectWhitespace() {
    boolean result = validator.isValid("Valid Pass@2026", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.whitespace}");
  }

  @Test
  @DisplayName("isValid should reject a password without uppercase letter")
  void isValidShouldRejectMissingUppercase() {
    boolean result = validator.isValid("validpass@2026", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.uppercase}");
  }

  @Test
  @DisplayName("isValid should reject a password without lowercase letter")
  void isValidShouldRejectMissingLowercase() {
    boolean result = validator.isValid("VALIDPASS@2026", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.lowercase}");
  }

  @Test
  @DisplayName("isValid should reject a password without number")
  void isValidShouldRejectMissingNumber() {
    boolean result = validator.isValid("ValidPass@abcdef", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.number}");
  }

  @Test
  @DisplayName("isValid should reject a password without special character")
  void isValidShouldRejectMissingSpecialCharacter() {
    boolean result = validator.isValid("ValidPass2026", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.specialCharacter}");
  }

  @Test
  @DisplayName("isValid should reject common passwords case-insensitively")
  void isValidShouldRejectCommonPasswordCaseInsensitively() {
    boolean result = validator.isValid("PASSWORD", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.common}");
  }

  @Test
  @DisplayName("isValid should reject ascending numeric sequences of six digits")
  void isValidShouldRejectAscendingNumericSequence() {
    boolean result = validator.isValid("ValidPass@123456", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.sequence}");
  }

  @Test
  @DisplayName("isValid should reject descending numeric sequences of six digits")
  void isValidShouldRejectDescendingNumericSequence() {
    boolean result = validator.isValid("ValidPass@654321", context);

    assertThat(result).isFalse();
    assertThat(validationMessages()).contains("{user.password.sequence}");
  }

  @Test
  @DisplayName("isValid should accept six digits when they are not a consecutive sequence")
  void isValidShouldAcceptNonSequentialDigits() {
    boolean result = validator.isValid("ValidPass@135790", context);

    assertThat(result).isTrue();
    verifyNoInteractions(context);
  }

  private List<String> validationMessages() {
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(context).disableDefaultConstraintViolation();
    verify(context, org.mockito.Mockito.atLeastOnce()).buildConstraintViolationWithTemplate(messageCaptor.capture());
    return messageCaptor.getAllValues();
  }
}
