package com.albertsilva.dev.asjcatalog.security.oauth2.grant.password;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

/**
 * Token de autenticação customizado para o fluxo OAuth2 "password".
 *
 * <p>
 * Esta classe estende {@link OAuth2AuthorizationGrantAuthenticationToken} para
 * encapsular as credenciais de um usuário (username e password) durante o
 * processamento
 * do fluxo de autorização "password" (Resource Owner Password Credentials).
 * </p>
 *
 * <p>
 * <b>Responsabilidade:</b>
 * </p>
 * <p>
 * Armazenar de forma segura as credenciais do usuário e escopos solicitados,
 * permitindo que o conversor de autenticação e o provedor de autenticação
 * processem a requisição de token com segurança.
 * </p>
 *
 * <p>
 * <b>Estrutura do token:</b>
 * </p>
 * <ul>
 * <li>Grant type: "password" (immutable)</li>
 * <li>Client principal: Autenticação do cliente OAuth2</li>
 * <li>Username: Identificador único do usuário</li>
 * <li>Password: Senha em texto plano (hash é feito no provedor)</li>
 * <li>Scopes: Escopos solicitados (immutable set)</li>
 * <li>Parâmetros adicionais: Metadados da requisição</li>
 * </ul>
 *
 * <p>
 * <b>Ciclo de vida:</b>
 * </p>
 * <ol>
 * <li>Criado pelo {@link CustomPasswordAuthenticationConverter} a partir de
 * parâmetros HTTP</li>
 * <li>Processado pelo {@link CustomPasswordAuthenticationProvider} para
 * validação e autenticação</li>
 * <li>Convertido em
 * {@link org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken}
 * ao sucesso</li>
 * </ol>
 *
 * <p>
 * <b>Segurança:</b>
 * </p>
 * <ul>
 * <li>Credenciais mantidas em memória apenas durante processamento</li>
 * <li>Scopes armazenados como unmodifiable set para evitar modificações</li>
 * <li>Estende classe base que implementa {@link Authentication}</li>
 * </ul>
 *
 * @implNote
 *           Este token é serializable (serialVersionUID definido) para
 *           compatibilidade
 *           com frameworks de persistência. Os scopes são armazenados como
 *           conjunto
 *           imutável por segurança e para evitar modificações indesejadas.
 *
 * @apiNote
 *          Este token é parte do fluxo interno de segurança e não deve ser
 *          exposto diretamente a clientes. Use
 *          {@link CustomPasswordAuthenticationConverter}
 *          e {@link CustomPasswordAuthenticationProvider} para processamento.
 *
 * @see CustomPasswordAuthenticationConverter
 * @see CustomPasswordAuthenticationProvider
 */
public class CustomPasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
  private static final long serialVersionUID = 1L;

  private final String username;
  private final String password;
  private final Set<String> scopes;

  /**
   * Construtor do token de autenticação customizado.
   *
   * <p>
   * Cria um novo token encapsulando as credenciais de um usuário para
   * o fluxo "password" do OAuth2.
   * </p>
   *
   * <p>
   * <b>Inicializações:</b>
   * </p>
   * <ul>
   * <li>Grant type é fixado como "password"</li>
   * <li>Scopes são convertidos em unmodifiable set</li>
   * <li>Credenciais são armazenadas como fields finais</li>
   * <li>Parâmetros adicionais são passados para a classe base</li>
   * </ul>
   *
   * @param clientPrincipal      autenticação do cliente OAuth2 registrado
   * @param scopes               escopos solicitados (pode ser nulo)
   * @param additionalParameters parâmetros customizados da requisição (pode ser
   *                             nulo)
   * @param username             nome de usuário para autenticação
   * @param password             senha em texto plano para validação
   *
   * @implNote
   *           Scopes nulos são convertidos em conjunto vazio. O construtor
   *           força grant type "password" para garantir consistência.
   *
   * @apiNote
   *          Não crie instâncias diretamente; use
   *          {@link CustomPasswordAuthenticationConverter}
   *          para conversão segura de requisições HTTP.
   */
  public CustomPasswordAuthenticationToken(Authentication clientPrincipal, @Nullable Set<String> scopes,
      @Nullable Map<String, Object> additionalParameters, String username, String password) {

    super(new AuthorizationGrantType("password"), clientPrincipal, additionalParameters);

    this.username = username;
    this.password = password;
    this.scopes = Collections.unmodifiableSet(scopes != null ? new HashSet<>(scopes) : Collections.emptySet());
  }

  /**
   * Recupera o nome de usuário armazenado no token.
   *
   * @return nome de usuário para autenticação
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Recupera a senha em texto plano armazenada no token.
   *
   * <p>
   * A senha é comparada com hash no banco de dados pelo provedor de autenticação.
   * </p>
   *
   * @return senha em texto plano fornecida pelo cliente
   *
   * @implNote
   *           Esta senha não é criptografada no token; apenas transmitida via
   *           HTTPS
   *           e processada imediatamente pelo provedor de autenticação.
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * Recupera os escopos solicitados no token.
   *
   * <p>
   * Os escopos retornados são imutáveis e refletem os escopos solicitados
   * no momento da criação do token.
   * </p>
   *
   * @return conjunto imutável de escopos solicitados
   *
   * @implNote
   *           O conjunto retornado é imutável; tentativas de modificação
   *           lançarão {@link UnsupportedOperationException}.
   */
  public Set<String> getScopes() {
    return this.scopes;
  }
}
