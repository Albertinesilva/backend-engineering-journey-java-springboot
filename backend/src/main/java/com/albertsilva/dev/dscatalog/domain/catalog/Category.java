package com.albertsilva.dev.dscatalog.domain.catalog;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * Representa uma categoria de produtos no sistema.
 *
 * <p>
 * Esta entidade é utilizada para classificar e organizar os produtos,
 * permitindo agrupamentos lógicos que facilitam a navegação, busca e
 * gerenciamento no sistema.
 * </p>
 *
 * <p>
 * <b>Regras e invariantes:</b>
 * </p>
 * <ul>
 * <li>O nome é obrigatório e único, com até 80 caracteres (restrições do
 * mapeamento); a descrição é opcional, com até 255 caracteres</li>
 * <li>A categoria possui um indicador {@code active}, que a entidade apenas
 * armazena: ela não impõe nenhuma restrição com base nele. Uma instância nova
 * nasce inativa ({@code false}); quem a cria define o valor</li>
 * <li>A validação de formato do nome e da descrição não é feita nesta
 * entidade; ocorre nos DTOs de entrada</li>
 * <li>A identidade é definida somente pelo {@code id} (ver
 * {@link #equals(Object)})</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: {@code tb_category}</li>
 * <li>Relacionamento muitos-para-muitos com {@link Product}, no qual
 * {@code Product} é o lado dono; nesta entidade o lado é apenas inverso
 * ({@code mappedBy = "categories"})</li>
 * <li>Datas de auditoria preenchidas por callbacks JPA
 * ({@link #prePersist()} e {@link #preUpdate()})</li>
 * </ul>
 */
@Entity
@Table(name = "tb_category")
public class Category implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Identificador único da categoria.
   *
   * <p>
   * Gerado automaticamente pelo banco de dados.
   * </p>
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Nome da categoria.
   *
   * <p>
   * Utilizado para identificação e exibição.
   * </p>
   */
  @Column(nullable = false, unique = true, length = 80)
  private String name;

  /**
   * Descrição detalhada da categoria.
   *
   * <p>
   * Pode ser utilizada para fornecer mais contexto sobre o tipo de produtos
   * associados.
   * </p>
   */
  @Column(length = 255)
  private String description;

  /**
   * Indica se a categoria está ativa.
   *
   * <p>
   * A entidade apenas armazena o valor: não filtra nem bloqueia nada com base
   * nele. Vale {@code false} em uma instância recém-criada até que seja
   * definido (por {@link #setActive(boolean)} ou pelos construtores que
   * recebem o parâmetro).
   * </p>
   */
  private boolean active;

  /**
   * Data de criação do registro.
   *
   * <p>
   * Preenchida automaticamente no momento da persistência (callback
   * {@link #prePersist()}); permanece {@code null} em instâncias ainda não
   * persistidas.
   * </p>
   */
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant createdAt;

  /**
   * Data da última atualização do registro.
   *
   * <p>
   * Atualizada pelo callback {@link #preUpdate()} quando uma alteração é
   * sincronizada com o banco. Não é preenchida na criação: permanece
   * {@code null} até a primeira atualização.
   * </p>
   */
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant updatedAt;

  /**
   * Conjunto de produtos associados a esta categoria.
   *
   * <p>
   * Representa a relação de muitos para muitos entre categorias e produtos.
   * </p>
   *
   * <p>
   * É o lado inverso do relacionamento ({@code mappedBy}): a tabela de junção é
   * mantida por {@link Product#getCategories()}, portanto adicionar ou remover
   * produtos apenas neste conjunto não altera o vínculo persistido.
   * </p>
   */
  @ManyToMany(mappedBy = "categories")
  private Set<Product> products = new HashSet<>();

  /**
   * Cria uma categoria vazia: textos nulos, {@code active} igual a
   * {@code false} e nenhum produto associado. Construtor sem argumentos exigido
   * pela JPA.
   */
  public Category() {
  }

  /**
   * Cria uma categoria com identificador definido.
   *
   * <p>
   * Nenhum argumento é validado, e {@code createdAt}/{@code updatedAt} não são
   * preenchidos aqui (apenas pelos callbacks JPA).
   * </p>
   *
   * @param id          identificador da categoria
   * @param name        nome da categoria
   * @param description descrição da categoria
   * @param active      indica se a categoria está ativa
   */
  public Category(Long id, String name, String description, boolean active) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.active = active;
  }

  /**
   * Cria uma categoria ainda sem identificador (o banco o gera na persistência).
   *
   * <p>
   * Nenhum argumento é validado, e {@code createdAt}/{@code updatedAt} não são
   * preenchidos aqui (apenas pelos callbacks JPA).
   * </p>
   *
   * @param name        nome da categoria
   * @param description descrição da categoria
   * @param active      indica se a categoria está ativa
   */
  public Category(String name, String description, boolean active) {
    this.name = name;
    this.description = description;
    this.active = active;
  }

  /**
   * @return identificador único da categoria
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id identificador único da categoria
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return nome da categoria
   */
  public String getName() {
    return name;
  }

  /**
   * @param name nome da categoria
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return descrição da categoria
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description descrição detalhada da categoria
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return {@code true} se a categoria estiver ativa, {@code false} caso
   *         contrário
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @param active define se a categoria estará ativa ou não
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * @return data de criação do registro
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * @return data da última atualização do registro
   */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Método executado automaticamente antes da persistência da entidade.
   *
   * <p>
   * Responsável por definir a data de criação ({@code createdAt}); a data de
   * atualização ({@code updatedAt}) não é definida neste momento.
   * </p>
   */
  @PrePersist
  public void prePersist() {
    createdAt = Instant.now();
  }

  /**
   * Método executado automaticamente antes da atualização da entidade.
   *
   * <p>
   * Responsável por atualizar a data de modificação.
   * </p>
   */
  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  /**
   * Retorna o próprio conjunto interno (não uma cópia). Por ser o lado inverso
   * do relacionamento, alterações feitas nele não modificam o vínculo
   * persistido; ver {@link Product#getCategories()}.
   *
   * @return conjunto de produtos associados a esta categoria
   */
  public Set<Product> getProducts() {
    return products;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  /**
   * Compara duas categorias com base no identificador.
   *
   * <p>
   * Duas categorias são consideradas iguais quando possuem o mesmo ID.
   * Duas instâncias sem ID (ainda não persistidas) são consideradas iguais
   * entre si, e {@link #hashCode()} depende apenas do ID. A comparação exige a
   * mesma classe exata ({@code getClass()}), e não apenas compatibilidade de
   * tipo.
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
    Category other = (Category) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }
}