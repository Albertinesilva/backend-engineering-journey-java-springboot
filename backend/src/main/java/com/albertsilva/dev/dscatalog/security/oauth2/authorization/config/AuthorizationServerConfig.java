package com.albertsilva.dev.dscatalog.security.oauth2.authorization.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;

import com.albertsilva.dev.dscatalog.security.oauth2.grant.password.CustomPasswordAuthenticationConverter;
import com.albertsilva.dev.dscatalog.security.oauth2.grant.password.CustomPasswordAuthenticationProvider;
import com.albertsilva.dev.dscatalog.security.userdetails.AuthenticatedUser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Configuração do servidor de autorização OAuth2 para a aplicação.
 *
 * <p>
 * Esta classe é responsável por configurar todos os componentes necessários
 * para implementar um servidor de autorização OAuth2 baseado em JWT,
 * permitindo autenticação e autorização de clientes na aplicação.
 * </p>
 *
 * <p>
 * <b>Responsabilidades principais:</b>
 * </p>
 * <ul>
 * <li>Configurar o fluxo de autorização (Authorization Code Flow, Password
 * Grant)</li>
 * <li>Definir parâmetros de tokens JWT (formato, tempo de vida, claims
 * customizados)</li>
 * <li>Gerenciar clientes registrados e suas credenciais</li>
 * <li>Configurar geração e validação de JWT com RSA 2048</li>
 * <li>Customizar claims de autoridades e dados de usuário nos tokens</li>
 * </ul>
 *
 * <p>
 * <b>Fluxo de autorização:</b>
 * </p>
 * <p>
 * A aplicação utiliza o grant type "password" (Resource Owner Password
 * Credentials)
 * adaptado com um conversor customizado
 * ({@link CustomPasswordAuthenticationConverter})
 * e provedor de autenticação customizado
 * ({@link CustomPasswordAuthenticationProvider}).
 * Este fluxo permite que clientes obtenham tokens JWT fornecendo credenciais de
 * usuário
 * diretamente, adequado para aplicações desktop ou mobile confiáveis.
 * </p>
 *
 * <p>
 * <b>Segurança de tokens:</b>
 * </p>
 * <ul>
 * <li>Algoritmo: RSA 2048 para assinatura e validação de JWT</li>
 * <li>Formato: Self-Contained (toda informação no token JWT)</li>
 * <li>Duração configurável via propriedade {@code security.jwt.duration}</li>
 * <li>Claims customizados: authorities e username do usuário autenticado</li>
 * </ul>
 *
 * <p>
 * <b>Gerenciamento de estado:</b>
 * </p>
 * <p>
 * Utiliza repositórios em memória para armazenar autorizações e consentimentos
 * ({@link InMemoryOAuth2AuthorizationService} e
 * {@link InMemoryOAuth2AuthorizationConsentService}).
 * Para ambientes de produção com múltiplas instâncias, considere usar
 * implementações
 * baseadas em banco de dados.
 * </p>
 *
 * @implNote
 *           Os beans de autorização são registrados com @Order(2) no
 *           SecurityFilterChain,
 *           posicionando-se após a configuração H2 e antes da configuração de
 *           Resource Server.
 *           O cliente é registrado em memória com credenciais recuperadas de
 *           propriedades
 *           de configuração da aplicação.
 *
 * @apiNote
 *          Esta configuração implementa um OAuth2 Authorization Server seguindo
 *          as especificações de Spring Authorization Server, integrando-se com
 *          Spring Security para proteger endpoints e validar tokens JWT.
 */
@Configuration
public class AuthorizationServerConfig {

  @Value("${security.client-id}")
  private String clientId;

  @Value("${security.client-secret}")
  private String clientSecret;

  @Value("${security.jwt.duration}")
  private Integer jwtDurationSeconds;

  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;

