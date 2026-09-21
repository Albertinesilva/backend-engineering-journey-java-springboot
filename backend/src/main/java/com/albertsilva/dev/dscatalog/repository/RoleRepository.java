package com.albertsilva.dev.dscatalog.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.dscatalog.domain.user.Role;

/**
 * Repositório Spring Data JPA da entidade {@link Role} (tabela
 * {@code tb_role}), com chave primária {@code Long}.
 *
 * <p>
 * Estende {@link JpaRepository} e declara uma única consulta derivada,
 * {@link #findByAuthority(String)}. As demais operações são as herdadas; entre
 * elas, {@code findAllById} e {@code existsById} são usados para validar e
 * resolver os {@code roleIds} recebidos pela API ({@code UserService} e
 * {@code ValidRolesValidator}).
 * </p>
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * Busca uma role pelo nome exato da autoridade (por exemplo,
   * {@code ROLE_OPERATOR}), diferenciando maiúsculas de minúsculas.
   *
   * <p>
   * <b>Consulta derivada.</b> A coluna {@code authority} não possui restrição
   * {@code UNIQUE} (migration V005); a consulta assume que há no máximo uma role
   * por nome. Se houvesse duas, o Spring Data lançaria exceção por resultado não
   * único.
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code AccountService.register}, que busca
   * {@code ROLE_OPERATOR} para o usuário recém-registrado (e lança
   * {@code IllegalStateException} se a role não existir).
   * </p>
   *
   * @param authority nome da autoridade
   * @return {@link Optional} com a role, ou vazio se não existir
   */
  Optional<Role> findByAuthority(String authority);
}
