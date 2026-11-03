package com.albertsilva.dev.asjcatalog.service;

import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.ACTIVATION_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.NON_EXISTING_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.PASSWORD_RECOVERY_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ENCODED_NEW_PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ENCODED_PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NEW_PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.WRONG_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.albertsilva.dev.asjcatalog.domain.recovery.Token;
import com.albertsilva.dev.asjcatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.asjcatalog.domain.user.Role;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.PasswordUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.TokenFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.mapper.user.UserMapper;
import com.albertsilva.dev.asjcatalog.repository.RoleRepository;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;
import com.albertsilva.dev.asjcatalog.security.auth.AuthenticatedUserService;
import com.albertsilva.dev.asjcatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.asjcatalog.service.exception.PasswordUpdateException;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

import jakarta.mail.MessagingException;

/**
 * Testes de {@code AccountService}.
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>{@code deactivateAccount}: não implementado, sempre lança
 * {@code UnsupportedOperationException} (#9).</li>
 * </ul>
 */
@DisplayName("Tests for AccountService")
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @InjectMocks
  private AccountService service;

  @Mock
  private UserRepository userRepository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private TokenService tokenService;

  @Mock
  private EmailService emailService;

  @Mock
  private AuthenticatedUserService authenticatedUserService;

  private static User inactiveUser() {
    User user = UserFactory.createUser();
    user.deactivate();
    return user;
  }

  private static User userWithEncodedPassword() {
    User user = UserFactory.createUser();
    user.setPassword(ENCODED_PASSWORD);
    return user;
  }

  /**
   * Testes de {@code AccountService.register}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Role {@code ROLE_OPERATOR} ausente no banco ({@code IllegalStateException})
   * e a própria escolha dessa role para o cadastro público: a role da conta nova
   * ainda não foi decidida (#13).</li>
   * </ul>
   */
  @Nested
  @DisplayName("Register Operations")
  class RegisterOperations {

    @Test
    @DisplayName("register should create inactive user and send activation email")
    void registerShouldCreateInactiveUserAndSendActivationEmail() throws MessagingException {

      // Arrange
      UserRegisterRequest request = UserFactory.createUserRegisterRequest();
      Role role = RoleFactory.createOperatorRole();
      User user = UserFactory.createUser();
      Token activationToken = TokenFactory.createActivationToken(user);
      UserResponse expectedResponse = UserFactory.createUserResponse();

      when(roleRepository.findByAuthority(RoleFactory.OPERATOR_AUTHORITY)).thenReturn(Optional.of(role));
      when(userMapper.toEntity(request, Set.of(role))).thenReturn(user);
      when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
      when(userRepository.save(user)).thenReturn(user);
      when(tokenService.createActivationToken(user)).thenReturn(activationToken);
      when(userMapper.toResponse(user)).thenReturn(expectedResponse);

      // Act
      UserResponse result = service.register(request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(user.isActive()).isFalse();
      assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
      verify(userRepository).save(user);
      verify(emailService).sendActivationEmailAsync(user.getFirstName(), user.getEmail(), ACTIVATION_TOKEN);
    }
  }

  @Nested
  @DisplayName("ConfirmEmail Operations")
  class ConfirmEmailOperations {

    @Test
    @DisplayName("confirmEmail should activate user and disable token when token is valid")
    void confirmEmailShouldActivateUserAndDisableTokenWhenTokenIsValid() {

      // Arrange
      User user = inactiveUser();
      Token token = TokenFactory.createActivationToken(user);

      when(tokenService.findAndValidateToken(ACTIVATION_TOKEN, TokenType.ACTIVATION)).thenReturn(token);

      // Act
      service.confirmEmail(ACTIVATION_TOKEN);

      // Assert
      assertThat(user.isActive()).isTrue();
      assertThat(token.getDisabled()).isTrue();
    }

    @Test
    @DisplayName("confirmEmail should propagate InvalidTokenException when token is invalid")
    void confirmEmailShouldPropagateInvalidTokenExceptionWhenTokenIsInvalid() {

      // Arrange
      when(tokenService.findAndValidateToken(ACTIVATION_TOKEN, TokenType.ACTIVATION))
          .thenThrow(new InvalidTokenException("error.token.expired"));

      // Act & Assert
      assertThatThrownBy(() -> service.confirmEmail(ACTIVATION_TOKEN))
          .isInstanceOf(InvalidTokenException.class)
          .hasMessage("error.token.expired");
    }

    @Test
    @DisplayName("confirmEmail should propagate ResourceNotFoundException when token does not exist")
    void confirmEmailShouldPropagateResourceNotFoundExceptionWhenTokenDoesNotExist() {

      // Arrange
      when(tokenService.findAndValidateToken(NON_EXISTING_TOKEN, TokenType.ACTIVATION))
          .thenThrow(new ResourceNotFoundException("error.token.notFound"));

      // Act & Assert
      assertThatThrownBy(() -> service.confirmEmail(NON_EXISTING_TOKEN))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.token.notFound");
    }
  }

  /**
   * Testes de {@code AccountService.resendActivationEmail}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>E-mail com maiúsculas diferentes das gravadas: a busca diferencia
   * maiúsculas e não encontra o usuário (#11).</li>
   * </ul>
   */
  @Nested
  @DisplayName("ResendActivationEmail Operations")
  class ResendActivationEmailOperations {

    @Test
    @DisplayName("resendActivationEmail should do nothing when email does not exist")
    void resendActivationEmailShouldDoNothingWhenEmailDoesNotExist() throws MessagingException {

      // Arrange
      when(userRepository.findByEmail(NON_EXISTING_EMAIL)).thenReturn(Optional.empty());

      // Act
      service.resendActivationEmail(NON_EXISTING_EMAIL);

      // Assert
      verify(tokenService, never()).disableAllActivationTokens(any(User.class));
      verify(tokenService, never()).createActivationToken(any(User.class));
      verify(emailService, never()).sendActivationEmailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("resendActivationEmail should do nothing when user is already active")
    void resendActivationEmailShouldDoNothingWhenUserIsAlreadyActive() throws MessagingException {

      // Arrange
      User user = UserFactory.createUser();

      when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

      // Act
      service.resendActivationEmail(user.getEmail());

      // Assert
      verify(tokenService, never()).disableAllActivationTokens(any(User.class));
      verify(tokenService, never()).createActivationToken(any(User.class));
      verify(emailService, never()).sendActivationEmailAsync(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("resendActivationEmail should disable previous tokens, create a new one and send it when user is inactive")
    void resendActivationEmailShouldDisablePreviousTokensThenCreateAndSendNewTokenWhenUserIsInactive()
        throws MessagingException {

      // Arrange
      User user = inactiveUser();
      Token token = TokenFactory.createActivationToken(user);

      when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
      when(tokenService.createActivationToken(user)).thenReturn(token);

      // Act
      service.resendActivationEmail(user.getEmail());

      // Assert
      InOrder inOrder = inOrder(tokenService, emailService);
      inOrder.verify(tokenService).disableAllActivationTokens(user);
      inOrder.verify(tokenService).createActivationToken(user);
      inOrder.verify(emailService).sendActivationEmailAsync(user.getFirstName(), user.getEmail(), ACTIVATION_TOKEN);
    }
  }

  /**
   * Testes de {@code AccountService.requestPasswordRecovery}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>E-mail com maiúsculas diferentes das gravadas: a busca diferencia
   * maiúsculas e não encontra o usuário (#11).</li>
   * </ul>
   */
  @Nested
  @DisplayName("RequestPasswordRecovery Operations")
  class RequestPasswordRecoveryOperations {

    @Test
    @DisplayName("requestPasswordRecovery should disable previous tokens, create a new one and send it when user exists")
    void requestPasswordRecoveryShouldDisablePreviousTokensThenCreateAndSendNewTokenWhenUserExists() {

      // Arrange
      User user = UserFactory.createUser();
      Token token = TokenFactory.createPasswordRecoveryToken(user);

      when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
      when(tokenService.createPasswordRecoveryToken(user)).thenReturn(token);

      // Act
      service.requestPasswordRecovery(user.getEmail());

      // Assert
      InOrder inOrder = inOrder(tokenService, emailService);
      inOrder.verify(tokenService).disableAllPasswordRecoveryTokens(user);
      inOrder.verify(tokenService).createPasswordRecoveryToken(user);
      inOrder.verify(emailService).sendPasswordRecoveryEmailAsync(user, PASSWORD_RECOVERY_TOKEN);
    }

    @Test
    @DisplayName("requestPasswordRecovery should do nothing when user does not exist")
    void requestPasswordRecoveryShouldDoNothingWhenUserDoesNotExist() {

      // Arrange
      when(userRepository.findByEmail(NON_EXISTING_EMAIL)).thenReturn(Optional.empty());

      // Act
      service.requestPasswordRecovery(NON_EXISTING_EMAIL);

      // Assert
      verify(tokenService, never()).disableAllPasswordRecoveryTokens(any(User.class));
      verify(tokenService, never()).createPasswordRecoveryToken(any(User.class));
      verify(emailService, never()).sendPasswordRecoveryEmailAsync(any(User.class), anyString());
    }
  }

  @Nested
  @DisplayName("ResetPassword Operations")
  class ResetPasswordOperations {

    @Test
    @DisplayName("resetPassword should update password and disable token when token is valid")
    void resetPasswordShouldUpdatePasswordAndDisableTokenWhenTokenIsValid() {

      // Arrange
      User user = userWithEncodedPassword();
      Token token = TokenFactory.createPasswordRecoveryToken(user);

      when(tokenService.findAndValidateToken(PASSWORD_RECOVERY_TOKEN, TokenType.PASSWORD_RECOVERY)).thenReturn(token);
      when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);
      when(userRepository.save(user)).thenReturn(user);

      // Act
      service.resetPassword(PASSWORD_RECOVERY_TOKEN, NEW_PASSWORD);

      // Assert
      assertThat(user.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
      assertThat(token.getDisabled()).isTrue();
      verify(userRepository).save(user);
    }
  }

  @Nested
  @DisplayName("UpdateAuthenticatedUser Operations")
  class UpdateAuthenticatedUserOperations {

    // #12: o e-mail novo é gravado sem reconfirmação. O teste confere a troca como
    // ela é feita hoje e é mantido sem reforço até a decisão da issue.
    @Test
    @DisplayName("updateAuthenticatedUser should update profile and return response")
    void updateAuthenticatedUserShouldUpdateProfileAndReturnResponse() {

      // Arrange
      AuthenticatedUserUpdateRequest request = UserFactory.createAuthenticatedUserUpdateRequest();
      User user = UserFactory.createUser();
      UserResponse expectedResponse = UserFactory.createUserResponse();

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(userRepository.save(user)).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(expectedResponse);

      // Act
      UserResponse result = service.updateAuthenticatedUser(request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(user.getFirstName()).isEqualTo(request.firstName());
      assertThat(user.getLastName()).isEqualTo(request.lastName());
      assertThat(user.getEmail()).isEqualTo(request.email());
      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateAuthenticatedUser should trim first and last name when they have surrounding spaces")
    void updateAuthenticatedUserShouldTrimFirstAndLastNameWhenTheyHaveSurroundingSpaces() {

      // Arrange
      AuthenticatedUserUpdateRequest base = UserFactory.createAuthenticatedUserUpdateRequest();
      User user = UserFactory.createUser();
      AuthenticatedUserUpdateRequest request = new AuthenticatedUserUpdateRequest("  " + base.firstName() + "  ",
          "  " + base.lastName() + "  ", user.getEmail());

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(userRepository.save(user)).thenReturn(user);

      // Act
      service.updateAuthenticatedUser(request);

      // Assert
      assertThat(user.getFirstName()).isEqualTo(base.firstName());
      assertThat(user.getLastName()).isEqualTo(base.lastName());
    }
  }

  @Nested
  @DisplayName("UpdatePassword Operations")
  class UpdatePasswordOperations {

    @Test
    @DisplayName("updatePassword should encode new password when current password matches and new password differs")
    void updatePasswordShouldEncodeNewPasswordWhenCurrentPasswordMatchesAndNewPasswordDiffers() {

      // Arrange
      User user = userWithEncodedPassword();
      PasswordUpdateRequest request = new PasswordUpdateRequest(PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
      when(passwordEncoder.matches(NEW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);
      when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_NEW_PASSWORD);

      // Act
      service.updatePassword(request);

      // Assert
      assertThat(user.getPassword()).isEqualTo(ENCODED_NEW_PASSWORD);
    }

    @Test
    @DisplayName("updatePassword should throw PasswordUpdateException when confirmation does not match")
    void updatePasswordShouldThrowPasswordUpdateExceptionWhenConfirmationDoesNotMatch() {

      // Arrange
      PasswordUpdateRequest request = new PasswordUpdateRequest(PASSWORD, NEW_PASSWORD, WRONG_PASSWORD);

      // Act & Assert
      assertThatThrownBy(() -> service.updatePassword(request))
          .isInstanceOf(PasswordUpdateException.class)
          .hasMessage("error.account.password.confirmationMismatch");

      verify(authenticatedUserService, never()).getAuthenticatedUser();
    }

    @Test
    @DisplayName("updatePassword should throw PasswordUpdateException when current password is wrong")
    void updatePasswordShouldThrowPasswordUpdateExceptionWhenCurrentPasswordIsWrong() {

      // Arrange
      User user = userWithEncodedPassword();
      PasswordUpdateRequest request = new PasswordUpdateRequest(WRONG_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(passwordEncoder.matches(WRONG_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

      // Act & Assert
      assertThatThrownBy(() -> service.updatePassword(request))
          .isInstanceOf(PasswordUpdateException.class)
          .hasMessage("error.account.password.currentInvalid");

      verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("updatePassword should throw PasswordUpdateException when new password equals current")
    void updatePasswordShouldThrowPasswordUpdateExceptionWhenNewPasswordEqualsCurrent() {

      // Arrange
      User user = userWithEncodedPassword();
      PasswordUpdateRequest request = new PasswordUpdateRequest(PASSWORD, PASSWORD, PASSWORD);

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

      // Act & Assert
      assertThatThrownBy(() -> service.updatePassword(request))
          .isInstanceOf(PasswordUpdateException.class)
          .hasMessage("error.account.password.sameAsCurrent");

      verify(passwordEncoder, never()).encode(anyString());
    }
  }

  @Nested
  @DisplayName("GetAuthenticatedUser Operations")
  class GetAuthenticatedUserOperations {

    @Test
    @DisplayName("getAuthenticatedUser should return mapped user response")
    void getAuthenticatedUserShouldReturnMappedUserResponse() {

      // Arrange
      User user = UserFactory.createUser();
      UserResponse expectedResponse = UserFactory.createUserResponse();

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(expectedResponse);

      // Act
      UserResponse result = service.getAuthenticatedUser();

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
    }
  }
}
