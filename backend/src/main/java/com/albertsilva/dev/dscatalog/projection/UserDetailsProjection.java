package com.albertsilva.dev.dscatalog.projection;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Projeção (interface) que representa uma linha do resultado de
 * {@code UserRepository#searchUserAndRolesByEmail(String)}: os dados de
 * autenticação de um usuário combinados com <b>uma</b> de suas roles.
 *
 * <p>
 * É uma <em>interface projection</em> fechada do Spring Data: cada getter
 * corresponde a uma coluna (ou alias) do {@code SELECT} nativo, sem
 * {@code @Value} nem SpEL, e nenhuma entidade é carregada. Serve ao
 * {@link UserDetailsService} customizado do sistema, que precisa apenas de
 * credenciais e roles para autenticar.
 * </p>
 *
 * <p>
 * <b>Origem dos campos</b> (aliases da consulta nativa):
 * </p>
 * <ul>
 * <li>{@code getId()} ← {@code tb_user.id AS id}</li>
 * <li>{@code getUsername()} ← {@code tb_user.email AS username} (o e-mail é o
 * nome de usuário)</li>
 * <li>{@code getPassword()} ← {@code tb_user.password} (hash armazenado)</li>
 * <li>{@code getActive()} ← {@code tb_user.active}</li>
 * <li>{@code getRoleId()} ← {@code tb_role.id AS roleId}</li>
 * <li>{@code getAuthority()} ← {@code tb_role.authority}</li>
 * </ul>
 *
 * <p>
 * <b>Cardinalidade:</b> como a consulta faz junção com as roles, ela devolve
 * <b>uma linha por role</b>: os campos do usuário ({@code id}, {@code username},
 * {@code password}, {@code active}) se repetem em todas as linhas, e apenas
 * {@code roleId} e {@code authority} variam. Um usuário sem roles não gera
 * nenhuma linha.
 * </p>
 *
 * <p>
 * <b>Uso atual:</b> {@code UserService.loadUserByUsername} lê a primeira linha
 * para preencher id, e-mail, senha e {@code active} de um
 * {@link com.albertsilva.dev.dscatalog.domain.user.User} parcial e percorre
 * todas as linhas para adicionar as roles.
 * </p>
 *
 * @see UserDetailsService
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
   * @return e-mail do usuário, usado como nome de usuário (alias {@code username} de {@code tb_user.email})
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
   * Tipicamente no formato "ROLE_*" (ex: "ROLE_ADMIN", "ROLE_OPERATOR").
   * Este valor é direto e pronto para utilização no Spring Security.
   * </p>
   *
   * @return nome da autoridade/role (ex: "ROLE_ADMIN")
   *
   * @apiNote
   *          O valor é usado como está para compor a {@code Role} do usuário
   *          carregado (ver {@code UserService.loadUserByUsername}).
   */
  String getAuthority();

  /**
   * Indica se a conta do usuário está ativa ou não.
   *
   * <p>
   * Copia o valor de {@code tb_user.active}. A consulta não filtra por esse
   * campo: usuários inativos também são retornados, e a recusa do login ocorre
   * depois, no fluxo de autenticação, com base em {@code User#isEnabled()}.
   * </p>
   *
   * @return {@code true} se a conta do usuário está ativa; {@code false}
   *         caso contrário
   */
  boolean getActive();
}
