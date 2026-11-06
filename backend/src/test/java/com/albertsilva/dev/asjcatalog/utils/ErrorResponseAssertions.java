package com.albertsilva.dev.asjcatalog.utils;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.ResultActions;

import com.albertsilva.dev.asjcatalog.web.exception.enums.ApiErrorCode;

/**
 * Conferências do corpo de erro ({@code ProblemDetails} e {@code ValidationError})
 * nos testes HTTP. O texto traduzido das mensagens é conferido no
 * {@code ControllerExceptionHandlerTest} e no {@code AcceptLanguageIT}; aqui
 * ficam o status, o {@code code} estável e o caminho da requisição.
 */
public final class ErrorResponseAssertions {

  private ErrorResponseAssertions() {
  }

  /** 404 com {@code code = RESOURCE_NOT_FOUND} e o caminho da requisição. */
  public static void expectNotFound(ResultActions result, String path) throws Exception {
    result
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value(ApiErrorCode.RESOURCE_NOT_FOUND.name()))
        .andExpect(jsonPath("$.path").value(path));
  }

  /** 422 com {@code code = VALIDATION_ERROR} e erros nos campos informados. */
  public static void expectValidationError(ResultActions result, String path, String... fields) throws Exception {
    result
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.code").value(ApiErrorCode.VALIDATION_ERROR.name()))
        .andExpect(jsonPath("$.path").value(path))
        .andExpect(jsonPath("$.fieldErrors[*].fieldName", hasItems(fields)));
  }
}
