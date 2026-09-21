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
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
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
 * Autentica o usuário do grant {@code password} e emite o par de tokens
 * (access token JWT e refresh token). É o segundo passo do login
 * ({@code POST /oauth2/token}), depois do
 * {@link CustomPasswordAuthenticationConverter}, e está registrado em
 * {@code AuthorizationServerConfig}. <b>Não</b> trata o grant
 * {@code refresh_token}, que é resolvido pelo provider padrão do framework.
 *
 * <p>
 * <b>Sequência de {@link #authenticate(Authentication)}:</b>
 * </p>
 * <ol>
 * <li>exige que o principal do pedido seja um {@code OAuth2ClientAuthenticationToken}
 * autenticado (senão {@code invalid_client});</li>
 * <li>carrega o usuário com {@code UserDetailsService.loadUserByUsername}
 * (na aplicação, {@code UserService}: consulta nativa que devolve um
 * {@code User} <b>parcial</b>: id, e-mail, hash da senha, {@code active} e
 * roles);</li>
 * <li>confere a senha com {@code PasswordEncoder.matches} e, depois, o status
 * ({@code isEnabled} etc.); o <b>username digitado</b> não é comparado a mais
 * nada;</li>
 * <li>calcula os escopos autorizados: nomes das authorities do usuário que
 * também sejam escopos do cliente ({@code read}, {@code write}) — os escopos
 * pedidos na requisição são ignorados;</li>
 * <li>cria {@link AuthenticatedUser}(id, username digitado, authorities) e o
 * coloca nos {@code details} do token do cliente, além de substituir o
 * {@code SecurityContext} da thread por um contexto com esse token (é dali que
 * o customizador de JWT lê o usuário);</li>
 * <li>gera o access token (JWT) e o refresh token com o {@code OAuth2TokenGenerator};
 * falha na geração ⇒ {@code server_error};</li>
 * <li>monta e salva em {@code OAuth2AuthorizationService} (memória) uma
 * autorização cujo <b>{@code principalName} é o {@code clientId}</b> (não o
 * usuário), com o token do cliente como atributo principal;</li>
 * <li>devolve {@code OAuth2AccessTokenAuthenticationToken}.</li>
 * </ol>
 *
 * <p>
 * <b>Erros ({@link OAuth2AuthenticationException}):</b> usuário inexistente
 * ({@code UsernameNotFoundException}) ou senha incorreta ⇒
 * {@code invalid_grant} "Invalid credentials" (mesma mensagem); conta
 * desativada ⇒ {@code invalid_grant} "Your account has not been activated yet.
 * Please check your email." (só depois de a senha conferir); cliente inválido ⇒
 * {@code invalid_client}.
 * </p>
 */
public class CustomPasswordAuthenticationProvider implements AuthenticationProvider {

  /** Referência (RFC 6749, seção 5.2) incluída nos erros OAuth2 emitidos por este provider. */
  private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";
  private final OAuth2AuthorizationService authorizationService;
  private final UserDetailsService userDetailsService;
  private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
  private final PasswordEncoder passwordEncoder;

  /**
   * Cria o provider. Todos os argumentos são obrigatórios: {@code Assert.notNull}
   * lança {@link IllegalArgumentException} se algum for {@code null}.
   *
   * @param authorizationService serviço onde a autorização emitida é salva
   * @param tokenGenerator       gerador do JWT e do refresh token
   * @param userDetailsService   carrega o usuário pelo e-mail
   * @param passwordEncoder      confere a senha informada com o hash
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
   * Executa a autenticação e a emissão de tokens conforme a documentação da
   * classe.
   *
   * <p>
   * Efeitos colaterais: altera os {@code details} do token do cliente presente
   * no {@code SecurityContext}, substitui o contexto da thread e grava a
   * autorização (com o refresh token) no {@code OAuth2AuthorizationService}.
   * Este método <b>sempre</b> emite um refresh token.
   * </p>
   *
   * @param authentication deve ser um {@link CustomPasswordAuthenticationToken}
   * @return token de acesso autenticado, com access e refresh token
   * @throws AuthenticationException ({@link OAuth2AuthenticationException}) com
   *                                 {@code invalid_client}, {@code invalid_grant}
   *                                 ou {@code server_error}
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
    OAuth2TokenContext accessTokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();

    OAuth2Token generatedAccessToken = this.tokenGenerator.generate(accessTokenContext);

    if (generatedAccessToken == null) {
      OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
          "The token generator failed to generate the access token.", ERROR_URI);

      throw new OAuth2AuthenticationException(error);
    }

    OAuth2AccessToken accessToken = new OAuth2AccessToken(
        OAuth2AccessToken.TokenType.BEARER,
        generatedAccessToken.getTokenValue(),
        generatedAccessToken.getIssuedAt(),
        generatedAccessToken.getExpiresAt(),
        accessTokenContext.getAuthorizedScopes());

    if (generatedAccessToken instanceof ClaimAccessor) {
      authorizationBuilder.token(accessToken, metadata -> metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
          ((ClaimAccessor) generatedAccessToken).getClaims()));
    } else {
      authorizationBuilder.accessToken(accessToken);
    }

    // -----------REFRESH TOKEN----------
    OAuth2TokenContext refreshTokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();

    OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(refreshTokenContext);

    if (generatedRefreshToken == null) {
      OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
          "The token generator failed to generate the refresh token.", ERROR_URI);

      throw new OAuth2AuthenticationException(error);
    }

    OAuth2RefreshToken refreshToken = (OAuth2RefreshToken) generatedRefreshToken;

    authorizationBuilder.refreshToken(refreshToken);

    // -----------SAVE AUTHORIZATION----------
    OAuth2Authorization authorization = authorizationBuilder.build();

    this.authorizationService.save(authorization);

    // -----------RETURN TOKENS----------
    return new OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, refreshToken);
  }

  /**
   * @param authentication tipo de autenticação a avaliar
   * @return {@code true} apenas para {@link CustomPasswordAuthenticationToken}
   */
  @Override
  public boolean supports(Class<?> authentication) {
    return CustomPasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Confere a senha informada com o hash do usuário por
   * {@code PasswordEncoder.matches}. <b>Só a senha é verificada</b>: o username
   * não é comparado ao do {@code UserDetails}.
   *
   * @param user     usuário carregado (só o hash da senha é usado)
   * @param password senha em texto informada
   * @throws OAuth2AuthenticationException ({@code invalid_grant}, "Invalid
   *                                       credentials") se a senha não conferir
   */
  private void validateCredentials(UserDetails user, String password) {
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "Invalid credentials", ERROR_URI));
    }
  }

  /**
   * Extrai o cliente autenticado do principal da solicitação.
   *
   * @param authentication solicitação (o principal deve ser um
   *                       {@link OAuth2ClientAuthenticationToken} autenticado)
   * @return token do cliente autenticado
   * @throws OAuth2AuthenticationException ({@code invalid_client}) se o principal
   *                                       não for um token de cliente
   *                                       autenticado
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

  /**
   * Verifica o status da conta, lançando {@code invalid_grant} para o primeiro
   * critério que falhar, nesta ordem: {@code isEnabled()} ("Your account has not
   * been activated yet. Please check your email."), {@code isAccountNonLocked()},
   * {@code isAccountNonExpired()} e {@code isCredentialsNonExpired()}. Com a
   * entidade {@code User} atual, só o primeiro pode falhar (os demais retornam
   * sempre {@code true}); {@code isEnabled()} reflete o indicador {@code active}
   * lido pela consulta de login.
   *
   * @param user usuário carregado
   * @throws OAuth2AuthenticationException ({@code invalid_grant}) se a conta não
   *                                       puder autenticar
   */
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
