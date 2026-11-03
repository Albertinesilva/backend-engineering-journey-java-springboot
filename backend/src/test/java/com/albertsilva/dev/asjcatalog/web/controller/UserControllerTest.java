package com.albertsilva.dev.asjcatalog.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectNotFound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.repository.RoleRepository;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;
import com.albertsilva.dev.asjcatalog.security.oauth2.resource.config.ResourceServerConfig;
import com.albertsilva.dev.asjcatalog.service.UserService;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.asjcatalog.utils.MxRecordLookupTestConfig;
import com.albertsilva.dev.asjcatalog.web.exception.handler.ControllerExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@Import({ ControllerExceptionHandler.class, ResourceServerConfig.class, MxRecordLookupTestConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.h2.console.enabled=false" })
@DisplayName("Tests for UserController")
class UserControllerTest {

  private static final String BASE_URL = "/api/v1/users";
  private static final String NOT_FOUND_KEY = "error.user.notFound";

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

  private UserResponse userResponse;

  @BeforeEach
  void setUp() {
    userResponse = UserFactory.createUserResponse();

    // Os validadores do corpo (@UniqueEmail, @UserUpdateValid e @ValidRoles) consultam os repositórios.
    when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
    when(userRepository.existsByEmailIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
    when(roleRepository.existsById(RoleFactory.OPERATOR_ROLE_ID)).thenReturn(true);
  }

  private String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }

  @Nested
  @DisplayName("POST /users")
  class CreateOperations {

    @Test
    @WithMockUser(roles = { "ADMIN" })
    @DisplayName("POST /users should return 201 with location when request is valid")
    void createShouldReturnCreatedWithLocationWhenRequestIsValid() throws Exception {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      when(userService.create(request)).thenReturn(userResponse);

      // Act
      ResultActions result = mockMvc.perform(post(BASE_URL)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "http://localhost" + BASE_URL + "/" + userResponse.id()))
          .andExpect(jsonPath("$.id").value(userResponse.id()))
          .andExpect(jsonPath("$.firstName").value(userResponse.firstName()))
          .andExpect(jsonPath("$.email").value(userResponse.email()));
      verify(userService).create(request);
    }
  }

  @Nested
  @DisplayName("GET /users")
  class FindAllOperations {

    // #28: o formato JSON da página é o do Page serializado diretamente, que a issue
    // prevê trocar por um formato estável. O teste confere o formato atual sem reforço.
    @Test
    @WithMockUser(roles = { "ADMIN" })
    @DisplayName("GET /users should forward pagination to service when page, size and sort are informed")
    void findAllShouldForwardPaginationToServiceWhenPageSizeAndSortAreInformed() throws Exception {

      // Arrange
      Page<UserResponse> page = new PageImpl<>(List.of(userResponse), PageRequest.of(0, 10), 1);
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      when(userService.search(isNull(), pageableCaptor.capture())).thenReturn(page);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL)
          .param("page", "2")
          .param("size", "12")
          .param("sort", "firstName,asc")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(userResponse.id()))
          .andExpect(jsonPath("$.totalElements").value(1));
      Pageable pageable = pageableCaptor.getValue();
      assertThat(pageable.getPageNumber()).isEqualTo(2);
      assertThat(pageable.getPageSize()).isEqualTo(12);
      assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "firstName"));
    }
  }

  @Nested
  @DisplayName("GET /users/{id}")
  class FindByIdOperations {

    @Test
    @WithMockUser(roles = { "ADMIN" })
    @DisplayName("GET /users/{id} should return user details when id exists")
    void findByIdShouldReturnUserDetailsWhenIdExists() throws Exception {

      // Arrange
      UserDetailsResponse response = UserFactory.createUserDetailsResponse();

      when(userService.findById(EXISTING_ID)).thenReturn(response);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", EXISTING_ID)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(response.id()))
          .andExpect(jsonPath("$.firstName").value(response.firstName()))
          .andExpect(jsonPath("$.email").value(response.email()))
          .andExpect(jsonPath("$.active").value(response.active()));
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    @DisplayName("GET /users/{id} should return 404 when id does not exist")
    void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      when(userService.findById(NON_EXISTING_ID)).thenThrow(new ResourceNotFoundException(NOT_FOUND_KEY));

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }

  /**
   * Testes de {@code PUT /users/{id}}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>OPERATOR atualizando o próprio id: recebe 403, porque o
   * {@code @PreAuthorize} compara o id com {@code principal.id}, que é o
   * {@code jti} do token (#8).</li>
   * </ul>
   */
  @Nested
  @DisplayName("PUT /users/{id}")
  @WithMockUser(roles = "ADMIN")
  class UpdateOperations {

    @Test
    @DisplayName("PUT /users/{id} should return updated user when id exists")
    void updateShouldReturnUpdatedUserWhenIdExists() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();
      UserResponse updatedResponse = UserFactory.createUpdatedUserResponse();

      when(userService.update(EXISTING_ID, request)).thenReturn(updatedResponse);

      // Act
      ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(updatedResponse.id()))
          .andExpect(jsonPath("$.firstName").value(updatedResponse.firstName()))
          .andExpect(jsonPath("$.lastName").value(updatedResponse.lastName()));
      verify(userService).update(EXISTING_ID, request);
    }

    @Test
    @DisplayName("PUT /users/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      when(userService.update(NON_EXISTING_ID, request)).thenThrow(new ResourceNotFoundException(NOT_FOUND_KEY));

      // Act
      ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", NON_EXISTING_ID)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("PATCH /users/{id}/activate")
  @WithMockUser(roles = "ADMIN")
  class ActivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/activate should return 204 when id exists")
    void activateShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(userService).activate(EXISTING_ID);
    }

    @Test
    @DisplayName("PATCH /users/{id}/activate should return 404 when id does not exist")
    void activateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(userService).activate(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID + "/activate");
    }
  }

  @Nested
  @DisplayName("PATCH /users/{id}/deactivate")
  @WithMockUser(roles = "ADMIN")
  class DeactivateOperations {

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should return 204 when id exists")
    void deactivateShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(userService).deactivate(EXISTING_ID);
    }

    @Test
    @DisplayName("PATCH /users/{id}/deactivate should return 404 when id does not exist")
    void deactivateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(userService).deactivate(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID + "/deactivate");
    }
  }

  @Nested
  @DisplayName("DELETE /users/{id}")
  @WithMockUser(roles = "ADMIN")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /users/{id} should return 204 when id exists")
    void deleteShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(userService).delete(EXISTING_ID);
    }

    @Test
    @DisplayName("DELETE /users/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(userService).delete(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }
}
