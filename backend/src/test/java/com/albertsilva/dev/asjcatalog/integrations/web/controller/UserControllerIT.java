package com.albertsilva.dev.asjcatalog.integrations.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ACTIVE_USER_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.INACTIVE_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

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
      @DisplayName("GET /users should return paged users when sorted by firstName")
      void findAllShouldReturnSortedPagedWhenSortByFirstNameUsers() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .with(bearerToken())
            .accept(MediaType.APPLICATION_JSON)
            .param("page", "0")
            .param("size", "12")
            .param("sort", "firstName,asc"));

        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(totalUsersCount))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(12));
      }

      @Test
      @DisplayName("GET /users should return paged users")
      void findAllWithPaginationShouldReturnPagedUsers() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .with(bearerToken())
            .param("page", "0")
            .param("size", "20")
            .param("sort", "firstName,asc")
            .accept(MediaType.APPLICATION_JSON));

        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
      }

      @Test
      @DisplayName("GET /users with firstName filter should return filtered users")
      void findAllShouldReturnFilteredUsersWhenFirstNameParameterIsInformed() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .with(bearerToken())
            .param("firstName", "maria")
            .param("page", "0")
            .param("size", "10")
            .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
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
      @DisplayName("GET /users/{id} should return 404 when id does not exist")
      void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID)
            .with(bearerToken())
            .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isNotFound());
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
    @DisplayName("POST /users should create user with valid data")
    void createShouldCreateUserWithValidData() throws Exception {

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
          .andExpect(jsonPath("$.firstName").value(request.firstName()));

      assertEquals(initialCount + 1, userRepository.count());
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
    @DisplayName("PATCH /users/{id}/activate should activate user when id exists")
    void activateShouldActivateUserWhenIdExists() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", ACTIVE_USER_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(true, userRepository.findById(ACTIVE_USER_ID).get().isActive());
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
    @DisplayName("PATCH /users/{id}/deactivate should deactivate user when id exists")
    void deactivateShouldDeactivateUserWhenIdExists() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", INACTIVE_USER_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(false, userRepository.findById(INACTIVE_USER_ID).get().isActive());
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
