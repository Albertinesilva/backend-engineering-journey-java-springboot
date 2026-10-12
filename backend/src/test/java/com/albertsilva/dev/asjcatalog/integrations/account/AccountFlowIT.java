package com.albertsilva.dev.asjcatalog.integrations.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.asjcatalog.domain.recovery.Token;
import com.albertsilva.dev.asjcatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.PasswordResetRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.PasswordUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserEmailRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.asjcatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.asjcatalog.repository.TokenRepository;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * Fluxos de conta de ponta a ponta (HTTP real, H2 do perfil test, JWT real).
 *
 * <p>
 * O envio de e-mail é substituído por um mock de {@link JavaMailSenderImpl}
 * (tipo concreto, porque o validador de conexão SMTP do Spring Boot injeta a
 * implementação). Os tokens de conta são obtidos pelo {@link TokenRepository}.
 * Os e-mails usam o domínio gmail.com porque {@code @ValidEmail} consulta o
 * registro MX no DNS. As mensagens de erro são conferidas em inglês
 * ({@code Accept-Language: en}).
 * </p>
 */
@Transactional
@DisplayName("Account Flow Integration Tests")
class AccountFlowIT extends AbstractIT {

  private static final String ACCOUNTS_URL = "/api/v1/accounts";
  private static final String STRONG_PASSWORD = "Kx9#mPq2Lw!";
  private static final String NEW_STRONG_PASSWORD = "Tz4$vBn8Qe@";
  private static final String NEW_USER_EMAIL = "joana.flowit@gmail.com";
  private static final String EXISTING_EMAIL = "maria@gmail.com";

  @MockitoBean
  private JavaMailSenderImpl mailSender;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TokenRepository tokenRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Value("${security.client-id}")
  private String clientId;

  @Value("${security.client-secret}")
  private String clientSecret;

  @BeforeEach
  void setUp() {
    when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
  }

  @Nested
  @DisplayName("Registration and Activation")
  class RegistrationAndActivation {

    @Test
    @DisplayName("register should create an inactive user whose activation token enables login")
    void registerThenActivateShouldAllowLogin() throws Exception {

      // Arrange
      UserRegisterRequest request = new UserRegisterRequest("Joana", "Pereira", NEW_USER_EMAIL, STRONG_PASSWORD);

      // Act
      ResultActions registerResult = mockMvc.perform(post(ACCOUNTS_URL + "/register")
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
      User registered = userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow();
      String activationToken = singleOpenToken(registered, TokenType.ACTIVATION).getToken();
      ResultActions activateResult = mockMvc.perform(get(ACCOUNTS_URL + "/activate").param("token", activationToken));

      // Assert
      registerResult
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value(NEW_USER_EMAIL));
      activateResult.andExpect(status().isNoContent());

      assertTrue(userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow().isActive());
      assertTrue(tokenRepository.findByToken(activationToken).orElseThrow().getDisabled());
      assertFalse(tokenUtil.obtainAccessToken(mockMvc, NEW_USER_EMAIL, STRONG_PASSWORD).isBlank());
    }

    @Test
    @DisplayName("login should be rejected with invalid_grant before the account is activated")
    void loginShouldFailBeforeActivation() throws Exception {

      // Arrange
      registerNewUser();

      // Act
      ResultActions loginResult = login(NEW_USER_EMAIL, STRONG_PASSWORD);

      // Assert
      loginResult
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("invalid_grant"));
      assertFalse(userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow().isActive());
    }

