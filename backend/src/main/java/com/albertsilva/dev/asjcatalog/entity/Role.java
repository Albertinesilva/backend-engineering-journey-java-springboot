package com.albertsilva.dev.asjcatalog.entity;

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
 * atribuídas a instâncias de {@code User} por meio de relacionamento.
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

  public Role() {
  }

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
   * @return nome/identificador da autoridade (por exemplo, "ROLE_USER")
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
