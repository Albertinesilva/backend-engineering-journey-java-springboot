package com.albertsilva.dev.asjcatalog.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Representa um usuário do sistema.
 *
 * <p>
 * Esta entidade armazena informações de conta e credenciais do usuário,
 * além das roles atribuídas para controle de acesso.
 * </p>
 *
 * <p>
 * <b>Regras de negócio:</b>
 * </p>
 * <ul>
 * <li>O email deve ser único e identifica a conta do usuário.</li>
 * <li>As roles determinam as permissões associadas ao usuário.</li>
 * <li>Usuários inativos podem ser ignorados em operações sensíveis.</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: tb_user</li>
 * <li>Tabela de junção com {@code tb_role} para representar roles</li>
 * </ul>
 */
@Entity
@Table(name = "tb_user")
public class User implements UserDetails {
  private static final long serialVersionUID = 1L;

  /** Identificador único do usuário. Gerado pelo banco de dados. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Primeiro nome do usuário, para exibição e saudação. */
  private String firstName;

  /** Sobrenome do usuário. */
  private String lastName;

  /**
   * Email do usuário.
   *
   * <p>
   * Campo obrigatório e único, usado como identificador de conta.
   * </p>
   */
  @Column(nullable = false, unique = true)
  private String email;

  /** Senha criptografada da conta do usuário. */
  private String password;

  @ManyToMany
  @JoinTable(name = "tb_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
  /** Conjunto de roles/autoridades atribuídas ao usuário. */
  private Set<Role> roles = new HashSet<>();

  /** Indica se a conta do usuário está ativa. */
  private boolean active;

  public User() {
  }

  public User(Long id, String firstName, String lastName, String email, String password, boolean active) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.password = password;
    this.active = active;
  }

  /**
   * @return identificador único do usuário
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id identificador único do usuário
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return primeiro nome do usuário
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * @param firstName primeiro nome do usuário
   */
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /**
   * @return sobrenome do usuário
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * @param lastName sobrenome do usuário
   */
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /**
   * @return email do usuário
   */
  public String getEmail() {
    return email;
  }

  /**
   * @param email email do usuário
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * @return senha criptografada do usuário
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param password senha criptografada do usuário
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @return conjunto de roles/autoridades associadas ao usuário
   */
  public Set<Role> getRoles() {
    return roles;
  }

  /**
   * Adiciona uma role ao usuário.
   *
   * @param role role a ser adicionada
   */
  public void addRole(Role role) {
    this.roles.add(role);
  }

  /**
   * @return {@code true} se a conta do usuário estiver ativa
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @param active define se a conta do usuário está ativa
   */
  public void setActive(boolean active) {
    this.active = active;
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
    User other = (User) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

  /**
   * Retorna as autoridades (roles) associadas ao usuário.
   *
   * <p>
   * Este método é usado pelo Spring Security para determinar as permissões do
   * usuário durante a autenticação e autorização.
   * </p>
   *
   * @return coleção de autoridades (roles) do usuário
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles;
  }

  /**
   * Retorna o email do usuário, que é usado como nome de usuário para
   * autenticação.
   * 
   * @return email do usuário como nome de usuário para autenticação
   */
  @Override
  public String getUsername() {
    return email;
  }

  /**
   * Verifica se o usuário possui uma role com o nome informado.
   *
   * @param roleName nome da role a ser verificada
   * @return {@code true} se o usuário possui a role; {@code false} caso
   *         contrário
   */
  public boolean hasRole(String roleName) {

    if (roleName == null) {
      return false;
    }

    return roles.stream().anyMatch(role -> roleName.equals(role.getAuthority()));
  }

}
