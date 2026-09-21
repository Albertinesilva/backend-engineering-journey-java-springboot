package com.albertsilva.dev.dscatalog.integrations.web.controller;

import static com.albertsilva.dev.dscatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.ProductFactory.NON_EXISTING_ID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.albertsilva.dev.dscatalog.domain.catalog.Product;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.factory.ProductFactory;
import com.albertsilva.dev.dscatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;

@Transactional
@DisplayName("ProductController Integration Tests")
class ProductControllerIT extends AbstractIT {

  private static final String BASE_URL = "/api/v1/products";

  @Autowired
  private ProductRepository productRepository;

  private String username;
  private String password;
  private long totalProductsCount;

  @BeforeEach
  void setUp() throws Exception {
    totalProductsCount = productRepository.count();

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
      @DisplayName("GET /products should return sorted paged products when sort by name")
      void findAllShouldReturnSortedPagedWhenSortByNameProducts() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .param("page", "0")
            .param("size", "12")
            .param("sort", "name,asc")
            .accept(MediaType.APPLICATION_JSON));

        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(totalProductsCount))
            .andExpect(jsonPath("$.content[0].name").value("Macbook Pro"))
            .andExpect(jsonPath("$.content[1].name").value("PC Gamer"))
            .andExpect(jsonPath("$.content[2].name").value("PC Gamer Alfa"))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(12));
      }

      @Test
      @DisplayName("GET /products should return paged products")
      void findAllWithPaginationShouldReturnPagedProducts() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .param("page", "0")
            .param("size", "20")
            .param("sort", "name,asc")
            .accept(MediaType.APPLICATION_JSON));

        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
      }

      @Test
      @DisplayName("GET /products with name filter should return filtered products")
      void findAllShouldReturnFilteredProductsWhenNameParameterIsInformed() throws Exception {

        ResultActions resultActions = mockMvc.perform(get(BASE_URL)
            .param("name", "pc")
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
      @DisplayName("GET /products/{id} should return product details when id exists")
      void findByIdShouldReturnProductWhenIdExists() throws Exception {

        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/{id}", EXISTING_ID).accept(MediaType.APPLICATION_JSON));

        resultActions
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(EXISTING_ID))
            .andExpect(jsonPath("$.name").isNotEmpty())
            .andExpect(jsonPath("$.description").isNotEmpty())
            .andExpect(jsonPath("$.price").isNumber());
      }

      @Test
      @DisplayName("GET /products/{id} should return 404 when id does not exist")
      void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

        ResultActions resultActions = mockMvc
            .perform(get(BASE_URL + "/{id}", NON_EXISTING_ID).accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isNotFound());
      }
    }
  }

  @Nested
  @DisplayName("CREATE Operations")
  class CreateOperations {

    @Test
    @DisplayName("POST /products should create product with valid data")
    void insertShouldCreateProductWithValidData() throws Exception {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();
      String jsonRequest = asJson(request);
      long initialCount = productRepository.count();

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
          .andExpect(jsonPath("$.description").value(request.description()))
          .andExpect(jsonPath("$.price").value(request.price()))
          .andExpect(jsonPath("$.date").doesNotExist());

      assertEquals(productRepository.count(), initialCount + 1);
    }

    @Test
    @DisplayName("POST /products should accept date but discard it and fill audit dates on persistence")
    void insertShouldDiscardDateAndFillAuditDatesOnPersistence() throws Exception {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions.andExpect(status().isCreated());

      Long id = objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("id")
          .asLong();
      Product persisted = productRepository.findById(id).orElseThrow();

      assertNotNull(persisted.getCreatedAt());
      assertNotNull(persisted.getUpdatedAt());
      assertNotEquals(request.date(), persisted.getCreatedAt());
      assertEquals(persisted.getCreatedAt(), persisted.getUpdatedAt());
      assertEquals(true, persisted.isActive());
    }
  }

  @Nested
  @DisplayName("UPDATE Operations")
  class UpdateOperations {

    @Test
    @DisplayName("PUT /products/{id} should update product when id exists")
    void updateShouldReturnProductResponseWhenIdExists() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
      String jsonRequest = asJson(request);

      String expectedName = request.name();
      String expectedDescription = request.description();

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
          .andExpect(jsonPath("$.name").value(expectedName))
          .andExpect(jsonPath("$.description").value(expectedDescription));
    }

    @Test
    @DisplayName("PUT /products/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
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
    @DisplayName("PATCH /products/{id}/activate should activate product when id exists")
    void activateShouldActivateProductWhenIdExists() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(true, productRepository.findById(EXISTING_ID).get().isActive());
    }

    @Test
    @DisplayName("PATCH /products/{id}/activate should return 404 when id does not exist")
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
    @DisplayName("PATCH /products/{id}/deactivate should deactivate product when id exists")
    void deactivateShouldDeactivateProductWhenIdExists() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(false, productRepository.findById(EXISTING_ID).get().isActive());
    }

    @Test
    @DisplayName("PATCH /products/{id}/deactivate should return 404 when id does not exist")
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
    @DisplayName("DELETE /products/{id} should delete product when id exists")
    void deleteShouldRemoveProductWhenIdExists() throws Exception {

      // Arrange
      long initialCount = productRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertEquals(productRepository.count(), initialCount - 1);
      assertFalse(productRepository.existsById(EXISTING_ID));
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }
}