  /**
   * Construtor da configuração de servidor de autorização.
   *
   * @param userDetailsService serviço para carregar detalhes de usuários
   * @param passwordEncoder    encoder para validação de senhas
   */
  public AuthorizationServerConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Configura a cadeia de filtros de segurança para o servidor de autorização.
   *
   * <p>
   * Este filtro intercita requisições aos endpoints OAuth2 ({@code /oauth2/**})
   * e ao endpoint de descoberta OpenID Connect ({@code /.well-known/**}),
   * aplicando as configurações do Authorization Server.
   * </p>
   *
   * <p>
   * <b>Conversores e provedores customizados:</b>
   * </p>
   * <ul>
   * <li>{@link CustomPasswordAuthenticationConverter}: Converte requisições de
   * token
   * com grant type "password" para tokens de autenticação</li>
   * <li>{@link CustomPasswordAuthenticationProvider}: Autentica usuários e gera
   * tokens JWT</li>
   * </ul>
   *
   * @param http construtor de segurança HTTP do Spring
   * @return {@link SecurityFilterChain} configurada para o Authorization Server
   * @throws Exception se houver erro na configuração de segurança
   *
   * @implNote
   *           Este bean é registrado com {@code @Order(2)}, posicionando-se
   *           após a configuração H2 ({@code @Order(1)}) e antes do Resource
   *           Server
   *           ({@code @Order(3)}).
   */
  @Bean
  @Order(2)
  public SecurityFilterChain asSecurityFilterChain(HttpSecurity http) throws Exception {

    http.securityMatcher("/oauth2/**", "/.well-known/**")
        .with(OAuth2AuthorizationServerConfigurer.authorizationServer(), Customizer.withDefaults());

    // @formatter:off
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).tokenEndpoint(tokenEndpoint -> tokenEndpoint.accessTokenRequestConverter
      (new CustomPasswordAuthenticationConverter()).authenticationProvider
      (new CustomPasswordAuthenticationProvider(authorizationService(), tokenGenerator(), userDetailsService, passwordEncoder)));

		http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(Customizer.withDefaults()));
		// @formatter:on

    return http.build();
  }

  /**
   * Cria um serviço de autorização em memória.
   *
   * <p>
   * Este serviço armazena as autorizações concedidas durante o fluxo OAuth2.
   * Utiliza uma implementação em memória adequada para desenvolvimento e testes;
   * para produção com múltiplas instâncias, implemente uma versão baseada em
   * banco de dados.
   * </p>
   *
   * @return {@link OAuth2AuthorizationService} para gerenciar autorizações
   *
   * @apiNote
   *          Integra-se com o Authorization Server para rastrear tokens emitidos
   *          e seus escopos e claims associados.
   */
  @Bean
  public OAuth2AuthorizationService authorizationService() {
    return new InMemoryOAuth2AuthorizationService();
  }

  /**
   * Cria um serviço de consentimento de autorização em memória.
   *
   * <p>
   * Este serviço gerencia o consentimento dos usuários para que clientes acessem
   * recursos específicos com determinados escopos. Também utiliza armazenamento
   * em memória, adequado para desenvolvimento.
   * </p>
   *
   * @return {@link OAuth2AuthorizationConsentService} para gerenciar
   *         consentimentos
   */
  @Bean
  public OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService() {
    return new InMemoryOAuth2AuthorizationConsentService();
  }

