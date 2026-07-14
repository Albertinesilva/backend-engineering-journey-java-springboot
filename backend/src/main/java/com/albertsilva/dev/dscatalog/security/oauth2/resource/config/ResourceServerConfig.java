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
 * Configuração do Resource Server OAuth2 para proteção de endpoints da API.
 *
 * <p>
 * Esta classe é responsável por configurar a segurança de todos os endpoints
 * da aplicação como um Resource Server protegido por OAuth2 com validação de
 * tokens JWT.
 * Define regras de acesso, proteção contra CSRF, validação de tokens e CORS.
 * </p>
 *
 * <p>
 * <b>Responsabilidades principais:</b>
 * </p>
 * <ul>
 * <li>Configurar proteção de endpoints com OAuth2 JWT</li>
 * <li>Definir endpoints públicos que não requerem autenticação</li>
 * <li>Permitir acesso ao H2 Console em desenvolvimento</li>
 * <li>Configurar CORS para aceitar requisições de origins específicos</li>
 * <li>Extrair authorities (roles) dos claims JWT</li>
 * <li>Habilitar @PreAuthorize/@PostAuthorize para method-level security</li>
 * </ul>
 *
 * <p>
 * <b>Cadeias de filtros de segurança:</b>
 * </p>
 * <ul>
 * <li><b>Order 1 (H2Console):</b> Permite acesso ao console H2 sem autenticação
 * (desenvolvimento)</li>
 * <li><b>Order 3 (Resource Server):</b> Protege todos os endpoints da API com
 * JWT</li>
 * </ul>
 *
 * <p>
 * <b>Endpoints públicos:</b>
 * </p>
 * <ul>
 * <li>GET {@code /api/v1/categories/**}: Listagem e detalhes de categorias</li>
 * <li>GET {@code /api/v1/products/**}: Listagem e detalhes de produtos</li>
 * <li>{@code /docs-dscatalog}, {@code /swagger-ui/**}: Documentação
 * OpenAPI</li>
 * </ul>
 *
 * <p>
 * <b>Validação de tokens:</b>
 * </p>
 * <p>
 * Todos os outros endpoints requerem um token JWT válido no header
 * Authorization.
 * O token é validado usando a chave pública RSA publicada no Authorization
 * Server.
 * Claims de authorities são extraídos do claim "authorities" do JWT.
 * </p>
 *
 * <p>
 * <b>CORS:</b>
 * </p>
 * <p>
 * Configurado dinamicamente a partir da propriedade {@code cors.origins}.
 * Permite requisições preflight e credenciais de múltiplos origins.
 * Métodos HTTP permitidos: GET, POST, PUT, DELETE, PATCH.
 * Headers obrigatórios: Authorization, Content-Type.
 * </p>
 *
 * @implNote
 *           O ResourceServerConfig utiliza @Order para ordenar
 *           SecurityFilterChains
 *           em relação ao AuthorizationServerConfig. A ordem é crítica para que
 *           o servidor de autorização (order 2) processe tokens antes do
 *           Resource Server.
 *           O filtro CORS é registrado com HIGHEST_PRECEDENCE para interceptar
 *           requisições preflight antes de outros filtros.
 *
 * @apiNote
 *          Esta configuração implementa um Resource Server seguindo as
 *          especificações de Spring Security 6.x e Spring Authorization Server,
 *          permitindo que diferentes aplicações clientes obtenham tokens do
 *          Authorization Server e os utilizem para acessar este Resource
 *          Server.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

  private static final String[] DOCUMENTATION_OPENAPI = { "/docs-asjcatalog", "/docs-asjcatalog/**",
      "/docs-asjcatalog.html", "/swagger-ui/**" };
  private static final String[] PUBLIC_GET_ENDPOINTS = { "/api/v1/categories/**", "/api/v1/products/**" , "/api/v1/accounts/**"};

  private static final String[] PUBLIC_POST_ENDPOINTS = { "/api/v1/accounts/**" };

  @Value("${cors.origins}")
  private String corsOrigins;

  /**
   * Configura a cadeia de filtros de segurança para o H2 Console.
   *
   * <p>
   * Este filtro permitir acesso ao H2 Console sem autenticação, desabilitando
   * CSRF
   * e frame options. É aplicado apenas quando a propriedade
   * {@code spring.h2.console.enabled}
   * estiver definida como "true" (desenvolvimento).
   * </p>
   *
   * <p>
   * <b>Configurações aplicadas:</b>
   * </p>
   * <ul>
   * <li>CSRF desabilitado para permitir requisições POST do console</li>
   * <li>Frame options desabilitadas para permitir renderização em iframe</li>
   * <li>Sem autenticação ou autorização necessária</li>
   * </ul>
   *
   * @param http construtor de segurança HTTP do Spring
   * @return {@link SecurityFilterChain} configurada para H2 Console
   * @throws Exception se houver erro na configuração de segurança
   *
   * @implNote
   *           Este bean é registrado com {@code @Order(1)} e
   *           {@code @ConditionalOnProperty},
   *           garantindo que seja aplicado apenas em desenvolvimento. Em
   *           produção,
   *           a propriedade deve estar desabilitada para segurança.
   *
   * @apiNote
   *          H2 Console requer configurações especiais de segurança. Nunca
   *          habilite
   *          em ambientes de produção sem restrições adicionais de IP/rede.
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
   * Configura a cadeia de filtros de segurança para o Resource Server.
   *
   * <p>
   * Define as regras de autorização para todos os endpoints da API, permitindo
   * acesso público a documentação e endpoints específicos (GET para categorias e
   * produtos),
   * requerendo autenticação OAuth2 com JWT para demais endpoints.
   * </p>
   *
   * <p>
   * <b>Regras de autorização (em ordem de avaliação):</b>
   * </p>
   * <ol>
   * <li>GET {@code /api/v1/categories/**} e {@code /api/v1/products/**}:
   * Público</li>
   * <li>Endpoints de documentação OpenAPI: Público</li>
   * <li>Todos os demais endpoints: Requerem autenticação (JWT válido)</li>
   * </ol>
   *
   * <p>
   * <b>Segurança adicional:</b>
   * </p>
   * <ul>
   * <li>CSRF desabilitado (stateless API com JWT)</li>
   * <li>OAuth2 Resource Server com validação JWT automática</li>
   * <li>CORS habilitado com configuração customizada</li>
   * </ul>
   *
   * @param http construtor de segurança HTTP do Spring
   * @return {@link SecurityFilterChain} configurada para o Resource Server
   * @throws Exception se houver erro na configuração de segurança
   *
   * @implNote
   *           Este bean é registrado com {@code @Order(3)}, posicionando-se
   *           após H2Console (order 1) e AuthorizationServer (order 2).
   *           A ordem é crucial para que requisições sejam processadas
   *           corretamente.
   *
   * @apiNote
   *          Mudanças nas regras de autorização aqui requerem sincronização
   *          com autoridades de usuário (@PreAuthorize no controller).
   *          Use @PreAuthorize/@PostAuthorize para lógica de autorização
   *          detalhada.
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
   * Cria um conversor de JWT customizado para extrair authorities.
   *
   * <p>
   * Configura o {@link JwtAuthenticationConverter} para extrair roles/authorities
   * do claim "authorities" do JWT sem adicionar prefixo (ex: "ROLE_").
   * </p>
   *
   * <p>
   * <b>Configuração:</b>
   * </p>
   * <ul>
   * <li>Nome do claim: "authorities" (customizado, não o padrão "scope")</li>
   * <li>Prefixo de autoridade: "" (vazio, sem "ROLE_" adicional)</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo de JWT claim:</b>
   * </p>
   *
   * <pre>
   * {
   *   "authorities": ["ROLE_ADMIN", "ROLE_USER"],
   *   "username": "user123"
   * }
   * </pre>
   *
   * <p>
   * As authorities são então disponibilizadas para validação via
   * {@code @PreAuthorize("hasRole('ADMIN')")} nos controllers.
   * </p>
   *
   * @return {@link JwtAuthenticationConverter} configurado para extrair
   *         authorities
   *
   * @implNote
   *           Este bean é consumido automaticamente pelo
   *           {@code oauth2ResourceServer().jwt()}, garantindo que todos os
   *           tokens
   *           sejam processados com a configuração customizada.
   *
   * @apiNote
   *          O conversor garante que claims de authorities do Authorization
   *          Server
   *          sejam corretamente mapeados para a estrutura de autoridades do
   *          Spring Security.
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
   * Configura a fonte de configuração CORS para a aplicação.
   *
   * <p>
   * Define os origins permitidos, métodos HTTP, headers, e credenciais
   * a partir de propriedades de configuração. Origins são carregados de
   * {@code cors.origins} (separados por vírgula).
   * </p>
   *
   * <p>
   * <b>Configuração aplicada:</b>
   * </p>
   * <ul>
   * <li>Origins: Configuráveis via propriedade (ex:
   * "http://localhost:3000,https://example.com")</li>
   * <li>Métodos: POST, GET, PUT, DELETE, PATCH</li>
   * <li>Credenciais: Habilitadas (permite cookies e headers Authorization)</li>
   * <li>Headers: Authorization, Content-Type</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo de configuração (application.properties):</b>
   * </p>
   *
   * <pre>
   * cors.origins=http://localhost:3000,http://localhost:8080,https://app.example.com
   * </pre>
   *
   * @return {@link CorsConfigurationSource} configurada com origins e métodos
   *         permitidos
   *
   * @implNote
   *           A configuração é aplicada a todos os caminhos ("/**").
   *           Origins são convertidos em padrões de origem usando
   *           setAllowedOriginPatterns(),
   *           que suporta wildcards (ex: "http://*.example.com").
   *
   * @apiNote
   *          CORS é fundamental para aplicações web. Certifique-se de que
   *          apenas origins confiáveis estejam listados em produção.
   *          Nunca use "*" para origins em produção sem validação adicional.
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
   * Registra um filtro CORS no pipeline de filtros de servlet.
   *
   * <p>
   * Cria um bean {@link FilterRegistrationBean} que envolve o {@link CorsFilter}
   * e o registra com a ordem mais alta ({@code HIGHEST_PRECEDENCE}),
   * garantindo que requisições CORS preflight sejam processadas antes de
   * qualquer outro filtro de segurança.
   * </p>
   *
   * <p>
   * <b>Razão da ordem alta:</b>
   * </p>
   * <ul>
   * <li>Requisições preflight (OPTIONS) devem ser respondidas rapidamente</li>
   * <li>Não devem ser processadas por filtros de autenticação/autorização</li>
   * <li>Ordem alta garante execução antes de SecurityFilter</li>
   * </ul>
   *
   * @return {@link FilterRegistrationBean} com o CorsFilter configurado
   *
   * @implNote
   *           O {@code CorsFilter} é criado usando
   *           {@link #corsConfigurationSource()}
   *           para garantir consistência com a configuração de CORS do
   *           SecurityFilterChain.
   *           A ordem {@code Ordered.HIGHEST_PRECEDENCE} posiciona este filtro
   *           no início do pipeline, antes de todos os outros filtros.
   *
   * @apiNote
   *          Este bean é necessário para que o CORS funcione corretamente
   *          em aplicações Spring Boot com Security. O filtro processa
   *          requisições OPTIONS automaticamente.
   */
  @Bean
  FilterRegistrationBean<CorsFilter> filterRegistrationBeanCorsFilter() {
    FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource()));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
}