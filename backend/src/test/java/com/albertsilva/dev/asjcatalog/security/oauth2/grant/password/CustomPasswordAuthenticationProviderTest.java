package com.albertsilva.dev.asjcatalog.security.oauth2.grant.password;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ENCODED_PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.WRONG_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.security.userdetails.AuthenticatedUser;

@DisplayName("Tests for CustomPasswordAuthenticationProvider")
@ExtendWith(MockitoExtension.class)
class CustomPasswordAuthenticationProviderTest {

  private static final String CLIENT_ID = "myclientid";
  private static final String CLIENT_SECRET = "myclientsecret";
  private static final AuthorizationGrantType PASSWORD_GRANT = new AuthorizationGrantType("password");
  private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";
  private static final String INVALID_CREDENTIALS = "Invalid credentials";
  private static final String ACCESS_TOKEN_VALUE = "access-token";
  private static final String REFRESH_TOKEN_VALUE = "refresh-token";

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

  private RegisteredClient registeredClient;
  private OAuth2ClientAuthenticationToken clientPrincipal;
  private User user;
  private OAuth2AccessToken accessToken;
  private OAuth2RefreshToken refreshToken;

  @BeforeEach
  void setUp() {
    registeredClient = RegisteredClient.withId("client-id")
        .clientId(CLIENT_ID)
        .clientSecret(CLIENT_SECRET)
        .scope("read")
        .scope("write")
        .authorizationGrantType(PASSWORD_GRANT)
        .build();
    clientPrincipal = new OAuth2ClientAuthenticationToken(registeredClient,
        ClientAuthenticationMethod.CLIENT_SECRET_BASIC, CLIENT_SECRET);

    user = UserFactory.createUser();
    user.setId(EXISTING_ID);
    user.setPassword(ENCODED_PASSWORD);
    user.addRole(RoleFactory.createOperatorRole());

    Instant now = Instant.now();
    accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, ACCESS_TOKEN_VALUE, now,
        now.plus(1, ChronoUnit.HOURS));
    refreshToken = new OAuth2RefreshToken(REFRESH_TOKEN_VALUE, now, now.plus(1, ChronoUnit.DAYS));

