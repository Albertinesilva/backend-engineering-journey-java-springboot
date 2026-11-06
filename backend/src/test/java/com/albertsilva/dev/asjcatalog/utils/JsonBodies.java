package com.albertsilva.dev.asjcatalog.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Corpos JSON para os testes HTTP que precisam de um campo ausente, nulo ou
 * vazio. Os DTOs são {@code record}s, então o campo ausente e a string vazia
 * não podem ser montados pelo construtor: o corpo é gerado a partir do request
 * e alterado como árvore JSON.
 */
public final class JsonBodies {

  private JsonBodies() {
  }

  /** Forma de enviar um campo sem valor. */
  public enum MissingValue {

    /** {@code "campo": null}. */
    NULL,

    /** Corpo sem o campo. */
    ABSENT,

    /** {@code "campo": ""}. */
    EMPTY_STRING
  }

  /**
   * Serializa {@code request} e troca o valor de {@code field} conforme
   * {@code missingValue}.
   *
   * @param objectMapper mapper usado na serialização
   * @param request      request de origem
   * @param field        nome do campo no JSON
   * @param missingValue forma de enviar o campo sem valor
   * @return corpo JSON alterado
   */
  public static String withMissingField(ObjectMapper objectMapper, Object request, String field,
      MissingValue missingValue) throws Exception {
    ObjectNode body = objectMapper.valueToTree(request);
    switch (missingValue) {
      case NULL -> body.putNull(field);
      case ABSENT -> body.remove(field);
      case EMPTY_STRING -> body.put(field, "");
    }
    return objectMapper.writeValueAsString(body);
  }
}
