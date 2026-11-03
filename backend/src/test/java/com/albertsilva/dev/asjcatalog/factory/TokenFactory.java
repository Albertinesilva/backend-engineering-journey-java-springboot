package com.albertsilva.dev.asjcatalog.factory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.albertsilva.dev.asjcatalog.domain.recovery.Token;
import com.albertsilva.dev.asjcatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.asjcatalog.domain.user.User;

public class TokenFactory {

  public static final String ACTIVATION_TOKEN = "activation-token";
  public static final String PASSWORD_RECOVERY_TOKEN = "password-recovery-token";
  public static final String NON_EXISTING_TOKEN = "non-existing-token";
  public static final Long ACTIVATION_EXPIRATION_HOURS = 24L;
  public static final Long PASSWORD_RECOVERY_EXPIRATION_MINUTES = 30L;

  public static Token createActivationToken(User user) {
    return new Token(ACTIVATION_TOKEN, user, Instant.now().plus(1, ChronoUnit.HOURS), TokenType.ACTIVATION);
  }

  public static Token createPasswordRecoveryToken(User user) {
    return new Token(PASSWORD_RECOVERY_TOKEN, user, Instant.now().plus(1, ChronoUnit.HOURS),
        TokenType.PASSWORD_RECOVERY);
  }
}
