package com.albertsilva.dev.dscatalog.integrations.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import com.albertsilva.dev.dscatalog.integrations.common.AbstractIT;

/**
 * Requisições de preflight CORS ({@code OPTIONS}) contra a configuração de
 * {@code ResourceServerConfig}. A origem permitida é lida de
 * {@code cors.origins}, para não depender de um valor fixo no teste.
 */
@DisplayName("CORS Integration Tests")
class CorsIT extends AbstractIT {

  private static final String PRODUCTS_URL = "/api/v1/products";
  private static final String NOT_ALLOWED_ORIGIN = "https://not-allowed.example.com";

  @Value("${cors.origins}")
  private String corsOrigins;

  @Test
  @DisplayName("preflight from an allowed origin should be accepted with the expected CORS headers")
  void preflightFromAllowedOriginShouldBeAccepted() throws Exception {

    // Arrange
    String allowedOrigin = corsOrigins.split(",")[0].trim();

    // Act
    ResultActions result = mockMvc.perform(options(PRODUCTS_URL)
        .header(HttpHeaders.ORIGIN, allowedOrigin)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"));

    // Assert
    result
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  @DisplayName("preflight from an origin that is not allowed should be rejected without CORS headers")
  void preflightFromNotAllowedOriginShouldBeRejected() throws Exception {

    // Act
    ResultActions result = mockMvc.perform(options(PRODUCTS_URL)
        .header(HttpHeaders.ORIGIN, NOT_ALLOWED_ORIGIN)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

    // Assert
    result
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }
}
