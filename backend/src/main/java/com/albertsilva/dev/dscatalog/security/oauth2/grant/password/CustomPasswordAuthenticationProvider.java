package com.albertsilva.dev.dscatalog.security.oauth2.grant.password;

import java.security.Principal;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.security.userdetails.AuthenticatedUser;

/**
 * Provedor de autenticação customizado para o fluxo OAuth2 "password".
 *
 * <p>
 * Esta classe implementa {@link AuthenticationProvider} para processar tokens
 * de autenticação
 * customizados do fluxo "password" (Resource Owner Password Credentials),
 * realizando validação
 * de credenciais e geração de tokens de acesso JWT.
 * </p>
 *
 * <p>
 * <b>Responsabilidades principais:</b>
 * </p>
 * <ul>
 * <li>Autenticar usuários comparando credenciais fornecidas com dados do
 * banco</li>
 * <li>Validar cliente OAuth2 registrado</li>
 * <li>Gerar tokens de acesso JWT com autoridades customizadas</li>
 * <li>Gerenciar contexto de segurança com detalhes do usuário autenticado</li>
 * <li>Persistir autorizações no serviço de autorização</li>
 * </ul>
 *
 * <p>
 * <b>Fluxo de autenticação:</b>
 * </p>
 * <ol>
 * <li>Extrai principal do cliente OAuth2 do token de autenticação</li>
 * <li>Carrega dados de usuário pelo username usando
 * {@link UserDetailsService}</li>
 * <li>Valida senha comparando com hash armazenado usando
 * {@link PasswordEncoder}</li>
 * <li>Filtra autoridades do usuário para escopos autorizados do cliente</li>
 * <li>Atualiza contexto de segurança com detalhes de usuário customizado</li>
 * <li>Gera token de acesso JWT com claims customizados</li>
 * <li>Persiste autorização no serviço</li>
 * <li>Retorna token de acesso autenticado</li>
 * </ol>
 *
 * <p>
 * <b>Integração com Spring Security:</b>
 * </p>
 * <p>
 * Este provedor é registrado no {@code AuthorizationServerConfig} como parte
 * do pipeline de token endpoint, trabalhando em conjunto com
 * {@link CustomPasswordAuthenticationConverter} para processar requisições
 * completas.
 * </p>
 *
 * @implNote
 *           Todos os parâmetros construtor devem ser não-nulos; validações
 *           são feitas com {@link Assert#notNull(Object, String)}.
 *           O contexto de segurança é atualizado com detalhes de usuário
 *           para que o customizador de token possa acessar informações do
 *           usuário.
 *
 * @apiNote
 *          Este provedor implementa validação rigorosa de credenciais,
 *          incluindo comparação de username e password, filtragem de escopos
 *          e tratamento de erros OAuth2 padrão.
 *
 * @see CustomPasswordAuthenticationToken
 * @see CustomPasswordAuthenticationConverter
 * @see AuthenticatedUser
 */
public class CustomPasswordAuthenticationProvider implements AuthenticationProvider {

  private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";
  private final OAuth2AuthorizationService authorizationService;
  private final UserDetailsService userDetailsService;
  private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
  private final PasswordEncoder passwordEncoder;

