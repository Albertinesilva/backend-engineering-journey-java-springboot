package com.albertsilva.dev.dscatalog.config.i18n;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class MessageSourceConfig {

  @Bean
  MessageSource messageSource() {

    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();

    messageSource.setBasename("classpath:messages");
    messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
    messageSource.setFallbackToSystemLocale(false);
    messageSource.setDefaultLocale(new Locale("pt", "BR"));

    return messageSource;
  }

  @Bean
  AcceptHeaderLocaleResolver localeResolver() {

    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

    resolver.setDefaultLocale(new Locale("pt", "BR"));

    return resolver;
  }

}