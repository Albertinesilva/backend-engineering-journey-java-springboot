package com.albertsilva.dev.dscatalog.integrations.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.dscatalog.repository.UserRepository;

@Transactional
@DisplayName("Resource Server Authorization Integration Tests")
class ResourceServerAuthorizationIT extends AbstractIT {

  private static final String USERS_URL = "/api/v1/users";
  private static final String CATEGORIES_URL = "/api/v1/categories";
  private static final String PASSWORD = "123456";

  @Autowired
  private UserRepository userRepository;

  private User operator;
  private User admin;
  private String operatorToken;
  private String adminToken;

  @BeforeEach
  void setUp() throws Exception {
    operator = userRepository.findByEmail("albert@gmail.com").orElseThrow();
    admin = userRepository.findByEmail("maria@gmail.com").orElseThrow();

    operatorToken = tokenUtil.obtainAccessToken(mockMvc, operator.getEmail(), PASSWORD);
    adminToken = tokenUtil.obtainAccessToken(mockMvc, admin.getEmail(), PASSWORD);
  }

  @Test
  @DisplayName("protected endpoint should return 401 when no bearer token is provided")
  void protectedEndpointShouldReturnUnauthorizedWithoutToken() throws Exception {
    mockMvc.perform(get(USERS_URL).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("protected endpoint should return 401 when bearer token is invalid")
  void protectedEndpointShouldReturnUnauthorizedWithInvalidToken() throws Exception {
    mockMvc.perform(get(USERS_URL)
        .header("Authorization", "Bearer invalid-token")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("ADMIN should access an endpoint restricted to ADMIN")
  void adminShouldAccessAdminOnlyEndpoint() throws Exception {
    mockMvc.perform(get(USERS_URL).with(bearerToken(adminToken)).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("OPERATOR should receive 403 on an endpoint restricted to ADMIN")
  void operatorShouldBeForbiddenOnAdminOnlyEndpoint() throws Exception {
    mockMvc.perform(get(USERS_URL).with(bearerToken(operatorToken)).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("OPERATOR should access its own user resource")
  void operatorShouldAccessOwnUserResource() throws Exception {
    ResultActions result = mockMvc.perform(get(USERS_URL + "/{id}", operator.getId())
        .with(bearerToken(operatorToken))
        .accept(MediaType.APPLICATION_JSON));

    result.andExpect(status().isOk());
  }

  @Test
  @DisplayName("OPERATOR should receive 403 when accessing another user resource")
  void operatorShouldBeForbiddenFromAccessingAnotherUser() throws Exception {
    assertThat(operator.getId()).isNotEqualTo(admin.getId());

    mockMvc.perform(get(USERS_URL + "/{id}", admin.getId())
        .with(bearerToken(operatorToken))
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("public category listing should be accessible without bearer token")
  void publicCategoryListingShouldBeAccessibleWithoutToken() throws Exception {
    mockMvc.perform(get(CATEGORIES_URL)
        .param("page", "0")
        .param("size", "1")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  private RequestPostProcessor bearerToken(String token) {
    return request -> {
      request.addHeader("Authorization", "Bearer " + token);
      return request;
    };
  }
}
