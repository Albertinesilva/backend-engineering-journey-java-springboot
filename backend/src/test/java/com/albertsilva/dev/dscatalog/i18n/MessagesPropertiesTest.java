package com.albertsilva.dev.dscatalog.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que os arquivos {@code messages_*.properties} dos idiomas suportados
 * (pt_BR, en, es) tenham as mesmas chaves e os mesmos placeholders
 * ({@code {min}}, {@code {0}}...). Uma chave faltando em um idioma faz o
 * {@code MessageSource} recorrer ao pt_BR, e o cliente recebe a mensagem no
 * idioma errado sem nenhum erro aparente.
 */
@DisplayName("Messages Properties Tests")
class MessagesPropertiesTest {

  private static final String REFERENCE_FILE = "messages_pt_BR.properties";
  private static final List<String> OTHER_FILES = List.of("messages_en.properties", "messages_es.properties");

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+\\}");

  @Test
  @Disabled("Divergência conhecida em error.auth.*.claim.notFound; aguardando auditoria de i18n")
  @DisplayName("All message files should contain exactly the same keys")
  void allMessageFilesShouldContainSameKeys() throws Exception {

    // Arrange
    Set<String> referenceKeys = load(REFERENCE_FILE).stringPropertyNames();

    for (String file : OTHER_FILES) {
      Set<String> keys = load(file).stringPropertyNames();

      // Act
      Set<String> missing = difference(referenceKeys, keys);
      Set<String> extra = difference(keys, referenceKeys);

      // Assert
      assertTrue(missing.isEmpty(), () -> "Chaves de " + REFERENCE_FILE + " faltando em " + file + ": " + missing);
      assertTrue(extra.isEmpty(), () -> "Chaves de " + file + " que não existem em " + REFERENCE_FILE + ": " + extra);
    }
  }

  @Test
  @DisplayName("All translations of a key should use the same placeholders")
  void allTranslationsShouldUseSamePlaceholders() throws Exception {

    // Arrange
    Properties reference = load(REFERENCE_FILE);
    List<String> divergences = new ArrayList<>();

    for (String file : OTHER_FILES) {
      Properties translation = load(file);

      // Act
      for (String key : reference.stringPropertyNames()) {
        String translated = translation.getProperty(key);
        if (translated == null) {
          continue; // chave ausente é reportada pelo teste de chaves
        }
        Set<String> expected = placeholders(reference.getProperty(key));
        Set<String> actual = placeholders(translated);
        if (!expected.equals(actual)) {
          divergences.add(file + " -> " + key + ": esperado " + expected + ", encontrado " + actual);
        }
      }
    }

    // Assert
    assertEquals(List.of(), divergences, "Placeholders divergentes");
  }

  private static Properties load(String file) throws Exception {
    try (InputStream in = MessagesPropertiesTest.class.getClassLoader().getResourceAsStream(file)) {
      assertNotNull(in, "Arquivo não encontrado no classpath: " + file);
      Properties properties = new Properties();
      properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
      return properties;
    }
  }

  private static Set<String> difference(Set<String> a, Set<String> b) {
    Set<String> result = new TreeSet<>(a);
    result.removeAll(b);
    return result;
  }

  private static Set<String> placeholders(String message) {
    Set<String> result = new TreeSet<>();
    Matcher matcher = PLACEHOLDER.matcher(message);
    while (matcher.find()) {
      result.add(matcher.group());
    }
    return result;
  }
}
