package com.albertsilva.dev.dscatalog.security.oauth2.resource.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuração do <b>Resource Server</b>: protege a API com JWT, define quais
 * rotas são públicas, habilita a segurança por método e configura o CORS.
 *
 * <p>
 * <b>Cadeias de filtros:</b>
 * </p>
 * <ul>
 * <li>{@code @Order(1)} — H2 Console: só existe se
 * {@code spring.h2.console.enabled=true} (perfil {@code test}); casa somente
 * com o console, sem regras de autorização, com CSRF e {@code frameOptions}
 * desabilitados;</li>
 * <li>{@code @Order(2)} — Authorization Server (em
 * {@code AuthorizationServerConfig}): {@code /oauth2/**} e
 * {@code /.well-known/**};</li>
 * <li>{@code @Order(3)} — Resource Server (este {@link #rsSecurityFilterChain}):
 * sem {@code securityMatcher}, atende todas as demais requisições.</li>
 * </ul>
 *
 * <p>
 * <b>Regras de URL da cadeia do Resource Server</b> (em ordem): {@code GET} em
 * {@code /api/v1/categories/**}, {@code /api/v1/products/**} e
 * {@code /api/v1/accounts/**} — públicos; {@code POST} em
 * {@code /api/v1/accounts/**} — público; documentação
 * ({@code /docs-asjcatalog}, {@code /docs-asjcatalog/**},
 * {@code /docs-asjcatalog.html}, {@code /swagger-ui/**}) — pública; qualquer
 * outra requisição — exige autenticação. Esses caminhos de documentação
 * correspondem ao perfil {@code test}; {@code application-dev.properties}
 * configura outros. A autorização fina é feita por {@code @PreAuthorize} nos
 * controllers ({@code @EnableMethodSecurity}); rotas públicas "por URL" podem
 * ter regra própria no método (por exemplo, {@code GET /api/v1/accounts/me}).
 * </p>
 *
 * <p>
 * <b>Autenticação:</b> {@code oauth2ResourceServer().jwt()} com o
 * {@code JwtDecoder} da aplicação; o {@link #jwtAuthenticationConverter()}
 * transforma o claim {@code authorities} em authorities do Spring Security.
 * CSRF está desabilitado nesta cadeia; a política de sessão não é configurada
 * explicitamente.
 * </p>
 *
 * <p>
 * <b>CORS:</b> origens de {@code cors.origins}; ver
 * {@link #corsConfigurationSource()}.
 * </p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

  /** Caminhos públicos da documentação OpenAPI/Swagger (os do perfil {@code test}). */
  private static final String[] DOCUMENTATION_OPENAPI = { "/docs-asjcatalog", "/docs-asjcatalog/**",
      "/docs-asjcatalog.html", "/swagger-ui/**" };
  /** Prefixos liberados sem autenticação para requisições {@code GET}. */
  private static final String[] PUBLIC_GET_ENDPOINTS = { "/api/v1/categories/**", "/api/v1/products/**" , "/api/v1/accounts/**"};

  /** Prefixos liberados sem autenticação para requisições {@code POST}. */
  private static final String[] PUBLIC_POST_ENDPOINTS = { "/api/v1/accounts/**" };

  /** Origens CORS permitidas, separadas por vírgula ({@code cors.origins}). */
  @Value("${cors.origins}")
  private String corsOrigins;

  /**
   * Cadeia do console do H2 ({@code @Order(1)}), criada somente quando
   * {@code spring.h2.console.enabled=true}.
   *
   * <p>
   * Casa apenas com o caminho do console ({@code PathRequest.toH2Console()}),
   * desabilita CSRF e {@code frameOptions} e <b>não define regras de
   * autorização</b>, isto é, o console fica acessível sem autenticação nessa
   * condição.
   * </p>
   *
   * @param http construtor de segurança HTTP
   * @return cadeia do H2 Console
   * @throws Exception se a configuração falhar
   */
  @Bean
  @Order(1)
  @ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
  public SecurityFilterChain h2SecurityFilterChain(HttpSecurity http) throws Exception {

    http.securityMatcher(PathRequest.toH2Console()).csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
    return http.build();
  }

  /**
   * Cadeia principal do Resource Server ({@code @Order(3)}).
   *
   * <p>
   * Desabilita CSRF; aplica as regras de URL descritas na documentação da classe
   * (públicos por {@code GET}/{@code POST}, documentação pública, demais rotas
   * autenticadas); ativa {@code oauth2ResourceServer().jwt()} e o CORS com
   * {@link #corsConfigurationSource()}. Requisição sem token válido em rota
   * protegida é rejeitada pelo filtro de bearer token (o formato da resposta
   * será detalhado na análise da camada web).
   * </p>
   *
   * @param http construtor de segurança HTTP
   * @return cadeia do Resource Server
   * @throws Exception se a configuração falhar
   */
  @Bean
  @Order(3)
  public SecurityFilterChain rsSecurityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.authorizeHttpRequests(authorize -> authorize
        // Public endpoints
        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()

        // Public POST endpoints
        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()

        // Swagger / OpenAPI
        .requestMatchers(DOCUMENTATION_OPENAPI).permitAll()

        // Any other request
        .anyRequest().authenticated());
    http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
    return http.build();
  }

  /**
   * Converte um JWT validado em {@code JwtAuthenticationToken}, lendo as
   * authorities do claim <b>{@code authorities}</b> com prefixo <b>vazio</b>.
   *
   * <ul>
   * <li>Cada texto do claim vira uma {@code GrantedAuthority} <b>como está</b>
   * (por exemplo, {@code ROLE_ADMIN}); assim {@code hasRole('ADMIN')}, que
   * procura {@code ROLE_ADMIN}, funciona.</li>
   * <li>O claim {@code scope} <b>não</b> é convertido em authorities (o nome do
   * claim foi trocado).</li>
   * <li>O principal do {@code Authentication} é o próprio {@code Jwt}; o nome
   * ({@code getName()}) usa o claim padrão do conversor ({@code sub}), que não é
   * o id nem o e-mail do usuário.</li>
   * </ul>
   *
   * <p>
   * O bean é usado pelo {@code jwt()} da cadeia (o framework o localiza no
   * contexto; os testes de integração de autorização confirmam o efeito).
   * </p>
   *
   * @return conversor de JWT para autenticação
   */
  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
    grantedAuthoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }

  /**
   * Configuração de CORS aplicada a todos os caminhos ({@code /**}).
   *
   * <p>
   * Origens: {@code cors.origins} dividido por vírgula, usado como <em>padrões de
   * origem</em> (aceitam curingas). Métodos permitidos: {@code POST, GET, PUT,
   * DELETE, PATCH}. Cabeçalhos permitidos: apenas {@code Authorization} e
   * {@code Content-Type}. Credenciais permitidas. {@code OPTIONS} não consta da
   * lista de métodos; como o CORS confere o método <em>solicitado</em> no
   * pré-voo, isso, em princípio, não impede o pré-voo (a confirmar em testes).
   * </p>
   *
   * @return fonte de configuração de CORS
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {

    String[] origins = corsOrigins.split(",");

    CorsConfiguration corsConfig = new CorsConfiguration();
    corsConfig.setAllowedOriginPatterns(Arrays.asList(origins));
    corsConfig.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "DELETE", "PATCH"));
    corsConfig.setAllowCredentials(true);
    corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);
    return source;
  }

  /**
   * Registra um {@code CorsFilter} como filtro de servlet, com a maior
   * precedência, usando a mesma configuração de
   * {@link #corsConfigurationSource()}. Existe além do {@code http.cors(...)} da
   * cadeia de segurança.
   *
   * @return registro do filtro CORS
   */
  @Bean
  FilterRegistrationBean<CorsFilter> filterRegistrationBeanCorsFilter() {
    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource()));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
}