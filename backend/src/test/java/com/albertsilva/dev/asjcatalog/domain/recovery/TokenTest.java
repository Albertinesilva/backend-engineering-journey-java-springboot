package com.albertsilva.dev.asjcatalog.domain.recovery;

import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.ACTIVATION_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.albertsilva.dev.asjcatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.factory.TokenFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.service.exception.InvalidTokenException;

/**
 * Testes de {@code Token}. O {@code equals} e o {@code hashCode} ficam no
 * {@code EntityIdentityContractTest}.
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>{@code isValid()}: não é usado em produção; a decisão de remover ou manter
 * é da #25.</li>
 * </ul>
 */
@DisplayName("Tests for Token")
class TokenTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = UserFactory.createUser();
  }

  @Nested
  @DisplayName("Constructor Operations")
  class ConstructorOperations {

    @Test
    @DisplayName("constructor should create enabled token with creation date when data is given")
    void constructorShouldCreateEnabledTokenWithCreationDateWhenDataIsGiven() {

      // Arrange
      Instant expireDate = Instant.now().plus(1, ChronoUnit.HOURS);
      Instant before = Instant.now();

      // Act
      Token token = new Token(ACTIVATION_TOKEN, user, expireDate, TokenType.ACTIVATION);
      Instant after = Instant.now();

      // Assert
      assertThat(token.getToken()).isEqualTo(ACTIVATION_TOKEN);
      assertThat(token.getUser()).isSameAs(user);
      assertThat(token.getExpireDate()).isEqualTo(expireDate);
      assertThat(token.getType()).isEqualTo(TokenType.ACTIVATION);
      assertThat(token.getDisabled()).isFalse();
      assertThat(token.getCreatedAt()).isBetween(before, after);
    }
  }

  @Nested
  @DisplayName("Validate Operations")
  class ValidateOperations {

    @Test
    @DisplayName("validate should accept token when type matches and token is enabled and not expired")
    void validateShouldAcceptTokenWhenTypeMatchesAndTokenIsEnabledAndNotExpired() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);

      // Act & Assert
      assertThatCode(() -> token.validate(TokenType.ACTIVATION)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate should throw InvalidTokenException when type does not match")
    void validateShouldThrowInvalidTokenExceptionWhenTypeDoesNotMatch() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);

      // Act & Assert
      assertThatThrownBy(() -> token.validate(TokenType.PASSWORD_RECOVERY))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.type.invalid");
    }

    @Test
    @DisplayName("validate should throw InvalidTokenException when token is disabled")
    void validateShouldThrowInvalidTokenExceptionWhenTokenIsDisabled() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);
      token.disable();

      // Act & Assert
      assertThatThrownBy(() -> token.validate(TokenType.ACTIVATION))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.disabled");
    }

    @Test
    @DisplayName("validate should throw InvalidTokenException when token is expired")
    void validateShouldThrowInvalidTokenExceptionWhenTokenIsExpired() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);
      token.setExpireDate(Instant.now().minus(1, ChronoUnit.MINUTES));

      // Act & Assert
      assertThatThrownBy(() -> token.validate(TokenType.ACTIVATION))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.expired");
    }
  }

  @Nested
  @DisplayName("IsExpired Operations")
  class IsExpiredOperations {

    @Test
    @DisplayName("isExpired should return true when expire date is in the past")
    void isExpiredShouldReturnTrueWhenExpireDateIsInThePast() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);
      token.setExpireDate(Instant.now().minus(1, ChronoUnit.MINUTES));

      // Act
      boolean result = token.isExpired();

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isExpired should return false when expire date is in the future")
    void isExpiredShouldReturnFalseWhenExpireDateIsInTheFuture() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);

      // Act
      boolean result = token.isExpired();

      // Assert
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Disable Operations")
  class DisableOperations {

    @Test
    @DisplayName("disable should mark token as disabled")
    void disableShouldMarkTokenAsDisabled() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);

      // Act
      token.disable();

      // Assert
      assertThat(token.getDisabled()).isTrue();
    }
  }
}