  /**
   * Registra e configura clientes OAuth2 autorizados a utilizar a aplicação.
   *
   * <p>
   * Define um cliente único com credenciais recuperadas de propriedades de
   * configuração.
   * O cliente é configurado para utilizar o grant type "password" customizado,
   * apropriado para aplicações desktop/mobile confiáveis.
   * </p>
   *
   * <p>
   * <b>Configuração do cliente:</b>
   * </p>
   * <ul>
   * <li>ID e segredo carregados de {@code security.client-id} e
   * {@code security.client-secret}</li>
   * <li>Segredo é codificado com o {@link PasswordEncoder} para segurança</li>
   * <li>Escopos: "read" e "write" para controle de acesso baseado em escopos</li>
   * <li>Grant type: "password" para fluxo de credenciais do proprietário de
   * recurso</li>
   * </ul>
   *
   * @return {@link RegisteredClientRepository} contendo clientes autorizados
   *
   * @implNote
   *           Utiliza UUID aleatório como ID interno do cliente.
   *           Para adicionar mais clientes, expanda este método com
   *           {@code new InMemoryRegisteredClientRepository(client1, client2, ...)}
   */
  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    // @formatter:off
		RegisteredClient registeredClient = RegisteredClient
			.withId(UUID.randomUUID().toString())
			.clientId(clientId)
			.clientSecret(passwordEncoder.encode(clientSecret))
			.scope("read")
			.scope("write")
			.authorizationGrantType(new AuthorizationGrantType("password"))
			.tokenSettings(tokenSettings())
			.clientSettings(clientSettings())
			.build();
		// @formatter:on

