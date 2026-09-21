package com.albertsilva.dev.dscatalog.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Registra o {@code PasswordEncoder} da aplicação.
 *
 * <p>
 * O único bean é um {@link BCryptPasswordEncoder} com a <b>força padrão</b> do
 * framework (a força não é informada no código). Esse encoder é usado para
 * codificar senhas ({@code UserService}, {@code AccountService}), para
 * conferir a senha no login ({@code CustomPasswordAuthenticationProvider}) e
 * para codificar o segredo do cliente OAuth2
 * ({@code AuthorizationServerConfig}).
 * </p>
 */
@Configuration
public class SecurityBeansConfig {

  /**
   * Cria o {@link BCryptPasswordEncoder}. O método tem visibilidade de pacote e o
   * bean é um singleton do contexto. Cada {@code encode()} gera um hash diferente
   * (salt aleatório); {@code matches()} confere senha e hash.
   *
   * @return codificador BCrypt com a força padrão
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
