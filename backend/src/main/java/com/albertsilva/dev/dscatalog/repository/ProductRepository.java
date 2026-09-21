package com.albertsilva.dev.dscatalog.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.dscatalog.domain.catalog.Product;
import com.albertsilva.dev.dscatalog.projection.ProductProjection;

/**
 * Repositório Spring Data JPA da entidade {@link Product} (tabela
 * {@code tb_product}), com chave primária {@code Long}.
 *
 * <p>
 * Além das operações herdadas de {@link JpaRepository}, declara consultas de
 * três naturezas:
 * </p>
 * <ul>
 * <li><b>derivadas</b>: {@link #findByNameContainingIgnoreCase(String, Pageable)},
 * {@link #existsByNameIgnoreCase(String)} e
 * {@link #existsByNameIgnoreCaseAndIdNot(String, Long)}</li>
 * <li><b>SQL nativo</b>: {@link #searchProducts(List, String, Pageable)},
 * que retorna a projeção {@link ProductProjection} (somente {@code id} e
 * {@code name})</li>
 * <li><b>JPQL com {@code JOIN FETCH}</b>:
 * {@link #searchProductsWithCategories(List)}, que carrega produtos já com suas
 * categorias</li>
 * </ul>
 *
 * <p>
 * <b>Listagem em duas etapas:</b> {@code searchProducts} e
 * {@code searchProductsWithCategories} são usadas em sequência por
 * {@code ProductService.findAllPaged}. A primeira resolve filtro, paginação e
 * ordenação e devolve apenas os ids da página; a segunda carrega, sem
 * paginação, as entidades desses ids com as categorias inicializadas. Como a
 * segunda não garante ordem, o service reordena o resultado pela ordem da
 * primeira ({@code IdentifiableUtils.reorderByReference}).
 * </p>
 *
 * <p>
 * <b>Outros usos:</b> {@code ProductService} (CRUD; {@code search} usa a
 * consulta derivada) e {@code ProductCreateValidator} /
 * {@code ProductUpdateValidator} (unicidade de nome).
 * </p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  /**
   * Lista, de forma paginada, os produtos cujo nome contém o termo informado,
   * sem diferenciar maiúsculas de minúsculas.
   *
   * <p>
   * <b>Consulta derivada:</b> o SQL é gerado pelo Spring Data a partir do nome
   * do método, resultando em um {@code LIKE '%termo%'} aplicado ao campo
   * {@code name} sem distinção de caixa. Não há {@code @Query}.
   * </p>
   *
   * <p>
   * <b>Paginação e ordenação:</b> definidas pelo {@link Pageable}, com nomes de
   * propriedades da entidade. Sem ordenação informada, a consulta não define a
   * ordem. Como o retorno é {@link Page}, o Spring Data executa também uma
   * consulta de contagem.
   * </p>
   *
   * <p>
   * <b>Carregamento:</b> devolve entidades {@link Product} <b>sem</b> as
   * categorias inicializadas (relacionamento tardio). Converter o resultado em
   * resposta que exponha as categorias faz o acesso a
   * {@code Product.getCategories()} disparar uma consulta adicional por produto
   * da página (padrão N+1), a menos que outra consulta já as tenha carregado.
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code ProductService.search}. Esse método do service não
   * é chamado por nenhum controller; a listagem HTTP de produtos usa
   * {@link #searchProducts(List, String, Pageable)}.
   * </p>
   *
   * @param name     termo procurado no nome do produto
   * @param pageable configurações de paginação (página, tamanho, ordenação)
   * @return página de produtos encontrados (vazia se nenhum corresponder)
   */
  Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

  /**
   * Busca paginada, por <b>SQL nativo</b>, de produtos filtrando por nome e,
   * opcionalmente, por categorias. Devolve apenas {@code id} e {@code name} de
   * cada produto (projeção {@link ProductProjection}); nenhuma entidade é
   * carregada.
   *
   * <p>
   * <b>Dependências do schema:</b> a consulta usa diretamente as tabelas
   * {@code tb_product} e {@code tb_product_category} e as colunas {@code id},
   * {@code name}, {@code product_id} e {@code category_id} (migrations V002 e
   * V003). Ela não passa pelos mappings JPA: renomear tabelas ou colunas não seria
   * detectado na compilação.
   * </p>
   *
   * <p>
   * <b>Regras da consulta:</b>
   * </p>
   * <ul>
   * <li>{@code INNER JOIN} com {@code tb_product_category}: só aparecem produtos
   * que possuem ao menos uma categoria</li>
   * <li>filtro de categorias
   * ({@code :categoryIds IS NULL OR category_id IN :categoryIds}): o produto é
   * retornado se pertencer a <b>qualquer uma</b> das categorias informadas (OU,
   * não E). O ramo {@code IS NULL} prevê a ausência de filtro</li>
   * <li>filtro de nome: {@code LIKE '%nome%'} sem diferenciar maiúsculas de
   * minúsculas, montado com {@code LOWER(CONCAT(...))}. Diferentemente das
   * consultas derivadas, o termo não é escapado: {@code %} e {@code _}
   * funcionam como curingas</li>
   * <li>{@code DISTINCT} sobre ({@code id}, {@code name}) evita repetir o
   * produto quando ele pertence a mais de uma das categorias filtradas</li>
   * </ul>
   *
   * <p>
   * <b>Paginação e ordenação:</b> a consulta interna é envolvida por
   * {@code SELECT * FROM ( ... ) AS tb_result}, e a paginação e a ordenação do
   * {@link Pageable} são aplicadas ao resultado externo, que expõe somente as
   * colunas {@code id} e {@code name}. Uma ordenação por outro campo (por
   * exemplo, {@code price}) não corresponde a nenhuma coluna desse resultado.
   * O atributo {@code countQuery} fornece o total da paginação e repete os mesmos
   * {@code JOIN} e filtros dentro de um {@code COUNT(*)}.
   * </p>
   *
   * <p>
   * <b>Parâmetros:</b> {@code :categoryIds} e {@code :name} são associados pelo
   * <em>nome</em> dos parâmetros do método (não há {@code @Param}), o que exige
   * que o código seja compilado com {@code -parameters}, como configurado no
   * {@code pom.xml}. O {@code ProductService} envia uma lista vazia (e não
   * {@code null}) quando nenhum filtro de categoria é informado; a expansão de
   * uma lista vazia no {@code IN} é feita pelo Hibernate e não é controlada por
   * esta consulta.
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code ProductService.findAllPaged}, que em seguida chama
   * {@link #searchProductsWithCategories(List)} com os ids da página.
   * </p>
   *
   * @param categoryIds ids das categorias usadas como filtro (ver observação
   *                    sobre lista vazia)
   * @param name        trecho do nome do produto
   * @param pageable    configurações de paginação e ordenação (ver observação
   *                    sobre as colunas disponíveis)
   * @return página de projeções com {@code id} e {@code name}
   */
  @Query(nativeQuery = true, value = """
      SELECT * FROM (
      SELECT DISTINCT tb_product.id, tb_product.name
      FROM tb_product
      INNER JOIN tb_product_category ON tb_product.id = tb_product_category.product_id
      WHERE (:categoryIds IS NULL OR tb_product_category.category_id IN :categoryIds)
      AND (LOWER(tb_product.name) LIKE LOWER(CONCAT('%',:name,'%')))
      ) AS tb_result
      """, countQuery = """
      SELECT COUNT(*) FROM (
      SELECT DISTINCT tb_product.id, tb_product.name
      FROM tb_product
      INNER JOIN tb_product_category ON tb_product.id = tb_product_category.product_id
      WHERE (:categoryIds IS NULL OR tb_product_category.category_id IN :categoryIds)
      AND (LOWER(tb_product.name) LIKE LOWER(CONCAT('%',:name,'%')))
      ) AS tb_result
      """)
  Page<ProductProjection> searchProducts(List<Long> categoryIds, String name, Pageable pageable);

  /**
   * Carrega, em uma única consulta <b>JPQL</b>, os produtos cujos ids foram
   * informados, já com a coleção {@code categories} inicializada.
   *
   * <p>
   * O {@code JOIN FETCH obj.categories} faz o Hibernate buscar produtos e
   * categorias na mesma instrução, de modo que acessar
   * {@code Product.getCategories()} depois não dispara uma consulta por produto
   * (evita N+1). É um join interno: um produto sem categorias não seria
   * retornado.
   * </p>
   *
   * <p>
   * <b>Ordem e paginação:</b> a consulta não possui {@code ORDER BY} nem
   * paginação; a ordem do resultado é indefinida e o volume depende apenas da
   * quantidade de ids recebidos. Quem chama deve reordenar (o
   * {@code ProductService} usa a ordem da consulta paginada de
   * {@link #searchProducts(List, String, Pageable)}).
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code ProductService.findAllPaged}, com os ids de uma
   * página.
   * </p>
   *
   * @param productsIds ids dos produtos a carregar
   * @return produtos encontrados, com categorias carregadas, em ordem não
   *         garantida
   */
  @Query("SELECT obj FROM Product obj JOIN FETCH obj.categories WHERE obj.id IN :productsIds")
  List<Product> searchProductsWithCategories(List<Long> productsIds);

  /**
   * Verifica se existe um produto com o nome informado (ignorando
   * maiúsculas/minúsculas).
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de dados antes de criar ou atualizar um produto</li>
   * <li>Evitar duplicidade de nomes no sistema</li>
   * </ul>
   *
   * <p>
   * Consulta derivada de existência ({@code existsBy}): não carrega a
   * entidade. A comparação ignora a caixa, enquanto a restrição {@code UNIQUE}
   * da coluna {@code name} (migration V002) não recebe tratamento especial de
   * caixa. Usada por {@code ProductCreateValidator}.
   * </p>
   *
   * @param name nome do produto a ser verificado
   * @return true se existir um produto com o nome informado, false caso contrário
   */
  boolean existsByNameIgnoreCase(String name);

  /**
   * Verifica se existe um produto com o nome informado (ignorando
   * maiúsculas/minúsculas) e com ID diferente do informado.
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de dados antes de atualizar um produto</li>
   * <li>Permitir que o produto atual mantenha seu nome, mas evitar que outro
   * produto tenha o mesmo nome</li>
   * </ul>
   *
   * <p>
   * Consulta derivada de existência ({@code existsBy}) com a condição
   * {@code AndIdNot}. Usada por {@code ProductUpdateValidator}, que obtém o
   * {@code id} da variável de caminho da requisição.
   * </p>
   *
   * @param name nome do produto a ser verificado
   * @param id   ID do produto a ser ignorado na verificação
   * @return true se existir um produto com o nome informado e ID diferente, false
   *         caso contrário
   */
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}