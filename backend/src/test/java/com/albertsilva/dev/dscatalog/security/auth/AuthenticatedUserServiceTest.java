package com.albertsilva.dev.dscatalog.security.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException;

@DisplayName("Tests for AuthenticatedUserService")
@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

  @InjectMocks
  private AuthenticatedUserService service;

  @Mock
  private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("getAuthenticatedUser should return user when principal is a JWT with valid userId")
  void getAuthenticatedUserShouldReturnUserWhenPrincipalIsJwtWithValidUserId() {
    // Arrange
    User user = UserFactory.createUser();
    user.setId(1L);

    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        jwtWithUserId(1L), null, null));

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    // Act
    User result = service.getAuthenticatedUser();

    // Assert
    assertNotNull(result);
    assertEquals(user.getId(), result.getId());
    verify(userRepository, times(1)).findById(1L);
  }

  @Test
  @DisplayName("getAuthenticatedUser should throw AuthenticatedUserNotFoundException when authentication is null")
  void getAuthenticatedUserShouldThrowWhenAuthenticationIsNull() {
    // Act & Assert
    AuthenticatedUserNotFoundException exception = assertThrows(AuthenticatedUserNotFoundException.class,
        () -> service.getAuthenticatedUser());

    assertEquals("error.auth.invalid.principal", exception.getMessage());
  }

  @Test
  @DisplayName("getAuthenticatedUser should throw AuthenticatedUserNotFoundException when principal is not a JWT")
  void getAuthenticatedUserShouldThrowWhenPrincipalIsNotJwt() {
    // Arrange
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("not-jwt", null, null));

    // Act & Assert
    AuthenticatedUserNotFoundException exception = assertThrows(AuthenticatedUserNotFoundException.class,
        () -> service.getAuthenticatedUser());

    assertEquals("error.auth.invalid.principal", exception.getMessage());
  }

  @Test
  @DisplayName("getAuthenticatedUser should throw AuthenticatedUserNotFoundException when userId claim is missing")
  void getAuthenticatedUserShouldThrowWhenUserIdClaimIsMissing() {
    // Arrange
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        jwtWithoutUserId(), null, null));

    // Act & Assert
    AuthenticatedUserNotFoundException exception = assertThrows(AuthenticatedUserNotFoundException.class,
        () -> service.getAuthenticatedUser());

    assertEquals("error.auth.userId.claim.notFound", exception.getMessage());
  }

  @Test
  @DisplayName("getAuthenticatedUser should throw AuthenticatedUserNotFoundException when userId claim is invalid")
  void getAuthenticatedUserShouldThrowWhenUserIdClaimIsInvalid() {
    // Arrange
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        jwtWithUserId(0L), null, null));

    // Act & Assert
    AuthenticatedUserNotFoundException exception = assertThrows(AuthenticatedUserNotFoundException.class,
        () -> service.getAuthenticatedUser());

    assertEquals("error.auth.userId.claim.notFound", exception.getMessage());
  }

  @Test
  @DisplayName("getAuthenticatedUser should throw AuthenticatedUserNotFoundException when user is not found")
  void getAuthenticatedUserShouldThrowWhenUserIsNotFound() {
    // Arrange
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        jwtWithUserId(999L), null, null));

    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    AuthenticatedUserNotFoundException exception = assertThrows(AuthenticatedUserNotFoundException.class,
        () -> service.getAuthenticatedUser());

    assertEquals("error.auth.user.notFound", exception.getMessage());
    verify(userRepository, times(1)).findById(999L);
  }

  @Test
  @DisplayName("isCurrentUser should return true when userId matches authenticated user")
  void isCurrentUserShouldReturnTrueWhenUserIdMatchesAuthenticatedUser() {
    // Arrange
    User user = UserFactory.createUser();
    user.setId(7L);

    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        jwtWithUserId(7L), null, null));

    when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    // Act
    boolean result = service.isCurrentUser(7L);

    // Assert
    assertTrue(result);
  }

  @Test
  @DisplayName("isCurrentUser should return false when userId differs from authenticated user")
  void isCurrentUserShouldReturnFalseWhenUserIdDiffersFromAuthenticatedUser() {
    // Arrange
    User user = UserFactory.createUser();
    user.setId(7L);

    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        jwtWithUserId(7L), null, null));

    when(userRepository.findById(7L)).thenReturn(Optional.of(user));

    // Act
    boolean result = service.isCurrentUser(9L);

    // Assert
    assertFalse(result);
  }

  private Jwt jwtWithUserId(Long userId) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claims(claims -> claims.putAll(Map.of("userId", userId)))
        .build();
  }

  private Jwt jwtWithoutUserId() {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claims(claims -> claims.putAll(Map.of("sub", "user@example.com")))
        .build();
  }
}