    SecurityContextHolder.clearContext();
    AuthorizationServerContextHolder.setContext(mock(AuthorizationServerContext.class));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    AuthorizationServerContextHolder.resetContext();
  }

  private CustomPasswordAuthenticationToken loginRequest(String password) {
    return new CustomPasswordAuthenticationToken(clientPrincipal, null, null, user.getEmail(), password);
  }

  /** Prepara o login com credenciais válidas: cliente no contexto, usuário e senha conferidos. */
  @SuppressWarnings("unchecked")
  private void givenValidLogin() {
    SecurityContextHolder.getContext().setAuthentication(clientPrincipal);
    when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
    when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
    when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenReturn(accessToken, refreshToken);
  }

  /** Confere o código, a descrição e o {@code error_uri} do erro OAuth2 lançado. */
  private static void assertOAuth2Error(ThrowingCallable call, String errorCode, String description) {
    assertThatThrownBy(call).isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception -> {
      assertThat(exception.getError().getErrorCode()).isEqualTo(errorCode);
      assertThat(exception.getError().getDescription()).isEqualTo(description);
      assertThat(exception.getError().getUri()).isEqualTo(ERROR_URI);
    });
  }

  /** {@code UserDetails} com senha válida, ativo e com os três indicadores de conta informados. */
  private static UserDetails userDetailsWithStatus(boolean nonLocked, boolean nonExpired, boolean credentialsNonExpired) {
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetails.getPassword()).thenReturn(ENCODED_PASSWORD);
    when(userDetails.isEnabled()).thenReturn(true);
    when(userDetails.isAccountNonLocked()).thenReturn(nonLocked);
    if (nonLocked) {
      when(userDetails.isAccountNonExpired()).thenReturn(nonExpired);
    }
    if (nonLocked && nonExpired) {
      when(userDetails.isCredentialsNonExpired()).thenReturn(credentialsNonExpired);
    }
    return userDetails;
  }

  /**
   * Testes de {@code CustomPasswordAuthenticationProvider.authenticate}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Escopos autorizados: são as authorities do usuário filtradas pelos
   * escopos do cliente; como nenhuma {@code ROLE_*} coincide com {@code read} ou
   * {@code write}, o conjunto é sempre vazio (#39).</li>
   * </ul>
   */
  @Nested
  @DisplayName("Authenticate Operations")
  class AuthenticateOperations {

    @Test
    @DisplayName("authenticate should return access and refresh tokens when credentials are valid")
    void authenticateShouldReturnAccessAndRefreshTokensWhenCredentialsAreValid() {

      // Arrange
      givenValidLogin();

      // Act
      Authentication result = provider.authenticate(loginRequest(PASSWORD));

      // Assert
      assertThat(result).isInstanceOfSatisfying(OAuth2AccessTokenAuthenticationToken.class, tokens -> {
        assertThat(tokens.getRegisteredClient()).isEqualTo(registeredClient);
        assertThat(tokens.getAccessToken().getTokenType()).isEqualTo(OAuth2AccessToken.TokenType.BEARER);
        assertThat(tokens.getAccessToken().getTokenValue()).isEqualTo(ACCESS_TOKEN_VALUE);
        assertThat(tokens.getAccessToken().getIssuedAt()).isEqualTo(accessToken.getIssuedAt());
        assertThat(tokens.getAccessToken().getExpiresAt()).isEqualTo(accessToken.getExpiresAt());
        assertThat(tokens.getRefreshToken()).isSameAs(refreshToken);
      });
    }

    @Test
    @DisplayName("authenticate should save authorization when credentials are valid")
    void authenticateShouldSaveAuthorizationWhenCredentialsAreValid() {

      // Arrange
      givenValidLogin();
      ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(OAuth2Authorization.class);

      // Act
      provider.authenticate(loginRequest(PASSWORD));

      // Assert
      verify(authorizationService).save(captor.capture());
      OAuth2Authorization authorization = captor.getValue();
      assertThat(authorization.getRegisteredClientId()).isEqualTo(registeredClient.getId());
      assertThat(authorization.getPrincipalName()).isEqualTo(CLIENT_ID);
      assertThat(authorization.getAuthorizationGrantType()).isEqualTo(PASSWORD_GRANT);
      assertThat((Object) authorization.getAttribute(Principal.class.getName())).isSameAs(clientPrincipal);
      assertThat(authorization.getAccessToken().getToken().getTokenValue()).isEqualTo(ACCESS_TOKEN_VALUE);
      assertThat(authorization.getRefreshToken().getToken()).isSameAs(refreshToken);
    }

    @Test
    @DisplayName("authenticate should store authenticated user in security context when credentials are valid")
    void authenticateShouldStoreAuthenticatedUserInSecurityContextWhenCredentialsAreValid() {

      // Arrange
      givenValidLogin();

      // Act
      provider.authenticate(loginRequest(PASSWORD));

      // Assert
      Authentication current = SecurityContextHolder.getContext().getAuthentication();
      assertThat(current).isSameAs(clientPrincipal);
      assertThat(current.getDetails()).isInstanceOfSatisfying(AuthenticatedUser.class, details -> {
        assertThat(details.getId()).isEqualTo(EXISTING_ID);
        assertThat(details.getUsername()).isEqualTo(user.getEmail());
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority)
            .containsExactly(RoleFactory.OPERATOR_AUTHORITY);
      });
    }

    @Test
    @DisplayName("authenticate should request access then refresh token when credentials are valid")
    @SuppressWarnings("unchecked")
    void authenticateShouldRequestAccessThenRefreshTokenWhenCredentialsAreValid() {

      // Arrange
      givenValidLogin();
      ArgumentCaptor<OAuth2TokenContext> captor = ArgumentCaptor.forClass(OAuth2TokenContext.class);

      // Act
      provider.authenticate(loginRequest(PASSWORD));

      // Assert
      verify(tokenGenerator, times(2)).generate(captor.capture());
      List<OAuth2TokenContext> contexts = captor.getAllValues();
      assertThat(contexts).extracting(OAuth2TokenContext::getTokenType)
          .containsExactly(OAuth2TokenType.ACCESS_TOKEN, OAuth2TokenType.REFRESH_TOKEN);
      assertThat(contexts).allSatisfy(context -> {
        assertThat(context.getAuthorizationGrantType()).isEqualTo(PASSWORD_GRANT);
        assertThat(context.getRegisteredClient()).isEqualTo(registeredClient);
        assertThat((Object) context.getPrincipal()).isSameAs(clientPrincipal);
      });
    }

    @Test
    @DisplayName("authenticate should store token claims in authorization when access token has claims")
    @SuppressWarnings("unchecked")
    void authenticateShouldStoreTokenClaimsInAuthorizationWhenAccessTokenHasClaims() {

      // Arrange
      Jwt jwt = Jwt.withTokenValue(ACCESS_TOKEN_VALUE)
          .header("alg", "RS256")
          .claim("username", user.getEmail())
          .issuedAt(accessToken.getIssuedAt())
          .expiresAt(accessToken.getExpiresAt())
          .build();
      SecurityContextHolder.getContext().setAuthentication(clientPrincipal);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
      when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenReturn(jwt, refreshToken);
      ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(OAuth2Authorization.class);

      // Act
      provider.authenticate(loginRequest(PASSWORD));

      // Assert
      verify(authorizationService).save(captor.capture());
      assertThat(captor.getValue().getAccessToken().getClaims()).isEqualTo(jwt.getClaims());
    }

    @Test
    @DisplayName("authenticate should reject with invalid credentials when user does not exist")
    void authenticateShouldRejectWithInvalidCredentialsWhenUserDoesNotExist() {

      // Arrange
      when(userDetailsService.loadUserByUsername(user.getEmail()))
          .thenThrow(new UsernameNotFoundException("User not found with email: " + user.getEmail()));

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(PASSWORD)), OAuth2ErrorCodes.INVALID_GRANT,
          INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("authenticate should reject with invalid credentials when password is wrong")
    void authenticateShouldRejectWithInvalidCredentialsWhenPasswordIsWrong() {

      // Arrange
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
      when(passwordEncoder.matches(WRONG_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(WRONG_PASSWORD)), OAuth2ErrorCodes.INVALID_GRANT,
          INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("authenticate should reject with invalid credentials when account is inactive and password is wrong")
    void authenticateShouldRejectWithInvalidCredentialsWhenAccountIsInactiveAndPasswordIsWrong() {

      // Arrange
      user.setActive(false);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
      when(passwordEncoder.matches(WRONG_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(WRONG_PASSWORD)), OAuth2ErrorCodes.INVALID_GRANT,
          INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("authenticate should reject when account is inactive")
    void authenticateShouldRejectWhenAccountIsInactive() {

      // Arrange
      user.setActive(false);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(PASSWORD)), OAuth2ErrorCodes.INVALID_GRANT,
          "Your account has not been activated yet. Please check your email.");
    }

    @Test
    @DisplayName("authenticate should reject when account is locked")
    void authenticateShouldRejectWhenAccountIsLocked() {

      // Arrange
      UserDetails userDetails = userDetailsWithStatus(false, true, true);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(userDetails);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(PASSWORD)), OAuth2ErrorCodes.INVALID_GRANT,
          "Account is locked");
    }

    @Test
    @DisplayName("authenticate should reject when account is expired")
    void authenticateShouldRejectWhenAccountIsExpired() {

      // Arrange
      UserDetails userDetails = userDetailsWithStatus(true, false, true);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(userDetails);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(PASSWORD)), OAuth2ErrorCodes.INVALID_GRANT,
          "Account expired");
    }

    @Test
    @DisplayName("authenticate should reject when credentials are expired")
    void authenticateShouldRejectWhenCredentialsAreExpired() {

      // Arrange
      UserDetails userDetails = userDetailsWithStatus(true, true, false);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(userDetails);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(PASSWORD)), OAuth2ErrorCodes.INVALID_GRANT,
          "Credentials expired");
    }

    @Test
    @DisplayName("authenticate should throw server error when access token is not generated")
    @SuppressWarnings("unchecked")
    void authenticateShouldThrowServerErrorWhenAccessTokenIsNotGenerated() {

      // Arrange
      SecurityContextHolder.getContext().setAuthentication(clientPrincipal);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
      when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenReturn(null);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(PASSWORD)), OAuth2ErrorCodes.SERVER_ERROR,
          "The token generator failed to generate the access token.");

      verify(authorizationService, never()).save(any());
    }

    @Test
    @DisplayName("authenticate should throw server error when refresh token is not generated")
    @SuppressWarnings("unchecked")
    void authenticateShouldThrowServerErrorWhenRefreshTokenIsNotGenerated() {

      // Arrange
      SecurityContextHolder.getContext().setAuthentication(clientPrincipal);
      when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
      when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenReturn(accessToken, (OAuth2Token) null);

      // Act & Assert
      assertOAuth2Error(() -> provider.authenticate(loginRequest(PASSWORD)), OAuth2ErrorCodes.SERVER_ERROR,
          "The token generator failed to generate the refresh token.");

      verify(authorizationService, never()).save(any());
    }

    @Test
    @DisplayName("authenticate should reject with invalid client when client is not authenticated")
    void authenticateShouldRejectWithInvalidClientWhenClientIsNotAuthenticated() {

      // Arrange
      OAuth2ClientAuthenticationToken unauthenticatedClient = new OAuth2ClientAuthenticationToken(CLIENT_ID,
          ClientAuthenticationMethod.CLIENT_SECRET_BASIC, CLIENT_SECRET, null);
      CustomPasswordAuthenticationToken authentication = new CustomPasswordAuthenticationToken(unauthenticatedClient,
          null, null, user.getEmail(), PASSWORD);

      // Act & Assert
      assertThatThrownBy(() -> provider.authenticate(authentication))
          .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
              exception -> assertThat(exception.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT));

      verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    @DisplayName("authenticate should reject with invalid client when principal is not client authentication")
    void authenticateShouldRejectWithInvalidClientWhenPrincipalIsNotClientAuthentication() {

      // Arrange
      Authentication otherPrincipal = UsernamePasswordAuthenticationToken.authenticated(CLIENT_ID, null, Set.of());
      CustomPasswordAuthenticationToken authentication = new CustomPasswordAuthenticationToken(otherPrincipal, null,
          null, user.getEmail(), PASSWORD);

      // Act & Assert
      assertThatThrownBy(() -> provider.authenticate(authentication))
          .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
              exception -> assertThat(exception.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_CLIENT));

      verify(userDetailsService, never()).loadUserByUsername(any());
    }
  }

  @Nested
  @DisplayName("Supports Operations")
  class SupportsOperations {

    @Test
    @DisplayName("supports should return true when type is CustomPasswordAuthenticationToken")
    void supportsShouldReturnTrueWhenTypeIsCustomPasswordAuthenticationToken() {

      // Act
      boolean result = provider.supports(CustomPasswordAuthenticationToken.class);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("supports should return false when type is another authentication")
    void supportsShouldReturnFalseWhenTypeIsAnotherAuthentication() {

      // Act
      boolean result = provider.supports(UsernamePasswordAuthenticationToken.class);

      // Assert
      assertThat(result).isFalse();
    }
  }
}
