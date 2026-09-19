package com.albertsilva.dev.dscatalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.repository.TokenRepository;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

@DisplayName("Tests for TokenService")
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  @InjectMocks
  private TokenService service;

  @Mock
  private TokenRepository tokenRepository;

  @Nested
  @DisplayName("Creation Operations")
  class CreationOperations {

    @Test
    @DisplayName("createActivationToken should save an activation token with valid metadata")
    void createActivationTokenShouldSaveActivationTokenWithValidMetadata() {
      // Arrange
      ReflectionTestUtils.setField(service, "activationTokenExpirationHours", 24L);
      User user = UserFactory.createUser();
      user.setId(10L);

      when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      Token result = service.createActivationToken(user);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getUser()).isEqualTo(user);
      assertThat(result.getType()).isEqualTo(TokenType.ACTIVATION);
      assertThat(result.getToken()).isNotBlank();
      assertThat(result.getDisabled()).isFalse();
      assertThat(result.getExpireDate()).isAfter(Instant.now());
      verify(tokenRepository).save(any(Token.class));
    }

    @Test
    @DisplayName("createPasswordRecoveryToken should save a recovery token with valid metadata")
    void createPasswordRecoveryTokenShouldSavePasswordRecoveryTokenWithValidMetadata() {
      // Arrange
      ReflectionTestUtils.setField(service, "passwordRecoveryTokenExpirationMinutes", 30L);
      User user = UserFactory.createUser();
      user.setId(11L);

      when(tokenRepository.save(any(Token.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // Act
      Token result = service.createPasswordRecoveryToken(user);

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getUser()).isEqualTo(user);
      assertThat(result.getType()).isEqualTo(TokenType.PASSWORD_RECOVERY);
      assertThat(result.getToken()).isNotBlank();
      assertThat(result.getDisabled()).isFalse();
      assertThat(result.getExpireDate()).isAfter(Instant.now());
      verify(tokenRepository).save(any(Token.class));
    }
  }

  @Nested
  @DisplayName("Disable Operations")
  class DisableOperations {

    @Test
    @DisplayName("disableAllActivationTokens should disable all active activation tokens for the user")
    void disableAllActivationTokensShouldDisableAllActiveActivationTokensForUser() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(12L);
      Token first = new Token("activation-1", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);
      Token second = new Token("activation-2", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);

      when(tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.ACTIVATION))
          .thenReturn(List.of(first, second));

      // Act
      service.disableAllActivationTokens(user);

      // Assert
      assertThat(first.getDisabled()).isTrue();
      assertThat(second.getDisabled()).isTrue();
      verify(tokenRepository).findByUserAndTypeAndDisabledFalse(user, TokenType.ACTIVATION);
    }

    @Test
    @DisplayName("disableAllPasswordRecoveryTokens should disable all active recovery tokens for the user")
    void disableAllPasswordRecoveryTokensShouldDisableAllActiveRecoveryTokensForUser() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(13L);
      Token first = new Token("recovery-1", user, Instant.now().plusSeconds(1800), TokenType.PASSWORD_RECOVERY);
      Token second = new Token("recovery-2", user, Instant.now().plusSeconds(1800), TokenType.PASSWORD_RECOVERY);

      when(tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.PASSWORD_RECOVERY))
          .thenReturn(List.of(first, second));

      // Act
      service.disableAllPasswordRecoveryTokens(user);

      // Assert
      assertThat(first.getDisabled()).isTrue();
      assertThat(second.getDisabled()).isTrue();
      verify(tokenRepository).findByUserAndTypeAndDisabledFalse(user, TokenType.PASSWORD_RECOVERY);
    }
  }

  @Nested
  @DisplayName("Validation Operations")
  class ValidationOperations {

    @Test
    @DisplayName("findAndValidateToken should return token when valid")
    void findAndValidateTokenShouldReturnTokenWhenValid() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(14L);
      Token token = new Token("valid-token", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);

      when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

      // Act
      Token result = service.findAndValidateToken("valid-token", TokenType.ACTIVATION);

      // Assert
      assertThat(result).isEqualTo(token);
    }

    @Test
    @DisplayName("findAndValidateToken should throw ResourceNotFoundException when token does not exist")
    void findAndValidateTokenShouldThrowResourceNotFoundExceptionWhenTokenDoesNotExist() {
      // Arrange
      when(tokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken("missing-token", TokenType.ACTIVATION))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.token.notFound");
    }

    @Test
    @DisplayName("findAndValidateToken should throw InvalidTokenException when token type does not match")
    void findAndValidateTokenShouldThrowInvalidTokenExceptionWhenTypeDoesNotMatch() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(15L);
      Token token = new Token("type-mismatch-token", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);

      when(tokenRepository.findByToken("type-mismatch-token")).thenReturn(Optional.of(token));

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken("type-mismatch-token", TokenType.PASSWORD_RECOVERY))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.type.invalid");
    }

    @Test
    @DisplayName("findAndValidateToken should throw InvalidTokenException when token is disabled")
    void findAndValidateTokenShouldThrowInvalidTokenExceptionWhenTokenIsDisabled() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(16L);
      Token token = new Token("disabled-token", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);
      token.disable();

      when(tokenRepository.findByToken("disabled-token")).thenReturn(Optional.of(token));

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken("disabled-token", TokenType.ACTIVATION))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.disabled");
    }

    @Test
    @DisplayName("findAndValidateToken should throw InvalidTokenException when token is expired")
    void findAndValidateTokenShouldThrowInvalidTokenExceptionWhenTokenIsExpired() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(17L);
      Token token = new Token("expired-token", user, Instant.now().minusSeconds(60), TokenType.PASSWORD_RECOVERY);

      when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

      // Act & Assert
      assertThatThrownBy(() -> service.findAndValidateToken("expired-token", TokenType.PASSWORD_RECOVERY))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.expired");
    }
  }
}
