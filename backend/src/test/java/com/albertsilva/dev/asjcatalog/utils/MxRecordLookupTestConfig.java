package com.albertsilva.dev.asjcatalog.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.albertsilva.dev.asjcatalog.validation.user.dns.MxRecordLookup;

/**
 * Substitui a consulta de MX do {@code @ValidEmail} nos testes que sobem o
 * contexto do Spring, para que eles não dependam de DNS (#20).
 *
 * <p>
 * O bean é {@code @Primary}: nos {@code @SpringBootTest} ele tem prioridade
 * sobre o {@code JndiMxRecordLookup} real; nos {@code @WebMvcTest}, que não
 * carregam componentes comuns, ele é a única implementação disponível.
 * Considera que todo domínio tem MX; o formato do e-mail continua validado
 * pela expressão regular real.
 * </p>
 */
@TestConfiguration
public class MxRecordLookupTestConfig {

  @Bean
  @Primary
  MxRecordLookup mxRecordLookup() {
    return domain -> true;
  }
}
