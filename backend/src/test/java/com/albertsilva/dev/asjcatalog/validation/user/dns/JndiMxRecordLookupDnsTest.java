package com.albertsilva.dev.asjcatalog.validation.user.dns;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Teste opcional contra o DNS real: confere que a consulta JNDI de produção
 * continua funcionando. Precisa de internet e, por isso, fica fora do build
 * padrão (a tag {@code dns} é excluída pela propriedade
 * {@code surefire.excludedGroups} do {@code pom.xml}). Como rodar à mão: veja
 * o TESTING.md.
 */
@Tag("dns")
@DisplayName("Tests for JndiMxRecordLookup against the real DNS")
class JndiMxRecordLookupDnsTest {

  private final JndiMxRecordLookup lookup = new JndiMxRecordLookup();

  @Test
  @DisplayName("hasMxRecord should return true when domain has public MX record")
  void hasMxRecordShouldReturnTrueWhenDomainHasPublicMxRecord() {
    assertThat(lookup.hasMxRecord("gmail.com")).isTrue();
  }

  @Test
  @DisplayName("hasMxRecord should return false when domain does not exist")
  void hasMxRecordShouldReturnFalseWhenDomainDoesNotExist() {
    assertThat(lookup.hasMxRecord("asjcatalog.invalid")).isFalse();
  }
}
