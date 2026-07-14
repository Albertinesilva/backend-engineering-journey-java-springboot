package com.albertsilva.dev.dscatalog.web.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.dscatalog.web.exception.enums.ApiErrorCode;
import com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails;

import jakarta.servlet.http.HttpServletRequest;

class ControllerExceptionHandlerTest {

  @Test
  void shouldReturnLocalizedProblemDetailsForResourceNotFound() {
    MessageSource messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(eq("error.resource.title"), any(), eq("error.resource.title"), any(Locale.class)))
        .thenReturn("Recurso não encontrado");
    when(messageSource.getMessage(eq("error.resource.message"), any(), eq("error.resource.message"),
        any(Locale.class)))
        .thenReturn("O recurso solicitado não foi encontrado");
    when(messageSource.getMessage(eq("Entity not found"), isNull(), any(Locale.class)))
        .thenThrow(new NoSuchMessageException("Entity not found", Locale.forLanguageTag("pt-BR")));

    ControllerExceptionHandler handler = new ControllerExceptionHandler(messageSource);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/products/1");

    ResponseEntity<ProblemDetails> response = handler.handleResourceNotFound(
        new ResourceNotFoundException("Entity not found"), request, Locale.forLanguageTag("pt-BR"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND);
    assertThat(response.getBody().getError()).isEqualTo("Recurso não encontrado");
    assertThat(response.getBody().getMessage()).isEqualTo("O recurso solicitado não foi encontrado");
    assertThat(response.getBody().getPath()).isEqualTo("/products/1");
  }

  @Test
  void shouldResolveMessageFromLocalizedExceptionKey() {
    MessageSource messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(eq("error.resource.title"), isNull(), eq("error.resource.title"), any(Locale.class)))
        .thenReturn("Recurso não encontrado");
    when(messageSource.getMessage(eq("error.token.notFound"), isNull(), any(Locale.class)))
        .thenReturn("Token não encontrado");

    ControllerExceptionHandler handler = new ControllerExceptionHandler(messageSource);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/tokens/1");

    ResponseEntity<ProblemDetails> response = handler.handleResourceNotFound(
        new ResourceNotFoundException("error.token.notFound"), request, Locale.forLanguageTag("pt-BR"));

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Token não encontrado");
  }
}
