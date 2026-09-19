package com.albertsilva.dev.dscatalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.factory.UserFactory;

@DataJpaTest
@DisplayName("TokenRepository Tests")
class TokenRepositoryTest {

  @Autowired
  private TokenRepository tokenRepository;

  @Autowired
  private UserRepository userRepository;

  @Nested
  @DisplayName("FindByToken Operations")
  class FindByTokenOperations {

    @Test
    @DisplayName("should return token when value exists")
    void shouldReturnTokenWhenValueExists() {
      // Arrange
      User user = saveUser();
      Token token = new Token("token-exists-1", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);
      tokenRepository.saveAndFlush(token);

      // Act
      Optional<Token> result = tokenRepository.findByToken("token-exists-1");

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().getToken()).isEqualTo("token-exists-1");
      assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("should return empty when token value does not exist")
    void shouldReturnEmptyWhenTokenValueDoesNotExist() {
      // Act
      Optional<Token> result = tokenRepository.findByToken("definitely-missing-token");

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("FindByUserAndTypeAndDisabledFalse Operations")
  class FindByUserAndTypeAndDisabledFalseOperations {

    @Test
    @DisplayName("should return only active tokens for the user and type")
    void shouldReturnOnlyActiveTokensForUserAndType() {
      // Arrange
      User user = saveUser();

      Token active = new Token("active-token-1", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);
      Token anotherActive = new Token("active-token-2", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);
      Token disabled = new Token("disabled-token-1", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);
      disabled.disable();
      Token otherType = new Token("password-recovery-token-1", user, Instant.now().plusSeconds(1800),
          TokenType.PASSWORD_RECOVERY);

      tokenRepository.saveAllAndFlush(List.of(active, anotherActive, disabled, otherType));

      // Act
      List<Token> result = tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.ACTIVATION);

      // Assert
      assertThat(result).hasSize(2);
      assertThat(result).extracting(Token::getToken).containsExactlyInAnyOrder("active-token-1", "active-token-2");
      assertThat(result).extracting(Token::getToken).doesNotContain("disabled-token-1");
    }

    @Test
    @DisplayName("should return empty when no active tokens match user and type")
    void shouldReturnEmptyWhenNoActiveTokensMatchUserAndType() {
      // Arrange
      User user = saveUser();
      Token disabled = new Token("disabled-token-2", user, Instant.now().plusSeconds(3600), TokenType.ACTIVATION);
      disabled.disable();
      tokenRepository.saveAndFlush(disabled);

      // Act
      List<Token> result = tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.ACTIVATION);

      // Assert
      assertThat(result).isEmpty();
    }
  }

  private User saveUser() {
    User user = UserFactory.createUser();
    user.setEmail("token-user-" + UUID.randomUUID() + "@gmail.com");
    return userRepository.saveAndFlush(user);
  }
}
