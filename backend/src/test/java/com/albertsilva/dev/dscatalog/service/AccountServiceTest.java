package com.albertsilva.dev.dscatalog.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.Role;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.PasswordUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.mapper.user.UserMapper;
import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.security.auth.AuthenticatedUserService;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.dscatalog.service.exception.PasswordUpdateException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.mail.MessagingException;

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

  @Nested
  @DisplayName("Registration Operations")
  class RegistrationOperations {

    @Test
    @DisplayName("register should create inactive user and send activation email")
    void registerShouldCreateInactiveUserAndSendActivationEmail() throws MessagingException {
      // Arrange
      UserRegisterRequest request = new UserRegisterRequest("Pedro", "Santos", "pedro@gmail.com",
          "Java!234567");
      Role role = new Role(1L, "ROLE_OPERATOR");
      User user = UserFactory.createUser();
      user.setId(10L);
      user.addRole(role);
      user.deactivate();
      Token activationToken = new Token("token-123", user, java.time.Instant.now().plusSeconds(3600),
          TokenType.ACTIVATION);
      UserResponse response = new UserResponse(10L, "Pedro", "Santos", "pedro@gmail.com", Set.of());

      when(roleRepository.findByAuthority("ROLE_OPERATOR")).thenReturn(Optional.of(role));
      when(userMapper.toEntity(request, Set.of(role))).thenReturn(user);
      when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
      when(userRepository.save(user)).thenReturn(user);
      when(tokenService.createActivationToken(user)).thenReturn(activationToken);
      when(userMapper.toResponse(user)).thenReturn(response);

      // Act
      UserResponse result = service.register(request);

      // Assert
      assertNotNull(result);
      assertEquals(response, result);
      verify(roleRepository, times(1)).findByAuthority("ROLE_OPERATOR");
      verify(userRepository, times(1)).save(user);
      verify(tokenService, times(1)).createActivationToken(user);
      verify(emailService, times(1)).sendActivationEmailAsync(user.getFirstName(), user.getEmail(),
          activationToken.getToken());
    }
  }

  @Nested
  @DisplayName("Activation Operations")
  class ActivationOperations {

    @Test
    @DisplayName("confirmEmail should activate user and disable token when token is valid")
    void confirmEmailShouldActivateUserAndDisableTokenWhenTokenIsValid() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(5L);
      Token token = new Token("token-activation", user, java.time.Instant.now().plusSeconds(3600),
          TokenType.ACTIVATION);

      when(tokenService.findAndValidateToken("token-activation", TokenType.ACTIVATION)).thenReturn(token);

      // Act
      service.confirmEmail("token-activation");

      // Assert
      assertEquals(true, user.isActive());
      assertEquals(true, token.getDisabled());
      verify(tokenService, times(1)).findAndValidateToken("token-activation", TokenType.ACTIVATION);
    }

    @Test
    @DisplayName("confirmEmail should propagate InvalidTokenException when token is invalid")
    void confirmEmailShouldPropagateInvalidTokenExceptionWhenTokenIsInvalid() {
      // Arrange
      when(tokenService.findAndValidateToken("bad-token", TokenType.ACTIVATION))
          .thenThrow(new InvalidTokenException("error.token.invalid"));

      // Act & Assert
      assertThrows(InvalidTokenException.class, () -> service.confirmEmail("bad-token"));
    }
  }

  @Nested
  @DisplayName("Password Recovery Operations")
  class PasswordRecoveryOperations {

    @Test
    @DisplayName("requestPasswordRecovery should send recovery email when user exists")
    void requestPasswordRecoveryShouldSendRecoveryEmailWhenUserExists() {
      // Arrange
      User user = UserFactory.createUser();
      user.setEmail("joao@gmail.com");
      Token token = new Token("recovery-token", user, java.time.Instant.now().plusSeconds(600),
          TokenType.PASSWORD_RECOVERY);

      when(userRepository.findByEmail("joao@gmail.com")).thenReturn(Optional.of(user));
      when(tokenService.createPasswordRecoveryToken(user)).thenReturn(token);

      // Act
      service.requestPasswordRecovery("joao@gmail.com");

      // Assert
      verify(emailService, times(1)).sendPasswordRecoveryEmailAsync(user, token.getToken());
    }

    @Test
    @DisplayName("requestPasswordRecovery should do nothing when user does not exist")
    void requestPasswordRecoveryShouldDoNothingWhenUserDoesNotExist() {
      // Arrange
      when(userRepository.findByEmail("ghost@gmail.com")).thenReturn(Optional.empty());

      // Act
      assertDoesNotThrow(() -> service.requestPasswordRecovery("ghost@gmail.com"));

      // Assert
      verify(emailService, never()).sendPasswordRecoveryEmailAsync(any(User.class), anyString());
    }

    @Test
    @DisplayName("resetPassword should update password and disable token when token is valid")
    void resetPasswordShouldUpdatePasswordAndDisableTokenWhenTokenIsValid() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(11L);
      user.setPassword("old-password");
      Token token = new Token("recovery-token", user, java.time.Instant.now().plusSeconds(600),
          TokenType.PASSWORD_RECOVERY);

      when(tokenService.findAndValidateToken("recovery-token", TokenType.PASSWORD_RECOVERY)).thenReturn(token);
      when(passwordEncoder.encode("newStrongPassword1!")).thenReturn("encoded-new-password");
      when(userRepository.save(user)).thenReturn(user);

      // Act
      service.resetPassword("recovery-token", "newStrongPassword1!");

      // Assert
      assertEquals("encoded-new-password", user.getPassword());
      assertEquals(true, token.getDisabled());
      verify(userRepository, times(1)).save(user);
    }
  }

  @Nested
  @DisplayName("Authenticated User Operations")
  class AuthenticatedUserOperations {

    @Test
    @DisplayName("updateAuthenticatedUser should update profile and return response")
    void updateAuthenticatedUserShouldUpdateProfileAndReturnResponse() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(22L);
      AuthenticatedUserUpdateRequest request = new AuthenticatedUserUpdateRequest("Maria", "Souza", "maria@gmail.com");
      UserResponse response = new UserResponse(22L, "Maria", "Souza", "maria@gmail.com", Set.of());

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(userRepository.save(user)).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(response);

      // Act
      UserResponse result = service.updateAuthenticatedUser(request);

      // Assert
      assertNotNull(result);
      assertEquals("Maria", user.getFirstName());
      assertEquals("Souza", user.getLastName());
      assertEquals("maria@gmail.com", user.getEmail());
      verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("updatePassword should confirm password, validate current and new password and persist update")
    void updatePasswordShouldValidateAndPersistUpdate() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(23L);
      user.setPassword("$2a$10$encodedCurrentPassword");

      PasswordUpdateRequest request = new PasswordUpdateRequest("oldPassword1!", "newPassword2@", "newPassword2@");

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(passwordEncoder.matches("oldPassword1!", user.getPassword())).thenReturn(true);
      when(passwordEncoder.matches("newPassword2@", user.getPassword())).thenReturn(false);
      when(passwordEncoder.encode("newPassword2@")).thenReturn("encoded-new-password");

      // Act
      service.updatePassword(request);

      // Assert
      assertEquals("encoded-new-password", user.getPassword());
      verify(passwordEncoder, times(1)).matches("oldPassword1!", "$2a$10$encodedCurrentPassword");
      verify(passwordEncoder, times(1)).matches("newPassword2@", "$2a$10$encodedCurrentPassword");
      verify(passwordEncoder, times(1)).encode("newPassword2@");
    }

    @Test
    @DisplayName("updatePassword should throw PasswordUpdateException when confirmation mismatch")
    void updatePasswordShouldThrowWhenConfirmationMismatch() {
      // Arrange
      PasswordUpdateRequest request = new PasswordUpdateRequest("oldPassword1!", "newPassword2@", "otherPassword2@");

      // Act & Assert
      assertThrows(PasswordUpdateException.class, () -> service.updatePassword(request));
      verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("updatePassword should throw PasswordUpdateException when current password is invalid")
    void updatePasswordShouldThrowWhenCurrentPasswordIsInvalid() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(25L);
      user.setPassword("$2a$10$encodedCurrentPassword");
      PasswordUpdateRequest request = new PasswordUpdateRequest("wrongPassword1!", "newPassword2@", "newPassword2@");

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(passwordEncoder.matches("wrongPassword1!", user.getPassword())).thenReturn(false);

      // Act & Assert
      assertThrows(PasswordUpdateException.class, () -> service.updatePassword(request));
    }

    @Test
    @DisplayName("updatePassword should throw PasswordUpdateException when new password is same as current")
    void updatePasswordShouldThrowWhenNewPasswordIsSameAsCurrent() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(26L);
      user.setPassword("$2a$10$encodedCurrentPassword");
      PasswordUpdateRequest request = new PasswordUpdateRequest("oldPassword1!", "oldPassword1!", "oldPassword1!");

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(passwordEncoder.matches("oldPassword1!", user.getPassword())).thenReturn(true);
      when(passwordEncoder.matches("oldPassword1!", user.getPassword())).thenReturn(true);

      // Act & Assert
      assertThrows(PasswordUpdateException.class, () -> service.updatePassword(request));
    }

    @Test
    @DisplayName("getAuthenticatedUser should return mapped user response")
    void getAuthenticatedUserShouldReturnMappedUserResponse() {
      // Arrange
      User user = UserFactory.createUser();
      user.setId(27L);
      UserResponse response = new UserResponse(27L, "João", "Silva", "joao@gmail.com", Set.of());

      when(authenticatedUserService.getAuthenticatedUser()).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(response);

      // Act
      UserResponse result = service.getAuthenticatedUser();

      // Assert
      assertEquals(response, result);
    }
  }

  @Nested
  @DisplayName("Token Handling")
  class TokenHandling {

    @Test
    @DisplayName("findAndValidateToken should throw ResourceNotFoundException when token is not found")
    void findAndValidateTokenShouldThrowResourceNotFoundExceptionWhenTokenIsNotFound() {
      // Arrange
      when(tokenService.findAndValidateToken("missing-token", TokenType.ACTIVATION))
          .thenThrow(new ResourceNotFoundException("error.token.notFound"));

      // Act & Assert
      assertThrows(ResourceNotFoundException.class, () -> service.confirmEmail("missing-token"));
    }
  }
}
