package com.albertsilva.dev.dscatalog.domain.user;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
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
 * A classe também implementa {@link UserDetails}: esta entidade de domínio é
 * usada diretamente pelo Spring Security como representação do usuário
 * autenticável.
 * </p>
 * <ul>
 * <li>{@link #getUsername()} devolve o e-mail</li>
 * <li>{@link #getAuthorities()} devolve o próprio conjunto de roles</li>
 * <li>{@link #isEnabled()} devolve o indicador {@code active}</li>
 * <li>{@link #isAccountNonExpired()}, {@link #isAccountNonLocked()} e
 * {@link #isCredentialsNonExpired()} retornam sempre {@code true}: o modelo
 * atual não possui expiração ou bloqueio de conta nem expiração de
 * credenciais</li>
 * </ul>
 *
 * <p>
 * <b>Regras e invariantes:</b>
 * </p>
 * <ul>
 * <li>O e-mail é obrigatório e único (mapeamento) e funciona como nome de
 * usuário; o formato e a unicidade "de negócio" são verificados fora da
 * entidade (DTOs e validators)</li>
 * <li>Nome, sobrenome e senha não possuem restrições no mapeamento</li>
 * <li>A entidade não codifica a senha: o valor informado em
 * {@link #setPassword(String)} é guardado como está, e espera-se que seja o
 * hash já calculado por quem chama</li>
 * <li>Uma instância nova nasce inativa ({@code active = false}). Contas
 * inativas continuam com suas roles, mas {@link #isEnabled()} devolve
 * {@code false}, e o fluxo de login customizado do sistema rejeita contas
 * nessa situação</li>
 * <li>As roles são um conjunto: adicionar a mesma role duas vezes não duplica o
 * vínculo</li>
 * <li>A identidade é definida somente pelo {@code id} (ver
 * {@link #equals(Object)})</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: {@code tb_user}</li>
 * <li>Muitos-para-muitos com {@link Role} (lado dono, tabela de junção
 * {@code tb_user_role})</li>
 * <li>Um-para-muitos com {@link Token} (lado inverso; {@code cascade = ALL} e
 * {@code orphanRemoval = true})</li>
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

  /** Senha da conta, esperada já codificada (hash); a entidade não a codifica. */
  private String password;

  /**
   * Conjunto de roles/autoridades atribuídas ao usuário.
   *
   * <p>
   * Lado dono do relacionamento (tabela {@code tb_user_role}), sem
   * {@code cascade}: as roles precisam já existir.
   * </p>
   */
  @ManyToMany
  @JoinTable(name = "tb_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  /**
   * Tokens de ativação de conta e de recuperação de senha emitidos para este
   * usuário.
   *
   * <p>
   * Lado inverso do relacionamento (o lado dono é {@link Token#getUser()}). Com
   * {@code cascade = ALL} e {@code orphanRemoval = true}, as operações de
   * persistência aplicadas ao usuário se propagam aos tokens (inclusive a
   * remoção do usuário, que remove seus tokens), e um token retirado deste
   * conjunto é excluído. A classe não expõe getter para este campo.
   * </p>
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Token> tokens = new HashSet<>();

  /**
   * Indica se a conta do usuário está ativa.
   *
   * <p>
   * É o valor devolvido por {@link #isEnabled()}. Vale {@code false} em uma
   * instância recém-criada. Pode ser alterado por {@link #activate()},
   * {@link #deactivate()} ou {@link #setActive(boolean)}.
   * </p>
   */
  private boolean active;

  /**
   * Cria um usuário vazio: campos nulos, {@code active} igual a {@code false},
   * sem roles e sem tokens. Construtor sem argumentos exigido pela JPA.
   */
  public User() {
  }

  /**
   * Cria um usuário com os dados informados.
   *
   * <p>
   * Nenhum argumento é validado, a senha não é codificada e os conjuntos de
   * roles e de tokens começam vazios.
   * </p>
   *
   * @param id        identificador do usuário
   * @param firstName primeiro nome
   * @param lastName  sobrenome
   * @param email     e-mail (também usado como nome de usuário)
   * @param password  senha, esperada já codificada
   * @param active    indica se a conta está ativa
   */
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
   * <p>
   * Como as roles formam um conjunto, adicionar uma role já presente não a
   * duplica. Não há validação: a role não é verificada nem persistida por este
   * método.
   * </p>
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
   * Define diretamente o indicador de conta ativa. Tem o mesmo efeito de
   * {@link #activate()} (com {@code true}) e de {@link #deactivate()} (com
   * {@code false}).
   *
   * @param active {@code true} para ativar a conta, {@code false} para
   *               desativá-la
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Ativa a conta ({@code active = true}). Operação idempotente, sem outros
   * efeitos: não altera roles nem tokens.
   */
  public void activate() {
    this.active = true;
  }

  /**
   * Desativa a conta ({@code active = false}). Operação idempotente, sem outros
   * efeitos: não altera roles nem tokens.
   */
  public void deactivate() {
    this.active = false;
  }

  /**
   * Verifica se o usuário possui uma role com o nome informado.
   *
   * <p>
   * A comparação é exata (diferencia maiúsculas de minúsculas) e feita com o
   * valor de {@link Role#getAuthority()}; portanto o nome deve incluir o
   * prefixo, por exemplo {@code ROLE_ADMIN}.
   * </p>
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  /**
   * Compara dois usuários pelo identificador ({@code id}).
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
   * @return coleção de autoridades (roles) do usuário; é o próprio conjunto
   *         interno {@code roles}, e não uma cópia
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
   * Retorna sempre {@code true}: o modelo atual não possui expiração de conta.
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * Retorna sempre {@code true}: o modelo atual não possui bloqueio de conta.
   */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /**
   * Retorna sempre {@code true}: o modelo atual não possui expiração de
   * credenciais.
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * Indica se a conta está habilitada para autenticação. Devolve o valor do
   * indicador {@code active}.
   *
   * @return {@code true} se a conta estiver ativa
   */
  @Override
  public boolean isEnabled() {
    return active;
  }

}
