package com.albertsilva.dev.dscatalog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.projection.UserDetailsProjection;

/**
 * Repositório Spring Data JPA da entidade {@link User} (tabela
 * {@code tb_user}), com chave primária {@code Long}.
 *
 * <p>
 * Além das operações herdadas de {@link JpaRepository}, declara:
 * </p>
 * <ul>
 * <li><b>consultas derivadas</b>:
 * {@link #findByFirstNameContainingIgnoreCase(String, Pageable)},
 * {@link #findByEmail(String)}, {@link #existsByEmailIgnoreCase(String)} e
 * {@link #existsByEmailIgnoreCaseAndIdNot(String, Long)}</li>
 * <li><b>uma consulta SQL nativa</b> com projeção:
 * {@link #searchUserAndRolesByEmail(String)}, usada na autenticação</li>
 * </ul>
 *
 * <p>
 * <b>Diferença de comparação de e-mail:</b> os métodos {@code existsBy...}
 * ignoram maiúsculas/minúsculas, mas {@code findByEmail} e
 * {@code searchUserAndRolesByEmail} comparam o texto exato.
 * </p>
 *
 * <p>
 * <b>Uso atual:</b> {@code UserService} (CRUD, listagem e
 * {@code loadUserByUsername}), {@code AccountService} ({@code findByEmail} e
 * {@code save}), {@code AuthenticatedUserService} ({@code findById}) e os
 * validators de e-mail ({@code UniqueEmailValidator},
 * {@code UserUpdateValidator},
 * {@code UniqueEmailForAuthenticatedUserValidator}).
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Lista, de forma paginada, os usuários cujo primeiro nome ({@code firstName})
   * contém o termo informado, sem diferenciar maiúsculas de minúsculas.
   *
   * <p>
   * <b>Consulta derivada:</b> o SQL é gerado pelo Spring Data a partir do nome
   * do método, resultando em um {@code LIKE '%termo%'} aplicado a
   * {@code firstName} sem distinção de caixa. O sobrenome não participa do
   * filtro. Não há {@code @Query}.
   * </p>
   *
   * <p>
   * <b>Paginação e ordenação:</b> definidas pelo {@link Pageable}, com nomes de
   * propriedades da entidade. Sem ordenação informada, a consulta não define a
   * ordem. Como o retorno é {@link Page}, o Spring Data executa também uma
   * consulta de contagem.
   * </p>
   *
   * <p>
   * <b>Carregamento:</b> devolve entidades {@link User} sem as roles
   * inicializadas (relacionamento tardio). Converter cada usuário em resposta
   * que exponha as roles faz o acesso a {@code User.getRoles()} disparar uma
   * consulta adicional por usuário da página (padrão N+1).
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code UserService.search}, somente quando há termo de
   * filtro; sem filtro, o service usa {@code findAll(Pageable)}.
   * </p>
   *
   * @param firstName termo procurado no primeiro nome
   * @param pageable  configurações de paginação (página, tamanho, ordenação)
   * @return página de usuários encontrados (vazia se nenhum corresponder)
   */
  Page<User> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

  /**
   * Busca um usuário pelo e-mail, comparando o texto <b>exato</b>.
   *
   * <p>
   * <b>Consulta derivada</b> ({@code findBy} + {@code Email}), sem
   * {@code IgnoreCase}: a comparação diferencia maiúsculas de minúsculas e não
   * aplica {@code trim}. O e-mail é único na tabela (restrição
   * {@code UNIQUE}), então há no máximo um resultado.
   * </p>
   *
   * <p>
   * Devolve a entidade {@link User} completa, sem as roles inicializadas
   * (relacionamento tardio). Não filtra por {@code active}.
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code AccountService} (recuperação de senha e reenvio do
   * e-mail de ativação).
   * </p>
   *
   * @param email e-mail do usuário a ser buscado
   * @return {@link Optional} com o usuário encontrado, ou vazio se não existir
   */
  Optional<User> findByEmail(String email);

  /**
   * Verifica se existe um usuário com o email informado, ignorando diferenças
   * entre maiúsculas e minúsculas.
   *
   * <p>
   * <b>Como o Spring interpreta esse método:</b>
   * </p>
   * <ul>
   * <li><b>existsBy</b> → operação de existência</li>
   * <li><b>Email</b> → campo da entidade</li>
   * <li><b>IgnoreCase</b> → ignora diferenças entre maiúsculas/minúsculas</li>
   * </ul>
   *
   * <p>
   * Consulta de existência: não carrega a entidade. Ao contrário de
   * {@link #findByEmail(String)}, ignora a caixa. Usada por
   * {@code UniqueEmailValidator}.
   * </p>
   *
   * @param email email a ser verificado
   * @return {@code true} se existir um usuário com o email, {@code false}
   *         caso contrário
   */
  boolean existsByEmailIgnoreCase(String email);

  /**
   * Verifica se existe um usuário com o email informado, ignorando diferenças
   * entre maiúsculas e minúsculas, e que tenha um ID diferente do fornecido.
   *
   * <p>
   * Este método é útil para validação de atualização, garantindo que o email
   * seja único entre os usuários, exceto o próprio usuário que está sendo
   * atualizado.
   * </p>
   *
   * <p>
   * <b>Como o Spring interpreta esse método:</b>
   * </p>
   * <ul>
   * <li><b>existsBy</b> → operação de existência</li>
   * <li><b>Email</b> → campo da entidade</li>
   * <li><b>IgnoreCase</b> → ignora diferenças entre maiúsculas/minúsculas</li>
   * <li><b>AndIdNot</b> → condição adicional para excluir um ID específico</li>
   * </ul>
   *
   * <p>
   * Usada por {@code UserUpdateValidator} (com o {@code id} da variável de
   * caminho da requisição) e por {@code UniqueEmailForAuthenticatedUserValidator}
   * (com o {@code id} do usuário autenticado).
   * </p>
   *
   * @param email email a ser verificado
   * @param id    ID do usuário a ser excluído da verificação
   * @return {@code true} se existir um usuário com o email (exceto o ID),
   *         {@code false}
   *         caso contrário
   */
  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  /**
   * Consulta <b>SQL nativa</b> usada na autenticação: para o e-mail informado,
   * devolve <b>uma linha por role</b> do usuário, com os dados mínimos para
   * montar um {@code UserDetails} sem carregar as entidades {@link User} e
   * {@code Role}.
   *
   * <p>
   * <b>Colunas retornadas</b> (os aliases correspondem aos getters de
   * {@link UserDetailsProjection}): {@code id} ({@code tb_user.id}),
   * {@code username} (alias de {@code tb_user.email}), {@code password} (hash),
   * {@code active}, {@code roleId} (alias de {@code tb_role.id}) e
   * {@code authority}. Como o mapeamento é feito por esses nomes, os aliases da
   * consulta e os getters da projeção precisam permanecer alinhados.
   * </p>
   *
   * <p>
   * <b>Regras da consulta:</b>
   * </p>
   * <ul>
   * <li>duas junções internas ({@code tb_user_role} e {@code tb_role}): um
   * usuário <b>sem nenhuma role</b> não gera linhas e, para quem chama, fica
   * indistinguível de um e-mail inexistente (lista vazia)</li>
   * <li>{@code WHERE tb_user.email = :email}: comparação exata (diferencia
   * maiúsculas de minúsculas, sem {@code trim})</li>
   * <li>não filtra por {@code active}: usuários inativos também são retornados, e
   * o campo vem na projeção para que o chamador decida</li>
   * <li>sem paginação nem ordenação</li>
   * </ul>
   *
   * <p>
   * <b>Dependências do schema:</b> tabelas {@code tb_user}, {@code tb_user_role}
   * e {@code tb_role} e suas colunas (migrations V005 a V008). O parâmetro
   * {@code :email} é associado pelo nome do parâmetro do método (não há
   * {@code @Param}), o que depende da compilação com {@code -parameters}.
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code UserService.loadUserByUsername}, chamado no fluxo
   * de login (grant {@code password}).
   * </p>
   *
   * @param email e-mail (nome de usuário) a ser procurado
   * @return uma projeção por role do usuário; lista vazia se o e-mail não existir
   *         ou se o usuário não tiver roles
   */
  @Query(nativeQuery = true, value = """
        SELECT tb_user.id AS id, tb_user.email AS username, tb_user.password, tb_user.active, tb_role.id AS roleId, tb_role.authority
        FROM tb_user
        INNER JOIN tb_user_role ON tb_user.id = tb_user_role.user_id
        INNER JOIN tb_role ON tb_role.id = tb_user_role.role_id
        WHERE tb_user.email = :email
      """)
  List<UserDetailsProjection> searchUserAndRolesByEmail(String email);
}
