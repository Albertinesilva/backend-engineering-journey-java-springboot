package com.albertsilva.dev.dscatalog.validation.user.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

@DisplayName("Tests for UserUpdateValidator")
@ExtendWith(MockitoExtension.class)
class UserUpdateValidatorTest {

  private static final String UNIQUE_EMAIL_MESSAGE = "{user.email.unique}";
  private static final String PERSONAL_DATA_MESSAGE = "{user.password.personalData}";

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

  @BeforeEach
  void setUp() {
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addPropertyNode("email")).thenReturn(nodeBuilder);
    lenient().when(violationBuilder.addPropertyNode("password")).thenReturn(nodeBuilder);
    lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Nested
  @DisplayName("valid updates")
  class ValidUpdatesTests {

    @Test
    @DisplayName("isValid should accept an update with available email and unrelated password")
    void isValidShouldAcceptAvailableEmailAndUnrelatedPassword() {
      givenUpdateId("7");
      when(repository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 7L)).thenReturn(false);

      boolean result = validator.isValid(update("Maria", "Silva", "new@example.com", "Secure@2026"), context);

      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCaseAndIdNot("new@example.com", 7L);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should skip email uniqueness when URI id is missing")
    void isValidShouldSkipEmailUniquenessWhenUriIdIsMissing() {
      when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(null);

      boolean result = validator.isValid(update("Maria", "Silva", "new@example.com", null), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip email uniqueness when URI id is invalid")
    void isValidShouldSkipEmailUniquenessWhenUriIdIsInvalid() {
      givenUpdateId("invalid-id");

      boolean result = validator.isValid(update("Maria", "Silva", "new@example.com", null), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should skip blank email and password")
    void isValidShouldSkipBlankEmailAndPassword() {
      boolean result = validator.isValid(update("Maria", "Silva", "   ", "   "), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, request, context);
    }

    @Test
    @DisplayName("isValid should skip null email and password")
    void isValidShouldSkipNullEmailAndPassword() {
      boolean result = validator.isValid(update("Maria", "Silva", null, null), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, request, context);
    }

    @Test
    @DisplayName("isValid should ignore personal tokens shorter than three characters")
    void isValidShouldIgnoreShortPersonalTokens() {
      boolean result = validator.isValid(update("Al", "Li", "jo@example.com", "Secure@2026"), context);

      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
      verify(request).getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }
  }

  @Nested
  @DisplayName("invalid updates")
  class InvalidUpdatesTests {

    @Test
    @DisplayName("isValid should reject an email already used by another user")
    void isValidShouldRejectEmailUsedByAnotherUser() {
      givenUpdateId("7");
      when(repository.existsByEmailIgnoreCaseAndIdNot("user@example.com", 7L)).thenReturn(true);

      boolean result = validator.isValid(update("Maria", "Silva", "  USER@EXAMPLE.COM  ", null), context);

      assertThat(result).isFalse();
      verify(repository).existsByEmailIgnoreCaseAndIdNot("user@example.com", 7L);
      verifyViolation(UNIQUE_EMAIL_MESSAGE, "email");
    }

    @Test
    @DisplayName("isValid should reject a password containing the first name")
    void isValidShouldRejectPasswordContainingFirstName() {
      boolean result = validator.isValid(update("Maria", "Silva", "new@example.com", "Secure@MARIA2026"), context);

      assertThat(result).isFalse();
      verifyViolation(PERSONAL_DATA_MESSAGE, "password");
    }

    @Test
    @DisplayName("isValid should reject a password containing the last name")
    void isValidShouldRejectPasswordContainingLastName() {
      boolean result = validator.isValid(update("Maria", "Silva", "new@example.com", "Secure@Silva2026"), context);

      assertThat(result).isFalse();
      verifyViolation(PERSONAL_DATA_MESSAGE, "password");
    }

    @Test
    @DisplayName("isValid should reject a password containing the email local part")
    void isValidShouldRejectPasswordContainingEmailLocalPart() {
      boolean result = validator.isValid(update("Maria", "Silva", "maria.silva@example.com",
          "Secure@maria.silva2026"), context);

      assertThat(result).isFalse();
      verifyViolation(PERSONAL_DATA_MESSAGE, "password");
    }

    @Test
    @DisplayName("isValid should reject a null update object according to the current implementation")
    void isValidShouldRejectNullUpdateObjectAccordingToCurrentImplementation() {
      assertThatThrownBy(() -> validator.isValid(null, context))
          .isInstanceOf(NullPointerException.class);
    }
  }

  private UserUpdateRequest update(String firstName, String lastName, String email, String password) {
    return new UserUpdateRequest(firstName, lastName, email, password, Set.of(1L));
  }

  private void givenUpdateId(String id) {
    when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
        .thenReturn(Map.of("id", id));
  }

  private void verifyViolation(String message, String field) {
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(message);
    verify(violationBuilder).addPropertyNode(field);
    verify(nodeBuilder).addConstraintViolation();
  }
}
