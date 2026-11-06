package com.albertsilva.dev.asjcatalog.web.controller;

import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NAME_FILTER;
import static com.albertsilva.dev.asjcatalog.factory.ProductFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectNotFound;
import static com.albertsilva.dev.asjcatalog.utils.ErrorResponseAssertions.expectValidationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import com.albertsilva.dev.asjcatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.repository.CategoryRepository;
import com.albertsilva.dev.asjcatalog.repository.ProductRepository;
import com.albertsilva.dev.asjcatalog.security.oauth2.resource.config.ResourceServerConfig;
import com.albertsilva.dev.asjcatalog.service.ProductService;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.asjcatalog.web.exception.handler.ControllerExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ProductController.class)
@Import({ ControllerExceptionHandler.class, ResourceServerConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.h2.console.enabled=false" })
@DisplayName("Tests for ProductController")
class ProductControllerTest {

  private static final String BASE_URL = "/api/v1/products";
  private static final String NOT_FOUND_KEY = "error.product.notFound";
  private static final String NO_CATEGORY_FILTER = "0";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ProductService productService;

  @MockitoBean
  private ProductRepository productRepository;

  @MockitoBean
  private CategoryRepository categoryRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  private Page<ProductResponse> page;
  private ProductResponse productResponse;

  @BeforeEach
  void setUp() {
    productResponse = ProductFactory.createProductResponse();
    page = new PageImpl<>(List.of(productResponse), PageRequest.of(0, 10), 1);

    // Os validadores de produto (@ProductCreateValid e @ProductUpdateValid) consultam os repositórios.
    when(categoryRepository.existsById(anyLong())).thenReturn(true);
    when(productRepository.existsByNameIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
  }

  private String asJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }

  /** Request de criação da fábrica com outro preço. */
  private static ProductCreateRequest createRequestWithPrice(BigDecimal price) {
    ProductCreateRequest base = ProductFactory.createProductCreateRequest();
    return new ProductCreateRequest(base.name(), base.description(), price, base.imgUrl(), base.date(),
        base.categoryIds());
  }

  /** Request de atualização da fábrica com outro preço. */
  private static ProductUpdateRequest updateRequestWithPrice(BigDecimal price) {
    ProductUpdateRequest base = ProductFactory.createProductUpdateRequest();
    return new ProductUpdateRequest(base.name(), base.description(), price, base.imgUrl(), base.categoryIds());
  }

  /**
   * Testes de {@code POST /products}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code price} nulo, ausente ou string vazia: o campo não tem
   * {@code @NotNull}, então a requisição passa pela validação e só é recusada
   * pelo banco, em que a coluna é {@code NOT NULL} desde a V106; a API ainda não
   * responde com 422 (#44).</li>
   * </ul>
   */
  @Nested
  @DisplayName("POST /products")
  class CreateOperations {

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = { "0", "-1" })
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("POST /products should return 422 with error on price when price is not positive")
    void createShouldReturnUnprocessableEntityWithErrorOnPriceWhenPriceIsNotPositive(String price) throws Exception {

      // Arrange
      ProductCreateRequest request = createRequestWithPrice(new BigDecimal(price));

      // Act
      ResultActions result = mockMvc.perform(post(BASE_URL)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(result, BASE_URL, "price");
      verify(productService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("POST /products should return 201 with location when request is valid")
    void createShouldReturnCreatedWithLocationWhenRequestIsValid() throws Exception {

      // Arrange
      ProductCreateRequest request = ProductFactory.createProductCreateRequest();

      when(productService.create(request)).thenReturn(productResponse);

      // Act
      ResultActions result = mockMvc.perform(post(BASE_URL)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "http://localhost" + BASE_URL + "/" + productResponse.id()))
          .andExpect(jsonPath("$.id").value(productResponse.id()))
          .andExpect(jsonPath("$.name").value(productResponse.name()))
          .andExpect(jsonPath("$.description").value(productResponse.description()))
          .andExpect(jsonPath("$.price").value(comparesEqualTo(productResponse.price()), BigDecimal.class));
      verify(productService).create(request);
    }
  }

  /**
   * Testes de {@code GET /products}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code sort=price}: responde 500, porque a listagem só ordena por
   * {@code id} e {@code name} (#3).</li>
   * <li>{@code categoryIds} não numérico (por exemplo, {@code abc}): responde 500
   * em vez de 400 (#36).</li>
   * </ul>
   */
  @Nested
  @DisplayName("GET /products")
  class FindAllOperations {

    // #28: o formato JSON da página é o do Page serializado diretamente, que a issue
    // prevê trocar por um formato estável. Os testes conferem o formato atual sem reforço.
    @Test
    @DisplayName("GET /products should return page when no filter is informed")
    void findAllShouldReturnPageWhenNoFilterIsInformed() throws Exception {

      // Arrange
      when(productService.findAllPaged(eq(""), eq(NO_CATEGORY_FILTER), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(productResponse.id()))
          .andExpect(jsonPath("$.content[0].name").value(productResponse.name()))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("GET /products should forward name and pagination to service when they are informed")
    void findAllShouldForwardNameAndPaginationToServiceWhenTheyAreInformed() throws Exception {

      // Arrange
      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      when(productService.findAllPaged(eq(NAME_FILTER), eq(NO_CATEGORY_FILTER), pageableCaptor.capture()))
          .thenReturn(page);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL)
          .param("name", NAME_FILTER)
          .param("page", "0")
          .param("size", "12")
          .param("sort", "name,desc")
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result.andExpect(status().isOk());
      Pageable pageable = pageableCaptor.getValue();
      assertThat(pageable.getPageNumber()).isZero();
      assertThat(pageable.getPageSize()).isEqualTo(12);
      assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "name"));
    }

    @Test
    @DisplayName("GET /products should forward categoryIds to service when they are informed")
    void findAllShouldForwardCategoryIdsToServiceWhenTheyAreInformed() throws Exception {

      // Arrange
      String categoryIds = CategoryFactory.EXISTING_ID + "," + CategoryFactory.OTHER_ID;

      when(productService.findAllPaged(eq(""), eq(categoryIds), any(Pageable.class))).thenReturn(page);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL).param("categoryIds", categoryIds)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(productResponse.id()));
      verify(productService).findAllPaged(eq(""), eq(categoryIds), any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("GET /products/{id}")
  class FindByIdOperations {

    @Test
    @DisplayName("GET /products/{id} should return product when id exists")
    void findByIdShouldReturnProductWhenIdExists() throws Exception {

      // Arrange
      ProductDetailsResponse response = ProductFactory.createProductDetailsResponse();

      when(productService.findById(EXISTING_ID)).thenReturn(response);

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", EXISTING_ID)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(response.id()))
          .andExpect(jsonPath("$.name").value(response.name()))
          .andExpect(jsonPath("$.description").value(response.description()))
          .andExpect(jsonPath("$.price").value(comparesEqualTo(response.price()), BigDecimal.class));
    }

    @Test
    @DisplayName("GET /products/{id} should return 404 when id does not exist")
    void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      when(productService.findById(NON_EXISTING_ID)).thenThrow(new ResourceNotFoundException(NOT_FOUND_KEY));

      // Act
      ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }

  /**
   * Testes de {@code PUT /products/{id}}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code price} nulo, ausente ou string vazia: responde 200 e mantém o
   * preço atual, em vez de 422 (#44).</li>
   * </ul>
   */
  @Nested
  @DisplayName("PUT /products/{id}")
  class UpdateOperations {

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = { "0", "-1" })
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PUT /products/{id} should return 422 with error on price when price is not positive")
    void updateShouldReturnUnprocessableEntityWithErrorOnPriceWhenPriceIsNotPositive(String price) throws Exception {

      // Arrange
      ProductUpdateRequest request = updateRequestWithPrice(new BigDecimal(price));

      // Act
      ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      expectValidationError(result, BASE_URL + "/" + EXISTING_ID, "price");
      verify(productService, never()).update(anyLong(), any());
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PUT /products/{id} should return updated product when id exists")
    void updateShouldReturnUpdatedProductWhenIdExists() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();
      ProductResponse updatedResponse = ProductFactory.createUpdatedProductResponse();

      when(productService.update(EXISTING_ID, request)).thenReturn(updatedResponse);

      // Act
      ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", EXISTING_ID)
          .content(asJson(request))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON));

      // Assert
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(updatedResponse.id()))
          .andExpect(jsonPath("$.name").value(updatedResponse.name()))
          .andExpect(jsonPath("$.description").value(updatedResponse.description()))
          .andExpect(jsonPath("$.price").value(comparesEqualTo(updatedResponse.price()), BigDecimal.class));
      verify(productService).update(EXISTING_ID, request);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PUT /products/{id} should return 404 when id does not exist")
    void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      ProductUpdateRequest request = ProductFactory.createProductUpdateRequest();

      when(productService.update(NON_EXISTING_ID, request)).thenThrow(new ResourceNotFoundException(NOT_FOUND_KEY));

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
  @DisplayName("PATCH /products/{id}/activate")
  class ActivateOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /products/{id}/activate should return 204 when id exists")
    void activateShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/activate", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(productService).activate(EXISTING_ID);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /products/{id}/activate should return 404 when id does not exist")
    void activateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(productService).activate(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/activate", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID + "/activate");
    }
  }

  @Nested
  @DisplayName("PATCH /products/{id}/deactivate")
  class DeactivateOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /products/{id}/deactivate should return 204 when id exists")
    void deactivateShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(productService).deactivate(EXISTING_ID);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("PATCH /products/{id}/deactivate should return 404 when id does not exist")
    void deactivateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(productService).deactivate(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(patch(BASE_URL + "/{id}/deactivate", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID + "/deactivate");
    }
  }

  @Nested
  @DisplayName("DELETE /products/{id}")
  class DeleteOperations {

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("DELETE /products/{id} should return 204 when id exists")
    void deleteShouldReturnNoContentWhenIdExists() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", EXISTING_ID));

      // Assert
      result.andExpect(status().isNoContent());
      verify(productService).delete(EXISTING_ID);
    }

    @Test
    @WithMockUser(roles = { "ADMIN", "OPERATOR" })
    @DisplayName("DELETE /products/{id} should return 404 when id does not exist")
    void deleteShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

      // Arrange
      doThrow(new ResourceNotFoundException(NOT_FOUND_KEY)).when(productService).delete(NON_EXISTING_ID);

      // Act
      ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", NON_EXISTING_ID));

      // Assert
      expectNotFound(result, BASE_URL + "/" + NON_EXISTING_ID);
    }
  }
}
