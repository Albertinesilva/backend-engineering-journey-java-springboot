package com.albertsilva.dev.asjcatalog.security.oauth2.grant.password;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.WRONG_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

@DisplayName("Tests for CustomPasswordAuthenticationConverter")
class CustomPasswordAuthenticationConverterTest {

  private static final String PASSWORD_GRANT = "password";

  private final CustomPasswordAuthenticationConverter converter = new CustomPasswordAuthenticationConverter();

  private Authentication clientAuthentication;

  @BeforeEach
  void setUp() {
    clientAuthentication = UsernamePasswordAuthenticationToken.authenticated("client", null, Set.of());
    SecurityContextHolder.getContext().setAuthentication(clientAuthentication);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** Requisição com {@code grant_type=password} e as credenciais informadas (nulas não são enviadas). */
  private static MockHttpServletRequest request(String username, String password) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter(OAuth2ParameterNames.GRANT_TYPE, PASSWORD_GRANT);
    if (username != null) {
      request.addParameter(OAuth2ParameterNames.USERNAME, username);
    }
    if (password != null) {
      request.addParameter(OAuth2ParameterNames.PASSWORD, password);
    }
    return request;
  }

  private static MockHttpServletRequest requestWithCredentials() {
    return request(EXISTING_EMAIL, PASSWORD);
  }

  private CustomPasswordAuthenticationToken convert(MockHttpServletRequest request) {
    Authentication result = converter.convert(request);
    assertThat(result).isInstanceOf(CustomPasswordAuthenticationToken.class);
    return (CustomPasswordAuthenticationToken) result;
  }

  static Stream<Arguments> invalidRequests() {
    MockHttpServletRequest duplicatedUsername = requestWithCredentials();
    duplicatedUsername.addParameter(OAuth2ParameterNames.USERNAME, NON_EXISTING_EMAIL);

    MockHttpServletRequest duplicatedPassword = requestWithCredentials();
    duplicatedPassword.addParameter(OAuth2ParameterNames.PASSWORD, WRONG_PASSWORD);

    MockHttpServletRequest duplicatedScope = requestWithCredentials();
    duplicatedScope.addParameter(OAuth2ParameterNames.SCOPE, "read");
    duplicatedScope.addParameter(OAuth2ParameterNames.SCOPE, "write");

    return Stream.of(
        Arguments.of("username ausente", request(null, PASSWORD)),
        Arguments.of("username em branco", request("   ", PASSWORD)),
        Arguments.of("password ausente", request(EXISTING_EMAIL, null)),
        Arguments.of("password em branco", request(EXISTING_EMAIL, "   ")),
        Arguments.of("username duplicado", duplicatedUsername),
        Arguments.of("password duplicado", duplicatedPassword),
        Arguments.of("scope duplicado", duplicatedScope));
  }

  @Nested
  @DisplayName("Convert Operations")
  class ConvertOperations {

    @Test
    @DisplayName("convert should create password token when credentials are given")
    void convertShouldCreatePasswordTokenWhenCredentialsAreGiven() {

      // Arrange
      MockHttpServletRequest request = requestWithCredentials();

      // Act
      CustomPasswordAuthenticationToken result = convert(request);

      // Assert
      assertThat(result.getUsername()).isEqualTo(EXISTING_EMAIL);
      assertThat(result.getPassword()).isEqualTo(PASSWORD);
      assertThat(result.getGrantType().getValue()).isEqualTo(PASSWORD_GRANT);
      assertThat(result.getPrincipal()).isSameAs(clientAuthentication);
      assertThat(result.getScopes()).isEmpty();
      assertThat(result.getAdditionalParameters()).isEmpty();
    }

    @Test
    @DisplayName("convert should split scopes when scope is given")
    void convertShouldSplitScopesWhenScopeIsGiven() {

      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter(OAuth2ParameterNames.SCOPE, "read write");

      // Act
      CustomPasswordAuthenticationToken result = convert(request);

      // Assert
      assertThat(result.getScopes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    @DisplayName("convert should keep first value of additional parameters when they are given")
    void convertShouldKeepFirstValueOfAdditionalParametersWhenTheyAreGiven() {

      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter(OAuth2ParameterNames.CLIENT_ID, "myclientid");
      request.addParameter("custom_parameter", "first-value");
      request.addParameter("custom_parameter", "second-value");

      // Act
      CustomPasswordAuthenticationToken result = convert(request);

      // Assert
      assertThat(result.getAdditionalParameters())
          .containsOnly(
              entry(OAuth2ParameterNames.CLIENT_ID, "myclientid"),
              entry("custom_parameter", "first-value"));
    }

    @Test
    @DisplayName("convert should ignore parameter when it has no values")
    void convertShouldIgnoreParameterWhenItHasNoValues() {

      // Arrange
      MockHttpServletRequest request = requestWithCredentials();
      request.addParameter("empty_parameter", new String[0]);

      // Act
      CustomPasswordAuthenticationToken result = convert(request);

      // Assert
      assertThat(result.getAdditionalParameters()).doesNotContainKey("empty_parameter");
    }

    @Test
    @DisplayName("convert should return null when grant type is not password")
    void convertShouldReturnNullWhenGrantTypeIsNotPassword() {

      // Arrange
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addParameter(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");

      // Act
      Authentication result = converter.convert(request);

      // Assert
      assertThat(result).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.albertsilva.dev.asjcatalog.security.oauth2.grant.password.CustomPasswordAuthenticationConverterTest#invalidRequests")
    @DisplayName("convert should throw invalid request when parameters are invalid")
    void convertShouldThrowInvalidRequestWhenParametersAreInvalid(String scenario, MockHttpServletRequest request) {

      // Act & Assert
      assertThatThrownBy(() -> converter.convert(request))
          .isInstanceOfSatisfying(OAuth2AuthenticationException.class,
              exception -> assertThat(exception.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST));
    }
  }
}
