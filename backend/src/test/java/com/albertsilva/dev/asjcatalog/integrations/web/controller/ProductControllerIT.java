package com.albertsilva.dev.dscatalog.integrations.web.controller;

import static com.albertsilva.dev.dscatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.ProductFactory.NON_EXISTING_ID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.factory.ProductFactory;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("ProductController Integration Tests")
class ProductControllerIT {

  private static final String BASE_URL = "/api/v1/products";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProductRepository productRepository;

  private long totalProductsCount;

  @BeforeEach
  void setUp() {
    totalProductsCount = productRepository.count();
  }

  @Nested
  @DisplayName("READ Operations")
  class ReadOperations {

    @Test
    @DisplayName("GET /products should return sorted paged products when sort by name")
    void findAllShouldReturnSortedPagedWhenSortByNameProducts() throws Exception {

      ResultActions resultActions = mockMvc
          .perform(get(BASE_URL + "?page=0&size=12&sort=name,asc").accept(MediaType.APPLICATION_JSON));

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
    void findAllWithNameFilterShouldReturnFilteredProducts() throws Exception {

      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("name", "pc")
          .param("page", "0")
          .param("size", "10")
          .accept(MediaType.APPLICATION_JSON));

      resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /products/{id} should return product details when id exists")
    void findByIdShouldReturnProductDetailsWhenIdExists() throws Exception {

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

  @Nested
  @DisplayName("CREATE Operations")
  class CreateOperations {

    @Test
    @DisplayName("POST /products should insert product and return 201")
    void insertShouldCreateProductAndReturnCreated() throws Exception {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();
      String jsonRequest = asJson(request);
      long initialCount = productRepository.count();

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
          .andExpect(jsonPath("$.description").value(request.description()))
          .andExpect(jsonPath("$.price").value(request.price()));

      assert productRepository.count() == initialCount + 1;
    }

    @Test
    @DisplayName("POST /products should create product with valid data")
    void insertShouldCreateProductWithValidData() throws Exception {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();
      String jsonRequest = asJson(request);
      long initialCount = productRepository.count();

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

      assert productRepository.count() == initialCount + 1;
    }
  }

  @Nested
  @DisplayName("UPDATE Operations")
  class UpdateOperations {

    @Test
    @DisplayName("PATCH /products/{id} should update product when id exists")
    void updateShouldReturnProductResponseWhenIdExists() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
      String jsonRequest = asJson(request);

      String expectedName = request.name();
      String expectedDescription = request.description();

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}", EXISTING_ID)
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
    @DisplayName("PATCH /products/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
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
  @DisplayName("DELETE Operations")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /products/{id} should delete product when id exists")
    void deleteShouldRemoveProductWhenIdExists() throws Exception {

      // Arrange
      long initialCount = productRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assert productRepository.count() == initialCount - 1;
      assert !productRepository.existsById(EXISTING_ID);
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      resultActions.andExpect(status().isNotFound());
    }
  }

  private String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }
}