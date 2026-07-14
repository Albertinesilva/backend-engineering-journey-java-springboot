package com.albertsilva.dev.dscatalog.domain.recovery;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_token")
public class Token implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant createdAt;

  @Column(nullable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant expireDate;

  @Column(nullable = false)
  private boolean disabled;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TokenType type;

  protected Token() {
  }

  public Token(String token, User user, Instant expireDate, TokenType type) {
    this.token = token;
    this.user = user;
    this.createdAt = Instant.now();
    this.expireDate = expireDate;
    this.type = type;
    this.disabled = false;
  }

  public boolean isExpired() {
    return expireDate.isBefore(Instant.now());
  }

  public boolean isValid() {
    return !disabled && !isExpired();
  }

  public void disable() {
    this.disabled = true;
  }

  public static Token activationToken(User user, long expirationHours) {
    return new Token(UUID.randomUUID().toString(), user,
        Instant.now().plus(expirationHours, ChronoUnit.HOURS), TokenType.ACTIVATION);
  }

  public static Token passwordRecoveryToken(User user, long expirationMinutes) {
    return new Token(UUID.randomUUID().toString(), user,
        Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES), TokenType.PASSWORD_RECOVERY);
  }

  public void validate(TokenType expectedType) {

    if (type != expectedType) {
      throw new InvalidTokenException("error.token.type.invalid");
    }

    if (disabled) {
      throw new InvalidTokenException("error.token.disabled");
    }

    if (isExpired()) {
      throw new InvalidTokenException("error.token.expired");
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpireDate() {
    return expireDate;
  }

  public void setExpireDate(Instant expireDate) {
    this.expireDate = expireDate;
  }

  public boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public TokenType getType() {
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Token other = (Token) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

}