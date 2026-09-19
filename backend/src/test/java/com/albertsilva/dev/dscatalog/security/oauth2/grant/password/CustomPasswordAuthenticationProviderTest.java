package com.albertsilva.dev.dscatalog.security.oauth2.grant.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import com.albertsilva.dev.dscatalog.domain.user.Role;
import com.albertsilva.dev.dscatalog.domain.user.User;

@DisplayName("CustomPasswordAuthenticationProvider tests")
@ExtendWith(MockitoExtension.class)
class CustomPasswordAuthenticationProviderTest {

  @Mock
  private OAuth2AuthorizationService authorizationService;

  @Mock
  @SuppressWarnings("rawtypes")
  private OAuth2TokenGenerator tokenGenerator;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private CustomPasswordAuthenticationProvider provider;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    AuthorizationServerContextHolder.setContext(mock(AuthorizationServerContext.class));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("authenticate")
  class AuthenticateTests {

    @Test
    @DisplayName("should authenticate successfully when user and password are valid")
    void shouldAuthenticateSuccessfullyWhenUserAndPasswordAreValid() {
      // Arrange
      User user = createUserWithRole("maria@gmail.com", "encoded-password", "read");
      OAuth2ClientAuthenticationToken clientPrincipal = createAuthenticatedClientPrincipal();
      SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

      when(userDetailsService.loadUserByUsername("maria@gmail.com")).thenReturn(user);
      when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);

      OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token",
          Instant.now(), Instant.now().plusSeconds(3600), Set.of("read"));
      OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh-token", Instant.now(),
          Instant.now().plusSeconds(86400));

      when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenReturn(accessToken, refreshToken);

      CustomPasswordAuthenticationToken authentication = new CustomPasswordAuthenticationToken(clientPrincipal,
          Set.of("read"), null, "maria@gmail.com", "123456");

      // Act
      Authentication result = provider.authenticate(authentication);

      // Assert
      assertThat(result).isInstanceOf(OAuth2AccessTokenAuthenticationToken.class);
      OAuth2AccessTokenAuthenticationToken accessTokenAuthentication = (OAuth2AccessTokenAuthenticationToken) result;
      assertThat(accessTokenAuthentication.getAccessToken().getTokenValue()).isEqualTo("access-token");
      assertThat(accessTokenAuthentication.getRefreshToken().getTokenValue()).isEqualTo("refresh-token");
      verify(authorizationService).save(any());
    }

    @Test
    @DisplayName("should reject when user does not exist")
    void shouldRejectWhenUserDoesNotExist() {
      // Arrange
      OAuth2ClientAuthenticationToken clientPrincipal = createAuthenticatedClientPrincipal();
      SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

      when(userDetailsService.loadUserByUsername("missing@gmail.com"))
          .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

      CustomPasswordAuthenticationToken authentication = new CustomPasswordAuthenticationToken(clientPrincipal,
          Set.of("read"), null, "missing@gmail.com", "123456");

      // Act / Assert
      assertThatThrownBy(() -> provider.authenticate(authentication))
          .isInstanceOf(OAuth2AuthenticationException.class)
          .extracting("error")
          .satisfies(error -> {
            assertThat(((org.springframework.security.oauth2.core.OAuth2Error) error).getErrorCode())
                .isEqualTo(OAuth2ErrorCodes.INVALID_GRANT);
            assertThat(((org.springframework.security.oauth2.core.OAuth2Error) error).getDescription())
                .isEqualTo("Invalid credentials");
          });
    }

    @Test
    @DisplayName("should reject when password is incorrect")
    void shouldRejectWhenPasswordIsIncorrect() {
      // Arrange
      User user = createUserWithRole("maria@gmail.com", "encoded-password", "read");
      OAuth2ClientAuthenticationToken clientPrincipal = createAuthenticatedClientPrincipal();
      SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

      when(userDetailsService.loadUserByUsername("maria@gmail.com")).thenReturn(user);
      when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

      CustomPasswordAuthenticationToken authentication = new CustomPasswordAuthenticationToken(clientPrincipal,
          Set.of("read"), null, "maria@gmail.com", "wrong-password");

      // Act / Assert
      assertThatThrownBy(() -> provider.authenticate(authentication))
          .isInstanceOf(OAuth2AuthenticationException.class)
          .extracting("error")
          .satisfies(error -> {
            assertThat(((org.springframework.security.oauth2.core.OAuth2Error) error).getErrorCode())
                .isEqualTo(OAuth2ErrorCodes.INVALID_GRANT);
            assertThat(((org.springframework.security.oauth2.core.OAuth2Error) error).getDescription())
                .isEqualTo("Invalid credentials");
          });
    }

    @Test
    @DisplayName("should reject when account is inactive")
    void shouldRejectWhenAccountIsInactive() {
      // Arrange
      User user = createUserWithRole("maria@gmail.com", "encoded-password", "read");
      user.setActive(false);
      OAuth2ClientAuthenticationToken clientPrincipal = createAuthenticatedClientPrincipal();
      SecurityContextHolder.getContext().setAuthentication(clientPrincipal);

      when(userDetailsService.loadUserByUsername("maria@gmail.com")).thenReturn(user);
      when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);

      CustomPasswordAuthenticationToken authentication = new CustomPasswordAuthenticationToken(clientPrincipal,
          Set.of("read"), null, "maria@gmail.com", "123456");

      // Act / Assert
      assertThatThrownBy(() -> provider.authenticate(authentication))
          .isInstanceOf(OAuth2AuthenticationException.class)
          .extracting("error")
          .satisfies(error -> {
            assertThat(((org.springframework.security.oauth2.core.OAuth2Error) error).getErrorCode())
                .isEqualTo(OAuth2ErrorCodes.INVALID_GRANT);
            assertThat(((org.springframework.security.oauth2.core.OAuth2Error) error).getDescription())
                .isEqualTo("Your account has not been activated yet. Please check your email.");
          });
    }

    @Test
    @DisplayName("should reject when client principal is not authenticated")
    void shouldRejectWhenClientPrincipalIsNotAuthenticated() {
      // Arrange
      OAuth2ClientAuthenticationToken clientPrincipal = mock(OAuth2ClientAuthenticationToken.class);
      when(clientPrincipal.isAuthenticated()).thenReturn(false);

      CustomPasswordAuthenticationToken authentication = new CustomPasswordAuthenticationToken(clientPrincipal,
          Set.of("read"), null, "maria@gmail.com", "123456");

      // Act / Assert
      assertThatThrownBy(() -> provider.authenticate(authentication))
          .isInstanceOf(OAuth2AuthenticationException.class)
          .extracting("error")
          .satisfies(error -> {
            assertThat(((org.springframework.security.oauth2.core.OAuth2Error) error).getErrorCode())
                .isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT);
          });
    }
  }

  @Nested
  @DisplayName("supports")
  class SupportsTests {

    @Test
    @DisplayName("should support CustomPasswordAuthenticationToken")
    void shouldSupportCustomPasswordAuthenticationToken() {
      assertThat(provider.supports(CustomPasswordAuthenticationToken.class)).isTrue();
    }

    @Test
    @DisplayName("should reject other authentication types")
    void shouldRejectOtherAuthenticationTypes() {
      assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }
  }

  private User createUserWithRole(String email, String password, String authority) {
    User user = new User(1L, "Maria", "Green", email, password, true);
    user.addRole(new Role(1L, authority));
    return user;
  }

  private OAuth2ClientAuthenticationToken createAuthenticatedClientPrincipal() {
    RegisteredClient registeredClient = RegisteredClient.withId("client-id")
        .clientId("myclientid")
        .clientSecret("myclientsecret")
        .scope("read")
        .authorizationGrantType(new AuthorizationGrantType("password"))
        .build();

    OAuth2ClientAuthenticationToken clientPrincipal = mock(OAuth2ClientAuthenticationToken.class);
    when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
    when(clientPrincipal.isAuthenticated()).thenReturn(true);
    lenient().when(clientPrincipal.getName()).thenReturn("myclientid");
    return clientPrincipal;
  }
}
