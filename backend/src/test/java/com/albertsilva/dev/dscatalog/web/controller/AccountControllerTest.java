package com.albertsilva.dev.dscatalog.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

import com.albertsilva.dev.dscatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.PasswordResetRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.PasswordUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserEmailRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.security.oauth2.resource.config.ResourceServerConfig;
import com.albertsilva.dev.dscatalog.service.AccountService;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.dscatalog.service.exception.PasswordUpdateException;
import com.albertsilva.dev.dscatalog.web.exception.handler.ControllerExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AccountController.class)
@Import({ ControllerExceptionHandler.class, ResourceServerConfig.class })
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
  private com.albertsilva.dev.dscatalog.repository.UserRepository userRepository;

  @MockitoBean
  private com.albertsilva.dev.dscatalog.security.auth.AuthenticatedUserService authenticatedUserService;

  private UserResponse userResponse;

  @BeforeEach
  void setUp() {
    userResponse = UserFactory.createUserResponse();
  }

  @Nested
  @DisplayName("Registration Operations")
  class RegistrationOperations {

    @Test
    @DisplayName("POST /accounts/register should create account and return 201")
    void registerShouldReturn201WhenRequestIsValid() throws Exception {
      UserRegisterRequest request = new UserRegisterRequest("Pedro", "Santos", "pedro@gmail.com", "StrongPass@2026");

      when(userRepository.existsByEmailIgnoreCase("pedro@gmail.com")).thenReturn(false);
      when(accountService.register(any(UserRegisterRequest.class))).thenReturn(userResponse);

      ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/register")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));

      resultActions
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()))
          .andExpect(jsonPath("$.email").value(userResponse.email()));

      verify(accountService).register(any(UserRegisterRequest.class));
    }

    @Test
    @DisplayName("POST /accounts/register should return 422 when request is invalid")
    void registerShouldReturn422WhenRequestIsInvalid() throws Exception {
      String invalidJson = "{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"invalid\",\"password\":\"123\"}";

      ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/register")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(invalidJson));

      resultActions
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.fieldErrors").isArray())
          .andExpect(jsonPath("$.status").value(422));
    }
  }

  @Nested
  @DisplayName("Activation Operations")
  class ActivationOperations {

    @Test
    @DisplayName("GET /accounts/activate should return 204 and call service when token is valid")
    void activateAccountShouldReturn204WhenTokenIsValid() throws Exception {
      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/activate")
          .param("token", "valid-token")
          .accept(MediaType.APPLICATION_JSON));

      resultActions.andExpect(status().isNoContent());

      verify(accountService).confirmEmail("valid-token");
    }

    @Test
    @DisplayName("GET /accounts/activate should return 400 when token is invalid")
    void activateAccountShouldReturn400WhenTokenIsInvalid() throws Exception {
      doThrow(new InvalidTokenException("error.token.invalid")).when(accountService).confirmEmail("bad-token");

      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/activate")
          .param("token", "bad-token")
          .accept(MediaType.APPLICATION_JSON));

      resultActions
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400));
    }
  }

  @Nested
  @DisplayName("Resend Activation Operations")
  class ResendActivationOperations {

    @Test
    @DisplayName("POST /accounts/resend-activation should return 204 and call service")
    void resendActivationEmailShouldReturn204WhenRequestIsValid() throws Exception {
      UserEmailRequest request = new UserEmailRequest("pedro@gmail.com");

      doNothing().when(accountService).resendActivationEmail(request.email());

      ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/resend-activation")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));

      resultActions.andExpect(status().isNoContent());

      verify(accountService).resendActivationEmail("pedro@gmail.com");
    }

    @Test
    @DisplayName("POST /accounts/resend-activation should return 422 when email is invalid")
    void resendActivationEmailShouldReturn422WhenEmailIsInvalid() throws Exception {
      String invalidJson = "{\"email\":\"invalid-email\"}";

      ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/resend-activation")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(invalidJson));

      resultActions
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.fieldErrors").isArray())
          .andExpect(jsonPath("$.status").value(422));
    }
  }

  @Nested
  @DisplayName("Password Recovery Operations")
  class PasswordRecoveryOperations {

    @Test
    @DisplayName("POST /accounts/password-recovery should return 204 and call service")
    void requestPasswordRecoveryShouldReturn204WhenRequestIsValid() throws Exception {
      UserEmailRequest request = new UserEmailRequest("pedro@gmail.com");

      doNothing().when(accountService).requestPasswordRecovery(request.email());

      ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/password-recovery")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));

      resultActions.andExpect(status().isNoContent());

      verify(accountService).requestPasswordRecovery("pedro@gmail.com");
    }
  }

  @Nested
  @DisplayName("Password Reset Operations")
  class PasswordResetOperations {

    @Test
    @DisplayName("POST /accounts/reset-password should return 204 and call service")
    void resetPasswordShouldReturn204WhenRequestIsValid() throws Exception {
      PasswordResetRequest request = new PasswordResetRequest("valid-token", "StrongPass@2026");

      when(userRepository.existsByEmailIgnoreCase(any(String.class))).thenReturn(false);
      doNothing().when(accountService).resetPassword(request.token(), request.password());

      ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/reset-password")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));

      resultActions.andExpect(status().isNoContent());

      verify(accountService).resetPassword("valid-token", "StrongPass@2026");
    }

    @Test
    @DisplayName("POST /accounts/reset-password should return 422 when request is invalid")
    void resetPasswordShouldReturn422WhenRequestIsInvalid() throws Exception {
      String invalidJson = "{\"token\":\"\",\"password\":\"short\"}";

      ResultActions resultActions = mockMvc.perform(post(BASE_URL + "/reset-password")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(invalidJson));

      resultActions
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.fieldErrors").isArray())
          .andExpect(jsonPath("$.status").value(422));
    }
  }

  @Nested
  @DisplayName("Authenticated User Operations")
  class AuthenticatedUserOperations {

    @Test
    @WithMockUser(username = "maria@gmail.com")
    @DisplayName("GET /accounts/me should return the authenticated user with 200")
    void getAuthenticatedUserShouldReturn200AndUserResponse() throws Exception {
      when(accountService.getAuthenticatedUser()).thenReturn(userResponse);

      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/me")
          .accept(MediaType.APPLICATION_JSON));

      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()))
          .andExpect(jsonPath("$.email").value(userResponse.email()));

      verify(accountService).getAuthenticatedUser();
    }

    @Test
    @WithMockUser(username = "maria@gmail.com")
    @DisplayName("PUT /accounts/me should update the authenticated user and return 200")
    void updateAuthenticatedUserShouldReturn200WhenRequestIsValid() throws Exception {
      AuthenticatedUserUpdateRequest request = new AuthenticatedUserUpdateRequest("Maria", "Silva", "maria@gmail.com");
      com.albertsilva.dev.dscatalog.domain.user.User authenticatedUser = com.albertsilva.dev.dscatalog.factory.UserFactory
          .createUser();
      authenticatedUser.setId(1L);
      authenticatedUser.setEmail("maria@gmail.com");

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(authenticatedUser);
      when(userRepository.existsByEmailIgnoreCaseAndIdNot("maria@gmail.com", 1L)).thenReturn(false);
      when(accountService.updateAuthenticatedUser(any(AuthenticatedUserUpdateRequest.class))).thenReturn(userResponse);

      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/me")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));

      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()));

      verify(accountService).updateAuthenticatedUser(any(AuthenticatedUserUpdateRequest.class));
    }

    @Test
    @WithMockUser(username = "maria@gmail.com")
    @DisplayName("PATCH /accounts/me/password should return 204 and call service")
    void updatePasswordShouldReturn204WhenRequestIsValid() throws Exception {
      PasswordUpdateRequest request = new PasswordUpdateRequest("OldPassword1!", "NewPassword2@", "NewPassword2@");

      doNothing().when(accountService).updatePassword(any(PasswordUpdateRequest.class));

      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/me/password")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));

      resultActions.andExpect(status().isNoContent());

      verify(accountService).updatePassword(any(PasswordUpdateRequest.class));
    }

    @Test
    @WithMockUser(username = "maria@gmail.com")
    @DisplayName("PATCH /accounts/me/password should return 422 when service reports password validation error")
    void updatePasswordShouldReturn422WhenServiceReportsValidationError() throws Exception {
      PasswordUpdateRequest request = new PasswordUpdateRequest("OldPassword1!", "NewPassword2@", "OtherPassword2@");

      doThrow(new PasswordUpdateException("error.account.password.confirmationMismatch"))
          .when(accountService).updatePassword(any(PasswordUpdateRequest.class));

      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/me/password")
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));

      resultActions
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.status").value(422));
    }
  }
}
