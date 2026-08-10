package com.albertsilva.dev.dscatalog.projection;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Projeção para otimizar consultas de detalhes de usuário para autenticação.
 *
 * <p>
 * Esta interface define uma projeção do Spring Data JPA que recupera apenas
 * as colunas necessárias para autenticação e autorização de usuários,
 * evitando carregar entidades completas do banco de dados.
 * </p>
 *
 * <p>
 * <b>Propósito:</b>
 * </p>
 * <p>
 * Otimizar queries SQL recuperando apenas username, password e informações
 * de role/authority necessárias para validação de credenciais em login.
 * Reduz uso de memória e acelera autenticação comparado a carregar
 * entidades completas (User, Role, etc.).
 * </p>
 *
 * <p>
 * <b>Estrutura da projeção:</b>
 * </p>
 * <ul>
 * <li>{@code username}: Identificador único do usuário</li>
 * <li>{@code password}: Hash de senha para validação</li>
 * <li>{@code roleId}: ID da role/autoridade do usuário</li>
 * <li>{@code authority}: Nome da role em formato string (ex: "ROLE_ADMIN")</li>
 * </ul>
 *
 * <p>
 * <b>Padrão de uso:</b>
 * </p>
 * <p>
 * Tipicamente utilizada em repositórios com queries customizadas
 * usando {@code @Query} com {@code @Projection}, permitindo que
 * Spring Data JPA mapeie resultados de SELECT diretamente para
 * esta interface sem carregar entidades completas.
 * </p>
 *
 * <p>
 * <b>Exemplo de query no repositório:</b>
 * </p>
 *
 * <pre>
 * &#64;Query("SELECT u.username, u.password, r.id, r.authority " +
 *     "FROM users u " +
 *     "LEFT JOIN user_role ur ON u.id = ur.user_id " +
 *     "LEFT JOIN role r ON ur.role_id = r.id " +
 *     "WHERE u.username = :username")
 * List&lt;UserDetailsProjection&gt; findUserDetailsByUsername(&#64;Param("username") String username);
 * </pre>
 *
 * <p>
 * <b>Benefícios:</b>
 * </p>
 * <ul>
 * <li>Reduz dados trafegando entre banco e aplicação</li>
 * <li>Menos overhead de mapeamento ORM</li>
 * <li>Melhor performance em queries de autenticação frequentes</li>
 * <li>Type-safe: Compile-time checks vs strings de SQL</li>
 * </ul>
 *
 * @implNote
 *           Spring Data JPA mapeia automaticamente as colunas do SELECT
 *           para os getters desta interface. Os nomes dos getters devem
 *           corresponder aos aliases do SQL (ex: getUsername() → username).
 *           Pode ser necessário utilizar {@code @Value} em {@code @Query}
 *           para aliases explícitos.
 *
 * @apiNote
 *          Esta projeção é utilizada pelo {@link UserDetailsService}
 *          customizado para carregar detalhes de usuário na autenticação.
 *          Para cenários onde múltiplas roles são necessárias por usuário,
 *          esta projeção retorna múltiplas linhas (uma por role).
 *
 * @see org.springframework.data.jpa.repository.Query
 * @see org.springframework.security.core.userdetails.UserDetailsService
 */
public interface UserDetailsProjection {

  /**
   * Obtém o ID do usuário da projeção.
   *
   * <p>
   * Este ID é útil para transmitir informações de usuário autenticado
   * em tokens JWT ou contextos de segurança.
   * </p>
   *
   * @return ID único do usuário no banco de dados
   */
  Long getId();

  /**
   * Obtém o nome de usuário (login) da projeção.
   *
   * @return nome de usuário único
   */
  String getUsername();

  /**
   * Obtém o hash de senha do usuário da projeção.
   *
   * <p>
   * A senha é armazenada como hash (BCrypt) no banco de dados,
   * nunca em texto plano.
   * </p>
   *
   * @return hash de senha para validação na autenticação
   *
   * @apiNote
   *          Este hash será comparado com a senha fornecida via
   *          {@link org.springframework.security.crypto.password.PasswordEncoder#matches(CharSequence, String)}
   */
  String getPassword();

  /**
   * Obtém o ID da role/autoridade do usuário.
   *
   * <p>
   * Cada linha da projeção representa uma role diferente.
   * Se um usuário tem múltiplas roles, múltiplas linhas serão retornadas.
   * </p>
   *
   * @return ID único da role no banco de dados
   *
   * @implNote
   *           Este valor é frequentemente utilizado para mapeamento
   *           entre role ID e nome de authority na lógica de serviço.
   */
  Long getRoleId();

  /**
   * Obtém o nome da authority/role em formato string.
   *
   * <p>
   * Tipicamente no formato "ROLE_*" (ex: "ROLE_ADMIN", "ROLE_USER").
   * Este valor é direto e pronto para utilização no Spring Security.
   * </p>
   *
   * @return nome da autoridade/role (ex: "ROLE_ADMIN")
   *
   * @apiNote
   *          Este valor é adicionado diretamente aos authorities
   *          do usuário durante criação de
   *          {@link org.springframework.security.core.Authentication}
   */
  String getAuthority();

  /**
   * Indica se a conta do usuário está ativa ou não.
   *
   * <p>
   * Este campo é utilizado para determinar se o usuário pode autenticar
   * ou não. Se {@code false}, o usuário não poderá fazer login.
   * </p>
   *
   * @return {@code true} se a conta do usuário está ativa; {@code false}
   *         caso contrário
   */
  boolean getActive();
}
