package com.albertsilva.dev.asjcatalog.integrations.oauth2;

import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.OPERATOR_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ADMIN_USER_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.SEED_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.albertsilva.dev.asjcatalog.integrations.common.AbstractIT;

/**
 * Login ({@code grant_type=password}), claims do JWT e rotação do refresh token
 * no {@code POST /oauth2/token}.
 *
 * <p>
 * Estende {@link AbstractIT} (contexto, perfil test e MockMvc), mas mantém um
 * helper próprio: o {@code TokenUtil} devolve só o access token, e estes testes
 * precisam também do refresh token e da resposta completa do endpoint.
 * </p>
 *
 * <p>
 * Dependem do seed ({@code import.sql}): o login de {@code maria@gmail.com}
 * com a senha do seed, o id e as roles dela (#21).
 * </p>
 */
@DisplayName("Integration tests for the OAuth2 token endpoint")
class OAuth2TokenIT extends AbstractIT {

  private static final String TOKEN_URL = "/oauth2/token";

  @Autowired
  private JwtDecoder jwtDecoder;

  @Value("${security.client-id}")
  private String clientId;

  @Value("${security.client-secret}")
  private String clientSecret;

  @Test
  @DisplayName("password grant should issue access and refresh tokens with user claims when credentials are valid")
  void passwordGrantShouldIssueAccessAndRefreshTokensWithUserClaimsWhenCredentialsAreValid() throws Exception {

    // Act
    Map<String, String> tokens = obtainTokens(EXISTING_EMAIL, SEED_PASSWORD);

    // Assert
    assertThat(tokens.get("access_token")).isNotBlank();
    assertThat(tokens.get("refresh_token")).isNotBlank();
    assertUserClaims(jwtDecoder.decode(tokens.get("access_token")));
  }

  @Test
  @DisplayName("refresh token grant should rotate the token pair and keep user claims when refresh token is valid")
  void refreshTokenGrantShouldRotateTokenPairAndKeepUserClaimsWhenRefreshTokenIsValid() throws Exception {

    // Arrange
    Map<String, String> initialTokens = obtainTokens(EXISTING_EMAIL, SEED_PASSWORD);

    // Act
    Map<String, String> refreshedTokens = requestTokens(refreshTokenParams(initialTokens.get("refresh_token")));

    // Assert
    assertThat(refreshedTokens.get("access_token")).isNotBlank().isNotEqualTo(initialTokens.get("access_token"));
    assertThat(refreshedTokens.get("refresh_token")).isNotBlank().isNotEqualTo(initialTokens.get("refresh_token"));
    assertUserClaims(jwtDecoder.decode(refreshedTokens.get("access_token")));
  }

  @Test
  @DisplayName("refresh token grant should return invalid_grant when old refresh token is reused after rotation")
  void refreshTokenGrantShouldReturnInvalidGrantWhenOldRefreshTokenIsReusedAfterRotation() throws Exception {

    // Arrange
    String originalRefreshToken = obtainTokens(EXISTING_EMAIL, SEED_PASSWORD).get("refresh_token");
    String rotatedRefreshToken = requestTokens(refreshTokenParams(originalRefreshToken)).get("refresh_token");
    assertThat(rotatedRefreshToken).isNotBlank().isNotEqualTo(originalRefreshToken);

    // Act
    ResultActions result = postToken(refreshTokenParams(originalRefreshToken));

    // Assert
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  @Test
  @DisplayName("refresh token grant should return invalid_grant when refresh token does not exist")
  void refreshTokenGrantShouldReturnInvalidGrantWhenRefreshTokenDoesNotExist() throws Exception {

    // Act
    ResultActions result = postToken(refreshTokenParams("invalid-refresh-token"));

    // Assert
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  private void assertUserClaims(Jwt jwt) {
    assertThat(jwt.getClaimAsString("username")).isEqualTo(EXISTING_EMAIL);
    assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(ADMIN_USER_ID);
    assertThat(jwt.getClaimAsStringList("authorities"))
        .containsExactlyInAnyOrder(OPERATOR_AUTHORITY, ADMIN_AUTHORITY);
  }

  private Map<String, String> obtainTokens(String username, String password) throws Exception {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "password");
    params.add("client_id", clientId);
    params.add("username", username);
    params.add("password", password);

    return requestTokens(params);
  }

  private ResultActions postToken(MultiValueMap<String, String> params) throws Exception {
    return mockMvc.perform(post(TOKEN_URL)
        .params(params)
        .with(httpBasic(clientId, clientSecret))
        .accept("application/json;charset=UTF-8"));
  }

  private Map<String, String> requestTokens(MultiValueMap<String, String> params) throws Exception {
    ResultActions result = postToken(params).andExpect(status().isOk());

    String response = result.andReturn().getResponse().getContentAsString();
    Map<String, Object> parsed = new JacksonJsonParser().parseMap(response);
    return parsed.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
  }

  private MultiValueMap<String, String> refreshTokenParams(String refreshToken) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "refresh_token");
    params.add("refresh_token", refreshToken);
    params.add("client_id", clientId);
    return params;
  }
}
