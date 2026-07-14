package com.albertsilva.dev.dscatalog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.repository.TokenRepository;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

@Service
@Transactional
public class TokenService {

  @Value("${account.activation.token.hours}")
  private Long activationTokenExpirationHours;

  @Value("${account.password-recovery.token.minutes}")
  private Long passwordRecoveryTokenExpirationMinutes;

  private final TokenRepository tokenRepository;

  public TokenService(TokenRepository tokenRepository) {
    this.tokenRepository = tokenRepository;
  }

  public Token createActivationToken(User user) {
    return tokenRepository.save(Token.activationToken(user, activationTokenExpirationHours));
  }

  public Token createPasswordRecoveryToken(User user) {
    return tokenRepository.save(Token.passwordRecoveryToken(user, passwordRecoveryTokenExpirationMinutes));
  }

  public void disableAllActivationTokens(User user) {

    List<Token> tokens = tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.ACTIVATION);

    tokens.forEach(Token::disable);
  }

  public void disableAllPasswordRecoveryTokens(User user) {

    List<Token> tokens = tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.PASSWORD_RECOVERY);

    tokens.forEach(Token::disable);
  }

  public Token findAndValidateToken(String tokenValue, TokenType type) {

    Token token = tokenRepository.findByToken(tokenValue)
        .orElseThrow(() -> new ResourceNotFoundException("error.token.notFound"));
    token.validate(type);
    return token;
  }
}
