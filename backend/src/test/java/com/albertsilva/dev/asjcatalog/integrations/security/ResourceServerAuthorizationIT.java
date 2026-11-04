package com.albertsilva.dev.asjcatalog.integrations.security;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.OPERATOR_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.SEED_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;

/**
 * Autenticação e autorização do Resource Server com JWT real: {@code 401} sem
 * token ou com token inválido, rotas restritas a ADMIN, acesso do OPERATOR ao
 * próprio usuário e listagem pública de categorias.
 *
 * <p>
 * Dependem do seed ({@code import.sql}): o OPERATOR {@code albert@gmail.com},
 * a ADMIN {@code maria@gmail.com} e a senha do seed (#21).
 * </p>
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>Corpo do {@code 401}: hoje a resposta vem sem corpo, e os testes conferem
 * só o status (#22).</li>
 * </ul>
 */
@Transactional
@DisplayName("Integration tests for Resource Server authorization")
class ResourceServerAuthorizationIT extends AbstractIT {

  private static final String USERS_URL = "/api/v1/users";
  private static final String CATEGORIES_URL = "/api/v1/categories";

  @Autowired
  private UserRepository userRepository;

  private User operator;
  private User admin;
  private String operatorToken;
  private String adminToken;

  @BeforeEach
  void setUp() throws Exception {
    operator = userRepository.findByEmail(OPERATOR_EMAIL).orElseThrow();
    admin = userRepository.findByEmail(EXISTING_EMAIL).orElseThrow();

    operatorToken = tokenUtil.obtainAccessToken(mockMvc, operator.getEmail(), SEED_PASSWORD);
    adminToken = tokenUtil.obtainAccessToken(mockMvc, admin.getEmail(), SEED_PASSWORD);
  }

  private RequestPostProcessor bearerToken(String token) {
    return request -> {
      request.addHeader("Authorization", "Bearer " + token);
      return request;
    };
  }

  @Nested
  @DisplayName("GET /users")
  class FindAllUsersOperations {

    @Test
    @DisplayName("GET /users should return 401 when token is missing")
    void findAllUsersShouldReturnUnauthorizedWhenTokenIsMissing() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(get(USERS_URL).accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users should return 401 when token is invalid")
    void findAllUsersShouldReturnUnauthorizedWhenTokenIsInvalid() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(get(USERS_URL)
          .with(bearerToken("invalid-token"))
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users should return 200 when user is ADMIN")
    void findAllUsersShouldReturnOkWhenUserIsAdmin() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(get(USERS_URL)
          .with(bearerToken(adminToken))
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /users should return 403 when user is only OPERATOR")
    void findAllUsersShouldReturnForbiddenWhenUserIsOnlyOperator() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(get(USERS_URL)
          .with(bearerToken(operatorToken))
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
  }

  @Nested
  @DisplayName("GET /users/{id}")
  class FindUserByIdOperations {

    @Test
    @DisplayName("GET /users/{id} should return 200 when OPERATOR requests own user")
    void findUserByIdShouldReturnOkWhenOperatorRequestsOwnUser() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(get(USERS_URL + "/{id}", operator.getId())
          .with(bearerToken(operatorToken))
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(operator.getId()))
          .andExpect(jsonPath("$.email").value(OPERATOR_EMAIL));
    }

    @Test
    @DisplayName("GET /users/{id} should return 403 when OPERATOR requests another user")
    void findUserByIdShouldReturnForbiddenWhenOperatorRequestsAnotherUser() throws Exception {

      // Arrange
      assertThat(operator.getId()).isNotEqualTo(admin.getId());

      // Act
      ResultActions result = mockMvc.perform(get(USERS_URL + "/{id}", admin.getId())
          .with(bearerToken(operatorToken))
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
  }

  @Nested
  @DisplayName("GET /categories")
  class FindAllCategoriesOperations {

    @Test
    @DisplayName("GET /categories should return 200 when token is missing")
    void findAllCategoriesShouldReturnOkWhenTokenIsMissing() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(get(CATEGORIES_URL)
          .param("page", "0")
          .param("size", "1")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isOk());
    }
  }
}
