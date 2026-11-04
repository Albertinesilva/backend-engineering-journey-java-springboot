package com.albertsilva.dev.asjcatalog.validation.user.validator;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for UserUpdateValidator")
@ExtendWith(MockitoExtension.class)
class UserUpdateValidatorTest {

  private static final String UNIQUE_EMAIL_MESSAGE = "{user.email.unique}";
  private static final String PERSONAL_DATA_MESSAGE = "{user.password.personalData}";
  private static final String FIRST_NAME = "Maria";
  private static final String LAST_NAME = "Silva";
  private static final String SAFE_PASSWORD = "Secure@2026";

  @InjectMocks
  private UserUpdateValidator validator;

  @Mock
  private UserRepository repository;

  @Mock
  private HttpServletRequest request;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @Mock
  private NodeBuilderCustomizableContext nodeBuilder;

  private static UserUpdateRequest update(String firstName, String lastName, String email, String password) {
    return new UserUpdateRequest(firstName, lastName, email, password, Set.of(RoleFactory.OPERATOR_ROLE_ID));
  }

  private void givenUriVariables(Map<String, String> variables) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(variables);
  }

  /** Prepara o contexto para registrar violações em qualquer campo. */
  private void givenViolations() {
    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
  }

  static Stream<Arguments> passwordsWithPersonalData() {
    return Stream.of(
        Arguments.of("primeiro nome em maiúsculas",
            update(FIRST_NAME, LAST_NAME, NON_EXISTING_EMAIL, "Secure@" + FIRST_NAME.toUpperCase() + "2026")),
        Arguments.of("sobrenome",
            update(FIRST_NAME, LAST_NAME, NON_EXISTING_EMAIL, "Secure@" + LAST_NAME + "2026")),
        Arguments.of("parte local do e-mail",
            update(FIRST_NAME, LAST_NAME, "loja.central@example.com", "Secure@loja.central2026")));
  }

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should return true when email is available and password has no personal data")
    void isValidShouldReturnTrueWhenEmailIsAvailableAndPasswordHasNoPersonalData() {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      when(repository.existsByEmailIgnoreCaseAndIdNot(NON_EXISTING_EMAIL, EXISTING_ID)).thenReturn(false);

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, NON_EXISTING_EMAIL, SAFE_PASSWORD), context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCaseAndIdNot(NON_EXISTING_EMAIL, EXISTING_ID);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should skip email uniqueness when URI variables are missing")
    void isValidShouldSkipEmailUniquenessWhenUriVariablesAreMissing() {

      // Arrange
      givenUriVariables(null);

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, NON_EXISTING_EMAIL, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip email uniqueness when URI variables have no id")
    void isValidShouldSkipEmailUniquenessWhenUriVariablesHaveNoId() {

      // Arrange
      givenUriVariables(Map.of("other", EXISTING_ID.toString()));

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, NON_EXISTING_EMAIL, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip email uniqueness when URI id is not numeric")
    void isValidShouldSkipEmailUniquenessWhenUriIdIsNotNumeric() {

      // Arrange
      givenUriVariables(Map.of("id", "invalid-id"));

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, NON_EXISTING_EMAIL, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip all checks when email and password are blank")
    void isValidShouldSkipAllChecksWhenEmailAndPasswordAreBlank() {

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, "   ", "   "), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, request, context);
    }

    @Test
    @DisplayName("isValid should skip all checks when email and password are null")
    void isValidShouldSkipAllChecksWhenEmailAndPasswordAreNull() {

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, null, null), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, request, context);
    }

    @Test
    @DisplayName("isValid should return true when personal tokens are shorter than three characters")
    void isValidShouldReturnTrueWhenPersonalTokensAreShorterThanThreeCharacters() {

      // Arrange
      givenUriVariables(null);

      // Act
      boolean result = validator.isValid(update("Al", "Li", "jo@example.com", "Secure@AlLijo26"), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @ParameterizedTest(name = "[e-mail = {0}]")
    @NullSource
    @ValueSource(strings = "loja.central.example.com")
    @DisplayName("isValid should ignore email in password check when email is null or has no at sign")
    void isValidShouldIgnoreEmailInPasswordCheckWhenEmailIsNullOrHasNoAtSign(String email) {

      // Arrange
      if (email != null) {
        givenUriVariables(null);
      }

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, email, "Secure@loja.central2026"), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should return true when first name is null")
    void isValidShouldReturnTrueWhenFirstNameIsNull() {

      // Act
      boolean result = validator.isValid(update(null, LAST_NAME, null, SAFE_PASSWORD), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should reject email and attach error to email when email is used by another user")
    void isValidShouldRejectEmailAndAttachErrorToEmailWhenEmailIsUsedByAnotherUser() {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      givenViolations();
      when(repository.existsByEmailIgnoreCaseAndIdNot(EXISTING_EMAIL, EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, "  " + EXISTING_EMAIL.toUpperCase() + "  ",
          null), context);

      // Assert
      assertThat(result).isFalse();
      verify(repository).existsByEmailIgnoreCaseAndIdNot(EXISTING_EMAIL, EXISTING_ID);
      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate(UNIQUE_EMAIL_MESSAGE);
      verify(violationBuilder).addPropertyNode("email");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.validation.user.validator.UserUpdateValidatorTest#passwordsWithPersonalData")
    @DisplayName("isValid should reject password and attach error to password when password has personal data")
    void isValidShouldRejectPasswordAndAttachErrorToPasswordWhenPasswordHasPersonalData(String scenario,
        UserUpdateRequest update) {

      // Arrange
      givenUriVariables(null);
      givenViolations();

      // Act
      boolean result = validator.isValid(update, context);

      // Assert
      assertThat(result).isFalse();
      verify(context).buildConstraintViolationWithTemplate(PERSONAL_DATA_MESSAGE);
      verify(violationBuilder).addPropertyNode("password");
    }

    @Test
    @DisplayName("isValid should report a single password error when password has several personal data")
    void isValidShouldReportASinglePasswordErrorWhenPasswordHasSeveralPersonalData() {

      // Arrange
      givenUriVariables(null);
      givenViolations();

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, NON_EXISTING_EMAIL,
          "Secure@" + FIRST_NAME + LAST_NAME + "26"), context);

      // Assert
      assertThat(result).isFalse();
      verify(context).buildConstraintViolationWithTemplate(PERSONAL_DATA_MESSAGE);
      verify(nodeBuilder).addConstraintViolation();
    }

    @Test
    @DisplayName("isValid should report email and password errors when both are invalid")
    void isValidShouldReportEmailAndPasswordErrorsWhenBothAreInvalid() {

      // Arrange
      givenUriVariables(Map.of("id", EXISTING_ID.toString()));
      givenViolations();
      when(repository.existsByEmailIgnoreCaseAndIdNot(EXISTING_EMAIL, EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(update(FIRST_NAME, LAST_NAME, EXISTING_EMAIL, "Secure@" + LAST_NAME + "2026"),
          context);

      // Assert
      assertThat(result).isFalse();
      verify(context).buildConstraintViolationWithTemplate(UNIQUE_EMAIL_MESSAGE);
      verify(context).buildConstraintViolationWithTemplate(PERSONAL_DATA_MESSAGE);
      verify(violationBuilder).addPropertyNode("email");
      verify(violationBuilder).addPropertyNode("password");
    }
  }
}
