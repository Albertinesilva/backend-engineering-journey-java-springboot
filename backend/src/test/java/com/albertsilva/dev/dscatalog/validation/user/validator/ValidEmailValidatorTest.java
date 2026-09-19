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

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for ValidEmailValidator")
@ExtendWith(MockitoExtension.class)
class ValidEmailValidatorTest {

  private static final String INVALID_EMAIL_MESSAGE = "{user.email.invalid}";

  @InjectMocks
  private ValidEmailValidator validator;

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
  @DisplayName("values accepted before DNS validation")
  class OptionalValuesTests {

    @Test
    @DisplayName("isValid should accept null")
    void isValidShouldAcceptNull() {
      boolean result = validator.isValid(null, context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should accept blank input")
    void isValidShouldAcceptBlankInput() {
      boolean result = validator.isValid("   ", context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }
  }

  @Nested
  @DisplayName("format validation")
  class FormatValidationTests {

    @Test
    @DisplayName("isValid should accept a valid email with a public MX domain")
    void isValidShouldAcceptEmailWithMxRecord() {
      boolean result = validator.isValid("user@gmail.com", context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should normalize surrounding spaces and case before validation")
    void isValidShouldNormalizeEmailBeforeValidation() {
      boolean result = validator.isValid("  USER@GMAIL.COM  ", context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should reject an email without at sign")
    void isValidShouldRejectEmailWithoutAtSign() {
      assertInvalidEmail("user.example.com");
    }

    @Test
    @DisplayName("isValid should reject an email with multiple at signs")
    void isValidShouldRejectEmailWithMultipleAtSigns() {
      assertInvalidEmail("user@@example.com");
    }

    @Test
    @DisplayName("isValid should reject an email without domain")
    void isValidShouldRejectEmailWithoutDomain() {
      assertInvalidEmail("user@");
    }

    @Test
    @DisplayName("isValid should reject an email without a valid extension")
    void isValidShouldRejectEmailWithoutValidExtension() {
      assertInvalidEmail("user@example.c");
    }

    @Test
    @DisplayName("isValid should reject whitespace inside an email")
    void isValidShouldRejectWhitespaceInsideEmail() {
      assertInvalidEmail("user name@example.com");
    }

    @Test
    @DisplayName("isValid should reject an email with unsupported local-part characters")
    void isValidShouldRejectUnsupportedLocalPartCharacters() {
      assertInvalidEmail("user/name@example.com");
    }

    @Test
    @DisplayName("isValid should reject an email with an invalid domain character")
    void isValidShouldRejectInvalidDomainCharacter() {
      assertInvalidEmail("user@example_domain.com");
    }
  }

  private void assertInvalidEmail(String email) {
    boolean result = validator.isValid(email, context);

    assertThat(result).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(INVALID_EMAIL_MESSAGE);
    verify(violationBuilder).addConstraintViolation();
  }
}
