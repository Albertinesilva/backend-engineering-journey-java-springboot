package com.albertsilva.dev.asjcatalog.validation.user.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.asjcatalog.dto.user.request.UserRegisterRequest;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

/**
 * Testes de {@code PasswordPersonalDataValidator}. O candidato é um
 * {@link UserRegisterRequest}, um dos DTOs que implementam
 * {@code PasswordPersonalDataCandidate}.
 */
@DisplayName("Tests for PasswordPersonalDataValidator")
@ExtendWith(MockitoExtension.class)
class PasswordPersonalDataValidatorTest {

  private static final String PERSONAL_DATA_MESSAGE = "{user.password.personalData}";
  private static final String FIRST_NAME = "Maria";
  private static final String LAST_NAME = "Silva";
  private static final String UNRELATED_EMAIL = "contato@example.com";
  private static final String SAFE_PASSWORD = "Secure@2026";

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
  }

  private static UserRegisterRequest candidate(String firstName, String lastName, String email, String password) {
    return new UserRegisterRequest(firstName, lastName, email, password);
  }

  /** Prepara o contexto para registrar a violação de dados pessoais no campo {@code password}. */
  private void givenPasswordViolation() {
    when(context.buildConstraintViolationWithTemplate(PERSONAL_DATA_MESSAGE)).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode("password")).thenReturn(nodeBuilder);
  }

  static Stream<Arguments> passwordsWithPersonalData() {
    return Stream.of(
        Arguments.of("primeiro nome em maiúsculas",
            candidate(FIRST_NAME, LAST_NAME, UNRELATED_EMAIL, "Secure" + FIRST_NAME.toUpperCase() + "2026")),
        Arguments.of("sobrenome",
            candidate(FIRST_NAME, LAST_NAME, UNRELATED_EMAIL, "Secure@" + LAST_NAME + "2026")),
        Arguments.of("parte local do e-mail",
            candidate(FIRST_NAME, LAST_NAME, "loja.central@example.com", "Secure@loja.central2026")),
        Arguments.of("primeiro nome com espaços nas pontas",
            candidate("  " + FIRST_NAME + "  ", LAST_NAME, UNRELATED_EMAIL, "Secure@" + FIRST_NAME + "2026")));
  }

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should return true when password has no personal data")
    void isValidShouldReturnTrueWhenPasswordHasNoPersonalData() {

      // Act
      boolean result = validator.isValid(candidate(FIRST_NAME, LAST_NAME, UNRELATED_EMAIL, SAFE_PASSWORD), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when candidate is null")
    void isValidShouldReturnTrueWhenCandidateIsNull() {

      // Act
      boolean result = validator.isValid(null, context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when password is null")
    void isValidShouldReturnTrueWhenPasswordIsNull() {

      // Act
      boolean result = validator.isValid(candidate(FIRST_NAME, LAST_NAME, UNRELATED_EMAIL, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when personal tokens are shorter than three characters")
    void isValidShouldReturnTrueWhenPersonalTokensAreShorterThanThreeCharacters() {

      // Act
      boolean result = validator.isValid(candidate("Al", "Li", "jo@example.com", "Secure@AlLijo26"), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when name has accents and password has the name without them")
    void isValidShouldReturnTrueWhenNameHasAccentsAndPasswordHasTheNameWithoutThem() {

      // Act
      boolean result = validator.isValid(candidate("João", LAST_NAME, UNRELATED_EMAIL, "Joao@Safe9"), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when email has no at sign")
    void isValidShouldReturnTrueWhenEmailHasNoAtSign() {

      // Act
      boolean result = validator.isValid(candidate("Ana", "Souza", "loja.central.example.com",
          "Secure@loja.central2026"), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when email is null")
    void isValidShouldReturnTrueWhenEmailIsNull() {

      // Act
      boolean result = validator.isValid(candidate(FIRST_NAME, LAST_NAME, null, SAFE_PASSWORD), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when first name is null")
    void isValidShouldReturnTrueWhenFirstNameIsNull() {

      // Act
      boolean result = validator.isValid(candidate(null, LAST_NAME, UNRELATED_EMAIL, SAFE_PASSWORD), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.albertsilva.dev.asjcatalog.validation.user.validator.PasswordPersonalDataValidatorTest#passwordsWithPersonalData")
    @DisplayName("isValid should reject password and attach error to password when password has personal data")
    void isValidShouldRejectPasswordAndAttachErrorToPasswordWhenPasswordHasPersonalData(String scenario,
        UserRegisterRequest candidate) {

      // Arrange
      givenPasswordViolation();

      // Act
      boolean result = validator.isValid(candidate, context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    @DisplayName("isValid should report a single error when password has several personal data")
    void isValidShouldReportASingleErrorWhenPasswordHasSeveralPersonalData() {

      // Arrange
      givenPasswordViolation();

      // Act
      boolean result = validator.isValid(candidate(FIRST_NAME, LAST_NAME, UNRELATED_EMAIL,
          "Secure@" + FIRST_NAME + LAST_NAME + "26"), context);

      // Assert
      assertThat(result).isFalse();
      verify(context).buildConstraintViolationWithTemplate(PERSONAL_DATA_MESSAGE);
      verify(nodeBuilder).addConstraintViolation();
    }
  }
}
