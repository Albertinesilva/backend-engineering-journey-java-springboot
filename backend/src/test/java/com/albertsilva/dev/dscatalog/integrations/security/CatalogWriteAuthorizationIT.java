package com.albertsilva.dev.dscatalog.integrations.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.domain.catalog.Category;
import com.albertsilva.dev.dscatalog.domain.catalog.Product;
import com.albertsilva.dev.dscatalog.domain.user.Role;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.factory.CategoryFactory;
import com.albertsilva.dev.dscatalog.factory.ProductFactory;
import com.albertsilva.dev.dscatalog.integrations.common.AbstractIT;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.repository.UserRepository;

/**
 * Rotas de escrita de categorias e produtos com JWT real: {@code 401} sem
 * token e {@code 403} com um usuário autenticado sem {@code ROLE_ADMIN} nem
 * {@code ROLE_OPERATOR}. Esse usuário (com uma role {@code ROLE_CLIENT} criada
 * só para o teste) é necessário porque todo usuário do seed tem ao menos
 * {@code ROLE_OPERATOR}, que pode escrever no catálogo. Os corpos enviados são
 * válidos, para que a negação venha da autorização e não da validação. Cada
 * teste confere também que nada foi alterado no banco.
 */
@Transactional
@DisplayName("Catalog Write Authorization Integration Tests")
class CatalogWriteAuthorizationIT extends AbstractIT {

  private static final String CATEGORIES_URL = "/api/v1/categories";
  private static final String PRODUCTS_URL = "/api/v1/products";
  private static final String CLIENT_EMAIL = "client.only@gmail.com";
  private static final String CLIENT_PASSWORD = "123456";
  private static final Long CATEGORY_ID = CategoryFactory.EXISTING_ID;
  private static final Long DELETABLE_CATEGORY_ID = CategoryFactory.NON_DEPENDENT_ID;
  private static final Long PRODUCT_ID = ProductFactory.EXISTING_ID;

  @Autowired
  private CategoryRepository categoryRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private String clientToken;

  @BeforeEach
  void setUp() throws Exception {
    Role clientRole = roleRepository.save(new Role(null, "ROLE_CLIENT"));
    User client = new User(null, "Client", "Only", CLIENT_EMAIL, passwordEncoder.encode(CLIENT_PASSWORD), true);
    client.addRole(clientRole);
    userRepository.saveAndFlush(client);
    clientToken = tokenUtil.obtainAccessToken(mockMvc, CLIENT_EMAIL, CLIENT_PASSWORD);
  }

  @Nested
  @DisplayName("Category write routes")
  class CategoryWriteRoutes {

    @Test
    @DisplayName("POST /categories should return 401 without token and create nothing")
    void createShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      long count = categoryRepository.count();

      // Act
      ResultActions result = mockMvc.perform(withJson(post(CATEGORIES_URL), CategoryFactory.createCategoryCreateRequest()));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertEquals(count, categoryRepository.count());
    }

    @Test
    @DisplayName("POST /categories should return 403 for a user without ADMIN or OPERATOR role and create nothing")
    void createShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      long count = categoryRepository.count();

      // Act
      ResultActions result = mockMvc.perform(asClient(withJson(post(CATEGORIES_URL),
          CategoryFactory.createCategoryCreateRequest())));

