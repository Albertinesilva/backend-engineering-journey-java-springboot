package com.albertsilva.dev.asjcatalog.validation.user.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for StrongPasswordValidator")
@ExtendWith(MockitoExtension.class)
class StrongPasswordValidatorTest {

  private static final String WHITESPACE = "{user.password.whitespace}";
  private static final String UPPERCASE = "{user.password.uppercase}";
  private static final String LOWERCASE = "{user.password.lowercase}";
  private static final String NUMBER = "{user.password.number}";
  private static final String SPECIAL_CHARACTER = "{user.password.specialCharacter}";
  private static final String COMMON = "{user.password.common}";
  private static final String SEQUENCE = "{user.password.sequence}";

  private StrongPasswordValidator validator;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @BeforeEach
  void setUp() {
    validator = new StrongPasswordValidator();
  }

  /** Mensagens registradas no contexto, na ordem em que foram registradas. */
  private List<String> validationMessages() {
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(context).disableDefaultConstraintViolation();
    verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(messageCaptor.capture());
    return messageCaptor.getAllValues();
  }

  static Stream<Arguments> weakPasswords() {
    return Stream.of(
        Arguments.of("senha vazia", "", List.of(UPPERCASE, LOWERCASE, NUMBER, SPECIAL_CHARACTER)),
        Arguments.of("espaço em branco", "Valid Pass@2026", List.of(WHITESPACE)),
        Arguments.of("sem letra maiúscula", "validpass@2026", List.of(UPPERCASE)),
        Arguments.of("sem letra minúscula", "VALIDPASS@2026", List.of(LOWERCASE)),
        Arguments.of("sem número", "ValidPass@abcdef", List.of(NUMBER)),
        Arguments.of("sem caractere especial", "ValidPass2026", List.of(SPECIAL_CHARACTER)),
        Arguments.of("senha comum em maiúsculas", "PASSWORD", List.of(LOWERCASE, NUMBER, SPECIAL_CHARACTER, COMMON)),
        Arguments.of("sequência crescente de seis dígitos", "ValidPass@123456", List.of(SEQUENCE)),
        Arguments.of("sequência decrescente de seis dígitos", "ValidPass@654321", List.of(SEQUENCE)));
  }

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should return true when password meets all rules")
    void isValidShouldReturnTrueWhenPasswordMeetsAllRules() {

      // Act
      boolean result = validator.isValid("Catalogo#2026", context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when password is null")
    void isValidShouldReturnTrueWhenPasswordIsNull() {

      // Act
      boolean result = validator.isValid(null, context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when six digits are not a consecutive sequence")
    void isValidShouldReturnTrueWhenSixDigitsAreNotAConsecutiveSequence() {

      // Act
      boolean result = validator.isValid("ValidPass@135790", context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.validation.user.validator.StrongPasswordValidatorTest#weakPasswords")
    @DisplayName("isValid should reject password with the broken rules when password is weak")
    void isValidShouldRejectPasswordWithTheBrokenRulesWhenPasswordIsWeak(String scenario, String password,
        List<String> expectedMessages) {

      // Arrange
      when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

      // Act
      boolean result = validator.isValid(password, context);

      // Assert
      assertThat(result).isFalse();
      assertThat(validationMessages()).containsExactlyInAnyOrderElementsOf(expectedMessages);
    }
  }
}
