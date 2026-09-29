package com.albertsilva.dev.asjcatalog.integrations.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.OPERATOR_USER_ID;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;

@Transactional
@DisplayName("UserController Integration Tests")
class UserControllerIT extends AbstractIT {

  private static final String BASE_URL = "/api/v1/users";

  @Autowired
  private UserRepository userRepository;

  private String adminUsername;
  private String adminPassword;
  private long totalUsersCount;

  @BeforeEach
  void setUp() throws Exception {
    totalUsersCount = userRepository.count();

    adminUsername = "maria@gmail.com";
    adminPassword = "123456";
    bearerToken = tokenUtil.obtainAccessToken(mockMvc, adminUsername, adminPassword);
  }

  @Nested
  @DisplayName("READ Operations")
  class ReadOperations {

    @Nested
    @DisplayName("FindAll Operations")
    class FindAllOperations {

      @Test
      @DisplayName("GET /users should return paged users sorted by firstName descending")
      void findAllShouldReturnPagedUsersSortedByFirstNameDescending() throws Exception {

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
      @DisplayName("GET /users with firstName filter should return only users whose firstName contains the filter")
      void findAllShouldReturnFilteredUsersWhenFirstNameParameterIsInformed() throws Exception {

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
            .andExpect(jsonPath("$.content").isNotEmpty())
            .andExpect(jsonPath("$.content[*].firstName").value(everyItem(containsStringIgnoringCase("mar"))));
      }
    }

    @Nested
    @DisplayName("FindById Operations")
    class FindByIdOperations {

      @Test
      @DisplayName("GET /users/{id} should return user details when id exists")
      void findByIdShouldReturnUserWhenIdExists() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", EXISTING_ID)
            .with(bearerToken())
            .accept(MediaType.APPLICATION_JSON));

        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(EXISTING_ID))
            .andExpect(jsonPath("$.firstName").isNotEmpty())
            .andExpect(jsonPath("$.lastName").isNotEmpty())
            .andExpect(jsonPath("$.email").isNotEmpty());
      }

      @Test
      @DisplayName("GET /users/{id} should return 404 with RESOURCE_NOT_FOUND code and translated message when id does not exist")
      void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID)
            .with(bearerToken())
            .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
            .accept(MediaType.APPLICATION_JSON));

        resultActions
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("User not found"));
      }
    }
  }

  @Nested
  @DisplayName("CREATE Operations")
  class CreateOperations {

    @Test
    @DisplayName("POST /users should create user and return 201")
    void createShouldCreateUserAndReturnCreated() throws Exception {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();
      String jsonRequest = asJson(request);
      long initialCount = userRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(header().exists("Location"))
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.firstName").value(request.firstName()))
          .andExpect(jsonPath("$.lastName").value(request.lastName()))
          .andExpect(jsonPath("$.email").value(request.email()));

      assertEquals(initialCount + 1, userRepository.count());
    }

    @Test
    @DisplayName("POST /users should return 422 with the translated field error when firstName is blank and create nothing")
    void createShouldReturnUnprocessableEntityWhenFirstNameIsBlank() throws Exception {

      // Arrange
      UserCreateRequest request = new UserCreateRequest("", "Santos", "pedro@gmail.com", "JAVA!@#ResTIc18",
          Set.of(1L));
      long initialCount = userRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'firstName')].message")
              .value(hasItem("First name is required")));

      assertEquals(initialCount, userRepository.count());
    }
  }

  @Nested
  @DisplayName("UPDATE Operations")
  class UpdateOperations {

    @Test
    @DisplayName("PUT /users/{id} should update user when id exists")
    void updateShouldReturnUserResponseWhenIdExists() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();
      String jsonRequest = asJson(request);

      String expectedFirstName = request.firstName();
      String expectedLastName = request.lastName();

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.firstName").value(expectedFirstName))
          .andExpect(jsonPath("$.lastName").value(expectedLastName));
    }

    @Test
    @DisplayName("PUT /users/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();
      String jsonRequest = asJson(request);

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken())
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("ACTIVATE Operations")
  class ActivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/activate should activate an inactive user")
    void activateShouldActivateUserWhenUserIsInactive() throws Exception {

      // Arrange
      User user = userRepository.findById(OPERATOR_USER_ID).orElseThrow();
      user.deactivate();
      userRepository.saveAndFlush(user);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", OPERATOR_USER_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertTrue(userRepository.findById(OPERATOR_USER_ID).orElseThrow().isActive());
    }

    @Test
    @DisplayName("PATCH /users/{id}/activate should return 404 when id does not exist")
    void activateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DEACTIVATE Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should deactivate an active user")
    void deactivateShouldDeactivateUserWhenUserIsActive() throws Exception {

      // Arrange
      assertTrue(userRepository.findById(OPERATOR_USER_ID).orElseThrow().isActive());

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", OPERATOR_USER_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertFalse(userRepository.findById(OPERATOR_USER_ID).orElseThrow().isActive());
    }

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should return 404 when id does not exist")
    void deactivateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("DELETE Operations")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /users/{id} should delete user when id exists")
    void deleteShouldRemoveUserWhenIdExists() throws Exception {

      // Arrange
      long initialCount = userRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", 1L)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(initialCount - 1, userRepository.count());
      assertFalse(userRepository.existsById(1L));
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }
}
