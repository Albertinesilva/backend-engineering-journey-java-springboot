package com.albertsilva.dev.asjcatalog.integrations.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.OPERATOR_USER_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.SEED_PASSWORD;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectNotFound;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectValidationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;

/**
 * Testes de integração dos endpoints de {@code /api/v1/users}, com o contexto
 * completo, o banco H2 e o token de uma usuária ADMIN.
 *
 * <p>
 * Dependem do seed ({@code import.sql}): o login de {@code maria@gmail.com}
 * com a senha do seed, os dois usuários Albert ({@code OPERATOR_USER_ID}) e
 * Maria e os primeiros nomes esperados na ordenação (#21).
 * </p>
 */
@Transactional
@DisplayName("Integration tests for UserController")
class UserControllerIT extends AbstractIT {

  private static final String BASE_URL = "/api/v1/users";

  @Autowired
  private UserRepository userRepository;

  private long totalUsersCount;

  @BeforeEach
  void setUp() throws Exception {
    totalUsersCount = userRepository.count();
    bearerToken = tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, SEED_PASSWORD);
  }

  @Nested
  @DisplayName("GET /users")
  class FindAllOperations {

    @Test
    @DisplayName("GET /users should return users sorted by firstName descending when sort is firstName descending")
    void findAllShouldReturnUsersSortedByFirstNameDescendingWhenSortIsFirstNameDescending() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .with(bearerToken())
          .accept(MediaType.APPLICATION_JSON)
          .param("page", "0")
          .param("size", "12")
          .param("sort", "firstName,desc"));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(totalUsersCount))
          .andExpect(jsonPath("$.content[0].firstName").value("Maria"))
          .andExpect(jsonPath("$.content[1].firstName").value("Albert"))
          .andExpect(jsonPath("$.number").value(0))
          .andExpect(jsonPath("$.size").value(12));
    }

    @Test
    @DisplayName("GET /users should return only matching users when firstName is informed")
    void findAllShouldReturnOnlyMatchingUsersWhenFirstNameIsInformed() throws Exception {

      // Arrange
      User created = userRepository.saveAndFlush(
          new User(null, "Marcos", "Souza", "marcos@gmail.com", "encoded-password", true));

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .with(bearerToken())
          .param("firstName", "mar")
          .param("page", "0")
          .param("size", "10")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[*].firstName").value(hasItem(created.getFirstName())))
          .andExpect(jsonPath("$.content[*].firstName").value(everyItem(containsStringIgnoringCase("mar"))));
    }
  }

  @Nested
  @DisplayName("GET /users/{id}")
  class FindByIdOperations {

    @Test
    @DisplayName("GET /users/{id} should return user details when id exists")
    void findByIdShouldReturnUserDetailsWhenIdExists() throws Exception {

      // Arrange
      User expected = userRepository.findById(EXISTING_ID).orElseThrow();

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.firstName").value(expected.getFirstName()))
          .andExpect(jsonPath("$.lastName").value(expected.getLastName()))
          .andExpect(jsonPath("$.email").value(expected.getEmail()));
    }

    @Test
    @DisplayName("GET /users/{id} should return 404 with translated message when id does not exist")
    void findByIdShouldReturnNotFoundWithTranslatedMessageWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
      resultActions.andExpect(jsonPath("$.message").value("User not found."));
    }
  }

  @Nested
  @DisplayName("POST /users")
  class CreateOperations {

    @Test
    @DisplayName("POST /users should return 201 with location and persist user when request is valid")
    void createShouldReturnCreatedWithLocationAndPersistUserWhenRequestIsValid() throws Exception {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();
      long initialCount = userRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").isNumber())
          .andExpect(jsonPath("$.firstName").value(request.firstName()))
          .andExpect(jsonPath("$.lastName").value(request.lastName()))
          .andExpect(jsonPath("$.email").value(request.email()));

      Long id = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("id")
          .asLong();
      resultActions.andExpect(header().string("Location", "http://localhost" + BASE_URL + "/" + id));

      assertThat(userRepository.count()).isEqualTo(initialCount + 1);
      assertThat(userRepository.findById(id).orElseThrow())
          .extracting(User::getFirstName, User::getLastName, User::getEmail)
          .containsExactly(request.firstName(), request.lastName(), request.email());
    }

    @Test
    @DisplayName("POST /users should return 422 with translated field error and create nothing when firstName is blank")
    void createShouldReturnUnprocessableEntityAndCreateNothingWhenFirstNameIsBlank() throws Exception {

      // Arrange
      UserCreateRequest request = new UserCreateRequest("", "Santos", "pedro@gmail.com", PASSWORD,
          Set.of(RoleFactory.OPERATOR_ROLE_ID));
      long initialCount = userRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(resultActions, BASE_URL, "firstName");
      resultActions.andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'firstName')].message")
          .value(hasItem("First name is required.")));

      assertThat(userRepository.count()).isEqualTo(initialCount);
    }
  }

  @Nested
  @DisplayName("PUT /users/{id}")
  class UpdateOperations {

    @Test
    @DisplayName("PUT /users/{id} should return 200 and persist changes when id exists")
    void updateShouldReturnOkAndPersistChangesWhenIdExists() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.firstName").value(request.firstName()))
          .andExpect(jsonPath("$.lastName").value(request.lastName()))
          .andExpect(jsonPath("$.email").value(request.email()));

      assertThat(userRepository.findById(EXISTING_ID).orElseThrow())
          .extracting(User::getFirstName, User::getLastName, User::getEmail)
          .containsExactly(request.firstName(), request.lastName(), request.email());
    }

    @Test
    @DisplayName("PUT /users/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("PATCH /users/{id}/activate")
  class ActivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/activate should return 204 and activate user when user is inactive")
    void activateShouldReturnNoContentAndActivateUserWhenUserIsInactive() throws Exception {

      // Arrange
      User user = userRepository.findById(OPERATOR_USER_ID).orElseThrow();
      user.deactivate();
      userRepository.saveAndFlush(user);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", OPERATOR_USER_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(userRepository.findById(OPERATOR_USER_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("PATCH /users/{id}/activate should return 404 when id does not exist")
    void activateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID + "/activate");
    }
  }

  @Nested
  @DisplayName("PATCH /users/{id}/deactivate")
  class DeactivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should return 204 and deactivate user when user is active")
    void deactivateShouldReturnNoContentAndDeactivateUserWhenUserIsActive() throws Exception {

      // Arrange
      assertThat(userRepository.findById(OPERATOR_USER_ID).orElseThrow().isActive()).isTrue();

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", OPERATOR_USER_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(userRepository.findById(OPERATOR_USER_ID).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should return 404 when id does not exist")
    void deactivateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID + "/deactivate");
    }
  }

  @Nested
  @DisplayName("DELETE /users/{id}")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /users/{id} should return 204 and remove user when id exists")
    void deleteShouldReturnNoContentAndRemoveUserWhenIdExists() throws Exception {

      // Arrange
      long initialCount = userRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", OPERATOR_USER_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(userRepository.count()).isEqualTo(initialCount - 1);
      assertThat(userRepository.existsById(OPERATOR_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }
}
