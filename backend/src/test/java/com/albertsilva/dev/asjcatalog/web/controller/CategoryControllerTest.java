package com.albertsilva.dev.asjcatalog.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NAME_FILTER;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectNotFound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;
import com.albertsilva.dev.asjcatalog.security.oauth2.resource.config.ResourceServerConfig;
import com.albertsilva.dev.asjcatalog.service.CategoryService;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.asjcatalog.web.exception.handler.ControllerExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CategoryController.class)
@Import({ ControllerExceptionHandler.class, ResourceServerConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.h2.console.enabled=false" })
@DisplayName("Tests for CategoryController")
class CategoryControllerTest {

  private static final String BASE_URL = "/api/v1/categories";
  private static final String NOT_FOUND_KEY = "error.category.notFound";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CategoryService categoryService;

  @MockitoBean
  private CategoryRepository categoryRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  private Page<CategoryResponse> page;
  private CategoryResponse categoryResponse;

  @BeforeEach
  void setUp() {
    categoryResponse = CategoryFactory.createCategoryResponse();
    page = new PageImpl<>(List.of(categoryResponse), PageRequest.of(0, 10), 1);

    // Os validadores de nome único (@CategoryCreateValid e @CategoryUpdateValid) consultam o repositório.
    when(categoryRepository.existsByNameIgnoreCase(anyString())).thenReturn(false);
    when(categoryRepository.existsByNameIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
  }

  private String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }

  @Nested
  @DisplayName("POST /categories")
  class CreateOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("POST /categories should return 201 with location when request is valid")
    void createShouldReturnCreatedWithLocationWhenRequestIsValid() throws Exception {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();

      when(categoryService.create(request)).thenReturn(categoryResponse);

      // Act
      ResultActions result = mockMvc.perform(post(BASE_URL)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "http://localhost" + BASE_URL + "/" + categoryResponse.id()))
          .andExpect(jsonPath("$.id").value(categoryResponse.id()))
          .andExpect(jsonPath("$.name").value(categoryResponse.name()));
      verify(categoryService).create(request);
    }

    @Test
    @DisplayName("POST /categories should return 401 when request is not authenticated")
    void createShouldReturnUnauthorizedWhenRequestIsNotAuthenticated() throws Exception {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();

      // Act
      ResultActions result = mockMvc.perform(post(BASE_URL)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isUnauthorized());
      verify(categoryService, never()).create(any(CategoryCreateRequest.class));
    }
  }

  @Nested
  @DisplayName("GET /categories")
  class FindAllOperations {

    // #28: o formato JSON da página é o do Page serializado diretamente, que a issue
    // prevê trocar por um formato estável. Os testes conferem o formato atual sem reforço.
    @Test
    @DisplayName("GET /categories should return all categories when name is not informed")
    void findAllShouldReturnAllCategoriesWhenNameIsNotInformed() throws Exception {

      // Arrange
      when(categoryService.search(isNull(), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(categoryResponse.id()))
          .andExpect(jsonPath("$.content[0].name").value(categoryResponse.name()))
          .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /categories should forward name to service when name is informed")
    void findAllShouldForwardNameToServiceWhenNameIsInformed() throws Exception {

      // Arrange
      when(categoryService.search(eq(NAME_FILTER), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL).param("name", NAME_FILTER)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].name").value(categoryResponse.name()));
      verify(categoryService).search(eq(NAME_FILTER), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /categories should forward pagination to service when page, size and sort are informed")
    void findAllShouldForwardPaginationToServiceWhenPageSizeAndSortAreInformed() throws Exception {

      // Arrange
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      when(categoryService.search(isNull(), pageableCaptor.capture())).thenReturn(page);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL)
          .param("page", "2")
          .param("size", "12")
          .param("sort", "name,desc")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isOk());
      Pageable pageable = pageableCaptor.getValue();
      assertThat(pageable.getPageNumber()).isEqualTo(2);
      assertThat(pageable.getPageSize()).isEqualTo(12);
      assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "name"));
    }
  }

  @Nested
  @DisplayName("GET /categories/{id}")
  class FindByIdOperations {

    @Test
    @DisplayName("GET /categories/{id} should return category when id exists")
    void findByIdShouldReturnCategoryWhenIdExists() throws Exception {

      // Arrange
      CategoryDetailsResponse response = CategoryFactory.createCategoryDetailsResponse();

      when(categoryService.findById(EXISTING_ID)).thenReturn(response);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", EXISTING_ID)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(response.id()))
          .andExpect(jsonPath("$.name").value(response.name()))
          .andExpect(jsonPath("$.description").value(response.description()));
    }

    @Test
    @DisplayName("GET /categories/{id} should return 404 when id does not exist")
    void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      when(categoryService.findById(NON_EXISTING_ID)).thenThrow(new ResourceNotFoundException(NOT_FOUND_KEY));

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }

  /**
   * Testes de {@code PATCH /categories/{id}}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Atualização parcial sem {@code name}: apesar de ser um {@code PATCH}, o
   * DTO exige o nome e responde 422; o padrão de atualização ainda não foi
   * decidido (#28).</li>
   * </ul>
   */
  @Nested
  @DisplayName("PATCH /categories/{id}")
  class UpdateOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /categories/{id} should return updated category when id exists")
    void updateShouldReturnUpdatedCategoryWhenIdExists() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      CategoryResponse updatedResponse = CategoryFactory.createUpdatedCategoryResponse();

      when(categoryService.update(EXISTING_ID, request)).thenReturn(updatedResponse);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}", EXISTING_ID)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(updatedResponse.id()))
          .andExpect(jsonPath("$.name").value(updatedResponse.name()));
      verify(categoryService).update(EXISTING_ID, request);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /categories/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      when(categoryService.update(NON_EXISTING_ID, request)).thenThrow(new ResourceNotFoundException(NOT_FOUND_KEY));

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}", NON_EXISTING_ID)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("PATCH /categories/{id}/activate")
  class ActivateOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /categories/{id}/activate should return 204 when id exists")
    void activateShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(categoryService).activate(EXISTING_ID);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /categories/{id}/activate should return 404 when id does not exist")
    void activateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(categoryService).activate(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID + "/activate");
    }
  }

  @Nested
  @DisplayName("PATCH /categories/{id}/deactivate")
  class DeactivateOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /categories/{id}/deactivate should return 204 when id exists")
    void deactivateShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(categoryService).deactivate(EXISTING_ID);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /categories/{id}/deactivate should return 404 when id does not exist")
    void deactivateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(categoryService).deactivate(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID + "/deactivate");
    }
  }

  @Nested
  @DisplayName("DELETE /categories/{id}")
  class DeleteOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("DELETE /categories/{id} should return 204 when id exists")
    void deleteShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(categoryService).delete(EXISTING_ID);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("DELETE /categories/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(categoryService).delete(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }
}