  /**
   * Construtor do provedor de autenticação customizado.
   *
   * <p>
   * Inicializa o provedor com as dependências necessárias para autenticação,
   * validação de senhas e geração de tokens. Todos os parâmetros devem ser
   * não-nulos.
   * </p>
   *
   * @param authorizationService serviço para persistir autorizações emitidas
   * @param tokenGenerator       gerador de tokens JWT com customizadores
   * @param userDetailsService   serviço para carregar detalhes de usuários
   * @param passwordEncoder      encoder para validação de senhas armazenadas
   * @throws IllegalArgumentException se algum parâmetro for nulo
   *
   * @implNote
   *           As validações de não-nulidade são feitas imediatamente no
   *           construtor
   *           para garantir falha rápida se alguma dependência estiver ausente.
   */
  public CustomPasswordAuthenticationProvider(OAuth2AuthorizationService authorizationService,
      OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator, UserDetailsService userDetailsService,
      PasswordEncoder passwordEncoder) {

    Assert.notNull(authorizationService, "authorizationService cannot be null");
    Assert.notNull(tokenGenerator, "TokenGenerator cannot be null");
    Assert.notNull(userDetailsService, "UserDetailsService cannot be null");
    Assert.notNull(passwordEncoder, "PasswordEncoder cannot be null");
    this.authorizationService = authorizationService;
    this.tokenGenerator = tokenGenerator;
    this.userDetailsService = userDetailsService;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Autentica um usuário e gera um token de acesso.
   *
   * <p>
   * Processa um {@link CustomPasswordAuthenticationToken} validando credenciais
   * do usuário e gerando um JWT com claims customizados (authorities e username).
   * </p>
   *
   * <p>
   * <b>Etapas do processo:</b>
   * </p>
   * <ol>
   * <li>Valida autenticação do cliente OAuth2</li>
   * <li>Carrega detalhes de usuário pelo username</li>
   * <li>Valida senha fornecida contra hash armazenado</li>
   * <li>Filtra autoridades do usuário para escopos do cliente</li>
   * <li>Atualiza contexto de segurança com {@link AuthenticatedUser}</li>
   * <li>Constrói contexto de token com autoridades e client</li>
   * <li>Gera token de acesso JWT</li>
   * <li>Persiste autorização no serviço</li>
   * <li>Retorna token autenticado</li>
   * </ol>
   *
   * <p>
   * <b>Tratamento de erros:</b>
   * </p>
   * <ul>
   * <li>{@code UsernameNotFoundException}: Convertido em "Invalid
   * credentials"</li>
   * <li>Senha inválida: Lança "Invalid credentials"</li>
   * <li>Username não corresponde: Lança "Invalid credentials"</li>
   * <li>Cliente não autenticado: Lança {@code INVALID_CLIENT}</li>
   * <li>Falha ao gerar token: Lança {@code SERVER_ERROR}</li>
   * </ul>
   *
   * @param authentication token de autenticação com credenciais a validar
   * @return {@link OAuth2AccessTokenAuthenticationToken} contendo o JWT gerado
   * @throws OAuth2AuthenticationException se validação de credenciais ou
   *                                       cliente falhar
   * @throws AuthenticationException       se credenciais forem inválidas
   *
   * @implNote
   *           O contexto de segurança é atualizado para que o customizador de
   *           token
   *           possa acessar detalhes do usuário autenticado. O token é persistido
   *           no serviço de autorização para rastreamento de sessões.
   *
   * @apiNote
   *          Mensagens de erro genéricas ("Invalid credentials") são utilizadas
   *          para evitar enumeração de usuários válidos. O cliente é responsável
   *          por fornecer credenciais válidas.
   */
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    CustomPasswordAuthenticationToken customPasswordAuthenticationToken = (CustomPasswordAuthenticationToken) authentication;
    OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(
        customPasswordAuthenticationToken);
    RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
    String username = customPasswordAuthenticationToken.getUsername();
    String password = customPasswordAuthenticationToken.getPassword();

    UserDetails userDetails;

    try {
      userDetails = userDetailsService.loadUserByUsername(username);
      validateCredentials(userDetails, password);
      validateUserStatus(userDetails);
    } catch (UsernameNotFoundException e) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid credentials", ERROR_URI));
    }

    Set<String> authorizedScopes = userDetails.getAuthorities().stream().map(scope -> scope.getAuthority())
        .filter(scope -> registeredClient.getScopes().contains(scope)).collect(Collectors.toSet());

    Long userId = ((User) userDetails).getId(); // Necessário para acessar o ID da entidade

    // -----------Create a new Security Context Holder Context----------
    OAuth2ClientAuthenticationToken oAuth2ClientAuthenticationToken = (OAuth2ClientAuthenticationToken) SecurityContextHolder
        .getContext().getAuthentication();
    AuthenticatedUser customPasswordUser = new AuthenticatedUser(userId, username, userDetails.getAuthorities());
    oAuth2ClientAuthenticationToken.setDetails(customPasswordUser);

    var newcontext = SecurityContextHolder.createEmptyContext();
    newcontext.setAuthentication(oAuth2ClientAuthenticationToken);
    SecurityContextHolder.setContext(newcontext);

    // -----------TOKEN BUILDERS----------
    DefaultOAuth2TokenContext.Builder tokenContextBuilder = DefaultOAuth2TokenContext.builder()
        .registeredClient(registeredClient)
        .principal(clientPrincipal)
        .authorizationServerContext(AuthorizationServerContextHolder.getContext())
        .authorizedScopes(authorizedScopes)
        .authorizationGrantType(new AuthorizationGrantType("password"))
        .authorizationGrant(customPasswordAuthenticationToken);

    OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
        .attribute(Principal.class.getName(), clientPrincipal)
        .principalName(clientPrincipal.getName())
        .authorizationGrantType(new AuthorizationGrantType("password"))
        .authorizedScopes(authorizedScopes);

