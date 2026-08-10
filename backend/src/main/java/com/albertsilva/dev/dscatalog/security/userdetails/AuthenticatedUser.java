package com.albertsilva.dev.dscatalog.security.userdetails;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

/**
 * Objeto de domínio que representa um usuário autenticado com suas autoridades.
 *
 * <p>
 * Esta classe encapsula informações essenciais de um usuário que foi
 * autenticado
 * com sucesso no sistema, incluindo seu nome de usuário e autoridades
 * (roles/permissions).
 * </p>
 *
 * <p>
 * <b>Propósito:</b>
 * </p>
 * <p>
 * Servir como um container de dados para informações de usuário autenticado,
 * facilitando o acesso e manipulação desses dados em toda a aplicação,
 * especialmente em camadas de segurança e customizadores de tokens.
 * </p>
 *
 * <p>
 * <b>Casos de uso:</b>
 * </p>
 * <ul>
 * <li>Armazenar detalhes de usuário no contexto de segurança durante
 * autenticação OAuth2</li>
 * <li>Injetar dados de usuário em customizadores de JWT para adicionar
 * claims</li>
 * <li>Passar informações de usuário através do pipeline de autenticação</li>
 * <li>Recuperar autoridades do usuário para autorização de método
 * (@PreAuthorize)</li>
 * </ul>
 *
 * <p>
 * <b>Diferença com UserDetails:</b>
 * </p>
 * <p>
 * Enquanto {@link org.springframework.security.core.userdetails.UserDetails}
 * é a interface padrão do Spring Security com muitos métodos
 * (isAccountNonExpired, isEnabled, etc.),
 * {@code AuthenticatedUser} é um DTO simples focado apenas em username e
 * authorities,
 * adequado para cenários OAuth2 onde outras propriedades não são necessárias.
 * </p>
 *
 * <p>
 * <b>Exemplo de uso:</b>
 * </p>
 *
 * <pre>
 * // Criando um usuário autenticado
 * Collection&lt;GrantedAuthority&gt; authorities = Arrays.asList(
 *     new SimpleGrantedAuthority("ROLE_USER"),
 *     new SimpleGrantedAuthority("ROLE_ADMIN"));
 * AuthenticatedUser user = new AuthenticatedUser("john.doe", authorities);
 *
 * // Acessando informações
 * String username = user.getUsername();
 * Collection&lt;? extends GrantedAuthority&gt; roles = user.getAuthorities();
 * </pre>
 *
 * @implNote
 *           A classe é imutável após construção, com campos finais.
 *           As authorities são armazenadas como referência à coleção original,
 *           não uma cópia; cuidado se a coleção puder ser modificada
 *           externamente.
 *
 * @apiNote
 *          Use esta classe para encapsular detalhes de usuário autenticado
 *          em fluxos OAuth2 customizados. Pode ser utilizada em
 *          {@link org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken}
 *          como details para transmitir informações de usuário.
 *
 * @see org.springframework.security.core.userdetails.UserDetails
 * @see org.springframework.security.core.GrantedAuthority
 */
public class AuthenticatedUser {

  private final Long id;

  private final String username;

  private final Collection<? extends GrantedAuthority> authorities;

  /**
   * Construtor que inicializa um usuário autenticado.
   *
   * <p>
   * Cria uma nova instância com username e autoridades fornecidas.
   * </p>
   *
   * @param username    identificador único do usuário (não nulo)
   * @param authorities coleção de autoridades/roles do usuário (não nulo)
   *
   * @implNote
   *           Não há validação de nulidade; verificação deve ser feita pelo
   *           chamador
   *           para evitar NullPointerException ao acessar getters.
   */
  public AuthenticatedUser(Long id, String username, Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.username = username;
    this.authorities = authorities;
  }

  /**
   * Obtém o ID do usuário.
   *
   * @return ID do usuário
   */
  public Long getId() {
    return id;
  }

  /**
   * Obtém o nome de usuário (login) do usuário autenticado.
   *
   * @return nome de usuário (identificador único)
   *
   * @apiNote
   *          Geralmente é o email ou login utilizado na autenticação.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Obtém as autoridades/roles concedidas ao usuário autenticado.
   *
   * <p>
   * As autoridades retornadas podem ser utilizadas para decisões de autorização
   * em métodos protegidos via @PreAuthorize/@PostAuthorize.
   * </p>
   *
   * @return coleção de {@link GrantedAuthority} do usuário
   *
   * @implNote
   *           A coleção retornada é a mesma passada no construtor;
   *           modificações externas podem afetar o objeto se a coleção não for
   *           imutável.
   *           Para segurança, considere usar
   *           Collections.unmodifiableCollection().
   *
   * @apiNote
   *          Cada autoridade é uma string como "ROLE_ADMIN" ou "ROLE_USER"
   *          que pode ser checada em anotações @PreAuthorize.
   */
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }
}