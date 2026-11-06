package com.albertsilva.dev.asjcatalog.service;

import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.ACTIVATION_EXPIRATION_HOURS;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.ACTIVATION_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.NON_EXISTING_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.PASSWORD_RECOVERY_EXPIRATION_MINUTES;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.PASSWORD_RECOVERY_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.albertsilva.dev.asjcatalog.domain.recovery.Token;
import com.albertsilva.dev.asjcatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.factory.TokenFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.repository.TokenRepository;
import com.albertsilva.dev.asjcatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

@DisplayName("Tests for TokenService")
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  @InjectMocks
  private TokenService service;

  @Mock
  private TokenRepository tokenRepository;

  private User user;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "activationTokenExpirationHours", ACTIVATION_EXPIRATION_HOURS);
    ReflectionTestUtils.setField(service, "passwordRecoveryTokenExpirationMinutes",
        PASSWORD_RECOVERY_EXPIRATION_MINUTES);
    user = UserFactory.createUser();
  }

  @Nested
  @DisplayName("CreateActivationToken Operations")
  class CreateActivationTokenOperations {

    @Test
    @DisplayName("createActivationToken should save unused token when user is given")
    void createActivationTokenShouldSaveUnusedTokenWhenUserIsGiven() {

      // Arrange
      when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));
      Instant before = Instant.now();

      // Act
      Token result = service.createActivationToken(user);
      Instant after = Instant.now();

      // Assert
      assertThat(result.getUser()).isEqualTo(user);
      assertThat(result.getType()).isEqualTo(TokenType.ACTIVATION);
      assertThat(UUID.fromString(result.getToken())).hasToString(result.getToken());
      assertThat(result.getDisabled()).isFalse();
      assertThat(result.getExpireDate()).isBetween(before.plus(ACTIVATION_EXPIRATION_HOURS, ChronoUnit.HOURS),
          after.plus(ACTIVATION_EXPIRATION_HOURS, ChronoUnit.HOURS));
      verify(tokenRepository).save(result);
    }
  }

  @Nested
  @DisplayName("CreatePasswordRecoveryToken Operations")
  class CreatePasswordRecoveryTokenOperations {

    @Test
    @DisplayName("createPasswordRecoveryToken should save unused token when user is given")
    void createPasswordRecoveryTokenShouldSaveUnusedTokenWhenUserIsGiven() {

      // Arrange
      when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));
      Instant before = Instant.now();

      // Act
      Token result = service.createPasswordRecoveryToken(user);
      Instant after = Instant.now();

      // Assert
      assertThat(result.getUser()).isEqualTo(user);
      assertThat(result.getType()).isEqualTo(TokenType.PASSWORD_RECOVERY);
      assertThat(UUID.fromString(result.getToken())).hasToString(result.getToken());
      assertThat(result.getDisabled()).isFalse();
      assertThat(result.getExpireDate()).isBetween(
          before.plus(PASSWORD_RECOVERY_EXPIRATION_MINUTES, ChronoUnit.MINUTES),
          after.plus(PASSWORD_RECOVERY_EXPIRATION_MINUTES, ChronoUnit.MINUTES));
      verify(tokenRepository).save(result);
    }
  }

  @Nested
  @DisplayName("DisableAllActivationTokens Operations")
  class DisableAllActivationTokensOperations {

    @Test
    @DisplayName("disableAllActivationTokens should disable all active activation tokens for the user")
    void disableAllActivationTokensShouldDisableAllActiveActivationTokensForUser() {

      // Arrange
      Token first = TokenFactory.createActivationToken(user);
      Token second = TokenFactory.createActivationToken(user);

      when(tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.ACTIVATION))
          .thenReturn(List.of(first, second));

      // Act
      service.disableAllActivationTokens(user);

      // Assert
      assertThat(first.getDisabled()).isTrue();
      assertThat(second.getDisabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("DisableAllPasswordRecoveryTokens Operations")
  class DisableAllPasswordRecoveryTokensOperations {

    @Test
    @DisplayName("disableAllPasswordRecoveryTokens should disable all active recovery tokens for the user")
    void disableAllPasswordRecoveryTokensShouldDisableAllActiveRecoveryTokensForUser() {

      // Arrange
      Token first = TokenFactory.createPasswordRecoveryToken(user);
      Token second = TokenFactory.createPasswordRecoveryToken(user);

      when(tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.PASSWORD_RECOVERY))
          .thenReturn(List.of(first, second));

      // Act
      service.disableAllPasswordRecoveryTokens(user);

      // Assert
      assertThat(first.getDisabled()).isTrue();
      assertThat(second.getDisabled()).isTrue();
    }
  }

  @Nested
  @DisplayName("FindAndValidateToken Operations")
  class FindAndValidateTokenOperations {

    @Test
    @DisplayName("findAndValidateToken should return token when token is valid")
    void findAndValidateTokenShouldReturnTokenWhenTokenIsValid() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);

      when(tokenRepository.findByToken(ACTIVATION_TOKEN)).thenReturn(Optional.of(token));

      // Act
      Token result = service.findAndValidateToken(ACTIVATION_TOKEN, TokenType.ACTIVATION);

      // Assert
      assertThat(result).isSameAs(token);
    }

    @Test
    @DisplayName("findAndValidateToken should throw ResourceNotFoundException when token does not exist")
    void findAndValidateTokenShouldThrowResourceNotFoundExceptionWhenTokenDoesNotExist() {

      // Arrange
      when(tokenRepository.findByToken(NON_EXISTING_TOKEN)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken(NON_EXISTING_TOKEN, TokenType.ACTIVATION))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.token.notFound");
    }

    @Test
    @DisplayName("findAndValidateToken should throw InvalidTokenException when token type does not match")
    void findAndValidateTokenShouldThrowInvalidTokenExceptionWhenTokenTypeDoesNotMatch() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);

      when(tokenRepository.findByToken(ACTIVATION_TOKEN)).thenReturn(Optional.of(token));

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken(ACTIVATION_TOKEN, TokenType.PASSWORD_RECOVERY))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.type.invalid");
    }

    @Test
    @DisplayName("findAndValidateToken should throw InvalidTokenException when token is disabled")
    void findAndValidateTokenShouldThrowInvalidTokenExceptionWhenTokenIsDisabled() {

      // Arrange
      Token token = TokenFactory.createActivationToken(user);
      token.disable();

      when(tokenRepository.findByToken(ACTIVATION_TOKEN)).thenReturn(Optional.of(token));

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken(ACTIVATION_TOKEN, TokenType.ACTIVATION))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.disabled");
    }

    @Test
    @DisplayName("findAndValidateToken should throw InvalidTokenException when token is expired")
    void findAndValidateTokenShouldThrowInvalidTokenExceptionWhenTokenIsExpired() {

      // Arrange
      Token token = TokenFactory.createPasswordRecoveryToken(user);
      token.setExpireDate(Instant.now().minus(1, ChronoUnit.MINUTES));

      when(tokenRepository.findByToken(PASSWORD_RECOVERY_TOKEN)).thenReturn(Optional.of(token));

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken(PASSWORD_RECOVERY_TOKEN, TokenType.PASSWORD_RECOVERY))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.expired");
    }
  }
}