    // -----------ACCESS TOKEN----------
    OAuth2TokenContext tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
    OAuth2Token generatedAccessToken = this.tokenGenerator.generate(tokenContext);
    if (generatedAccessToken == null) {
      OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
          "The token generator failed to generate the access token.", ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
        generatedAccessToken.getTokenValue(), generatedAccessToken.getIssuedAt(),
        generatedAccessToken.getExpiresAt(), tokenContext.getAuthorizedScopes());
    if (generatedAccessToken instanceof ClaimAccessor) {
      authorizationBuilder.token(accessToken, (metadata) -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
          ((ClaimAccessor) generatedAccessToken).getClaims()));
    } else {
      authorizationBuilder.accessToken(accessToken);
    }

    OAuth2Authorization authorization = authorizationBuilder.build();
    this.authorizationService.save(authorization);

    return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken);
  }

  /**
   * Verifica se este provedor suporta o tipo de autenticação fornecido.
   *
   * <p>
   * Retorna {@code true} apenas para tokens do tipo
   * {@link CustomPasswordAuthenticationToken},
   * permitindo que o Spring Security direione requisições ao provedor correto.
   * </p>
   *
   * @param authentication classe do token de autenticação a verificar
   * @return {@code true} se o token for
   *         {@link CustomPasswordAuthenticationToken},
   *         {@code false} caso contrário
   *
   * @apiNote
   *          Este método permite que múltiplos provedores de autenticação
   *          funcionem juntos no gerenciador de autenticação do Spring Security.
   */
  @Override
  public boolean supports(Class<?> authentication) {
    return CustomPasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Valida as credenciais do usuário comparando a senha fornecida com o hash
   * armazenado.
   *
   * <p>
   * Verifica se o username corresponde ao nome de usuário do UserDetails e se a
   * senha fornecida, quando codificada, corresponde à senha armazenada. Lança
   * {@code OAuth2AuthenticationException} com mensagem genérica "Invalid
   * credentials" se qualquer validação falhar.
   * </p>
   *
   * @param user     detalhes do usuário carregados do banco
   * @param username nome de usuário fornecido na autenticação
   * @param password senha fornecida na autenticação
   * @throws OAuth2AuthenticationException se o username não corresponder ou se a
   *                                       senha for inválida
   *
   * @implNote
   *           A mensagem de erro é genérica para evitar enumeração de usuários
   *           válidos. O método utiliza {@link PasswordEncoder#matches} para
   *           comparação segura de senhas.
   */
  private void validateCredentials(UserDetails user, String password) {
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid credentials", ERROR_URI));
    }
  }

  /**
   * Valida e extrai o principal do cliente OAuth2 da autenticação.
   *
   * <p>
   * Verifica se a autenticação contém um {@link OAuth2ClientAuthenticationToken}
   * válido
   * e autenticado, lançando exceção se não encontrar ou se estiver inválido.
   * </p>
   *
   * <p>
   * <b>Validações:</b>
   * </p>
   * <ul>
   * <li>Principal deve ser uma instância de
   * {@link OAuth2ClientAuthenticationToken}</li>
   * <li>Token deve estar autenticado</li>
   * <li>Lança {@code INVALID_CLIENT} se alguma validação falhar</li>
   * </ul>
   *
   * @param authentication token de autenticação contendo principal do cliente
   * @return {@link OAuth2ClientAuthenticationToken} validado e autenticado
   * @throws OAuth2AuthenticationException com erro {@code INVALID_CLIENT}
   *                                       se o cliente não estiver autenticado ou
   *                                       inválido
   *
   * @implNote
   *           Este método é privado e estático pois é apenas um utilitário
   *           de validação. A exceção lançada inclui um URI para referência
   *           às especificações OAuth2 RFC 6749.
   */
  private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
      Authentication authentication) {

    OAuth2ClientAuthenticationToken clientPrincipal = null;
    if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication.getPrincipal().getClass())) {
      clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
    }
    if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
      return clientPrincipal;
    }
    throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
  }

  private void validateUserStatus(UserDetails user) {

    if (!user.isEnabled()) {
      OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT,
          "Your account has not been activated yet. Please check your email.", ERROR_URI);

      throw new OAuth2AuthenticationException(error);
    }

    if (!user.isAccountNonLocked()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Account is locked", ERROR_URI));
    }

    if (!user.isAccountNonExpired()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Account expired", ERROR_URI));
    }

    if (!user.isCredentialsNonExpired()) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Credentials expired", ERROR_URI));
    }
  }
}
