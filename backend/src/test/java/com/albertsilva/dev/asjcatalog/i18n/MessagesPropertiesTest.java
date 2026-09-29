package com.albertsilva.dev.asjcatalog.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que os arquivos {@code messages_*.properties} dos idiomas suportados
 * (pt_BR, en, es) tenham as mesmas chaves e os mesmos placeholders
 * ({@code {min}}, {@code {0}}...). Uma chave faltando em um idioma faz o
 * {@code MessageSource} recorrer ao pt_BR, e o cliente recebe a mensagem no
 * idioma errado sem nenhum erro aparente. Também garante que as chaves
 * referenciadas no código de produção existam nos arquivos; sem isso, o Bean
 * Validation devolve a chave literal ({@code "{chave}"}) ao cliente.
 */
@DisplayName("Messages Properties Tests")
class MessagesPropertiesTest {

  private static final String REFERENCE_FILE = "messages_pt_BR.properties";
  private static final List<String> OTHER_FILES = List.of("messages_en.properties", "messages_es.properties");

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]+\\}");

  private static final Path MAIN_SOURCES = Path.of("src/main/java");
  private static final Path CUSTOM_EXCEPTIONS = MAIN_SOURCES
      .resolve("com/albertsilva/dev/asjcatalog/service/exception");

  /** Literal que contém só uma chave entre chaves: {@code "{user.email.invalid}"}. */
  private static final Pattern BRACED_KEY_LITERAL = Pattern.compile("\"\\{([A-Za-z0-9_.]+)\\}\"");

  /** Prefixos de chaves resolvidas pelos bundles do próprio Bean Validation. */
  private static final List<String> BUILT_IN_PREFIXES = List.of("jakarta.", "javax.", "org.hibernate.");

  @Test
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

  /**
   * Varre o código-fonte de produção ({@code src/main/java}) e exige que toda
   * chave de mensagem referenciada exista nos três arquivos. Reconhece:
   * <ul>
   * <li>literais que contêm só uma chave entre chaves, como
   * {@code message = "{user.email.invalid}"}, {@code default "{role.invalid}"},
   * {@code buildConstraintViolationWithTemplate("{role.invalid}")} e
   * {@code new FieldMessage("name", "{category.name.unique}")};</li>
   * <li>literais passados ao construtor das exceções de
   * {@code service/exception}, como
   * {@code new ResourceNotFoundException("error.category.notFound")}.</li>
   * </ul>
   *
   * <p>
   * Limitações (abordagem textual, sem compilar nem executar o código):
   * </p>
   * <ul>
   * <li>não detecta chaves montadas dinamicamente (concatenação, variáveis,
   * constantes) nem as passadas direto ao {@code MessageSource}, como os títulos
   * usados no {@code ControllerExceptionHandler};</li>
   * <li>só reconhece exceções declaradas em {@code service/exception}; uma
   * exceção customizada criada em outro pacote precisa ser incluída aqui;</li>
   * <li>ignora linhas de comentário que começam com {@code //} ou {@code *},
   * mas não trata comentários no fim de uma linha de código;</li>
   * <li>depende de o teste rodar com o diretório do projeto como diretório de
   * trabalho (padrão do Maven); por isso falha, em vez de passar vazio, se não
   * encontrar os fontes ou nenhuma referência.</li>
   * </ul>
   */
  @Test
  @DisplayName("Every message key referenced in production code should exist in all message files")
  void everyKeyReferencedInCodeShouldExistInAllMessageFiles() throws Exception {

    // Arrange
    assertTrue(Files.isDirectory(MAIN_SOURCES), () -> "Fontes não encontrados em " + MAIN_SOURCES.toAbsolutePath());
    Pattern exceptionKeyLiteral = exceptionKeyLiteralPattern();
    Map<String, String> referencedKeys = new TreeMap<>();

    // Act
    try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
      for (Path source : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
        List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
          String line = lines.get(i).strip();
          if (line.startsWith("//") || line.startsWith("*") || line.startsWith("/*")) {
            continue;
          }
          String location = MAIN_SOURCES.relativize(source) + ":" + (i + 1);
          collectKeys(BRACED_KEY_LITERAL, line, location, referencedKeys);
          collectKeys(exceptionKeyLiteral, line, location, referencedKeys);
        }
      }
    }
    referencedKeys.keySet().removeIf(key -> BUILT_IN_PREFIXES.stream().anyMatch(key::startsWith));

    // Assert
    assertTrue(referencedKeys.size() > 20,
        () -> "Poucas chaves encontradas (" + referencedKeys.size() + "); a varredura pode estar quebrada");

    List<String> missing = new ArrayList<>();
    for (String file : allFiles()) {
      Properties messages = load(file);
      referencedKeys.forEach((key, location) -> {
        if (!messages.containsKey(key)) {
          missing.add(file + " -> " + key + " (usada em " + location + ")");
        }
      });
    }
    assertEquals(List.of(), missing, "Chaves referenciadas no código e ausentes nos arquivos de mensagens");
  }

  private static Pattern exceptionKeyLiteralPattern() throws Exception {
    assertTrue(Files.isDirectory(CUSTOM_EXCEPTIONS), () -> "Pacote não encontrado: " + CUSTOM_EXCEPTIONS);
    String names;
    try (Stream<Path> files = Files.list(CUSTOM_EXCEPTIONS)) {
      names = files.map(p -> p.getFileName().toString())
          .filter(name -> name.endsWith("Exception.java"))
          .map(name -> name.replace(".java", ""))
          .collect(Collectors.joining("|"));
    }
    assertTrue(!names.isEmpty(), "Nenhuma exceção customizada encontrada em " + CUSTOM_EXCEPTIONS);
    return Pattern.compile("new (?:" + names + ")\\(\"([A-Za-z0-9_.]+)\"\\)");
  }

  private static void collectKeys(Pattern pattern, String line, String location, Map<String, String> keys) {
    Matcher matcher = pattern.matcher(line);
    while (matcher.find()) {
      keys.putIfAbsent(matcher.group(1), location);
    }
  }

  private static List<String> allFiles() {
    List<String> files = new ArrayList<>();
    files.add(REFERENCE_FILE);
    files.addAll(OTHER_FILES);
    return files;
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