      // Assert
      expectAccessDenied(result);
      assertEquals(count, categoryRepository.count());
    }

    @Test
    @DisplayName("PATCH /categories/{id} should return 401 without token and keep the category")
    void updateShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      String name = categoryName();

      // Act
      ResultActions result = mockMvc.perform(withJson(patch(CATEGORIES_URL + "/{id}", CATEGORY_ID),
          CategoryFactory.createCategoryUpdateRequest()));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertEquals(name, categoryName());
    }

    @Test
    @DisplayName("PATCH /categories/{id} should return 403 for a user without ADMIN or OPERATOR role and keep the category")
    void updateShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      String name = categoryName();

      // Act
      ResultActions result = mockMvc.perform(asClient(withJson(patch(CATEGORIES_URL + "/{id}", CATEGORY_ID),
          CategoryFactory.createCategoryUpdateRequest())));

      // Assert
      expectAccessDenied(result);
      assertEquals(name, categoryName());
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 401 without token and keep the category")
    void deleteShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(delete(CATEGORIES_URL + "/{id}", DELETABLE_CATEGORY_ID));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertTrue(categoryRepository.existsById(DELETABLE_CATEGORY_ID));
    }

    @Test
    @DisplayName("DELETE /categories/{id} should return 403 for a user without ADMIN or OPERATOR role and keep the category")
    void deleteShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(asClient(delete(CATEGORIES_URL + "/{id}", DELETABLE_CATEGORY_ID)));

      // Assert
      expectAccessDenied(result);
      assertTrue(categoryRepository.existsById(DELETABLE_CATEGORY_ID));
    }

    @Test
    @DisplayName("PATCH /categories/{id}/activate should return 401 without token and keep the category inactive")
    void activateShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      setCategoryActive(false);

      // Act
      ResultActions result = mockMvc.perform(patch(CATEGORIES_URL + "/{id}/activate", CATEGORY_ID));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertFalse(categoryActive());
    }

    @Test
    @DisplayName("PATCH /categories/{id}/activate should return 403 for a user without ADMIN or OPERATOR role and keep the category inactive")
    void activateShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      setCategoryActive(false);

      // Act
      ResultActions result = mockMvc.perform(asClient(patch(CATEGORIES_URL + "/{id}/activate", CATEGORY_ID)));

      // Assert
      expectAccessDenied(result);
      assertFalse(categoryActive());
    }

    @Test
    @DisplayName("PATCH /categories/{id}/deactivate should return 401 without token and keep the category active")
    void deactivateShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      setCategoryActive(true);

      // Act
      ResultActions result = mockMvc.perform(patch(CATEGORIES_URL + "/{id}/deactivate", CATEGORY_ID));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertTrue(categoryActive());
    }

    @Test
    @DisplayName("PATCH /categories/{id}/deactivate should return 403 for a user without ADMIN or OPERATOR role and keep the category active")
    void deactivateShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      setCategoryActive(true);

      // Act
      ResultActions result = mockMvc.perform(asClient(patch(CATEGORIES_URL + "/{id}/deactivate", CATEGORY_ID)));

      // Assert
      expectAccessDenied(result);
      assertTrue(categoryActive());
    }

    private String categoryName() {
      return categoryRepository.findById(CATEGORY_ID).orElseThrow().getName();
    }

    private boolean categoryActive() {
      return categoryRepository.findById(CATEGORY_ID).orElseThrow().isActive();
    }

    private void setCategoryActive(boolean active) {
      Category category = categoryRepository.findById(CATEGORY_ID).orElseThrow();
      category.setActive(active);
      categoryRepository.saveAndFlush(category);
    }
  }

  @Nested
  @DisplayName("Product write routes")
  class ProductWriteRoutes {

    @Test
    @DisplayName("POST /products should return 401 without token and create nothing")
    void createShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      long count = productRepository.count();

      // Act
      ResultActions result = mockMvc.perform(withJson(post(PRODUCTS_URL), ProductFactory.createProductCreateRequest()));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertEquals(count, productRepository.count());
    }

    @Test
    @DisplayName("POST /products should return 403 for a user without ADMIN or OPERATOR role and create nothing")
    void createShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      long count = productRepository.count();

      // Act
      ResultActions result = mockMvc.perform(asClient(withJson(post(PRODUCTS_URL),
          ProductFactory.createProductCreateRequest())));

      // Assert
      expectAccessDenied(result);
      assertEquals(count, productRepository.count());
    }

    @Test
    @DisplayName("PUT /products/{id} should return 401 without token and keep the product")
    void updateShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      String name = productName();

      // Act
      ResultActions result = mockMvc.perform(withJson(put(PRODUCTS_URL + "/{id}", PRODUCT_ID),
          ProductFactory.createProductUpdateRequest()));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertEquals(name, productName());
    }

    @Test
    @DisplayName("PUT /products/{id} should return 403 for a user without ADMIN or OPERATOR role and keep the product")
    void updateShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      String name = productName();

      // Act
      ResultActions result = mockMvc.perform(asClient(withJson(put(PRODUCTS_URL + "/{id}", PRODUCT_ID),
          ProductFactory.createProductUpdateRequest())));

      // Assert
      expectAccessDenied(result);
      assertEquals(name, productName());
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 401 without token and keep the product")
    void deleteShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(delete(PRODUCTS_URL + "/{id}", PRODUCT_ID));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertTrue(productRepository.existsById(PRODUCT_ID));
    }

    @Test
    @DisplayName("DELETE /products/{id} should return 403 for a user without ADMIN or OPERATOR role and keep the product")
    void deleteShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Act
      ResultActions result = mockMvc.perform(asClient(delete(PRODUCTS_URL + "/{id}", PRODUCT_ID)));

      // Assert
      expectAccessDenied(result);
      assertTrue(productRepository.existsById(PRODUCT_ID));
    }

    @Test
    @DisplayName("PATCH /products/{id}/activate should return 401 without token and keep the product inactive")
    void activateShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      setProductActive(false);

      // Act
      ResultActions result = mockMvc.perform(patch(PRODUCTS_URL + "/{id}/activate", PRODUCT_ID));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertFalse(productActive());
    }

    @Test
    @DisplayName("PATCH /products/{id}/activate should return 403 for a user without ADMIN or OPERATOR role and keep the product inactive")
    void activateShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      setProductActive(false);

      // Act
      ResultActions result = mockMvc.perform(asClient(patch(PRODUCTS_URL + "/{id}/activate", PRODUCT_ID)));

      // Assert
      expectAccessDenied(result);
      assertFalse(productActive());
    }

    @Test
    @DisplayName("PATCH /products/{id}/deactivate should return 401 without token and keep the product active")
    void deactivateShouldReturnUnauthorizedWithoutToken() throws Exception {

      // Arrange
      setProductActive(true);

      // Act
      ResultActions result = mockMvc.perform(patch(PRODUCTS_URL + "/{id}/deactivate", PRODUCT_ID));

      // Assert
      result.andExpect(status().isUnauthorized());
      assertTrue(productActive());
    }

    @Test
    @DisplayName("PATCH /products/{id}/deactivate should return 403 for a user without ADMIN or OPERATOR role and keep the product active")
    void deactivateShouldReturnForbiddenWithoutWriteRole() throws Exception {

      // Arrange
      setProductActive(true);

      // Act
      ResultActions result = mockMvc.perform(asClient(patch(PRODUCTS_URL + "/{id}/deactivate", PRODUCT_ID)));

      // Assert
      expectAccessDenied(result);
      assertTrue(productActive());
    }

    private String productName() {
      return productRepository.findById(PRODUCT_ID).orElseThrow().getName();
    }

    private boolean productActive() {
      return productRepository.findById(PRODUCT_ID).orElseThrow().isActive();
    }

    private void setProductActive(boolean active) {
      Product product = productRepository.findById(PRODUCT_ID).orElseThrow();
      product.setActive(active);
      productRepository.saveAndFlush(product);
    }
  }

  private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder request, Object body)
      throws Exception {
    return request.content(asJson(body)).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
  }

  private MockHttpServletRequestBuilder asClient(MockHttpServletRequestBuilder request) {
    return request.header("Authorization", "Bearer " + clientToken);
  }

  private void expectAccessDenied(ResultActions result) throws Exception {
    result
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }
}
