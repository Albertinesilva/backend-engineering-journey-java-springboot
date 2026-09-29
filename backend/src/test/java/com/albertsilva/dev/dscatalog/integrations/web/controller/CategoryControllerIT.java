package com.albertsilva.dev.dscatalog.integrations.web.controller;

import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_DEPENDENT_ID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.domain.catalog.Category;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.factory.CategoryFactory;
import com.albertsilva.dev.dscatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;

@Transactional
@DisplayName("CategoryController Integration Tests")
class CategoryControllerIT extends AbstractIT {

  private static final String BASE_URL = "/api/v1/categories";

  @Autowired
  private CategoryRepository categoryRepository;

  private String username;
  private String password;
  private long totalCategoriesCount;

  @BeforeEach
  void setUp() throws Exception {
    totalCategoriesCount = categoryRepository.count();

    username = "maria@gmail.com";
    password = "123456";
    bearerToken = tokenUtil.obtainAccessToken(mockMvc, username, password);
  }

  @Nested
  @DisplayName("READ Operations")
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
            .andExpect(jsonPath("$.content[0].name").value("Audio"))
            .andExpect(jsonPath("$.content[1].name").value("Automotive"))
            .andExpect(jsonPath("$.content[2].name").value("Automotive Tools"))
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
      @DisplayName("GET /categories with name filter should return only categories whose name contains the filter")
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
            .andExpect(jsonPath("$.content").isNotEmpty())
            .andExpect(jsonPath("$.content[*].name").value(everyItem(containsStringIgnoringCase("book"))))
            .andExpect(jsonPath("$.content[*].name").value(hasItem("Books")));
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
      @DisplayName("GET /categories/{id} should return 404 with RESOURCE_NOT_FOUND code and translated message when id does not exist")
      void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

        // Act
        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/{id}", NON_EXISTING_ID).header(HttpHeaders.ACCEPT_LANGUAGE, "en")
            .accept(MediaType.APPLICATION_JSON));

        // Assert
        resultActions
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Category not found"));
      }
    }

  }

  @Nested
  @DisplayName("CREATE Operations")
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
          .with(bearerToken())
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(header().exists("Location"))
          .andExpect(jsonPath("$.id").isNotEmpty())
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.description").doesNotExist());

      Long id = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("id")
          .asLong();

      assertEquals(categoryRepository.count(), initialCount + 1);
      assertEquals(request.description(), categoryRepository.findById(id).orElseThrow().getDescription());
    }

    @Test
    @DisplayName("POST /categories should return 422 with the translated field error when name is blank and create nothing")
    void createShouldReturnUnprocessableEntityWhenNameIsBlank() throws Exception {

      // Arrange
      CategoryCreateRequest request = new CategoryCreateRequest("", null);
      long initialCount = categoryRepository.count();

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
          .andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'name')].message").value(hasItem("Category name is required")));

      assertEquals(initialCount, categoryRepository.count());
    }
  }

  @Nested
  @DisplayName("UPDATE Operations")
  class UpdateOperations {

    @Test
    @DisplayName("PATCH /categories/{id} should update category when id exists")
    void updateShouldUpdateCategoryWhenIdExists() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      String jsonRequest = asJson(request);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .content(jsonRequest)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.description").doesNotExist());

      assertEquals(request.description(), categoryRepository.findById(EXISTING_ID).orElseThrow().getDescription());
    }

    @Test
    @DisplayName("PATCH /categories/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
      String jsonRequest = asJson(request);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", NON_EXISTING_ID)
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
    @DisplayName("PATCH /categories/{id}/activate should activate an inactive category")
    void activateShouldActivateCategoryWhenCategoryIsInactive() throws Exception {

      // Arrange
      Category category = categoryRepository.findById(EXISTING_ID).orElseThrow();
      category.setActive(false);
      categoryRepository.saveAndFlush(category);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertTrue(categoryRepository.findById(EXISTING_ID).orElseThrow().isActive());
    }

    @Test
    @DisplayName("PATCH /categories/{id}/activate should return 404 when id does not exist")
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
    @DisplayName("PATCH /categories/{id}/deactivate should deactivate category when id exists")
    void deactivateShouldDeactivateCategoryWhenIdExists() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(false, categoryRepository.findById(EXISTING_ID).get().isActive());
    }

    @Test
    @DisplayName("PATCH /categories/{id}/deactivate should return 404 when id does not exist")
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
    @DisplayName("DELETE /categories/{id} should delete category when id exists and has no dependencies")
    void deleteShouldRemoveCategoryWhenIdExistsAndHasNoDependencies() throws Exception {

      // Arrange
      long initialCount = categoryRepository.count();
      assertEquals(categoryRepository.existsById(NON_DEPENDENT_ID), true);

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_DEPENDENT_ID)
          .with(bearerToken())
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(categoryRepository.count(), initialCount - 1);
      assertFalse(categoryRepository.existsById(NON_DEPENDENT_ID));
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken())
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }
}