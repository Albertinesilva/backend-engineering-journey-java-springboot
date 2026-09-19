package com.albertsilva.dev.dscatalog.validation.user.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.dscatalog.validation.user.contract.PasswordPersonalDataCandidate;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for PasswordPersonalDataValidator")
@ExtendWith(MockitoExtension.class)
class PasswordPersonalDataValidatorTest {

  private static final String PERSONAL_DATA_MESSAGE = "{user.password.personalData}";

  private PasswordPersonalDataValidator validator;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @Mock
  private NodeBuilderCustomizableContext nodeBuilder;

  @BeforeEach
  void setUp() {
    validator = new PasswordPersonalDataValidator();
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addPropertyNode("password")).thenReturn(nodeBuilder);
    lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Nested
  @DisplayName("valid values")
  class ValidValuesTests {

    @Test
    @DisplayName("isValid should accept a password without personal data")
    void isValidShouldAcceptPasswordWithoutPersonalData() {
      PasswordPersonalDataCandidate candidate = candidate("Maria", "Silva", "maria@example.com",
          "Secure@2026");

      boolean result = validator.isValid(candidate, context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should accept a null candidate")
    void isValidShouldAcceptNullCandidate() {
      boolean result = validator.isValid(null, context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should accept a candidate with null password")
    void isValidShouldAcceptNullPassword() {
      PasswordPersonalDataCandidate candidate = candidate("Maria", "Silva", "maria@example.com", null);

      boolean result = validator.isValid(candidate, context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should ignore personal tokens shorter than three characters")
    void isValidShouldIgnoreShortPersonalTokens() {
      PasswordPersonalDataCandidate candidate = candidate("Al", "Li", "jo@example.com", "Secure@2026");

      boolean result = validator.isValid(candidate, context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should not remove accents during normalization")
    void isValidShouldNotRemoveAccentsDuringNormalization() {
      PasswordPersonalDataCandidate candidate = candidate("João", "Silva", "other@example.com",
          "Joao@Safe9");

      boolean result = validator.isValid(candidate, context);

      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }
  }

  @Nested
  @DisplayName("invalid values")
  class InvalidValuesTests {

    @Test
    @DisplayName("isValid should reject a password containing the first name case-insensitively")
    void isValidShouldRejectFirstNameCaseInsensitively() {
      PasswordPersonalDataCandidate candidate = candidate("Maria", "Silva", "other@example.com",
          "SecureMARIA2026");

      assertThat(validator.isValid(candidate, context)).isFalse();
      verifyPersonalDataViolation();
    }

    @Test
    @DisplayName("isValid should reject a password containing the last name")
    void isValidShouldRejectLastName() {
      PasswordPersonalDataCandidate candidate = candidate("Maria", "Silva", "other@example.com",
          "Secure@Silva2026");

      assertThat(validator.isValid(candidate, context)).isFalse();
      verifyPersonalDataViolation();
    }

    @Test
    @DisplayName("isValid should reject a password containing the email local part")
    void isValidShouldRejectEmailLocalPart() {
      PasswordPersonalDataCandidate candidate = candidate("Maria", "Silva", "maria.silva@example.com",
          "Secure@maria.silva2026");

      assertThat(validator.isValid(candidate, context)).isFalse();
      verifyPersonalDataViolation();
    }

    @Test
    @DisplayName("isValid should trim and compare personal data case-insensitively")
    void isValidShouldTrimAndComparePersonalDataCaseInsensitively() {
      PasswordPersonalDataCandidate candidate = candidate("  Maria  ", "Silva", "other@example.com",
          "Secure@MARIA2026");

      assertThat(validator.isValid(candidate, context)).isFalse();
      verifyPersonalDataViolation();
    }

    @Test
    @DisplayName("isValid should ignore an email without at sign")
    void isValidShouldIgnoreEmailWithoutAtSign() {
      PasswordPersonalDataCandidate candidate = candidate("Ana", "Souza", "maria.example.com",
          "Secure@maria2026");

      assertThat(validator.isValid(candidate, context)).isTrue();
      verifyNoInteractions(context);
    }
  }

  private void verifyPersonalDataViolation() {
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(PERSONAL_DATA_MESSAGE);
    verify(violationBuilder).addPropertyNode("password");
    verify(nodeBuilder).addConstraintViolation();
  }

  private PasswordPersonalDataCandidate candidate(String firstName, String lastName, String email,
      String password) {
    return new PasswordPersonalDataCandidate() {
      @Override
      public String firstName() {
        return firstName;
      }

      @Override
      public String lastName() {
        return lastName;
      }

      @Override
      public String email() {
        return email;
      }

      @Override
      public String password() {
        return password;
      }
    };
  }
}
