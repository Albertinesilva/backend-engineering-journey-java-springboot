package com.albertsilva.dev.asjcatalog.validation.user.validator;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;
import com.albertsilva.dev.asjcatalog.security.auth.AuthenticatedUserService;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for UniqueEmailForAuthenticatedUserValidator")
@ExtendWith(MockitoExtension.class)
class UniqueEmailForAuthenticatedUserValidatorTest {

  private static final String UNIQUE_EMAIL_MESSAGE = "{user.email.unique}";

  @InjectMocks
  private UniqueEmailForAuthenticatedUserValidator validator;

  @Mock
  private UserRepository repository;

  @Mock
  private AuthenticatedUserService authenticatedUserService;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  private User authenticatedUser;

  @BeforeEach
  void setUp() {
    authenticatedUser = UserFactory.createUser();
    authenticatedUser.setId(EXISTING_ID);
  }

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @ParameterizedTest(name = "e-mail = [{0}]")
    @NullSource
    @ValueSource(strings = "   ")
    @DisplayName("isValid should return true without lookup when email is null or blank")
    void isValidShouldReturnTrueWithoutLookupWhenEmailIsNullOrBlank(String email) {

      // Act
      boolean result = validator.isValid(email, context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, authenticatedUserService, context);
    }

    @Test
    @DisplayName("isValid should return true when email is not used by another user")
    void isValidShouldReturnTrueWhenEmailIsNotUsedByAnotherUser() {

      // Arrange
      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(authenticatedUser);
      when(repository.existsByEmailIgnoreCaseAndIdNot(NON_EXISTING_EMAIL, EXISTING_ID)).thenReturn(false);

      // Act
      boolean result = validator.isValid(NON_EXISTING_EMAIL, context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCaseAndIdNot(NON_EXISTING_EMAIL, EXISTING_ID);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should check normalized email when email has spaces and uppercase letters")
    void isValidShouldCheckNormalizedEmailWhenEmailHasSpacesAndUppercaseLetters() {

      // Arrange
      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(authenticatedUser);
      when(repository.existsByEmailIgnoreCaseAndIdNot(NON_EXISTING_EMAIL, EXISTING_ID)).thenReturn(false);

      // Act
      boolean result = validator.isValid("  " + NON_EXISTING_EMAIL.toUpperCase() + "  ", context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsByEmailIgnoreCaseAndIdNot(NON_EXISTING_EMAIL, EXISTING_ID);
    }

    @Test
    @DisplayName("isValid should return false when email is used by another user")
    void isValidShouldReturnFalseWhenEmailIsUsedByAnotherUser() {

      // Arrange
      when(context.buildConstraintViolationWithTemplate(UNIQUE_EMAIL_MESSAGE)).thenReturn(violationBuilder);
      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(authenticatedUser);
      when(repository.existsByEmailIgnoreCaseAndIdNot(EXISTING_EMAIL, EXISTING_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(EXISTING_EMAIL, context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(violationBuilder).addConstraintViolation();
    }
  }
}
