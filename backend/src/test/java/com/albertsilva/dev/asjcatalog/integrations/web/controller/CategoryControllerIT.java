package com.albertsilva.dev.dscatalog.integrations.web.controller;

import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_DEPENDENT_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.DEPENDENT_ID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.factory.CategoryFactory;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("CategoryController Integration Tests")
class CategoryControllerIT {

  private static final String BASE_URL = "/api/v1/categories";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CategoryRepository categoryRepository;

  private long totalCategoriesCount;

  @BeforeEach
  void setUp() {
    totalCategoriesCount = categoryRepository.count();
  }

  @Nested
  @DisplayName("Read Operations")
  class ReadOperations {

    @Nested
    @DisplayName("FindAll Operations")
    class FindAllOperations {

      @Test
      @DisplayName("GET /categories should return sorted paged categories when sort by name")
      void findAllShouldReturnSortedPagedWhenSortByNameCategories() throws Exception {

        // Act
        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .param("page", "0")
            .param("size", "12")
            .param("sort", "name,asc")
            .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(totalCategoriesCount))
            .andExpect(jsonPath("$.content[0].name").value("Automotive"))
            .andExpect(jsonPath("$.content[1].name").value("Beauty"))
            .andExpect(jsonPath("$.content[2].name").value("Books"))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(12));
      }

      @Test
      @DisplayName("GET /categories with pagination parameters should return paged categories")
      void findAllWithPaginationShouldReturnPagedCategories() throws Exception {

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
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(totalCategoriesCount));
      }

      @Test
      @DisplayName("GET /categories should return filtered categories when name parameter is informed")
      void findAllShouldReturnFilteredCategoriesWhenNameParameterIsInformed() throws Exception {

        // Act
        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .param("name", "book")
            .param("page", "0")
            .param("size", "10")
            .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].name").value("Books"));
      }
    }

    @Nested
    @DisplayName("FindById Operations")
    class FindByIdOperations {

      @Test
      @DisplayName("GET /categories/{id} should return category when id exists")
      void findByIdShouldReturnCategoryWhenIdExists() throws Exception {

        // Act
        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/{id}", EXISTING_ID).accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(EXISTING_ID))
            .andExpect(jsonPath("$.name").isNotEmpty());
      }

      @Test
      @DisplayName("GET /categories/{id} should return 404 when id does not exist")
      void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

        // Act
        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/{id}", NON_EXISTING_ID).accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions.andExpect(status().isNotFound());
      }
    }

  }

  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("POST /categories should insert category and return 201")
    void insertShouldCreateCategoryAndReturnCreated() throws Exception {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();
      String jsonRequest = asJson(request);
      long initialCount = categoryRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(header().exists("Location"))
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.description").value(request.description()));

      assert categoryRepository.count() == initialCount + 1;
    }

    @Test
    @DisplayName("POST /categories should create category with valid data")
    void insertShouldCreateCategoryWithValidData() throws Exception {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();
      String jsonRequest = asJson(request);
      long initialCount = categoryRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(header().exists("Location"))
          .andExpect(jsonPath("$.name").value(request.name()));

      assert categoryRepository.count() == initialCount + 1;
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("PATCH /categories/{id} should update category when id exists")
    void updateShouldUpdateCategoryWhenIdExists() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      String jsonRequest = asJson(request);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", EXISTING_ID)
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.description").value(request.description()));
    }

    @Test
    @DisplayName("PATCH /categories/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      String jsonRequest = asJson(request);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", NON_EXISTING_ID)
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /categories/{id} should delete category when id exists and has no dependencies")
    void deleteShouldRemoveCategoryWhenIdExistsAndHasNoDependencies() throws Exception {

      // Arrange
      long initialCount = categoryRepository.count();
      assert categoryRepository.existsById(NON_DEPENDENT_ID);

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_DEPENDENT_ID));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assert categoryRepository.count() == initialCount - 1;
      assert !categoryRepository.existsById(NON_DEPENDENT_ID);
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 409 when category has associated products")
    void deleteShouldReturnBadRequestWhenCategoryHasAssociatedProducts() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", DEPENDENT_ID));

      // Assert
      resultActions.andExpect(status().isConflict());
    }
  }

  private String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }
}
