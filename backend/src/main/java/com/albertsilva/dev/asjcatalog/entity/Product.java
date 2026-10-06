package com.albertsilva.dev.dscatalog.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/**
 * Representa um produto no sistema.
 *
 * <p>
 * Esta entidade contém as informações essenciais de um produto,
 * incluindo dados descritivos, preço e relacionamento com categorias.
 * </p>
 *
 * <p>
 * <b>Regras de negócio:</b>
 * </p>
 * <ul>
 * <li>O produto deve possuir um nome</li>
 * <li>O preço deve ser maior que zero</li>
 * <li>O produto pode estar ativo ou inativo</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: tb_product</li>
 * <li>Relacionamento Many-to-Many com Category</li>
 * </ul>
 */
@Entity
@Table(name = "tb_product")
public class Product implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Identificador único do produto.
   *
   * <p>
   * Gerado automaticamente pelo banco de dados.
   * </p>
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Nome do produto.
   *
   * <p>
   * Utilizado para identificação e exibição.
   * </p>
   */
  private String name;

  /**
   * Descrição detalhada do produto.
   *
   * <p>
   * Armazenada como texto longo no banco de dados.
   * </p>
   */
  @Column(columnDefinition = "TEXT")
  private String description;

  /**
   * Preço do produto.
   *
   * <p>
   * Utilizado em operações comerciais e cálculos financeiros.
   * </p>
   */
  private Double price;

  /**
   * URL da imagem do produto.
   *
   * <p>
   * Utilizada para exibição visual em interfaces.
   * </p>
   */
  private String imgUrl;

  /**
   * Data associada ao produto.
   *
   * <p>
   * Pode representar data de cadastro, publicação ou outro contexto definido pela
   * aplicação.
   * </p>
   */
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant date;

  /**
   * Indica se o produto está ativo.
   *
   * <p>
   * Produtos inativos podem ser ocultados ou desconsiderados em operações.
   * </p>
   */
  private boolean active;

  /**
   * Categorias associadas ao produto.
   *
   * <p>
   * Relacionamento muitos-para-muitos onde:
   * </p>
   * <ul>
   * <li>Um produto pode pertencer a várias categorias</li>
   * <li>Uma categoria pode conter vários produtos</li>
   * </ul>
   *
   * <p>
   * Este relacionamento é gerenciado pela tabela intermediária
   * {@code tb_product_category}.
   * </p>
   */
  @ManyToMany
  @JoinTable(name = "tb_product_category", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
  private Set<Category> categories = new HashSet<>();

  public Product() {
  }

  public Product(Long id, String name, String description, Double price, String imgUrl, Instant date, boolean active) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.price = price;
    this.imgUrl = imgUrl;
    this.date = date;
    this.active = active;
  }

  public Product(String name, String description, Double price, String imgUrl, Instant date, boolean active) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.imgUrl = imgUrl;
    this.date = date;
    this.active = active;
  }

  /**
   * @return identificador único do produto
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id identificador único do produto
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return nome do produto
   */
  public String getName() {
    return name;
  }

  /**
   * @param name nome do produto
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return descrição do produto
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description descrição detalhada do produto
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return preço do produto
   */
  public Double getPrice() {
    return price;
  }

  /**
   * @param price valor monetário do produto
   */
  public void setPrice(Double price) {
    this.price = price;
  }

  /**
   * @return URL da imagem do produto
   */
  public String getImgUrl() {
    return imgUrl;
  }

  /**
   * @param imgUrl URL da imagem do produto
   */
  public void setImgUrl(String imgUrl) {
    this.imgUrl = imgUrl;
  }

  /**
   * @return data associada ao produto
   */
  public Instant getDate() {
    return date;
  }

  /**
   * @param date data associada ao produto
   */
  public void setDate(Instant date) {
    this.date = date;
  }

  /**
   * @return conjunto de categorias associadas ao produto
   */
  public Set<Category> getCategories() {
    return categories;
  }

  /**
   * @return {@code true} se o produto estiver ativo
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @param active define se o produto estará ativo ou não
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

  /**
   * Compara dois produtos com base no identificador.
   *
   * <p>
   * Dois produtos são considerados iguais quando possuem o mesmo ID.
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
    Product other = (Product) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }
}
