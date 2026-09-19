package com.albertsilva.dev.dscatalog.integrations.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("OAuth2 Token Integration Tests")
class OAuth2TokenIT {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtDecoder jwtDecoder;

  @Value("${security.client-id}")
  private String clientId;

  @Value("${security.client-secret}")
  private String clientSecret;

  @Test
  @DisplayName("POST /oauth2/token with password grant should issue access and refresh tokens with user claims")
  void passwordGrantShouldIssueAccessAndRefreshTokenWithUserClaims() throws Exception {

    Map<String, String> tokens = obtainTokens("maria@gmail.com", "123456");
    String accessToken = tokens.get("access_token");
    String refreshToken = tokens.get("refresh_token");

    assertNotNull(accessToken);
    assertNotNull(refreshToken);
    assertFalse(accessToken.isBlank());
    assertFalse(refreshToken.isBlank());

    Jwt jwt = jwtDecoder.decode(accessToken);

    assertEquals("maria@gmail.com", jwt.getClaimAsString("username"));
    assertEquals(2, ((Number) jwt.getClaim("userId")).intValue());
    assertTrue(jwt.getClaimAsStringList("authorities").contains("ROLE_ADMIN"));
    assertTrue(jwt.getClaimAsStringList("authorities").contains("ROLE_OPERATOR"));
  }

  @Test
  @DisplayName("POST /oauth2/token with refresh token should rotate the token pair and keep the user claims")
  void refreshTokenGrantShouldIssueNewAccessTokenAndRotateRefreshToken() throws Exception {

    Map<String, String> initialTokens = obtainTokens("maria@gmail.com", "123456");
    String originalAccessToken = initialTokens.get("access_token");
    String originalRefreshToken = initialTokens.get("refresh_token");

    Map<String, String> refreshedTokens = requestTokens(buildRefreshTokenParams(originalRefreshToken));
    String refreshedAccessToken = refreshedTokens.get("access_token");
    String refreshedRefreshToken = refreshedTokens.get("refresh_token");

    assertNotNull(refreshedAccessToken);
    assertNotNull(refreshedRefreshToken);
    assertNotEquals(originalAccessToken, refreshedAccessToken);
    assertNotEquals(originalRefreshToken, refreshedRefreshToken);

    Jwt jwt = jwtDecoder.decode(refreshedAccessToken);
    assertEquals("maria@gmail.com", jwt.getClaimAsString("username"));
    assertEquals(2, ((Number) jwt.getClaim("userId")).intValue());
    assertTrue(jwt.getClaimAsStringList("authorities").contains("ROLE_ADMIN"));
  }

  @Test
  @DisplayName("POST /oauth2/token should reject reuse of the old refresh token after rotation")
  void refreshTokenGrantShouldRejectReuseOfTheOldRefreshTokenAfterRotation() throws Exception {

    Map<String, String> initialTokens = obtainTokens("maria@gmail.com", "123456");
    String originalRefreshToken = initialTokens.get("refresh_token");

    Map<String, String> refreshedTokens = requestTokens(buildRefreshTokenParams(originalRefreshToken));
    String rotatedRefreshToken = refreshedTokens.get("refresh_token");

    assertNotNull(originalRefreshToken);
    assertNotNull(rotatedRefreshToken);
    assertNotEquals(originalRefreshToken, rotatedRefreshToken);

    ResultActions result = mockMvc.perform(post("/oauth2/token")
        .params(buildRefreshTokenParams(originalRefreshToken))
        .with(httpBasic(clientId, clientSecret))
        .accept("application/json;charset=UTF-8"));

    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  @Test
  @DisplayName("POST /oauth2/token with an invalid refresh token should fail with invalid_grant")
  void refreshTokenGrantShouldRejectInvalidRefreshToken() throws Exception {

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "refresh_token");
    params.add("refresh_token", "invalid-refresh-token");
    params.add("client_id", clientId);

    ResultActions result = mockMvc.perform(post("/oauth2/token")
        .params(params)
        .with(httpBasic(clientId, clientSecret))
        .accept("application/json;charset=UTF-8"));

    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("invalid_grant"));
  }

  private Map<String, String> obtainTokens(String username, String password) throws Exception {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "password");
    params.add("client_id", clientId);
    params.add("username", username);
    params.add("password", password);

    return requestTokens(params);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> requestTokens(MultiValueMap<String, String> params) throws Exception {
    ResultActions result = mockMvc.perform(post("/oauth2/token")
        .params(params)
        .with(httpBasic(clientId, clientSecret))
        .accept("application/json;charset=UTF-8"))
        .andExpect(status().isOk());

    String response = result.andReturn().getResponse().getContentAsString();
    JacksonJsonParser jsonParser = new JacksonJsonParser();
    Map<String, Object> parsed = jsonParser.parseMap(response);
    return parsed.entrySet().stream()
        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
  }

  private MultiValueMap<String, String> buildRefreshTokenParams(String refreshToken) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "refresh_token");
    params.add("refresh_token", refreshToken);
    params.add("client_id", clientId);
    return params;
  }
}
