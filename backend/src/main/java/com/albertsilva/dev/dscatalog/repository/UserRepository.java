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
 * Repositório responsável pelo acesso a dados da entidade {@link User}.
 *
 * <p>
 * Esta interface utiliza o Spring Data JPA para fornecer operações
 * de persistência de forma automática, sem necessidade de implementação manual.
 * </p>
 *
 * <p>
 * <b>Funcionalidades fornecidas automaticamente:</b>
 * </p>
 * <ul>
 * <li>Salvar um usuário (save)</li>
 * <li>Buscar por ID (findById)</li>
 * <li>Listar todas (findAll)</li>
 * <li>Deletar (delete)</li>
 * </ul>
 *
 * <p>
 * <b>Importante para júnior:</b>
 * </p>
 * <ul>
 * <li>Você não implementa essa interface</li>
 * <li>O Spring gera tudo em tempo de execução</li>
 * <li>O nome dos métodos define a query automaticamente</li>
 * </ul>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Busca usuários cujo nome contenha o valor informado (ignorando diferenças
   * entre
   * maiúsculas/minúsculas).
   *
   * <p>
   * <b>Como o Spring interpreta esse método:</b>
   * </p>
   * <ul>
   * <li><b>findBy</b> → operação de busca</li>
   * <li><b>FirstName</b> → campo da entidade</li>
   * <li><b>Containing</b> → busca parcial (LIKE %valor%)</li>
   * <li><b>IgnoreCase</b> → ignora diferenças entre maiúsculas/minúsculas</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo prático:</b>
   * </p>
   * 
   * <pre>
   * Busca por "ele" pode retornar:
   * - "Alexandre"
   * - "Elenice"
   * </pre>
   *
   * <p>
   * <b>Paginação:</b>
   * </p>
   * <ul>
   * <li>O resultado é paginado usando {@link Pageable}</li>
   * <li>Evita retorno de grandes volumes de dados</li>
   * </ul>
   *
   * @param firstName termo para busca no campo "firstName"
   * @param pageable  configurações de paginação (página, tamanho, ordenação)
   * @return página contendo usuários encontrados
   */
  Page<User> findByFirstNameContainingIgnoreCase(String firstName, Pageable pageable);

  /**
   * Busca um usuário pelo email.
   *
   * <p>
   * <b>Como o Spring interpreta esse método:</b>
   * </p>
   * <ul>
   * <li><b>findBy</b> → operação de busca</li>
   * <li><b>Email</b> → campo da entidade</li>
   * </ul>
   *
   * @param email email do usuário a ser buscado
   * @return usuário encontrado ou {@code null} se não existir
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
   * @param email email a ser verificado
   * @param id    ID do usuário a ser excluído da verificação
   * @return {@code true} se existir um usuário com o email (exceto o ID),
   *         {@code false}
   *         caso contrário
   */
  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  @Query(nativeQuery = true, value = """
        SELECT tb_user.id AS id, tb_user.email AS username, tb_user.password, tb_user.active, tb_role.id AS roleId, tb_role.authority
        FROM tb_user
        INNER JOIN tb_user_role ON tb_user.id = tb_user_role.user_id
        INNER JOIN tb_role ON tb_role.id = tb_user_role.role_id
        WHERE tb_user.email = :email
      """)
  List<UserDetailsProjection> searchUserAndRolesByEmail(String email);
}
