package com.albertsilva.dev.dscatalog.web.controller;

import static com.albertsilva.dev.dscatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.UserFactory.NON_EXISTING_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.albertsilva.dev.dscatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.security.oauth2.resource.config.ResourceServerConfig;
import com.albertsilva.dev.dscatalog.service.UserService;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.dscatalog.web.exception.handler.ControllerExceptionHandler;
import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@Import({ ControllerExceptionHandler.class, ResourceServerConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.h2.console.enabled=false" })
@DisplayName("Tests for UserController")
class UserControllerTest {

  private static final String BASE_URL = "/api/v1/users";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private RoleRepository roleRepository;

  private Page<UserResponse> page;
  private UserResponse userResponse;
  private UserDetailsResponse userDetailsResponse;

  @BeforeEach
  void setUp() {
    userResponse = UserFactory.createUserResponse();
    userDetailsResponse = UserFactory.createUserDetailsResponse();

    page = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1);
  }

  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("POST /users should create user and return 201 with Location header")
    @WithMockUser(roles = { "ADMIN" })
    void shouldCreateUserAndReturn201WithLocationHeader() throws Exception {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

      when(roleRepository.existsById(1L)).thenReturn(true);

      when(userService.create(any(UserCreateRequest.class))).thenReturn(userResponse);

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(csrf())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(header().exists("Location"))
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()));

      verify(userService).create(any(UserCreateRequest.class));
    }
  }

  @Nested
  @DisplayName("Read Operations")
  class ReadOperations {

    @Test
    @WithMockUser(roles = { "ADMIN" })
    @DisplayName("GET /users should return paginated users with 200 status")
    void findAllShouldReturnPagedUsers() throws Exception {

      // Arrange
      when(userService.search(any(), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .accept(MediaType.APPLICATION_JSON)
          .param("page", "0")
          .param("size", "12")
          .param("sort", "firstName,asc"));

      // Assert
      resultActions.andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.number").value(0))
          .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    @DisplayName("GET /users/{id} should return user details with 200 status")
    void findByIdShouldReturnUserDetails() throws Exception {

      // Arrange
      when(userService.findById(EXISTING_ID)).thenReturn(userDetailsResponse);

      // Act
      ResultActions resultActions = mockMvc
          .perform(get(BASE_URL + "/{id}", EXISTING_ID).accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.firstName").value(userDetailsResponse.firstName()))
          .andExpect(jsonPath("$.email").value(userDetailsResponse.email()));
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    @DisplayName("GET /users/{id} should return 404 when user not found")
    void findByIdShouldReturn404WhenUserNotFound() throws Exception {

      // Arrange
      when(userService.findById(NON_EXISTING_ID))
          .thenThrow(new ResourceNotFoundException("error.user.notFound"));

      // Act
      ResultActions resultActions = mockMvc
          .perform(get(BASE_URL + "/{id}", NON_EXISTING_ID).accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Update Operations")
  @WithMockUser(roles = "ADMIN")
  class UpdateOperations {

    @Test
    @DisplayName("PUT /users/{id} should update user and return 200 status")
    void shouldUpdateUserAndReturn200() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      UserResponse updatedResponse = UserFactory.createUpdatedUserResponse();

      when(roleRepository.existsById(anyLong())).thenReturn(true);

      when(userRepository.existsByEmailIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);

      when(userService.update(eq(EXISTING_ID), any(UserUpdateRequest.class))).thenReturn(updatedResponse);

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(asJson(request)));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.firstName")
              .value(updatedResponse.firstName()));
    }

    @Test
    @DisplayName("PUT /users/{id} should return 404 when user not found")
    void shouldReturn404WhenUpdatingNonExistingUser() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

      when(roleRepository.existsById(anyLong())).thenReturn(true);

      when(userService.update(eq(NON_EXISTING_ID), any(UserUpdateRequest.class)))
          .thenThrow(new ResourceNotFoundException("error.user.notFound"));

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(csrf())
          .contentType(MediaType.APPLICATION_JSON)
          .content(asJson(request)));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Activate Operations")
  @WithMockUser(roles = "ADMIN")
  class ActivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/activate should activate user and return 204 status")
    void shouldActivateUserAndReturn204() throws Exception {

      // Arrange
      doNothing().when(userService).activate(EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc
          .perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID).with(csrf()));

      // Assert
      resultActions.andExpect(status().isNoContent());
      verify(userService).activate(EXISTING_ID);
    }

    @Test
    @DisplayName("PATCH /users/{id}/activate should return 404 when user not found")
    void shouldReturn404WhenActivatingNonExistingUser() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException("error.user.notFound")).when(userService)
          .activate(NON_EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc
          .perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID).with(csrf()));

      // Assert
      resultActions.andExpect(status().isNotFound());
      verify(userService).activate(NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  @WithMockUser(roles = "ADMIN")
  class DeactivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should deactivate user and return 204 status")
    void shouldDeactivateUserAndReturn204() throws Exception {

      // Arrange
      doNothing().when(userService).deactivate(EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc
          .perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID).with(csrf()));

      // Assert
      resultActions.andExpect(status().isNoContent());
      verify(userService).deactivate(EXISTING_ID);
    }

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should return 404 when user not found")
    void shouldReturn404WhenDeactivatingNonExistingUser() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException("error.user.notFound")).when(userService)
          .deactivate(NON_EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc
          .perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID).with(csrf()));

      // Assert
      resultActions.andExpect(status().isNotFound());
      verify(userService).deactivate(NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  @WithMockUser(roles = "ADMIN")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /users/{id} should delete user and return 204 status")
    void shouldDeleteUserAndReturn204() throws Exception {

      // Arrange
      doNothing().when(userService).delete(EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID)
          .with(csrf()));

      // Assert
      resultActions.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 404 when user not found")
    void shouldReturn404WhenDeletingNonExistingUser() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException("error.user.notFound")).when(userService)
          .delete(NON_EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc
          .perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID).with(csrf()));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }

  }

  private String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }

}
