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
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator;
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
 * Configuração do <b>Authorization Server</b> (Spring Authorization Server),
 * que roda no mesmo processo do Resource Server e emite os tokens usados pela
 * API.
 *
 * <p>
 * <b>O que esta classe configura:</b>
 * </p>
 * <ul>
 * <li>a cadeia de filtros dos endpoints OAuth2 ({@code /oauth2/**} e
 * {@code /.well-known/**}), registrando no <em>token endpoint</em> o conversor
 * e o provider do grant {@code password} customizado
 * ({@link CustomPasswordAuthenticationConverter} e
 * {@link CustomPasswordAuthenticationProvider});</li>
 * <li>um <b>único cliente registrado</b>, em memória, com os grants
 * {@code password} e {@code refresh_token} e os escopos {@code read} e
 * {@code write};</li>
 * <li>as configurações de token: access token JWT autocontido, com validade
 * lida de {@code security.jwt.duration} (segundos), e refresh token de 30 dias
 * com rotação ({@code reuseRefreshTokens = false});</li>
 * <li>o gerador de tokens (JWT, token de acesso opaco e refresh token) e o
 * customizador que acrescenta ao JWT os claims {@code authorities},
 * {@code userId} e {@code username};</li>
 * <li>a chave RSA/JWK e o {@link JwtDecoder}, usados também pelo Resource
 * Server ({@code ResourceServerConfig}).</li>
 * </ul>
 *
 * <p>
 * <b>Estado em memória:</b> o cliente, as autorizações (incluindo os refresh
 * tokens), os consentimentos e o par de chaves RSA existem apenas na JVM em
 * execução e são recriados a cada inicialização. Consequências técnicas: um
 * reinício invalida todos os tokens emitidos, e duas instâncias da aplicação
 * teriam chaves e autorizações diferentes.
 * </p>
 *
 * <p>
 * <b>O que não está configurado:</b> {@code authorization_code},
 * {@code client_credentials}, consentimento, URIs de redirecionamento, OpenID
 * Connect, persistência das autorizações e qualquer mecanismo próprio de
 * revogação ou de logout.
 * </p>
 *
 * <p>
 * <b>Propriedades usadas:</b> {@code security.client-id},
 * {@code security.client-secret} (valores sensíveis, com padrão de
 * desenvolvimento em {@code application.properties}) e
 * {@code security.jwt.duration}.
 * </p>
 */
@Configuration
public class AuthorizationServerConfig {

  /** Identificador do cliente OAuth2 ({@code security.client-id}). */
  @Value("${security.client-id}")
  private String clientId;

  /** Segredo do cliente OAuth2 ({@code security.client-secret}); dado sensível, nunca deve ser registrado em log. */
  @Value("${security.client-secret}")
  private String clientSecret;

  /** Validade do access token, em segundos ({@code security.jwt.duration}). */
  @Value("${security.jwt.duration}")
  private Integer jwtDurationSeconds;

  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;

  /**
   * @param userDetailsService serviço que carrega o usuário no login (na
   *                           aplicação, {@code UserService}); repassado ao
   *                           provider do grant {@code password}
   * @param passwordEncoder    codificador de senhas: usado para codificar o
   *                           segredo do cliente e repassado ao provider, que o
   *                           usa para conferir a senha do usuário
   */
  public AuthorizationServerConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Cadeia de filtros dos endpoints do Authorization Server ({@code @Order(2)}).
   *
   * <p>
   * <b>Escopo:</b> só atende requisições que casam com {@code /oauth2/**} ou
   * {@code /.well-known/**}; as demais seguem para a cadeia do Resource Server
   * ({@code @Order(3)}).
   * </p>
   *
   * <p>
   * <b>Configuração:</b> aplica {@code OAuth2AuthorizationServerConfigurer} com os
   * padrões e registra, no token endpoint, um
   * {@link CustomPasswordAuthenticationConverter} e um
   * {@link CustomPasswordAuthenticationProvider} (criados com {@code new}, não
   * são beans), este último com {@link #authorizationService()},
   * {@link #tokenGenerator()}, o {@code UserDetailsService} e o
   * {@code PasswordEncoder}. Como são acrescentados ao token endpoint, os
   * conversores e providers padrão do framework continuam ativos: é assim que o
   * grant {@code refresh_token} é tratado, sem código próprio da aplicação.
   * Também habilita {@code oauth2ResourceServer().jwt()} nesta cadeia.
   * </p>
   *
   * <p>
   * Não há {@code authorizeHttpRequests} nesta cadeia: a autenticação do cliente
   * no token endpoint é feita pelos filtros do próprio Authorization Server.
   * </p>
   *
   * @param http construtor de segurança HTTP
   * @return cadeia de filtros do Authorization Server
   * @throws Exception se a configuração falhar
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
   * Armazenamento <b>em memória</b> das autorizações emitidas (cada login gera
   * uma, com o access token, o refresh token e o principal). Não é persistido:
   * os dados se perdem ao reiniciar e não são compartilhados entre instâncias.
   *
   * @return serviço de autorizações em memória
   */
  @Bean
  public OAuth2AuthorizationService authorizationService() {
    return new InMemoryOAuth2AuthorizationService();
  }

  /**
   * Armazenamento em memória dos consentimentos. Mantido pelo framework, mas o
   * cliente registrado não exige consentimento e nenhum fluxo do projeto o usa.
   *
   * @return serviço de consentimentos em memória
   */
  @Bean
  public OAuth2AuthorizationConsentService oAuth2AuthorizationConsentService() {
    return new InMemoryOAuth2AuthorizationConsentService();
  }

  /**
   * Registra, em memória, o <b>único</b> cliente OAuth2 da aplicação.
   *
   * <ul>
   * <li>identificador interno: UUID novo a cada inicialização;</li>
   * <li>{@code clientId} e segredo vêm das propriedades; o segredo é codificado
   * com o {@code PasswordEncoder} (BCrypt) na inicialização;</li>
   * <li>escopos: {@code read} e {@code write};</li>
   * <li>grants: {@code password} (grant customizado, criado como
   * {@code new AuthorizationGrantType("password")}) e {@code refresh_token};</li>
   * <li>usa os beans {@link #tokenSettings()} e {@link #clientSettings()};</li>
   * <li>não declara método de autenticação do cliente (vale o padrão do
   * framework; os testes de integração usam HTTP Basic), nem URIs de
   * redirecionamento.</li>
   * </ul>
   *
   * @return repositório em memória com esse único cliente
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
			.authorizationGrantType(
        new AuthorizationGrantType("password")
      )
      .authorizationGrantType(
        AuthorizationGrantType.REFRESH_TOKEN
      )
			.tokenSettings(tokenSettings())
			.clientSettings(clientSettings())
			.build();
		// @formatter:on

    return new InMemoryRegisteredClientRepository(registeredClient);
  }

  /**
   * Configurações dos tokens do cliente registrado.
   *
   * <ul>
   * <li>access token no formato {@code SELF_CONTAINED} (JWT);</li>
   * <li>validade do access token: {@code security.jwt.duration} segundos (padrão
   * de 86400 em {@code application.properties});</li>
   * <li>validade do refresh token: <b>30 dias, fixa no código</b>;</li>
   * <li>{@code reuseRefreshTokens = false}: a cada renovação, um novo refresh
   * token é emitido e o anterior deixa de ser aceito.</li>
   * </ul>
   *
   * @return configurações de token
   */
  @Bean
  public TokenSettings tokenSettings() {
    // @formatter:off
		return TokenSettings.builder()
			.accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
			.accessTokenTimeToLive(Duration.ofSeconds(jwtDurationSeconds))
      .refreshTokenTimeToLive(Duration.ofDays(30))
      .reuseRefreshTokens(false)
			.build();
		// @formatter:on
  }

  /**
   * Configurações do cliente com os valores padrão do framework (sem exigir
   * consentimento, sem PKCE obrigatório etc.).
   *
   * @return configurações padrão de cliente
   */
  @Bean
  public ClientSettings clientSettings() {
    return ClientSettings.builder().build();
  }

  /**
   * Configurações do servidor com os valores padrão: caminhos padrão dos
   * endpoints ({@code /oauth2/token}, {@code /oauth2/jwks} etc.) e emissor
   * ({@code iss}) não fixado, resolvido a partir da requisição.
   *
   * @return configurações padrão do servidor
   */
  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    return AuthorizationServerSettings.builder().build();
  }

  /**
   * Monta o gerador de tokens usado pelo provider do grant {@code password} e
   * pelo fluxo de refresh.
   *
   * <p>
   * É um {@code DelegatingOAuth2TokenGenerator} com: {@code JwtGenerator}
   * (assina o JWT com o {@code NimbusJwtEncoder} e a chave de {@link #jwkSource()},
   * aplicando {@link #tokenCustomizer()}); {@code OAuth2AccessTokenGenerator}
   * (token opaco, não usado porque o formato é autocontido) e
   * {@code OAuth2RefreshTokenGenerator} (refresh token opaco).
   * </p>
   *
   * @return gerador de tokens
   */
  @Bean
  public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator() {
    NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource());
    JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
    jwtGenerator.setJwtCustomizer(tokenCustomizer());
    OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
    OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
    return new DelegatingOAuth2TokenGenerator(jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
  }

  /**
   * Acrescenta claims customizados ao <b>access token</b> JWT.
   *
   * <p>
   * <b>Origem dos dados:</b> o {@code principal} do contexto de geração é cast
   * para {@code OAuth2ClientAuthenticationToken}, e seus {@code details} para
   * {@link AuthenticatedUser} (colocado ali pelo provider do grant
   * {@code password}). Nada é relido do banco neste ponto.
   * </p>
   *
   * <p>
   * <b>Claims acrescentados</b> (somente quando o tipo de token é
   * {@code access_token}):
   * </p>
   * <ul>
   * <li>{@code authorities}: lista de textos, uma por authority do usuário
   * (por exemplo, {@code ROLE_ADMIN});</li>
   * <li>{@code userId}: identificador numérico do usuário;</li>
   * <li>{@code username}: valor de {@code username} informado no login.</li>
   * </ul>
   *
   * <p>
   * No fluxo de refresh, os mesmos dados são reaproveitados do principal guardado
   * na autorização em memória (os testes de integração confirmam que os claims
   * são mantidos), portanto refletem o estado do usuário <b>no momento do
   * login</b>, e não o estado atual.
   * </p>
   *
   * @return customizador de claims do JWT
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
   * Decodificador de JWT baseado na mesma {@code JWKSource} que assina os tokens
   * ({@code OAuth2AuthorizationServerConfiguration.jwtDecoder}).
   *
   * <p>
   * É o <b>único</b> bean {@link JwtDecoder}, portanto é o que as cadeias do
   * Authorization Server e do Resource Server usam ({@code jwt(withDefaults())}).
   * Valida a assinatura com as chaves em memória e, pelos validadores padrão, a
   * expiração; não há configuração de validação de emissor nem de audiência, e
   * o estado do usuário (ativo, senha) não é consultado.
   * </p>
   *
   * @param jwkSource fonte das chaves de assinatura
   * @return decodificador de JWT
   */
  @Bean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  /**
   * Fonte de chaves JWK com <b>uma única chave RSA</b> gerada na inicialização
   * (ver {@code generateRsa}).
   *
   * <p>
   * A chave não é lida de arquivo nem de propriedade e não é persistida: a cada
   * inicialização há uma nova chave (e um novo {@code kid}); tokens assinados
   * antes de um reinício deixam de validar, e instâncias diferentes assinariam
   * com chaves diferentes. A chave pública fica disponível no endpoint JWKS do
   * Authorization Server (caminho padrão {@code /oauth2/jwks}).
   * </p>
   *
   * @return fonte que seleciona a chave do conjunto em memória
   */
  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    RSAKey rsaKey = generateRsa();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
  }

  /**
   * Gera um par RSA (2048 bits) e o empacota como {@link RSAKey}, com a chave
   * privada incluída e um {@code kid} aleatório (UUID).
   *
   * @return chave RSA nova a cada chamada
   */
  private static RSAKey generateRsa() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    return new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(UUID.randomUUID().toString()).build();
  }

  /**
   * Gera o par de chaves com {@link KeyPairGenerator} ({@code "RSA"}, 2048
   * bits).
   *
   * @return par de chaves RSA
   * @throws IllegalStateException se a geração falhar
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