    return new InMemoryRegisteredClientRepository(registeredClient);
  }

  /**
   * Configura as propriedades dos tokens de acesso (JWT).
   *
   * <p>
   * Define formato self-contained (JWT completo com claims) e tempo de vida
   * do token recuperado de propriedade de configuração.
   * </p>
   *
   * <p>
   * <b>Configurações:</b>
   * </p>
   * <ul>
   * <li>Formato: {@link OAuth2TokenFormat#SELF_CONTAINED} (JWT com todas as
   * informações)</li>
   * <li>Tempo de vida: configurável via {@code security.jwt.duration} em
   * segundos</li>
   * </ul>
   *
   * @return {@link TokenSettings} com configurações de token
   *
   * @apiNote
   *          O tempo de vida deve ser configurado considerando segurança
   *          (tokens de longa duração aumentam exposição) versus UX (tokens
   *          de curta duração requerem refresh frequente).
   */
  @Bean
  public TokenSettings tokenSettings() {
    // @formatter:off
		return TokenSettings.builder()
			.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
			.accessTokenTimeToLive(Duration.ofSeconds(jwtDurationSeconds))
			.build();
		// @formatter:on
  }

  /**
   * Configura as propriedades dos clientes registrados.
   *
   * <p>
   * Atualmente utiliza configurações padrão do Spring Authorization Server.
   * Pode ser expandida para customizações específicas como requisitos de
   * autenticação
   * adicionais ou restrições de redirecionamento.
   * </p>
   *
   * @return {@link ClientSettings} com configurações de cliente
   */
  @Bean
  public ClientSettings clientSettings() {
    return ClientSettings.builder().build();
  }

  /**
   * Configura as propriedades gerais do servidor de autorização.
   *
   * <p>
   * Define configurações globais do Authorization Server, como endpoints
   * de descoberta e informações públicas do servidor.
   * </p>
   *
   * @return {@link AuthorizationServerSettings} com configurações do servidor
   */
  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }

  /**
   * Cria o gerador de tokens OAuth2 com suporte a JWT.
   *
   * <p>
   * Integra um {@link JwtGenerator} e um {@link OAuth2AccessTokenGenerator}
   * para gerar JWT assinados e tokens de acesso com claims customizados
   * (autoridades e nome de usuário).
   * </p>
   *
   * <p>
   * <b>Processo de geração:</b>
   * </p>
   * <ol>
   * <li>JWT é gerado com o {@link NimbusJwtEncoder} usando chave RSA privada</li>
   * <li>Customizador de token injeta claims adicionais (authorities,
   * username)</li>
   * <li>Token de acesso é gerado e retornado ao cliente</li>
   * </ol>
   *
   * @return {@link OAuth2TokenGenerator} capaz de gerar tokens OAuth2/JWT
   *
   * @implNote
   *           O {@link DelegatingOAuth2TokenGenerator} permite múltiplos
   *           geradores trabalharem em conjunto para criação de tokens complexos.
   */
  @Bean
  public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator() {
    NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
    JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
    jwtGenerator.setJwtCustomizer(tokenCustomizer());
    OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
    return new DelegatingOAuth2TokenGenerator(jwtGenerator, accessTokenGenerator);
  }

  /**
   * Customiza os claims do JWT com informações do usuário autenticado.
   *
   * <p>
   * Injeta as autoridades (roles) e nome de usuário no token JWT,
   * permitindo que o Resource Server valide permissões sem consultar o banco de
   * dados.
   * </p>
   *
   * <p>
   * <b>Claims adicionados ao access_token:</b>
   * </p>
   * <ul>
   * <li>{@code authorities}: Lista de autoridades/roles do usuário</li>
   * <li>{@code username}: Nome de login do usuário autenticado</li>
   * </ul>
   *
   * @return {@link OAuth2TokenCustomizer} para customização de JWT
   *
   * @apiNote
   *          Os claims adicionados tornam o token self-contained, eliminando
   *          necessidade de consulta ao servidor para validação de permissões.
   *          Cuidado: tokens contêm informações visíveis (não criptografadas),
   *          apenas assinadas. Nunca adicione dados sensíveis como senhas.
   */
  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return context -> {
      OAuth2ClientAuthenticationToken principal = context.getPrincipal();
      AuthenticatedUser user = (AuthenticatedUser) principal.getDetails();
      List<String> authorities = user.getAuthorities().stream().map(x -> x.getAuthority()).toList();
      if (context.getTokenType().getValue().equals("access_token")) {
        // @formatter:off
				context.getClaims()
          .claim("authorities", authorities)
          .claim("userId", user.getId())
          .claim("username", user.getUsername());
				// @formatter:on
      }
    };
  }

  /**
   * Cria um decodificador de JWT para validação de tokens.
   *
   * <p>
   * Utiliza o Resource Server do Spring Authorization para criar um decodificador
   * que valida a assinatura de JWT usando a chave pública RSA.
   * </p>
   *
   * @param jwkSource fonte de chaves JWK para validação de assinatura
   * @return {@link JwtDecoder} para decodificar e validar tokens JWT
   */
  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  /**
   * Cria a fonte de chaves JWK (JSON Web Key) para assinatura e validação de JWT.
   *
   * <p>
   * Gera um par de chaves RSA 2048, empacotando a chave pública em um conjunto
   * JWK
   * que pode ser publicado no endpoint de descoberta JWKS
   * ({@code /.well-known/jwks.json})
   * para que clientes externos validem tokens.
   * </p>
   *
   * @return {@link JWKSource} contendo o conjunto de chaves públicas
   *
   * @implNote
   *           A chave privada é mantida internamente e usada apenas para
   *           assinatura. A chave pública é publicada para que terceiros
   *           possam validar a autenticidade dos tokens.
   */
  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = generateRsa();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  /**
   * Gera um par de chaves RSA com ID único para assinatura de JWT.
   *
   * <p>
   * Cria uma chave RSA encapsulada em um {@link RSAKey} com ID único (UUID),
   * pronta para ser usada pelo encoder JWT.
   * </p>
   *
   * @return {@link RSAKey} contendo as chaves pública e privada RSA
   *
   * @implNote
   *           Método privado que encapsula a geração de chave RSA,
   *           delegando para {@link #generateRsaKey()}.
   */
  private static RSAKey generateRsa() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
  }

  /**
   * Gera um par de chaves RSA 2048.
   *
   * <p>
   * Utiliza {@link KeyPairGenerator} para criar um par de chaves RSA
   * com comprimento de 2048 bits, apropriado para produção.
   * </p>
   *
   * @return {@link KeyPair} contendo a chave pública e privada RSA
   * @throws IllegalStateException se houver erro ao gerar o par de chaves
   *
   * @implNote
   *           RSA 2048 oferece segurança adequada para a maioria dos casos.
   *           Para aplicações com requisitos de segurança extremamente altos,
   *           considere RSA 4096, com impacto na performance.
   */
  private static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }
}