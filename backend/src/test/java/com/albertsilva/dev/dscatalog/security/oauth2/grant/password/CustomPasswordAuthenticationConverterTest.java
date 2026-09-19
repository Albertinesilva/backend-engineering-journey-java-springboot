package com.albertsilva.dev.dscatalog.security.oauth2.grant.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

@DisplayName("Tests for CustomPasswordAuthenticationConverter")
class CustomPasswordAuthenticationConverterTest {

  private final CustomPasswordAuthenticationConverter converter = new CustomPasswordAuthenticationConverter();

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    SecurityContextHolder.getContext().setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated("client", null, Set.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("successful conversion")
  class SuccessfulConversionTests {

    @Test
    @DisplayName("convert should create a password authentication token with username and password")
    void convertShouldCreatePasswordAuthenticationToken() {
      // Arrange
      MockHttpServletRequest request = requestWithCredentials();

      // Act
      Authentication result = converter.convert(request);

      // Assert
      assertThat(result).isInstanceOf(CustomPasswordAuthenticationToken.class);
      CustomPasswordAuthenticationToken token = (CustomPasswordAuthenticationToken) result;
      assertThat(token.getUsername()).isEqualTo("albert@gmail.com");
      assertThat(token.getPassword()).isEqualTo("123456");
      assertThat(token.getGrantType().getValue()).isEqualTo("password");
      assertThat(token.getScopes()).isEmpty();
      assertThat(token.getAdditionalParameters()).isEmpty();
    }

    @Test
    @DisplayName("convert should preserve requested scopes as a set")
    void convertShouldPreserveRequestedScopes() {
      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter(OAuth2ParameterNames.SCOPE, "openid profile");

      // Act
      CustomPasswordAuthenticationToken result = convert(request);

      // Assert
      assertThat(result.getScopes()).containsExactlyInAnyOrder("openid", "profile");
    }

    @Test
    @DisplayName("convert should keep additional parameters and their first value")
    void convertShouldKeepAdditionalParameters() {
      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter(OAuth2ParameterNames.CLIENT_ID, "catalog-client");
      request.addParameter("custom_parameter", "custom-value");

      // Act
      CustomPasswordAuthenticationToken result = convert(request);

      // Assert
      assertThat(result.getAdditionalParameters())
          .containsEntry(OAuth2ParameterNames.CLIENT_ID, "catalog-client")
          .containsEntry("custom_parameter", "custom-value");
    }
  }

  @Nested
  @DisplayName("invalid conversion")
  class InvalidConversionTests {

    @Test
    @DisplayName("convert should return null for an unsupported grant type")
    void convertShouldReturnNullForUnsupportedGrantType() {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");

      // Act
      Authentication result = converter.convert(request);

      // Assert
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("convert should reject a missing username")
    void convertShouldRejectMissingUsername() {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addParameter(OAuth2ParameterNames.GRANT_TYPE, "password");
      request.addParameter(OAuth2ParameterNames.PASSWORD, "123456");

      // Act / Assert
      assertInvalidRequest(request);
    }

    @Test
    @DisplayName("convert should reject a missing password")
    void convertShouldRejectMissingPassword() {
      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addParameter(OAuth2ParameterNames.GRANT_TYPE, "password");
      request.addParameter(OAuth2ParameterNames.USERNAME, "albert@gmail.com");

      // Act / Assert
      assertInvalidRequest(request);
    }

    @Test
    @DisplayName("convert should reject duplicated username parameters")
    void convertShouldRejectDuplicatedUsername() {
      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter(OAuth2ParameterNames.USERNAME, "maria@gmail.com");

      // Act / Assert
      assertInvalidRequest(request);
    }

    @Test
    @DisplayName("convert should reject duplicated password parameters")
    void convertShouldRejectDuplicatedPassword() {
      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter(OAuth2ParameterNames.PASSWORD, "another-password");

      // Act / Assert
      assertInvalidRequest(request);
    }

    @Test
    @DisplayName("convert should reject duplicated non-blank scope parameters")
    void convertShouldRejectDuplicatedScope() {
      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter(OAuth2ParameterNames.SCOPE, "openid");
      request.addParameter(OAuth2ParameterNames.SCOPE, "profile");

      // Act / Assert
      assertInvalidRequest(request);
    }
  }

  private MockHttpServletRequest requestWithCredentials() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter(OAuth2ParameterNames.GRANT_TYPE, "password");
    request.addParameter(OAuth2ParameterNames.USERNAME, "albert@gmail.com");
    request.addParameter(OAuth2ParameterNames.PASSWORD, "123456");
    return request;
  }

  private CustomPasswordAuthenticationToken convert(MockHttpServletRequest request) {
    return (CustomPasswordAuthenticationToken) converter.convert(request);
  }

  private void assertInvalidRequest(MockHttpServletRequest request) {
    assertThatThrownBy(() -> converter.convert(request))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .satisfies(exception -> {
          OAuth2AuthenticationException oauthException = (OAuth2AuthenticationException) exception;
          assertThat(oauthException.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST);
        });
  }
}
