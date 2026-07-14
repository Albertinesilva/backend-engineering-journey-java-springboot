package com.albertsilva.dev.dscatalog.web.exception.handler;

import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException;
import com.albertsilva.dev.dscatalog.service.exception.DatabaseException;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.dscatalog.web.exception.enums.ApiErrorCode;
import com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails;
import com.albertsilva.dev.dscatalog.web.exception.response.ValidationError;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Classe responsável pelo tratamento global de exceções da API.
 */
@RestControllerAdvice
public class ControllerExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

  private final MessageSource messageSource;

  public ControllerExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetails> handleResourceNotFound(ResourceNotFoundException e,
      HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    logger.warn("ResourceNotFoundException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.RESOURCE_NOT_FOUND,
        "error.resource.title", e.getMessage(), "error.resource.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(DatabaseException.class)
  public ResponseEntity<ProblemDetails> handleDatabase(DatabaseException e, HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    logger.error("DatabaseException - path: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.DATABASE_ERROR,
        "error.database.title", e.getMessage(), "error.database.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetails> handleDataIntegrity(DataIntegrityViolationException e,
      HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.CONFLICT;
    logger.error("DataIntegrityViolationException - path: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.CONFLICT,
        "error.conflict.title", "error.conflict.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetails> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    logger.debug("Static resource not found - path: {}", request.getRequestURI());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.RESOURCE_NOT_FOUND,
        "error.resource.title", "error.resource.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetails> validation(MethodArgumentNotValidException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
    ValidationError err = buildValidationError(status, ApiErrorCode.VALIDATION_ERROR,
        "error.validation.title", "error.validation.message", request.getRequestURI(), locale);
    e.getBindingResult().getFieldErrors().forEach(f -> err.addError(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetails> handleAccessDenied(AccessDeniedException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.FORBIDDEN;
    logger.warn("AccessDeniedException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.ACCESS_DENIED,
        "error.access.title", "error.access.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ProblemDetails> handleInvalidToken(InvalidTokenException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    logger.warn("InvalidTokenException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.INVALID_TOKEN,
        "error.invalid.token.title", e.getMessage(), "error.invalid.token.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(AuthenticatedUserNotFoundException.class)
  public ResponseEntity<ProblemDetails> handleAuthenticatedUserNotFound(AuthenticatedUserNotFoundException e,
      HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    logger.warn("AuthenticatedUserNotFoundException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.AUTHENTICATION_REQUIRED,
        "error.authentication.title", e.getMessage(), "error.authentication.message", request.getRequestURI(), locale);

    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<ProblemDetails> handleDisabledException(DisabledException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.FORBIDDEN;
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.ACCESS_DISABLED,
        "error.account.disabled.title", "error.account.disabled.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetails> handleGeneric(Exception e, HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    logger.error("Unexpected error - path: {}", request.getRequestURI(), e);
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.INTERNAL_SERVER_ERROR,
        "error.internal.title", "error.internal.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  private ProblemDetails buildProblemDetails(HttpStatus status, ApiErrorCode code, String titleKey, String messageKey,
      String path, Locale locale) {
    return new ProblemDetails(Instant.now(), status.value(), code,
        getMessage(titleKey, locale), getMessage(messageKey, locale), path);
  }

  private ProblemDetails buildProblemDetails(HttpStatus status, ApiErrorCode code, String titleKey,
      String detailCodeOrMessage, String fallbackMessageKey, String path, Locale locale) {
    return new ProblemDetails(Instant.now(), status.value(), code,
        getMessage(titleKey, locale), resolveMessage(detailCodeOrMessage, fallbackMessageKey, locale), path);
  }

  private ValidationError buildValidationError(HttpStatus status, ApiErrorCode code, String titleKey, String messageKey,
      String path, Locale locale) {
    return new ValidationError(Instant.now(), status.value(), code,
        getMessage(titleKey, locale), getMessage(messageKey, locale), path);
  }

  private String resolveMessage(String codeOrMessage, String fallbackKey, Locale locale) {
    if (codeOrMessage == null) {
      return getMessage(fallbackKey, locale);
    }

    try {
      String message = messageSource.getMessage(codeOrMessage, null, locale);
      return message != null ? message : getMessage(fallbackKey, locale);
    } catch (NoSuchMessageException e) {
      return getMessage(fallbackKey, locale);
    }
  }

  private String getMessage(String key, Locale locale) {
    return messageSource.getMessage(key, null, key, locale);
  }
}