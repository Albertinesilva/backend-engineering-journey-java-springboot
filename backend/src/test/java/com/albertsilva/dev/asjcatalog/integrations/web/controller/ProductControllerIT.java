package com.albertsilva.dev.asjcatalog.integrations.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.SEED_PASSWORD;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectNotFound;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectValidationError;
import static com.albertsilva.dev.asjcatalog.utils.JsonBodies.withMissingField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.asjcatalog.domain.catalog.Product;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.asjcatalog.repository.ProductRepository;
import com.albertsilva.dev.asjcatalog.utils.JsonBodies.MissingValue;

/**
 * Testes de integração dos endpoints de {@code /api/v1/products}, com o
 * contexto completo, o banco H2 e o token de uma usuária ADMIN.
 *
 * <p>
 * Dependem do seed ({@code import.sql}): o login de {@code maria@gmail.com}
 * com a senha do seed, os ids fixos de produtos e de categorias, os nomes
 * esperados nas ordenações e os totais por categoria (#21).
 * </p>
 */
@Transactional
@DisplayName("Integration tests for ProductController")
class ProductControllerIT extends AbstractIT {

  private static final String BASE_URL = "/api/v1/products";

  /** Mensagens de preço em inglês ({@code Accept-Language: en}). */
  private static final String PRICE_REQUIRED_MESSAGE = "Product price is required.";
  private static final String PRICE_POSITIVE_MESSAGE = "Price must be a positive value.";
  private static final String PRICE_DIGITS_MESSAGE = "Price must have at most 8 integer digits and 2 decimal places.";

  @Autowired
  private ProductRepository productRepository;

  private long totalProductsCount;

  @BeforeEach
  void setUp() throws Exception {
    totalProductsCount = productRepository.count();
    bearerToken = tokenUtil.obtainAccessToken(mockMvc, EXISTING_EMAIL, SEED_PASSWORD);
  }

  private Long idFrom(ResultActions resultActions) throws Exception {
    return objectMapper.readTree(resultActions.andReturn().getResponse().getContentAsString()).get("id").asLong();
  }

  @Nested
  @DisplayName("GET /products")
  class FindAllOperations {

    @Test
    @DisplayName("GET /products should return products sorted by name when sort is name ascending")
    void findAllShouldReturnProductsSortedByNameWhenSortIsNameAscending() throws Exception {

      // Arrange
      // Nomes com acentos lidos do repositório (ids 155 e 162): literais Java com certos
      // caracteres acentuados são corrompidos pelo compilador deste ambiente mesmo com o
      // projeto configurado para UTF-8; os valores persistidos são lidos corretamente via
      // JDBC, então a comparação permanece exata.
      String expectedSecondName = productRepository.findById(155L).orElseThrow().getName();
      String expectedThirdName = productRepository.findById(162L).orElseThrow().getName();

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("page", "0")
          .param("size", "12")
          .param("sort", "name,asc")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(totalProductsCount))
          .andExpect(jsonPath("$.content.length()").value(12))
          .andExpect(jsonPath("$.content[0].name").value("Air Fryer 5L Digital"))
          .andExpect(jsonPath("$.content[1].name").value(expectedSecondName))
          .andExpect(jsonPath("$.content[2].name").value(expectedThirdName))
          .andExpect(jsonPath("$.number").value(0))
          .andExpect(jsonPath("$.size").value(12));
    }

