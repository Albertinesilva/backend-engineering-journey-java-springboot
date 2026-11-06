package com.albertsilva.dev.asjcatalog.web.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.albertsilva.dev.asjcatalog.service.exception.AuthenticatedUserNotFoundException;
import com.albertsilva.dev.asjcatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.asjcatalog.service.exception.PasswordUpdateException;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.asjcatalog.web.exception.enums.ApiErrorCode;
import com.albertsilva.dev.asjcatalog.web.exception.response.FieldMessage;
import com.albertsilva.dev.asjcatalog.web.exception.response.ProblemDetails;
import com.albertsilva.dev.asjcatalog.web.exception.response.ValidationError;

/**
 * Testes de {@code ControllerExceptionHandler}, com os arquivos de mensagens
 * reais.
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>{@code DatabaseException}: responde 400, mas a exclusão de entidade com
 * vínculos deve passar a 409 (#5).</li>
 * <li>{@code DataIntegrityViolationException}: responde 409 com mensagem
 * genérica, que deve passar a ser específica (#5).</li>
 * <li>{@code DisabledException}: o handler provavelmente não é alcançado por
 * nenhum fluxo; a decisão de remover ou manter é da #25 (item 5).</li>
 * </ul>
 */
@DisplayName("Tests for ControllerExceptionHandler")
class ControllerExceptionHandlerTest {

  private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
  private static final String PATH = "/api/v1/users/1";

  private static final String RESOURCE_NOT_FOUND_TITLE = "Recurso não encontrado";
  private static final String RESOURCE_NOT_FOUND_MESSAGE = "O recurso solicitado não foi encontrado.";
  private static final String USER_NOT_FOUND_MESSAGE = "Usuário não encontrado.";
  private static final String AUTHENTICATION_TITLE = "Autenticação obrigatória";
  private static final String USER_ID_CLAIM_NOT_FOUND_MESSAGE = "Claim de ID de usuário não encontrado.";
  private static final String INTERNAL_TITLE = "Erro interno";
  private static final String INTERNAL_MESSAGE = "Ocorreu um erro inesperado.";
  private static final String VALIDATION_TITLE = "Erro de validação";
  private static final String VALIDATION_MESSAGE = "Um ou mais campos são inválidos.";
  private static final String ACCESS_TITLE = "Acesso negado";
  private static final String ACCESS_MESSAGE = "Você não possui permissão para acessar este recurso.";
  private static final String INVALID_TOKEN_TITLE = "Token inválido";
  private static final String TOKEN_EXPIRED_MESSAGE = "Token expirado.";
  private static final String PASSWORD_UPDATE_TITLE = "Falha na alteração da senha";
  private static final String CURRENT_PASSWORD_INVALID_MESSAGE = "A senha atual está incorreta.";

