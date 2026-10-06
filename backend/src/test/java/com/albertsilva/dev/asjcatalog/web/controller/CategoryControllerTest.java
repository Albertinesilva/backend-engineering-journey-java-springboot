package com.albertsilva.dev.dscatalog.web.controller;

import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.factory.CategoryFactory;
import com.albertsilva.dev.dscatalog.service.CategoryService;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.dscatalog.web.exception.handler.ControllerExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CategoryController.class)
@Import(ControllerExceptionHandler.class)
@DisplayName("Tests for CategoryController")
class CategoryControllerTest {

  private static final String BASE_URL = "/api/v1/categories";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CategoryService categoryService;

  private Page<CategoryResponse> page;
  private CategoryResponse categoryResponse;

  @BeforeEach
  void setUp() {
    categoryResponse = CategoryFactory.createCategoryResponse();
    page = new PageImpl<>(List.of(categoryResponse), PageRequest.of(0, 10), 1);
  }

  @Nested
  @DisplayName("POST /categories")
  class InsertTests {

    @Test
    @DisplayName("Should create category successfully")
    void createShouldReturnCreatedCategory() throws Exception {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();
      String jsonRequest = asJson(request);

      when(categoryService.create(any(CategoryCreateRequest.class))).thenReturn(categoryResponse);

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(header().exists("Location"))
          .andExpect(jsonPath("$.id").value(categoryResponse.id()))
          .andExpect(jsonPath("$.name").value(categoryResponse.name()));

      verify(categoryService).create(any(CategoryCreateRequest.class));
    }
  }

  @Nested
  @DisplayName("GET /categories")
  class FindAllTests {

    @Test
    @DisplayName("Should return paged categories when name filter is not informed")
    void findAllShouldReturnPageWhenNameFilterIsNotInformed() throws Exception {

      // Arrange
      when(categoryService.search(isNull(), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content[0].id").value(EXISTING_ID))
          .andExpect(jsonPath("$.content[0].name").value(categoryResponse.name()))
          .andExpect(jsonPath("$.totalElements").value(1));

      verify(categoryService).search(isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return filtered categories when name filter is informed")
    void findAllShouldReturnFilteredCategoriesWhenNameFilterIsInformed() throws Exception {

      // Arrange
      String name = "books";

      when(categoryService.search(eq(name), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions resultActions = mockMvc
          .perform(get(BASE_URL).param("name", name).accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content[0].id").value(EXISTING_ID))
          .andExpect(jsonPath("$.content[0].name").value(categoryResponse.name()))
          .andExpect(jsonPath("$.totalElements").value(1));

      verify(categoryService).search(eq(name), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return paged categories with pagination parameters")
    void findAllShouldReturnPagedCategoriesWithPaginationParameters() throws Exception {

      // Arrange
      when(categoryService.search(isNull(), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("page", "0")
          .param("size", "10")
          .param("sort", "name,asc")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.totalElements").value(1));

      verify(categoryService).search(isNull(), any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("GET /categories/{id}")
  class FindByIdTests {

    @Test
    @DisplayName("Should return category when id exists")
    void findByIdShouldReturnCategoryWhenIdExists() throws Exception {

      // Arrange
      when(categoryService.findById(EXISTING_ID)).thenReturn(categoryResponse);

      // Act
      ResultActions resultActions = mockMvc
          .perform(get(BASE_URL + "/{id}", EXISTING_ID).accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.name").value(categoryResponse.name()));

      verify(categoryService).findById(EXISTING_ID);
    }

    @Test
    @DisplayName("Should return 404 when id does not exist")
    void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      when(categoryService.findById(NON_EXISTING_ID))
          .thenThrow(new ResourceNotFoundException("Entity not found id: " + NON_EXISTING_ID));

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      resultActions.andExpect(status().isNotFound());

      verify(categoryService).findById(NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("PATCH /categories/{id}")
  class UpdateTests {

    @Test
    @DisplayName("Should update category when id exists")
    void updateShouldReturnUpdatedCategoryWhenIdExists() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      String jsonRequest = asJson(request);

      CategoryResponse updatedResponse = CategoryFactory.createUpdatedCategoryResponse();

      when(categoryService.update(eq(EXISTING_ID), any(CategoryUpdateRequest.class))).thenReturn(updatedResponse);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", EXISTING_ID)
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(updatedResponse.id()))
          .andExpect(jsonPath("$.name").value(updatedResponse.name()));

      verify(categoryService).update(eq(EXISTING_ID), any(CategoryUpdateRequest.class));
    }

    @Test
    @DisplayName("Should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      String jsonRequest = asJson(request);

      when(categoryService.update(eq(NON_EXISTING_ID), any(CategoryUpdateRequest.class)))
          .thenThrow(new ResourceNotFoundException("Entity not found id: " + NON_EXISTING_ID));

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", NON_EXISTING_ID)
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isNotFound());

      verify(categoryService).update(eq(NON_EXISTING_ID), any(CategoryUpdateRequest.class));
    }
  }

  @Nested
  @DisplayName("DELETE /categories/{id}")
  class DeleteTests {

    @Test
    @DisplayName("Should delete category when id exists")
    void deleteShouldReturnNoContentWhenIdExists() throws Exception {

      // Arrange
      doNothing().when(categoryService).delete(EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID));

      // Assert
      resultActions.andExpect(status().isNoContent());

      verify(categoryService).delete(EXISTING_ID);
    }

    @Test
    @DisplayName("Should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException("Entity not found id: " + NON_EXISTING_ID)).when(categoryService)
          .delete(NON_EXISTING_ID);

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      resultActions.andExpect(status().isNotFound());

      verify(categoryService).delete(NON_EXISTING_ID);
    }
  }

  private String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }
}