    @Test
    @DisplayName("GET /products should return only products of the category when one categoryId is informed")
    void findAllShouldReturnOnlyProductsOfTheCategoryWhenOneCategoryIdIsInformed() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("categoryIds", "2")
          .param("page", "0")
          .param("size", "5")
          .param("sort", "name,asc")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(27))
          .andExpect(jsonPath("$.content[*].name").value(contains("Clean Architecture", "Clean Code",
              "Continuous Delivery", "Design Patterns: Elements of Reusable Object-Oriented Software",
              "Designing Data-Intensive Applications")))
          .andExpect(jsonPath("$.content[*].categories[*].id").value(hasItem(2)));
    }

    @Test
    @DisplayName("GET /products should return products without repetition when several categoryIds are informed")
    void findAllShouldReturnProductsWithoutRepetitionWhenSeveralCategoryIdsAreInformed() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("categoryIds", "29,33")
          .param("page", "0")
          .param("size", "30")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(7))
          .andExpect(jsonPath("$.content[*].id").value(containsInAnyOrder(74, 75, 76, 85, 86, 87, 155)));
    }

    @Test
    @DisplayName("GET /products should apply both filters when name and categoryIds are informed")
    void findAllShouldApplyBothFiltersWhenNameAndCategoryIdsAreInformed() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("name", "rails")
          .param("categoryIds", "2")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.content[0].name").value("Ruby on Rails For Dummies"));
    }

    @Test
    @DisplayName("GET /products should return only matching products when name is informed")
    void findAllShouldReturnOnlyMatchingProductsWhenNameIsInformed() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL)
          .param("name", "pc")
          .param("page", "0")
          .param("size", "10")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isNotEmpty())
          .andExpect(jsonPath("$.content[*].name").value(everyItem(containsStringIgnoringCase("pc"))));
    }
  }

  @Nested
  @DisplayName("GET /products/{id}")
  class FindByIdOperations {

    @Test
    @DisplayName("GET /products/{id} should return product details when id exists")
    void findByIdShouldReturnProductDetailsWhenIdExists() throws Exception {

      // Arrange
      Product expected = productRepository.findById(EXISTING_ID).orElseThrow();

      // Act
      ResultActions resultActions = mockMvc
          .perform(get(BASE_URL + "/{id}", EXISTING_ID).accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.name").value(expected.getName()))
          .andExpect(jsonPath("$.description").value(expected.getDescription()))
          .andExpect(jsonPath("$.price").value(comparesEqualTo(expected.getPrice()), BigDecimal.class));
    }

    @Test
    @DisplayName("GET /products/{id} should return 404 with translated message when id does not exist")
    void findByIdShouldReturnNotFoundWithTranslatedMessageWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID)
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
      resultActions.andExpect(jsonPath("$.message").value("Product not found."));
    }
  }

  @Nested
  @DisplayName("POST /products")
  class CreateOperations {

    @Test
    @DisplayName("POST /products should return 201 with location and persist product when request is valid")
    void createShouldReturnCreatedWithLocationAndPersistProductWhenRequestIsValid() throws Exception {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();
      long initialCount = productRepository.count();

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
          .andExpect(jsonPath("$.description").value(request.description()))
          .andExpect(jsonPath("$.price").value(comparesEqualTo(request.price()), BigDecimal.class))
          .andExpect(jsonPath("$.date").doesNotExist());

      Long id = idFrom(resultActions);
      resultActions.andExpect(header().string("Location", "http://localhost" + BASE_URL + "/" + id));

      assertThat(productRepository.count()).isEqualTo(initialCount + 1);
      assertThat(productRepository.findById(id).orElseThrow())
          .extracting(Product::getName, Product::getDescription, Product::getPrice)
          .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
          .containsExactly(request.name(), request.description(), request.price());
    }

    // #25 (item 2): o request ainda aceita "date", que o ProductMapper descarta; o teste passa por
    // esse campo enquanto ele existir.
    @Test
    @DisplayName("POST /products should fill audit dates and activate product when product is persisted")
    void createShouldFillAuditDatesAndActivateProductWhenProductIsPersisted() throws Exception {

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

      Product persisted = productRepository.findById(idFrom(resultActions)).orElseThrow();

      assertThat(persisted.getCreatedAt()).isNotNull().isNotEqualTo(request.date());
      assertThat(persisted.getUpdatedAt()).isEqualTo(persisted.getCreatedAt());
      assertThat(persisted.isActive()).isTrue();
    }

    @Test
    @DisplayName("POST /products should return 422 with translated field error and create nothing when categoryIds is empty")
    void createShouldReturnUnprocessableEntityAndCreateNothingWhenCategoryIdsIsEmpty() throws Exception {

      // Arrange
      ProductCreateRequest request = new ProductCreateRequest("Valid Product Name", "Valid description", new BigDecimal("10.00"),
          "https://img.com/product.png", null, List.of());
      long initialCount = productRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(resultActions, BASE_URL, "categoryIds");
      resultActions.andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'categoryIds')].message")
          .value(hasItem("Product must contain at least one category.")));

      assertThat(productRepository.count()).isEqualTo(initialCount);
    }

    @ParameterizedTest(name = "[{0}]")
    @EnumSource(MissingValue.class)
    @DisplayName("POST /products should return 422 with translated error on price and create nothing when price is missing")
    void createShouldReturnUnprocessableEntityAndCreateNothingWhenPriceIsMissing(MissingValue missingValue)
        throws Exception {

      // Arrange
      String body = withMissingField(objectMapper, ProductFactory.createProductCreateRequest(), "price", missingValue);
      long initialCount = productRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(body)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(resultActions, BASE_URL, "price");
      resultActions.andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'price')].message")
          .value(hasItem(PRICE_REQUIRED_MESSAGE)));

      assertThat(productRepository.count()).isEqualTo(initialCount);
    }

    @ParameterizedTest(name = "[{0}]")
    @CsvSource(delimiter = '|', value = {
        "0            | " + PRICE_POSITIVE_MESSAGE,
        "-1           | " + PRICE_POSITIVE_MESSAGE,
        "10.999       | " + PRICE_DIGITS_MESSAGE,
        "100000000.00 | " + PRICE_DIGITS_MESSAGE })
    @DisplayName("POST /products should return 422 with translated error on price and create nothing when price is invalid")
    void createShouldReturnUnprocessableEntityAndCreateNothingWhenPriceIsInvalid(String price, String expectedMessage)
        throws Exception {

      // Arrange
      ProductCreateRequest base = ProductFactory.createProductCreateRequest();
      ProductCreateRequest request = new ProductCreateRequest(base.name(), base.description(), new BigDecimal(price),
          base.imgUrl(), base.date(), base.categoryIds());
      long initialCount = productRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(resultActions, BASE_URL, "price");
      resultActions.andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'price')].message")
          .value(hasItem(expectedMessage)));

      assertThat(productRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("POST /products should return 201 and persist price when price is at upper column limit")
    void createShouldReturnCreatedAndPersistPriceWhenPriceIsAtUpperColumnLimit() throws Exception {

      // Arrange
      ProductCreateRequest base = ProductFactory.createProductCreateRequest();
      BigDecimal maxPrice = new BigDecimal("99999999.99");
      ProductCreateRequest request = new ProductCreateRequest(base.name(), base.description(), maxPrice,
          base.imgUrl(), base.date(), base.categoryIds());

      // Act
      ResultActions resultActions = mockMvc.perform(post(BASE_URL)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.price").value(comparesEqualTo(maxPrice), BigDecimal.class));

      // O id é IDENTITY: o INSERT roda no save, e o H2 já conferiu o valor contra a coluna NUMERIC(10,2).
      assertThat(productRepository.findById(idFrom(resultActions)).orElseThrow().getPrice())
          .isEqualByComparingTo(maxPrice);
    }
  }

  @Nested
  @DisplayName("PUT /products/{id}")
  class UpdateOperations {

    @Test
    @DisplayName("PUT /products/{id} should return 200 and persist changes when id exists")
    void updateShouldReturnOkAndPersistChangesWhenIdExists() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      resultActions
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(EXISTING_ID))
          .andExpect(jsonPath("$.name").value(request.name()))
          .andExpect(jsonPath("$.description").value(request.description()))
          .andExpect(jsonPath("$.price").value(comparesEqualTo(request.price()), BigDecimal.class));

      assertThat(productRepository.findById(EXISTING_ID).orElseThrow())
          .extracting(Product::getName, Product::getDescription, Product::getPrice)
          .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
          .containsExactly(request.name(), request.description(), request.price());
    }

    @Test
    @DisplayName("PUT /products/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken())
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
    }

    @ParameterizedTest(name = "[{0}]")
    @EnumSource(MissingValue.class)
    @DisplayName("PUT /products/{id} should return 422 with translated error on price and keep price when price is missing")
    void updateShouldReturnUnprocessableEntityAndKeepPriceWhenPriceIsMissing(MissingValue missingValue)
        throws Exception {

      // Arrange
      String body = withMissingField(objectMapper, ProductFactory.createProductUpdateRequest(), "price", missingValue);
      BigDecimal priceBefore = productRepository.findById(EXISTING_ID).orElseThrow().getPrice();

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(body)
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(resultActions, BASE_URL + "/" + EXISTING_ID, "price");
      resultActions.andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'price')].message")
          .value(hasItem(PRICE_REQUIRED_MESSAGE)));

      assertThat(productRepository.findById(EXISTING_ID).orElseThrow().getPrice()).isEqualByComparingTo(priceBefore);
    }

    @ParameterizedTest(name = "[{0}]")
    @CsvSource(delimiter = '|', value = {
        "0            | " + PRICE_POSITIVE_MESSAGE,
        "-1           | " + PRICE_POSITIVE_MESSAGE,
        "10.999       | " + PRICE_DIGITS_MESSAGE,
        "100000000.00 | " + PRICE_DIGITS_MESSAGE })
    @DisplayName("PUT /products/{id} should return 422 with translated error on price and keep price when price is invalid")
    void updateShouldReturnUnprocessableEntityAndKeepPriceWhenPriceIsInvalid(String price, String expectedMessage)
        throws Exception {

      // Arrange
      ProductUpdateRequest base = ProductFactory.createProductUpdateRequest();
      ProductUpdateRequest request = new ProductUpdateRequest(base.name(), base.description(), new BigDecimal(price),
          base.imgUrl(), base.categoryIds());
      BigDecimal priceBefore = productRepository.findById(EXISTING_ID).orElseThrow().getPrice();

      // Act
      ResultActions resultActions = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken())
          .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(resultActions, BASE_URL + "/" + EXISTING_ID, "price");
      resultActions.andExpect(jsonPath("$.fieldErrors[?(@.fieldName == 'price')].message")
          .value(hasItem(expectedMessage)));

      assertThat(productRepository.findById(EXISTING_ID).orElseThrow().getPrice()).isEqualByComparingTo(priceBefore);
    }
  }

  @Nested
  @DisplayName("PATCH /products/{id}/activate")
  class ActivateOperations {

    @Test
    @DisplayName("PATCH /products/{id}/activate should return 204 and activate product when product is inactive")
    void activateShouldReturnNoContentAndActivateProductWhenProductIsInactive() throws Exception {

      // Arrange
      Product product = productRepository.findById(EXISTING_ID).orElseThrow();
      product.setActive(false);
      productRepository.saveAndFlush(product);

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(productRepository.findById(EXISTING_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("PATCH /products/{id}/activate should return 404 when id does not exist")
    void activateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID + "/activate");
    }
  }

  @Nested
  @DisplayName("PATCH /products/{id}/deactivate")
  class DeactivateOperations {

    @Test
    @DisplayName("PATCH /products/{id}/deactivate should return 204 and deactivate product when product is active")
    void deactivateShouldReturnNoContentAndDeactivateProductWhenProductIsActive() throws Exception {

      // Arrange
      assertThat(productRepository.findById(EXISTING_ID).orElseThrow().isActive()).isTrue();

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(productRepository.findById(EXISTING_ID).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("PATCH /products/{id}/deactivate should return 404 when id does not exist")
    void deactivateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID + "/deactivate");
    }
  }

  @Nested
  @DisplayName("DELETE /products/{id}")
  class DeleteOperations {

    @Test
    @DisplayName("DELETE /products/{id} should return 204 and remove product when id exists")
    void deleteShouldReturnNoContentAndRemoveProductWhenIdExists() throws Exception {

      // Arrange
      long initialCount = productRepository.count();

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID)
          .with(bearerToken()));

      // Assert
      resultActions.andExpect(status().isNoContent());

      assertThat(productRepository.count()).isEqualTo(initialCount - 1);
      assertThat(productRepository.existsById(EXISTING_ID)).isFalse();
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Act
      ResultActions resultActions = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID)
          .with(bearerToken()));

      // Assert
      expectNotFound(resultActions, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }
}
