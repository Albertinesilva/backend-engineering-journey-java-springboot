package com.albertsilva.dev.asjcatalog.integrations.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NON_DEPENDENT_ID;
import static com.albertsilva.dev.asjcatalog.factory.CategoryFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;

/**
 * Testes de integração dos endpoints de {@code /api/v1/categories}, com o
 * contexto completo, o banco H2 e o token de uma usuária ADMIN.
 *
 * <p>
 * Dependem do seed ({@code import.sql}): o login de {@code maria@gmail.com}
 * com a senha do seed, os ids fixos de {@code CategoryFactory} e os nomes
 * esperados na ordenação (#21).
 * </p>
 */
@Transactional
@DisplayName("Integration tests for CategoryController")
class CategoryControllerIT extends AbstractIT {

  private static final String BASE_URL = "/api/v1/categories";

  @Autowired
  private CategoryRepository categoryRepository;

  private long totalCategoriesCount;

  @BeforeEach
  void setUp() throws Exception {
    totalCategoriesCount = categoryRepository.count();
    bearerToken = tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, SEED_PASSWORD);
  }

  @Nested
  @DisplayName("GET /categories")
  class FindAllOperations {

    @Test
    @DisplayName("GET /categories should return categories sorted by name when sort is name ascending")
    void findAllShouldReturnCategoriesSortedByNameWhenSortIsNameAscending() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("page", "0")
          .param("size", "12")
          .param("sort", "name,asc")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(totalCategoriesCount))
          .andExpect(jsonPath("$.content.length()").value(12))
          .andExpect(jsonPath("$.content[0].name").value("Audio"))
          .andExpect(jsonPath("$.content[1].name").value("Automotive"))
          .andExpect(jsonPath("$.content[2].name").value("Automotive Tools"))
          .andExpect(jsonPath("$.number").value(0))
          .andExpect(jsonPath("$.size").value(12));
    }

    @Test
    @DisplayName("GET /categories should return requested page when page and size are informed")
    void findAllShouldReturnRequestedPageWhenPageAndSizeAreInformed() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("page", "1")
          .param("size", "10")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.length()").value(10))
          .andExpect(jsonPath("$.number").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.totalElements").value(totalCategoriesCount));
    }

    @Test
    @DisplayName("GET /categories should return only matching categories when name is informed")
    void findAllShouldReturnOnlyMatchingCategoriesWhenNameIsInformed() throws Exception {

      // Arrange
      Category created = categoryRepository.save(new Category("Bookshelf Test", null, true));

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("name", "book")
          .param("page", "0")
          .param("size", "10")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[*].name").value(hasItem(created.getName())))
          .andExpect(jsonPath("$.content[*].name").value(everyItem(containsStringIgnoringCase("book"))));
    }
  }

  @Nested
  @DisplayName("GET /categories/{id}")
  class FindByIdOperations {

    @Test
    @DisplayName("GET /categories/{id} should return category when id exists")
    void findByIdShouldReturnCategoryWhenIdExists() throws Exception {

      // Arrange
      Category expected = categoryRepository.findById(EXISTING_ID).orElseThrow();

      // Act
      ResultActions resultActions = mockMvc
          .perform(get(BASE_URL + "/{id}", EXISTING_ID).accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.name").value(expected.getName()))
          .andExpect(jsonPath("$.active").value(expected.isActive()));
    }

    @Test
    @DisplayName("GET /categories/{id} should return 404 with translated message when id does not exist")
    void findByIdShouldReturnNotFoundWithTranslatedMessageWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID)
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
      resultActions.andExpect(jsonPath("$.message").value("Category not found."));
    }
  }

  @Nested
  @DisplayName("POST /categories")
  class CreateOperations {

    @Test
    @DisplayName("POST /categories should return 201 with location and persist category when request is valid")
    void createShouldReturnCreatedWithLocationAndPersistCategoryWhenRequestIsValid() throws Exception {

      // Arrange
      CategoryCreateRequest request = CategoryFactory.createCategoryCreateRequest();
      long initialCount = categoryRepository.count();

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
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.description").doesNotExist());

      Long id = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("id")
          .asLong();
      resultActions.andExpect(header().string("Location", "http://localhost" + BASE_URL + "/" + id));

      assertThat(categoryRepository.count()).isEqualTo(initialCount + 1);
      assertThat(categoryRepository.findById(id).orElseThrow())
          .extracting(Category::getName, Category::getDescription)
          .containsExactly(request.name(), request.description());
    }

    @Test
    @DisplayName("POST /categories should return 422 with translated field error and create nothing when name is blank")
    void createShouldReturnUnprocessableEntityAndCreateNothingWhenNameIsBlank() throws Exception {

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
      expectValidationError(resultActions, BASE_URL, "name");
      resultActions.andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'name')].message")
          .value(hasItem("Category name is required.")));

      assertThat(categoryRepository.count()).isEqualTo(initialCount);
    }
  }

  @Nested
  @DisplayName("PATCH /categories/{id}")
  class UpdateOperations {

    @Test
    @DisplayName("PATCH /categories/{id} should return 200 and persist changes when id exists")
    void updateShouldReturnOkAndPersistChangesWhenIdExists() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.description").doesNotExist());

      assertThat(categoryRepository.findById(EXISTING_ID).orElseThrow())
          .extracting(Category::getName, Category::getDescription)
          .containsExactly(request.name(), request.description());
    }

    @Test
    @DisplayName("PATCH /categories/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("PATCH /categories/{id}/activate")
  class ActivateOperations {

    @Test
    @DisplayName("PATCH /categories/{id}/activate should return 204 and activate category when category is inactive")
    void activateShouldReturnNoContentAndActivateCategoryWhenCategoryIsInactive() throws Exception {

      // Arrange
      Category category = categoryRepository.findById(EXISTING_ID).orElseThrow();
      category.setActive(false);
      categoryRepository.saveAndFlush(category);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(categoryRepository.findById(EXISTING_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("PATCH /categories/{id}/activate should return 404 when id does not exist")
    void activateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID + "/activate");
    }
  }

  @Nested
  @DisplayName("PATCH /categories/{id}/deactivate")
  class DeactivateOperations {

    @Test
    @DisplayName("PATCH /categories/{id}/deactivate should return 204 and deactivate category when category is active")
    void deactivateShouldReturnNoContentAndDeactivateCategoryWhenCategoryIsActive() throws Exception {

      // Arrange
      assertThat(categoryRepository.findById(EXISTING_ID).orElseThrow().isActive()).isTrue();

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(categoryRepository.findById(EXISTING_ID).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("PATCH /categories/{id}/deactivate should return 404 when id does not exist")
    void deactivateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID + "/deactivate");
    }
  }

  @Nested
  @DisplayName("DELETE /categories/{id}")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /categories/{id} should return 204 and remove category when category has no dependencies")
    void deleteShouldReturnNoContentAndRemoveCategoryWhenCategoryHasNoDependencies() throws Exception {

      // Arrange
      long initialCount = categoryRepository.count();
      assertThat(categoryRepository.existsById(NON_DEPENDENT_ID)).isTrue();

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_DEPENDENT_ID)
          .with(bearerToken())
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(categoryRepository.count()).isEqualTo(initialCount - 1);
      assertThat(categoryRepository.existsById(NON_DEPENDENT_ID)).isFalse();
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken())
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }
}
