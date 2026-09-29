package com.albertsilva.dev.dscatalog.integrations.i18n;

import static com.albertsilva.dev.dscatalog.factory.CategoryFactory.NON_EXISTING_ID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.albertsilva.dev.dscatalog.integrations.common.AbstractIT;

/**
 * Verifica que o idioma das mensagens de erro é escolhido pelo cabeçalho
 * {@code Accept-Language} (configurado em {@code MessageSourceConfig}).
 *
 * <p>
 * O projeto suporta SOMENTE pt_BR (padrão), en e es. O caso {@code fr} existe
 * apenas para provar que um idioma NÃO suportado cai no padrão pt_BR: não há
 * (nem deve haver) um {@code messages_fr.properties}.
 * </p>
 */
@DisplayName("Accept-Language Integration Tests")
class AcceptLanguageIT extends AbstractIT {

  private static final String CATEGORY_URL = "/api/v1/categories/{id}";

  @Test
  @DisplayName("GET /categories/{id} with Accept-Language en should return message in English")
  void findByIdShouldReturnEnglishMessageWhenAcceptLanguageIsEn() throws Exception {

    // Act
    ResultActions resultActions = mockMvc.perform(get(CATEGORY_URL, NON_EXISTING_ID)
        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
        .accept(MediaType.APPLICATION_JSON));

    // Assert
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Resource not found"))
        .andExpect(jsonPath("$.message").value("Category not found"));
  }

  @Test
  @DisplayName("GET /categories/{id} with Accept-Language es should return message in Spanish")
  void findByIdShouldReturnSpanishMessageWhenAcceptLanguageIsEs() throws Exception {

    // Act
    ResultActions resultActions = mockMvc.perform(get(CATEGORY_URL, NON_EXISTING_ID)
        .header(HttpHeaders.ACCEPT_LANGUAGE, "es")
        .accept(MediaType.APPLICATION_JSON));

    // Assert
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Recurso no encontrado"))
        .andExpect(jsonPath("$.message").value("Categoría no encontrada"));
  }

  @Test
  @DisplayName("GET /categories/{id} without Accept-Language should return message in Portuguese")
  void findByIdShouldReturnPortugueseMessageWhenAcceptLanguageIsAbsent() throws Exception {

    // Act
    ResultActions resultActions = mockMvc.perform(get(CATEGORY_URL, NON_EXISTING_ID)
        .accept(MediaType.APPLICATION_JSON));

    // Assert
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
        .andExpect(jsonPath("$.message").value("Categoria não encontrada"));
  }

  @Test
  @DisplayName("GET /categories/{id} with unsupported Accept-Language fr should fall back to Portuguese")
  void findByIdShouldFallBackToPortugueseWhenAcceptLanguageIsUnsupported() throws Exception {

    // Act
    ResultActions resultActions = mockMvc.perform(get(CATEGORY_URL, NON_EXISTING_ID)
        .header(HttpHeaders.ACCEPT_LANGUAGE, "fr")
        .accept(MediaType.APPLICATION_JSON));

    // Assert
    resultActions
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Recurso não encontrado"))
        .andExpect(jsonPath("$.message").value("Categoria não encontrada"));
  }
}
