package com.albertsilva.dev.dscatalog.domain.user;

import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Representa uma autoridade (role) do sistema.
 *
 * <p>
 * Roles são utilizadas para controle de acesso e autorização, e são
 * atribuídas a instâncias de {@link User} por meio de relacionamento.
 * </p>
 *
 * <p>
 * Implementa {@link GrantedAuthority}: o valor de {@code authority} é o texto
 * que o Spring Security trata como autoridade. Nos dados iniciais do projeto
 * ele já inclui o prefixo {@code ROLE_} (por exemplo, {@code ROLE_ADMIN} e
 * {@code ROLE_OPERATOR}). O relacionamento é unidirecional: {@code User}
 * conhece suas roles, mas {@code Role} não referencia os usuários.
 * </p>
 *
 * <p>
 * <b>Invariantes:</b> o mapeamento não declara restrições de nulidade ou
 * unicidade para {@code authority}; a identidade é definida somente pelo
 * {@code id} (ver {@link #equals(Object)}).
 * </p>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: tb_role</li>
 * </ul>
 */
@Entity
@Table(name = "tb_role")
public class Role implements GrantedAuthority {
  private static final long serialVersionUID = 1L;

  /** Identificador único da role. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Nome/identificador da autoridade (ex.: "ROLE_ADMIN"). */
  private String authority;

  /**
   * Cria uma role vazia ({@code id} e {@code authority} nulos). Construtor sem
   * argumentos exigido pela JPA.
   */
  public Role() {
  }

  /**
   * Cria uma role com identificador e autoridade informados, sem nenhuma
   * validação dos argumentos.
   *
   * @param id        identificador da role
   * @param authority nome da autoridade (por exemplo, {@code ROLE_ADMIN})
   */
  public Role(Long id, String authority) {
    this.id = id;
    this.authority = authority;
  }

  /**
   * @return identificador único da role
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id identificador único da role
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return nome/identificador da autoridade (por exemplo, "ROLE_ADMIN")
   */
  @Override
  public String getAuthority() {
    return authority;
  }

  /**
   * @param authority nome/identificador da autoridade
   */
  public void setAuthority(String authority) {
    this.authority = authority;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  /**
   * Compara duas roles pelo identificador ({@code id}).
   *
   * <p>
   * Duas instâncias sem {@code id} (ainda não persistidas) são consideradas
   * iguais entre si, e {@link #hashCode()} depende apenas do {@code id}. A
   * comparação exige a mesma classe exata ({@code getClass()}), e não apenas
   * compatibilidade de tipo.
   * </p>
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Role other = (Role) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

}
