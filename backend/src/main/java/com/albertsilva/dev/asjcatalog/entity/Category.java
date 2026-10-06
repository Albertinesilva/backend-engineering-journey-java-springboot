package com.albertsilva.dev.dscatalog.entity;

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
 * <b>Regras de negócio:</b>
 * </p>
 * <ul>
 * <li>O nome da categoria deve representar claramente o agrupamento entre os
 * produtos</li>
 * <li>A categoria pode estar ativa ou inativa</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: tb_category</li>
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
  private String name;

  /**
   * Descrição detalhada da categoria.
   *
   * <p>
   * Pode ser utilizada para fornecer mais contexto sobre o tipo de produtos
   * associados.
   * </p>
   */
  private String description;

  /**
   * Indica se a categoria está ativa.
   *
   * <p>
   * Categorias inativas podem ser desconsideradas em listagens ou operações
   * de negócio.
   * </p>
   */
  private boolean active;

  /**
   * Data de criação do registro.
   *
   * <p>
   * Preenchida automaticamente no momento da persistência.
   * </p>
   */
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant createdAt;

  /**
   * Data da última atualização do registro.
   *
   * <p>
   * Atualizada automaticamente sempre que a entidade é modificada.
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
   */
  @ManyToMany(mappedBy = "categories")
  private Set<Product> products = new HashSet<>();

  public Category() {
  }

  public Category(Long id, String name, String description, boolean active) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.active = active;
  }

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
   * Responsável por definir a data de criação.
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