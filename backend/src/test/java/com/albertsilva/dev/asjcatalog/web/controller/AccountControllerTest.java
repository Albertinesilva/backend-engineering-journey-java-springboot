package com.albertsilva.dev.asjcatalog.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.ACTIVATION_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.PASSWORD_RECOVERY_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NEW_PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.WRONG_PASSWORD;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectValidationError;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.PasswordResetRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.PasswordUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserEmailRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;
import com.albertsilva.dev.asjcatalog.security.auth.AuthenticatedUserService;
import com.albertsilva.dev.asjcatalog.security.oauth2.resource.config.ResourceServerConfig;
import com.albertsilva.dev.asjcatalog.service.AccountService;
import com.albertsilva.dev.asjcatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.asjcatalog.service.exception.PasswordUpdateException;
import com.albertsilva.dev.asjcatalog.utils.MxRecordLookupTestConfig;
import com.albertsilva.dev.asjcatalog.web.exception.enums.ApiErrorCode;
import com.albertsilva.dev.asjcatalog.web.exception.handler.ControllerExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Testes de {@code AccountController}.
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>{@code POST /accounts/deactivate}: o service não está implementado e
 * lança {@code UnsupportedOperationException}, que responde 500 (#9); sem
 * token, o endpoint responde 403 em vez de 401 (#10).</li>
 * </ul>
 */
@WebMvcTest(AccountController.class)
@Import({ ControllerExceptionHandler.class, ResourceServerConfig.class, MxRecordLookupTestConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.h2.console.enabled=false" })
@DisplayName("Tests for AccountController")
class AccountControllerTest {

  private static final String BASE_URL = "/api/v1/accounts";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AccountService accountService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private AuthenticatedUserService authenticatedUserService;

  private UserResponse userResponse;

  @BeforeEach
  void setUp() {
    userResponse = UserFactory.createUserResponse();
  }

  private ResultActions postJson(String path, Object body) throws Exception {
    return mockMvc.perform(post(BASE_URL + path)
        .content(body instanceof String json ? json : objectMapper.writeValueAsString(body))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON));
  }

  @Nested
  @DisplayName("POST /accounts/register")
  class RegisterOperations {

    @Test
    @DisplayName("POST /accounts/register should return 201 with created user when request is valid")
    void registerShouldReturnCreatedWhenRequestIsValid() throws Exception {

      // Arrange
      UserRegisterRequest request = UserFactory.createUserRegisterRequest();

      when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
      when(accountService.register(request)).thenReturn(userResponse);

      // Act
      ResultActions result = postJson("/register", request);

      // Assert
      result
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()))
          .andExpect(jsonPath("$.email").value(userResponse.email()));
      verify(accountService).register(request);
    }

    @Test
    @DisplayName("POST /accounts/register should return 422 when request is invalid")
    void registerShouldReturnUnprocessableEntityWhenRequestIsInvalid() throws Exception {

      // Arrange
      String invalidJson = "{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"invalid\",\"password\":\"123\"}";

      // Act
      ResultActions result = postJson("/register", invalidJson);

      // Assert
      expectValidationError(result, BASE_URL + "/register", "firstName", "lastName", "email", "password");
      verify(accountService, never()).register(any(UserRegisterRequest.class));
    }
  }

  /**
   * Testes de {@code GET /accounts/activate}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Sem o parâmetro {@code token}: a exceção do Spring cai no handler
   * genérico e responde 500 em vez de 400 (#36).</li>
   * </ul>
   */
  @Nested
  @DisplayName("GET /accounts/activate")
  class ActivateAccountOperations {

    @Test
    @DisplayName("GET /accounts/activate should return 204 when token is valid")
    void activateAccountShouldReturnNoContentWhenTokenIsValid() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/activate").param("token", ACTIVATION_TOKEN));

      // Assert
      result.andExpect(status().isNoContent());
      verify(accountService).confirmEmail(ACTIVATION_TOKEN);
    }

    @Test
    @DisplayName("GET /accounts/activate should return 400 when token is invalid")
    void activateAccountShouldReturnBadRequestWhenTokenIsInvalid() throws Exception {

      // Arrange
      doThrow(new InvalidTokenException("error.token.expired")).when(accountService).confirmEmail(ACTIVATION_TOKEN);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/activate").param("token", ACTIVATION_TOKEN)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value(ApiErrorCode.INVALID_TOKEN.name()))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/activate"));
    }
  }

  @Nested
  @DisplayName("POST /accounts/resend-activation")
  class ResendActivationEmailOperations {

    @Test
    @DisplayName("POST /accounts/resend-activation should return 204 when email is valid")
    void resendActivationEmailShouldReturnNoContentWhenEmailIsValid() throws Exception {

      // Arrange
      UserEmailRequest request = new UserEmailRequest(EXISTING_EMAIL);

      // Act
      ResultActions result = postJson("/resend-activation", request);

      // Assert
      result.andExpect(status().isNoContent());
      verify(accountService).resendActivationEmail(EXISTING_EMAIL);
    }

    @Test
    @DisplayName("POST /accounts/resend-activation should return 422 when email is invalid")
    void resendActivationEmailShouldReturnUnprocessableEntityWhenEmailIsInvalid() throws Exception {

      // Act
      ResultActions result = postJson("/resend-activation", "{\"email\":\"invalid-email\"}");

      // Assert
      expectValidationError(result, BASE_URL + "/resend-activation", "email");
    }
  }

  @Nested
  @DisplayName("POST /accounts/password-recovery")
  class RequestPasswordRecoveryOperations {

    @Test
    @DisplayName("POST /accounts/password-recovery should return 204 when email is valid")
    void requestPasswordRecoveryShouldReturnNoContentWhenEmailIsValid() throws Exception {

      // Arrange
      UserEmailRequest request = new UserEmailRequest(EXISTING_EMAIL);

      // Act
      ResultActions result = postJson("/password-recovery", request);

      // Assert
      result.andExpect(status().isNoContent());
      verify(accountService).requestPasswordRecovery(EXISTING_EMAIL);
    }

    @Test
    @DisplayName("POST /accounts/password-recovery should return 422 when email is invalid")
    void requestPasswordRecoveryShouldReturnUnprocessableEntityWhenEmailIsInvalid() throws Exception {

      // Act
      ResultActions result = postJson("/password-recovery", "{\"email\":\"invalid-email\"}");

      // Assert
      expectValidationError(result, BASE_URL + "/password-recovery", "email");
      verify(accountService, never()).requestPasswordRecovery(any());
    }
  }

  @Nested
  @DisplayName("POST /accounts/reset-password")
  class ResetPasswordOperations {

    @Test
    @DisplayName("POST /accounts/reset-password should return 204 when request is valid")
    void resetPasswordShouldReturnNoContentWhenRequestIsValid() throws Exception {

      // Arrange
      PasswordResetRequest request = new PasswordResetRequest(PASSWORD_RECOVERY_TOKEN, NEW_PASSWORD);

      // Act
      ResultActions result = postJson("/reset-password", request);

      // Assert
      result.andExpect(status().isNoContent());
      verify(accountService).resetPassword(PASSWORD_RECOVERY_TOKEN, NEW_PASSWORD);
    }

    @Test
    @DisplayName("POST /accounts/reset-password should return 422 when request is invalid")
    void resetPasswordShouldReturnUnprocessableEntityWhenRequestIsInvalid() throws Exception {

      // Act
      ResultActions result = postJson("/reset-password", "{\"token\":\"\",\"password\":\"short\"}");

      // Assert
      expectValidationError(result, BASE_URL + "/reset-password", "token", "password");
    }
  }

  /**
   * Testes de {@code GET /accounts/me}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Sem token: a rota é liberada pela regra de URL e negada só pelo
   * {@code @PreAuthorize}, então responde 403 em vez de 401 (#10).</li>
   * </ul>
   */
  @Nested
  @DisplayName("GET /accounts/me")
  class GetAuthenticatedUserOperations {

    @Test
    @WithMockUser(username = EXISTING_EMAIL)
    @DisplayName("GET /accounts/me should return authenticated user when user is authenticated")
    void getAuthenticatedUserShouldReturnUserWhenUserIsAuthenticated() throws Exception {

      // Arrange
      when(accountService.getAuthenticatedUser()).thenReturn(userResponse);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/me").accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()))
          .andExpect(jsonPath("$.email").value(userResponse.email()));
    }
  }

  @Nested
  @DisplayName("PUT /accounts/me")
  class UpdateAuthenticatedUserOperations {

    // #12: o e-mail novo é gravado sem reconfirmação. O teste confere a resposta como
    // ela é hoje e é mantido sem reforço até a decisão da issue.
    @Test
    @WithMockUser(username = EXISTING_EMAIL)
    @DisplayName("PUT /accounts/me should return updated user when request is valid")
    void updateAuthenticatedUserShouldReturnUpdatedUserWhenRequestIsValid() throws Exception {

      // Arrange
      AuthenticatedUserUpdateRequest request = UserFactory.createAuthenticatedUserUpdateRequest();
      User authenticatedUser = UserFactory.createUser();
      authenticatedUser.setId(EXISTING_ID);

      // O @UniqueEmailForAuthenticatedUser ignora o e-mail do próprio usuário autenticado.
      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(authenticatedUser);
      when(userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), EXISTING_ID)).thenReturn(false);
      when(accountService.updateAuthenticatedUser(request)).thenReturn(userResponse);

      // Act
      ResultActions result = mockMvc.perform(put(BASE_URL + "/me")
          .content(objectMapper.writeValueAsString(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()));
      verify(accountService).updateAuthenticatedUser(request);
    }
  }

  @Nested
  @DisplayName("PATCH /accounts/me/password")
  class UpdatePasswordOperations {

    @Test
    @WithMockUser(username = EXISTING_EMAIL)
    @DisplayName("PATCH /accounts/me/password should return 204 when request is valid")
    void updatePasswordShouldReturnNoContentWhenRequestIsValid() throws Exception {

      // Arrange
      PasswordUpdateRequest request = new PasswordUpdateRequest(PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/me/password")
          .content(objectMapper.writeValueAsString(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isNoContent());
      verify(accountService).updatePassword(request);
    }

    @Test
    @WithMockUser(username = EXISTING_EMAIL)
    @DisplayName("PATCH /accounts/me/password should return 422 when service reports password update error")
    void updatePasswordShouldReturnUnprocessableEntityWhenServiceReportsPasswordUpdateError() throws Exception {

      // Arrange
      PasswordUpdateRequest request = new PasswordUpdateRequest(PASSWORD, NEW_PASSWORD, WRONG_PASSWORD);

      doThrow(new PasswordUpdateException("error.account.password.confirmationMismatch"))
          .when(accountService).updatePassword(request);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/me/password")
          .content(objectMapper.writeValueAsString(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.status").value(422))
          .andExpect(jsonPath("$.code").value(ApiErrorCode.PASSWORD_UPDATE_ERROR.name()))
          .andExpect(jsonPath("$.path").value(BASE_URL + "/me/password"));
    }
  }
}
