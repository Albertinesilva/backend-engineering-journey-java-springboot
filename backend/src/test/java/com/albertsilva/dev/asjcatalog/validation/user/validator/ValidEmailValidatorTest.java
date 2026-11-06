package com.albertsilva.dev.asjcatalog.validation.user.validator;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.asjcatalog.validation.user.dns.MxRecordLookup;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for ValidEmailValidator")
@ExtendWith(MockitoExtension.class)
class ValidEmailValidatorTest {

  private static final String INVALID_EMAIL_MESSAGE = "{user.email.invalid}";
  private static final String DOMAIN = EXISTING_EMAIL.substring(EXISTING_EMAIL.indexOf('@') + 1);

  @InjectMocks
  private ValidEmailValidator validator;

  @Mock
  private MxRecordLookup mxRecordLookup;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  /** Prepara o contexto para registrar a violação de e-mail inválido. */
  private void givenInvalidEmailViolation() {
    when(context.buildConstraintViolationWithTemplate(INVALID_EMAIL_MESSAGE)).thenReturn(violationBuilder);
  }

  private void verifyInvalidEmailViolation() {
    verify(context).disableDefaultConstraintViolation();
    verify(violationBuilder).addConstraintViolation();
  }

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should return true without lookup when email is null")
    void isValidShouldReturnTrueWithoutLookupWhenEmailIsNull() {

      // Act
      boolean result = validator.isValid(null, context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context, mxRecordLookup);
    }

    @Test
    @DisplayName("isValid should return true without lookup when email is blank")
    void isValidShouldReturnTrueWithoutLookupWhenEmailIsBlank() {

      // Act
      boolean result = validator.isValid("   ", context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context, mxRecordLookup);
    }

    @Test
    @DisplayName("isValid should return true when domain has MX record")
    void isValidShouldReturnTrueWhenDomainHasMxRecord() {

      // Arrange
      when(mxRecordLookup.hasMxRecord(DOMAIN)).thenReturn(true);

      // Act
      boolean result = validator.isValid(EXISTING_EMAIL, context);

      // Assert
      assertThat(result).isTrue();
      verify(mxRecordLookup).hasMxRecord(DOMAIN);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should look up normalized domain when email has spaces and uppercase letters")
    void isValidShouldLookUpNormalizedDomainWhenEmailHasSpacesAndUppercaseLetters() {

      // Arrange
      when(mxRecordLookup.hasMxRecord(DOMAIN)).thenReturn(true);

      // Act
      boolean result = validator.isValid("  " + EXISTING_EMAIL.toUpperCase() + "  ", context);

      // Assert
      assertThat(result).isTrue();
      verify(mxRecordLookup).hasMxRecord(DOMAIN);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should reject email when domain has no MX record")
    void isValidShouldRejectEmailWhenDomainHasNoMxRecord() {

      // Arrange
      givenInvalidEmailViolation();
      when(mxRecordLookup.hasMxRecord(DOMAIN)).thenReturn(false);

      // Act
      boolean result = validator.isValid(EXISTING_EMAIL, context);

      // Assert
      assertThat(result).isFalse();
      verifyInvalidEmailViolation();
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {
        "user.example.com",
        "user@@example.com",
        "user@",
        "user@example.c",
        "user name@example.com",
        "user/name@example.com",
        "user@example_domain.com" })
    @DisplayName("isValid should reject email without lookup when format is invalid")
    void isValidShouldRejectEmailWithoutLookupWhenFormatIsInvalid(String email) {

      // Arrange
      givenInvalidEmailViolation();

      // Act
      boolean result = validator.isValid(email, context);

      // Assert
      assertThat(result).isFalse();
      verifyInvalidEmailViolation();
      verifyNoInteractions(mxRecordLookup);
    }
  }
}
