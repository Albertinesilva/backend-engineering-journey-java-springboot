package com.albertsilva.dev.dscatalog.security.oauth2.grant.password;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

/**
 * Representa, dentro do Spring Authorization Server, a <b>solicitação de token
 * do grant {@code password}</b>: guarda o usuário e a senha em texto, os
 * escopos solicitados e o cliente autenticado.
 *
 * <p>
 * Criado por {@link CustomPasswordAuthenticationConverter} e consumido por
 * {@link CustomPasswordAuthenticationProvider}. O tipo do grant é fixado como
 * {@code password} ({@code new AuthorizationGrantType("password")}); o
 * principal é o {@code OAuth2ClientAuthenticationToken} do cliente. Os escopos
 * são copiados para um conjunto não modificável ({@code null} vira conjunto
 * vazio).
 * </p>
 *
 * <p>
 * <b>Dado sensível:</b> {@code password} é a senha em texto informada pelo
 * usuário e existe só durante o processamento do login. Os escopos
 * ({@link #getScopes()}) são armazenados, mas o provider atual <b>não</b> os
 * usa: os escopos autorizados são calculados a partir das authorities do
 * usuário.
 * </p>
 */
public class CustomPasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
  private static final long serialVersionUID = 1L;

  private final String username;
  private final String password;
  private final Set<String> scopes;

  /**
   * Cria a solicitação de token do grant {@code password}.
   *
   * @param clientPrincipal      autenticação do cliente OAuth2 já autenticado
   * @param scopes               escopos solicitados (pode ser {@code null})
   * @param additionalParameters demais parâmetros da requisição (pode ser
   *                             {@code null})
   * @param username             nome de usuário informado no login
   * @param password             senha em texto informada no login
   */
  public CustomPasswordAuthenticationToken(Authentication clientPrincipal, @Nullable Set<String> scopes,
      @Nullable Map<String, Object> additionalParameters, String username, String password) {

    super(new AuthorizationGrantType("password"), clientPrincipal, additionalParameters);

    this.username = username;
    this.password = password;
    this.scopes = Collections.unmodifiableSet(scopes != null ? new HashSet<>(scopes) : Collections.emptySet());
  }

  /**
   * @return nome de usuário informado no login (e-mail)
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * @return senha em texto informada no login (dado sensível; será conferida contra
   *         o hash pelo provider)
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * @return escopos solicitados na requisição (conjunto não modificável; vazio se
   *         nenhum foi informado). Não usado pelo provider atual
   */
  public Set<String> getScopes() {
    return this.scopes;
  }
}