    @Test
    @DisplayName("resend-activation should disable the previous token and issue a new one that activates the account")
    void resendActivationShouldDisablePreviousTokenAndIssueAWorkingOne() throws Exception {

      // Arrange
      User user = registerNewUser();
      String firstToken = singleOpenToken(user, TokenType.ACTIVATION).getToken();

      // Act
      ResultActions resendResult = mockMvc.perform(post(ACCOUNTS_URL + "/resend-activation")
          .content(asJson(new UserEmailRequest(NEW_USER_EMAIL)))
          .contentType(MediaType.APPLICATION_JSON));
      String secondToken = singleOpenToken(user, TokenType.ACTIVATION).getToken();
      ResultActions firstTokenResult = activate(firstToken);
      ResultActions secondTokenResult = activate(secondToken);

      // Assert
      resendResult.andExpect(status().isNoContent());
      assertFalse(firstToken.equals(secondToken));
      firstTokenResult
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
          .andExpect(jsonPath("$.message").value("Token is disabled"));
      secondTokenResult.andExpect(status().isNoContent());
      assertTrue(userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow().isActive());
    }
  }

  @Nested
  @DisplayName("Password Recovery and Reset")
  class PasswordRecoveryAndReset {

    @Test
    @DisplayName("reset-password with the recovery token should make the new password work and the old one fail")
    void recoveryThenResetShouldReplaceThePassword() throws Exception {

      // Arrange
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      requestRecovery(EXISTING_EMAIL).andExpect(status().isNoContent());
      String recoveryToken = singleOpenToken(user, TokenType.PASSWORD_RECOVERY).getToken();

      // Act
      ResultActions resetResult = resetPassword(recoveryToken, NEW_STRONG_PASSWORD);

      // Assert
      resetResult.andExpect(status().isNoContent());
      assertFalse(tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, NEW_STRONG_PASSWORD).isBlank());
      login(EXISTING_EMAIL, "123456")
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    @Test
    @DisplayName("requesting recovery twice should reject the first token in reset-password")
    void secondRecoveryRequestShouldInvalidateTheFirstToken() throws Exception {

      // Arrange
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      requestRecovery(EXISTING_EMAIL).andExpect(status().isNoContent());
      String firstToken = singleOpenToken(user, TokenType.PASSWORD_RECOVERY).getToken();
      requestRecovery(EXISTING_EMAIL).andExpect(status().isNoContent());
      String secondToken = singleOpenToken(user, TokenType.PASSWORD_RECOVERY).getToken();

      // Act
      ResultActions firstTokenResult = resetPassword(firstToken, NEW_STRONG_PASSWORD);
      ResultActions secondTokenResult = resetPassword(secondToken, NEW_STRONG_PASSWORD);

      // Assert
      assertFalse(firstToken.equals(secondToken));
      firstTokenResult
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
          .andExpect(jsonPath("$.message").value("Token is disabled"));
      secondTokenResult.andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("Invalid Tokens")
  class InvalidTokens {

    @Test
    @DisplayName("activate should return 404 when the token does not exist")
    void activateShouldReturnNotFoundWhenTokenDoesNotExist() throws Exception {

      // Act
      ResultActions result = activate("non-existing-token");

      // Assert
      result
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("Token not found"));
    }

    @Test
    @DisplayName("activate should return 400 when the token is disabled")
    void activateShouldReturnBadRequestWhenTokenIsDisabled() throws Exception {

      // Arrange
      Token token = saveToken(registerNewUser(), TokenType.ACTIVATION, Instant.now().plusSeconds(3600), true);

      // Act
      ResultActions result = activate(token.getToken());

      // Assert
      expectInvalidToken(result, "Token is disabled");
      assertFalse(userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow().isActive());
    }

    @Test
    @DisplayName("activate should return 400 when the token is expired")
    void activateShouldReturnBadRequestWhenTokenIsExpired() throws Exception {

      // Arrange
      Token token = saveToken(registerNewUser(), TokenType.ACTIVATION, Instant.now().minusSeconds(60), false);

      // Act
      ResultActions result = activate(token.getToken());

      // Assert
      expectInvalidToken(result, "Token expired");
      assertFalse(userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow().isActive());
    }

    @Test
    @DisplayName("activate should return 400 when a password recovery token is used")
    void activateShouldReturnBadRequestWhenTokenTypeIsWrong() throws Exception {

      // Arrange
      Token token = saveToken(registerNewUser(), TokenType.PASSWORD_RECOVERY, Instant.now().plusSeconds(3600), false);

      // Act
      ResultActions result = activate(token.getToken());

      // Assert
      expectInvalidToken(result, "Token type is invalid");
      assertFalse(userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow().isActive());
    }

    @Test
    @DisplayName("reset-password should return 404 when the token does not exist")
    void resetPasswordShouldReturnNotFoundWhenTokenDoesNotExist() throws Exception {

      // Act
      ResultActions result = resetPassword("non-existing-token", NEW_STRONG_PASSWORD);

      // Assert
      result
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("Token not found"));
    }

    @Test
    @DisplayName("reset-password should return 400 and keep the password when the token is disabled")
    void resetPasswordShouldReturnBadRequestWhenTokenIsDisabled() throws Exception {

      // Arrange
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      Token token = saveToken(user, TokenType.PASSWORD_RECOVERY, Instant.now().plusSeconds(600), true);

      // Act
      ResultActions result = resetPassword(token.getToken(), NEW_STRONG_PASSWORD);

      // Assert
      expectInvalidToken(result, "Token is disabled");
      assertPasswordUnchanged();
    }

    @Test
    @DisplayName("reset-password should return 400 and keep the password when the token is expired")
    void resetPasswordShouldReturnBadRequestWhenTokenIsExpired() throws Exception {

      // Arrange
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      Token token = saveToken(user, TokenType.PASSWORD_RECOVERY, Instant.now().minusSeconds(60), false);

      // Act
      ResultActions result = resetPassword(token.getToken(), NEW_STRONG_PASSWORD);

      // Assert
      expectInvalidToken(result, "Token expired");
      assertPasswordUnchanged();
    }

    @Test
    @DisplayName("reset-password should return 400 and keep the password when an activation token is used")
    void resetPasswordShouldReturnBadRequestWhenTokenTypeIsWrong() throws Exception {

      // Arrange
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      Token token = saveToken(user, TokenType.ACTIVATION, Instant.now().plusSeconds(3600), false);

      // Act
      ResultActions result = resetPassword(token.getToken(), NEW_STRONG_PASSWORD);

      // Assert
      expectInvalidToken(result, "Token type is invalid");
      assertPasswordUnchanged();
    }
  }

  @Nested
  @DisplayName("Authenticated User")
  class AuthenticatedUser {

    @Test
    @DisplayName("GET /accounts/me should return the user identified by the JWT")
    void getMeShouldReturnTheAuthenticatedUser() throws Exception {

      // Arrange
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      bearerToken = tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, "123456");

      // Act
      ResultActions result = mockMvc.perform(get(ACCOUNTS_URL + "/me")
          .with(bearerToken())
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(user.getId()))
          .andExpect(jsonPath("$.email").value(EXISTING_EMAIL))
          .andExpect(jsonPath("$.firstName").value(user.getFirstName()));
    }

    @Test
    @DisplayName("PUT /accounts/me should update and persist the authenticated user's profile")
    void putMeShouldUpdateTheAuthenticatedUser() throws Exception {

      // Arrange
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      bearerToken = tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, "123456");
      AuthenticatedUserUpdateRequest request = new AuthenticatedUserUpdateRequest("Mariana", "Souza", EXISTING_EMAIL);

      // Act
      ResultActions result = mockMvc.perform(put(ACCOUNTS_URL + "/me")
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(user.getId()))
          .andExpect(jsonPath("$.firstName").value("Mariana"))
          .andExpect(jsonPath("$.lastName").value("Souza"));
      User updated = userRepository.findById(user.getId()).orElseThrow();
      assertEquals("Mariana", updated.getFirstName());
      assertEquals("Souza", updated.getLastName());
    }

    @Test
    @DisplayName("PATCH /accounts/me/password should make the new password work and the old one fail")
    void patchPasswordShouldReplaceThePassword() throws Exception {

      // Arrange
      bearerToken = loginWithStrongPassword();
      PasswordUpdateRequest request = new PasswordUpdateRequest(STRONG_PASSWORD, NEW_STRONG_PASSWORD,
          NEW_STRONG_PASSWORD);

      // Act
      ResultActions result = patchPassword(request);

      // Assert
      result.andExpect(status().isNoContent());
      assertFalse(tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, NEW_STRONG_PASSWORD).isBlank());
      login(EXISTING_EMAIL, STRONG_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /accounts/me/password should return 422 when the current password is wrong")
    void patchPasswordShouldReturnUnprocessableEntityWhenCurrentPasswordIsWrong() throws Exception {

      // Arrange
      bearerToken = loginWithStrongPassword();
      PasswordUpdateRequest request = new PasswordUpdateRequest("Wrong#Pass9x", NEW_STRONG_PASSWORD,
          NEW_STRONG_PASSWORD);

      // Act
      ResultActions result = patchPassword(request);

      // Assert
      expectPasswordUpdateError(result, "Current password is incorrect.");
    }

    @Test
    @DisplayName("PATCH /accounts/me/password should return 422 when the confirmation does not match")
    void patchPasswordShouldReturnUnprocessableEntityWhenConfirmationDoesNotMatch() throws Exception {

      // Arrange
      bearerToken = loginWithStrongPassword();
      PasswordUpdateRequest request = new PasswordUpdateRequest(STRONG_PASSWORD, NEW_STRONG_PASSWORD,
          "Ok7&different");

      // Act
      ResultActions result = patchPassword(request);

      // Assert
      expectPasswordUpdateError(result, "New password and confirmation do not match.");
    }

    @Test
    @DisplayName("PATCH /accounts/me/password should return 422 when the new password equals the current one")
    void patchPasswordShouldReturnUnprocessableEntityWhenNewPasswordEqualsCurrent() throws Exception {

      // Arrange
      bearerToken = loginWithStrongPassword();
      PasswordUpdateRequest request = new PasswordUpdateRequest(STRONG_PASSWORD, STRONG_PASSWORD, STRONG_PASSWORD);

      // Act
      ResultActions result = patchPassword(request);

      // Assert
      expectPasswordUpdateError(result, "New password must be different from the current password.");
    }

    private String loginWithStrongPassword() throws Exception {
      User user = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();
      user.setPassword(passwordEncoder.encode(STRONG_PASSWORD));
      userRepository.saveAndFlush(user);
      return tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, STRONG_PASSWORD);
    }

    private ResultActions patchPassword(PasswordUpdateRequest request) throws Exception {
      return mockMvc.perform(patch(ACCOUNTS_URL + "/me/password")
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));
    }

    private void expectPasswordUpdateError(ResultActions result, String message) throws Exception {
      result
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("PASSWORD_UPDATE_ERROR"))
          .andExpect(jsonPath("$.message").value(message));
      login(EXISTING_EMAIL, STRONG_PASSWORD).andExpect(status().isOk());
    }
  }

  private User registerNewUser() throws Exception {
    UserRegisterRequest request = new UserRegisterRequest("Joana", "Pereira", NEW_USER_EMAIL, STRONG_PASSWORD);
    mockMvc.perform(post(ACCOUNTS_URL + "/register")
        .content(asJson(request))
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
    return userRepository.findByEmail(NEW_USER_EMAIL).orElseThrow();
  }

  private Token singleOpenToken(User user, TokenType type) {
    List<Token> tokens = tokenRepository.findByUserAndTypeAndDisabledFalse(user, type);
    assertEquals(1, tokens.size(), "Esperado exatamente um token em aberto do tipo " + type);
    return tokens.get(0);
  }

  private Token saveToken(User user, TokenType type, Instant expireDate, boolean disabled) {
    Token token = new Token("it-token-" + type + "-" + expireDate.toEpochMilli(), user, expireDate, type);
    if (disabled) {
      token.disable();
    }
    return tokenRepository.saveAndFlush(token);
  }

  private ResultActions activate(String token) throws Exception {
    return mockMvc.perform(get(ACCOUNTS_URL + "/activate")
        .param("token", token)
        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
        .accept(MediaType.APPLICATION_JSON));
  }

  private ResultActions requestRecovery(String email) throws Exception {
    return mockMvc.perform(post(ACCOUNTS_URL + "/password-recovery")
        .content(asJson(new UserEmailRequest(email)))
        .contentType(MediaType.APPLICATION_JSON));
  }

  private ResultActions resetPassword(String token, String password) throws Exception {
    return mockMvc.perform(post(ACCOUNTS_URL + "/reset-password")
        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
        .content(asJson(new PasswordResetRequest(token, password)))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  private ResultActions login(String username, String password) throws Exception {
    return mockMvc.perform(post("/oauth2/token")
        .param("grant_type", "password")
        .param("username", username)
        .param("password", password)
        .with(httpBasic(clientId, clientSecret))
        .accept(MediaType.APPLICATION_JSON));
  }

  private void expectInvalidToken(ResultActions result, String message) throws Exception {
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
        .andExpect(jsonPath("$.message").value(message));
  }

  private void assertPasswordUnchanged() throws Exception {
    login(EXISTING_EMAIL, "123456").andExpect(status().isOk());
  }
}
