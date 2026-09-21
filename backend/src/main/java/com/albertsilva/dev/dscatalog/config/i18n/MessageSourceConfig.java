package com.albertsilva.dev.dscatalog.config.i18n;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Internacionalização das mensagens da API: {@code MessageSource} sobre
 * {@code messages_{pt_BR,en,es}.properties} e resolvedor de {@code Locale} a
 * partir do cabeçalho {@code Accept-Language}. Usados por
 * {@code ControllerExceptionHandler} e pela interpolação das mensagens de Bean
 * Validation.
 */
@Configuration
public class MessageSourceConfig {

  /**
   * @return {@code MessageSource} com base {@code classpath:messages}, UTF-8,
   *         idioma padrão {@code pt_BR} e sem recuo para o idioma do sistema
   */
  @Bean
  MessageSource messageSource() {

    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setDefaultLocale(new Locale("pt", "BR"));

    return messageSource;
  }

  /**
   * @return resolvedor que usa o {@code Accept-Language} da requisição, com
   *         {@code pt_BR} como padrão (este bean prevalece sobre as propriedades
   *         {@code spring.web.locale*})
   */
  @Bean
  AcceptHeaderLocaleResolver localeResolver() {

    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

    resolver.setDefaultLocale(new Locale("pt", "BR"));

    return resolver;
  }

}