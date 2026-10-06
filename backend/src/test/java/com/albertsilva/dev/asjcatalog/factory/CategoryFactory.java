package com.albertsilva.dev.dscatalog.factory;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.entity.Category;

public class CategoryFactory {

  public static final Long EXISTING_ID = 1L;
  public static final Long NON_EXISTING_ID = 1000L;
  public static final Long NON_DEPENDENT_ID = 11L;
  public static final Long DEPENDENT_ID = 1L;
  public static final Long COUNT_TOTAL_CATEGORIES = 15L;

  public static Category createCategory() {
    Category category = new Category("Eletrônicos", "Produtos eletrônicos, como TVs, smartphones e laptops.", true);
    return category;
  }

  public static CategoryResponse createCategoryResponse() {
    return new CategoryResponse(EXISTING_ID, "Eletrônicos", "Produtos eletrônicos, como TVs, smartphones e laptops.",
        true);
  }

  public static CategoryCreateRequest createCategoryCreateRequest() {
    return new CategoryCreateRequest("Eletrodomésticos",
        "Produtos eletrodomésticos, como geladeiras, fogões e máquinas de lavar.");
  }

  public static CategoryUpdateRequest createCategoryUpdateRequest() {
    return new CategoryUpdateRequest("Eletrodomésticos Atualizados",
        "Produtos eletrodomésticos atualizados, como geladeiras, fogões e máquinas de lavar.");
  }

  public static CategoryResponse createUpdatedCategoryResponse() {
    return new CategoryResponse(EXISTING_ID, "Eletrodomésticos Atualizados",
        "Produtos eletrodomésticos atualizados, como geladeiras, fogões e máquinas de lavar.", true);
  }
}
