package com.albertsilva.dev.dscatalog.domain.catalog;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.albertsilva.dev.dscatalog.domain.Identifiable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
 * <b>Regras e invariantes:</b>
 * </p>
 * <ul>
 * <li>O nome é obrigatório e único (restrições do mapeamento)</li>
 * <li>A entidade <b>não</b> valida o preço: o campo é um {@link Double}
 * opcional, e a exigência de valor positivo é feita no DTO de entrada
 * ({@code @Positive}), não aqui</li>
 * <li>O indicador {@code active} é apenas armazenado: a entidade não impõe
 * restrições com base nele. Uma instância nova nasce inativa ({@code false})
 * até que o valor seja definido</li>
 * <li>Um produto pode pertencer a várias categorias; a entidade não exige pelo
 * menos uma categoria (essa exigência está nos DTOs de entrada)</li>
 * <li>A identidade é definida somente pelo {@code id} (ver
 * {@link #equals(Object)})</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: {@code tb_product}</li>
 * <li>Relacionamento muitos-para-muitos com {@link Category}, no qual
 * {@code Product} é o lado dono (tabela de junção
 * {@code tb_product_category})</li>
 * <li>Datas de auditoria preenchidas por callbacks JPA privados
 * ({@code prePersist} e {@code preUpdate})</li>
 * <li>Implementa {@link Identifiable}, o que permite tratá-lo de forma genérica
 * pelo identificador</li>
 * </ul>
 */
@Entity
@Table(name = "tb_product")
public class Product implements Serializable, Identifiable<Long> {
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
  @Column(nullable = false, unique = true)
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
   * Valor do produto. É um {@link Double} sem restrição de nulidade ou de faixa
   * na entidade (a validação ocorre nos DTOs de entrada); por ser ponto
   * flutuante, não oferece precisão decimal exata.
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
   * Data de criação do registro.
   *
   * <p>
   * Preenchida automaticamente no momento da persistência (callback
   * {@code prePersist}); permanece {@code null} em instâncias ainda não
   * persistidas.
   * </p>
   */
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant createdAt;

  /**
   * Data da última atualização do registro.
   *
   * <p>
   * Definida na criação (com o mesmo instante de {@code createdAt}) e
   * atualizada pelo callback {@code preUpdate} quando uma alteração é
   * sincronizada com o banco. Permanece {@code null} em instâncias ainda não
   * persistidas.
   * </p>
   */
  @Column(columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant updatedAt;

  /**
   * Indica se o produto está ativo.
   *
   * <p>
   * A entidade apenas armazena o valor: não oculta nem bloqueia nada com base
   * nele. Vale {@code false} em uma instância recém-criada até que seja
   * definido (por {@link #setActive(boolean)} ou pelos construtores que
   * recebem o parâmetro).
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
   *
   * <p>
   * {@code Product} é o lado dono: adicionar ou remover categorias neste
   * conjunto altera o vínculo persistido. Não há {@code cascade}, portanto as
   * categorias precisam já existir (uma categoria nova não é criada ao salvar
   * o produto), e não há {@code fetch} explícito (vale o padrão da JPA para
   * coleções, carregamento tardio).
   * </p>
   */
  @ManyToMany
  @JoinTable(name = "tb_product_category", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
  private Set<Category> categories = new HashSet<>();

  /**
   * Cria um produto vazio: campos nulos, {@code active} igual a {@code false} e
   * nenhuma categoria associada. Construtor sem argumentos exigido pela JPA.
   */
  public Product() {
  }

  /**
   * Cria um produto com identificador definido.
   *
   * <p>
   * Nenhum argumento é validado; as categorias começam vazias e
   * {@code createdAt}/{@code updatedAt} não são preenchidos aqui (apenas pelos
   * callbacks JPA).
   * </p>
   *
   * @param id          identificador do produto
   * @param name        nome do produto
   * @param description descrição do produto
   * @param price       preço do produto
   * @param imgUrl      URL da imagem do produto
   * @param active      indica se o produto está ativo
   */
  public Product(Long id, String name, String description, Double price, String imgUrl, boolean active) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.price = price;
    this.imgUrl = imgUrl;
    this.active = active;
  }

  /**
   * Cria um produto ainda sem identificador (o banco o gera na persistência).
   *
   * <p>
   * Nenhum argumento é validado; as categorias começam vazias e
   * {@code createdAt}/{@code updatedAt} não são preenchidos aqui (apenas pelos
   * callbacks JPA).
   * </p>
   *
   * @param name        nome do produto
   * @param description descrição do produto
   * @param price       preço do produto
   * @param imgUrl      URL da imagem do produto
   * @param active      indica se o produto está ativo
   */
  public Product(String name, String description, Double price, String imgUrl, boolean active) {
    this.name = name;
    this.description = description;
    this.price = price;
    this.imgUrl = imgUrl;
    this.active = active;
  }

  /**
   * @return identificador único do produto
   */
  @Override
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
   * Retorna o próprio conjunto interno (não uma cópia). Como {@code Product} é
   * o lado dono do relacionamento, adicionar ou remover categorias aqui altera
   * o vínculo persistido em {@code tb_product_category}. Não existe método
   * auxiliar de vínculo na entidade.
   *
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
   * Callback JPA executado antes da primeira persistência: define
   * {@code createdAt} e {@code updatedAt} com o mesmo instante atual.
   */
  @PrePersist
  private void prePersist() {
    Instant now = Instant.now();

    createdAt = now;
    updatedAt = now;
  }

  /**
   * Callback JPA executado antes de uma atualização: define {@code updatedAt}
   * com o instante atual.
   */
  @PreUpdate
  private void preUpdate() {
    updatedAt = Instant.now();
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
   * Dois produtos são considerados iguais quando possuem o mesmo ID. Duas
   * instâncias sem ID (ainda não persistidas) são consideradas iguais entre
   * si, e {@link #hashCode()} depende apenas do ID. A comparação exige a mesma
   * classe exata ({@code getClass()}), e não apenas compatibilidade de tipo.
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
