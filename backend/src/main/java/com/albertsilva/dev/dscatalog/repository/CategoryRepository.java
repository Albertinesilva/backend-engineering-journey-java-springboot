package com.albertsilva.dev.dscatalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.dscatalog.domain.catalog.Category;

/**
 * Repositório Spring Data JPA da entidade {@link Category} (tabela
 * {@code tb_category}), com chave primária {@code Long}.
 *
 * <p>
 * Estende {@link JpaRepository} e, portanto, herda as operações padrão de
 * persistência ({@code save}, {@code findById}, {@code findAll},
 * {@code findAllById}, {@code getReferenceById}, {@code existsById},
 * {@code delete}, entre outras). Não há {@code @Query} nesta interface: as
 * consultas próprias são <b>consultas derivadas</b>, cujo SQL o Spring Data
 * gera a partir do nome do método.
 * </p>
 *
 * <p>
 * <b>Consultas próprias:</b>
 * </p>
 * <ul>
 * <li>{@link #findByNameContainingIgnoreCase(String, Pageable)}: listagem
 * paginada filtrada por nome</li>
 * <li>{@link #existsByNameIgnoreCase(String)} e
 * {@link #existsByNameIgnoreCaseAndIdNot(String, Long)}: verificações de
 * unicidade de nome</li>
 * </ul>
 *
 * <p>
 * <b>Uso atual:</b> {@code CategoryService} (CRUD e listagem);
 * {@code ProductService} (usa {@code findAllById} para resolver as categorias
 * de um produto); {@code CategoryCreateValidator} e
 * {@code CategoryUpdateValidator} (unicidade de nome);
 * {@code ProductCreateValidator} e {@code ProductUpdateValidator} (usam
 * {@code existsById}).
 * </p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  /**
   * Lista, de forma paginada, as categorias cujo nome contém o termo informado,
   * sem diferenciar maiúsculas de minúsculas.
   *
   * <p>
   * <b>Consulta derivada:</b> o SQL é gerado pelo Spring Data a partir do nome
   * do método ({@code findBy} + {@code Name} + {@code Containing} +
   * {@code IgnoreCase}), resultando em um {@code LIKE '%termo%'} aplicado ao
   * campo {@code name} sem distinção de caixa. Não há {@code @Query}.
   * </p>
   *
   * <p>
   * <b>Paginação e ordenação:</b> página, tamanho e ordenação vêm do
   * {@link Pageable}; a ordenação usa nomes de propriedades da entidade (por
   * exemplo, {@code name}). Sem ordenação informada, a consulta não define a
   * ordem dos resultados. Como o retorno é {@link Page}, o Spring Data executa
   * também uma consulta de contagem para calcular o total de elementos.
   * </p>
   *
   * <p>
   * <b>Carregamento:</b> devolve entidades {@link Category}; a coleção
   * {@code products} não é carregada (relacionamento tardio).
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> {@code CategoryService.search}, somente quando há termo
   * de filtro; sem filtro, o service usa {@code findAll(Pageable)}.
   * </p>
   *
   * @param name     termo procurado no nome da categoria
   * @param pageable configurações de paginação (página, tamanho, ordenação)
   * @return página de categorias encontradas (vazia se nenhuma corresponder)
   */
  Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

  /**
   * Verifica se existe uma categoria com o nome informado (ignorando
   * maiúsculas/minúsculas).
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de unicidade antes de criar ou atualizar uma categoria</li>
   * </ul>
   *
   * <p>
   * Consulta derivada de existência ({@code existsBy}): não carrega a
   * entidade. A comparação ignora a caixa, enquanto a restrição {@code UNIQUE}
   * da coluna {@code name} (migration V001) não recebe tratamento especial de
   * caixa. Usada por {@code CategoryCreateValidator}.
   * </p>
   *
   * @param name nome da categoria a ser verificada
   * @return {@code true} se existir uma categoria com o nome, {@code false} caso
   *         contrário
   */
  boolean existsByNameIgnoreCase(String name);

  /**
   * Verifica se existe uma categoria com o nome informado (ignorando
   * maiúsculas/minúsculas) e ID diferente do fornecido.
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de unicidade antes de atualizar uma categoria, ignorando a
   * própria categoria sendo atualizada</li>
   * </ul>
   *
   * <p>
   * Consulta derivada de existência ({@code existsBy}) com a condição
   * {@code AndIdNot}: o registro de identificador {@code id} é desconsiderado.
   * Usada por {@code CategoryUpdateValidator}, que obtém o {@code id} da
   * variável de caminho da requisição.
   * </p>
   *
   * @param name nome da categoria a ser verificada
   * @param id   ID da categoria que está sendo atualizada (a ser ignorada na
   *             verificação)
   * @return {@code true} se existir outra categoria com o mesmo nome,
   *         {@code false}
   *         caso contrário
   */
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

}