  private ControllerExceptionHandler handler;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new ControllerExceptionHandler(messageSource());
    request = new MockHttpServletRequest("GET", PATH);
  }

  /**
   * Mesma configuração do {@code MessageSourceConfig.messageSource()}, que não é
   * público: os testes usam os arquivos de mensagens reais, e não textos
   * simulados.
   */
  private static MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setDefaultLocale(PT_BR);
    return messageSource;
  }

  /**
   * Confere todos os campos do {@link ProblemDetails}; o {@code timestamp} deve
   * estar entre os instantes anterior e posterior à chamada do handler.
   */
  private static void assertProblemDetails(ResponseEntity<? extends ProblemDetails> response, Instant before,
      Instant after, HttpStatus status, ApiErrorCode code, String error, String message) {

    assertThat(response.getStatusCode()).isEqualTo(status);

    ProblemDetails body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.getStatus()).isEqualTo(status.value());
    assertThat(body.getCode()).isEqualTo(code);
    assertThat(body.getError()).isEqualTo(error);
    assertThat(body.getMessage()).isEqualTo(message);
    assertThat(body.getPath()).isEqualTo(PATH);
    assertThat(body.getTimestamp()).isBetween(before, after);
  }

  @Nested
  @DisplayName("HandleResourceNotFound Operations")
  class HandleResourceNotFoundOperations {

    @Test
    @DisplayName("handleResourceNotFound should return translated message when key exists")
    void handleResourceNotFoundShouldReturnTranslatedMessageWhenKeyExists() {

      // Arrange
      ResourceNotFoundException exception = new ResourceNotFoundException("error.user.notFound");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleResourceNotFound(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND,
          RESOURCE_NOT_FOUND_TITLE, USER_NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("handleResourceNotFound should return default message when key does not exist")
    void handleResourceNotFoundShouldReturnDefaultMessageWhenKeyDoesNotExist() {

      // Arrange
      ResourceNotFoundException exception = new ResourceNotFoundException("Entity not found");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleResourceNotFound(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND,
          RESOURCE_NOT_FOUND_TITLE, RESOURCE_NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("handleResourceNotFound should return default message when exception has no message")
    void handleResourceNotFoundShouldReturnDefaultMessageWhenExceptionHasNoMessage() {

      // Arrange
      ResourceNotFoundException exception = new ResourceNotFoundException(null);
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleResourceNotFound(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND,
          RESOURCE_NOT_FOUND_TITLE, RESOURCE_NOT_FOUND_MESSAGE);
    }
  }

  @Nested
  @DisplayName("HandleNoResourceFound Operations")
  class HandleNoResourceFoundOperations {

    @Test
    @DisplayName("handleNoResourceFound should return not found with default message")
    void handleNoResourceFoundShouldReturnNotFoundWithDefaultMessage() {

      // Arrange
      NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "favicon.ico");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleNoResourceFound(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.NOT_FOUND, ApiErrorCode.RESOURCE_NOT_FOUND,
          RESOURCE_NOT_FOUND_TITLE, RESOURCE_NOT_FOUND_MESSAGE);
    }
  }

  @Nested
  @DisplayName("HandleAuthenticatedUserNotFound Operations")
  class HandleAuthenticatedUserNotFoundOperations {

    @Test
    @DisplayName("handleAuthenticatedUserNotFound should return unauthorized with translated message")
    void handleAuthenticatedUserNotFoundShouldReturnUnauthorizedWithTranslatedMessage() {

      // Arrange
      AuthenticatedUserNotFoundException exception = new AuthenticatedUserNotFoundException(
          "error.auth.userId.claim.notFound");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleAuthenticatedUserNotFound(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.UNAUTHORIZED,
          ApiErrorCode.AUTHENTICATION_REQUIRED, AUTHENTICATION_TITLE, USER_ID_CLAIM_NOT_FOUND_MESSAGE);
    }
  }

  @Nested
  @DisplayName("HandleGeneric Operations")
  class HandleGenericOperations {

    @Test
    @DisplayName("handleGeneric should return internal server error without exposing exception message")
    void handleGenericShouldReturnInternalServerErrorWithoutExposingExceptionMessage() {

      // Arrange
      RuntimeException exception = new RuntimeException("Detalhe interno que não pode chegar ao cliente");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleGeneric(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.INTERNAL_SERVER_ERROR,
          ApiErrorCode.INTERNAL_SERVER_ERROR, INTERNAL_TITLE, INTERNAL_MESSAGE);
    }
  }

  @Nested
  @DisplayName("Validation Operations")
  class ValidationOperations {

    @Test
    @DisplayName("validation should return unprocessable entity with one entry per field error")
    void validationShouldReturnUnprocessableEntityWithOneEntryPerFieldError() {

      // Arrange
      MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "request");
      bindingResult.addError(new FieldError("request", "email", "E-mail inválido."));
      bindingResult.addError(new FieldError("request", "email", "E-mail já cadastrado."));
      bindingResult.addError(new FieldError("request", "firstName", "Nome é obrigatório."));
      MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.validation(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.UNPROCESSABLE_ENTITY,
          ApiErrorCode.VALIDATION_ERROR, VALIDATION_TITLE, VALIDATION_MESSAGE);
      assertThat(response.getBody()).isInstanceOf(ValidationError.class);
      assertThat(((ValidationError) response.getBody()).getFieldErrors()).containsExactly(
          new FieldMessage("email", "E-mail inválido."),
          new FieldMessage("email", "E-mail já cadastrado."),
          new FieldMessage("firstName", "Nome é obrigatório."));
    }
  }

  @Nested
  @DisplayName("HandleAccessDenied Operations")
  class HandleAccessDeniedOperations {

    @Test
    @DisplayName("handleAccessDenied should return forbidden with default message")
    void handleAccessDeniedShouldReturnForbiddenWithDefaultMessage() {

      // Arrange
      AccessDeniedException exception = new AccessDeniedException("Access Denied");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleAccessDenied(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.FORBIDDEN, ApiErrorCode.ACCESS_DENIED,
          ACCESS_TITLE, ACCESS_MESSAGE);
    }
  }

  @Nested
  @DisplayName("HandleInvalidToken Operations")
  class HandleInvalidTokenOperations {

    @Test
    @DisplayName("handleInvalidToken should return bad request with translated message")
    void handleInvalidTokenShouldReturnBadRequestWithTranslatedMessage() {

      // Arrange
      InvalidTokenException exception = new InvalidTokenException("error.token.expired");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handleInvalidToken(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_TOKEN,
          INVALID_TOKEN_TITLE, TOKEN_EXPIRED_MESSAGE);
    }
  }

  @Nested
  @DisplayName("HandlePasswordUpdate Operations")
  class HandlePasswordUpdateOperations {

    @Test
    @DisplayName("handlePasswordUpdate should return unprocessable entity with translated message")
    void handlePasswordUpdateShouldReturnUnprocessableEntityWithTranslatedMessage() {

      // Arrange
      PasswordUpdateException exception = new PasswordUpdateException("error.account.password.currentInvalid");
      Instant before = Instant.now();

      // Act
      ResponseEntity<ProblemDetails> response = handler.handlePasswordUpdate(exception, request, PT_BR);
      Instant after = Instant.now();

      // Assert
      assertProblemDetails(response, before, after, HttpStatus.UNPROCESSABLE_ENTITY,
          ApiErrorCode.PASSWORD_UPDATE_ERROR, PASSWORD_UPDATE_TITLE, CURRENT_PASSWORD_INVALID_MESSAGE);
    }
  }
}
