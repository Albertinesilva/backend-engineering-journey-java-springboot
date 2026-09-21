package com.albertsilva.dev.dscatalog.security.userdetails;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

/**
 * Contêiner imutável com id, nome de usuário e authorities do usuário que
 * acabou de fazer login, usado <b>apenas durante a emissão do access token</b>.
 *
 * <p>
 * <b>Ciclo de vida:</b> é criado por {@code CustomPasswordAuthenticationProvider}
 * após a autenticação e colocado nos {@code details} do
 * {@code OAuth2ClientAuthenticationToken}; o customizador de JWT de
 * {@code AuthorizationServerConfig} o lê para escrever os claims
 * {@code userId}, {@code username} e {@code authorities}. Como o token do
 * cliente é guardado na autorização em memória, o objeto também é reaproveitado
 * na renovação por refresh token.
 * </p>
 *
 * <p>
 * <b>O que não é:</b> não implementa {@code UserDetails}, não é o principal
 * das requisições à API (esse é o {@code Jwt}) e não é consultado por
 * {@code @PreAuthorize}. O {@code username} é o valor informado no login (a
 * consulta ao banco compara o e-mail exatamente, então coincide com o e-mail
 * gravado).
 * </p>
 */
public class AuthenticatedUser {

  private final Long id;

  private final String username;

  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Cria o contêiner. Nenhum argumento é validado; a coleção de authorities é
   * guardada por referência (não é copiada).
   *
   * @param id          identificador do usuário
   * @param username    nome de usuário informado no login
   * @param authorities authorities do usuário (as mesmas de {@code User})
   */
  public AuthenticatedUser(Long id, String username, Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.username = username;
    this.authorities = authorities;
  }

  /**
   * @return identificador do usuário (vira o claim {@code userId})
   */
  public Long getId() {
    return id;
  }

  /**
   * @return nome de usuário informado no login (vira o claim {@code username})
   */
  public String getUsername() {
    return username;
  }

  /**
   * @return authorities do usuário, na mesma coleção recebida no construtor (viram
   *         o claim {@code authorities})
   */
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }
}