package com.albertsilva.dev.dscatalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.albertsilva.dev.dscatalog.domain.catalog.Category;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.mapper.category.CategoryMapper;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Serviço de aplicação das categorias ({@link Category}): listagem paginada,
 * consulta, criação, atualização, ativação/desativação e exclusão.
 *
 * <p>
 * <b>Dependências:</b> {@code CategoryRepository} e {@code CategoryMapper}. Este
 * service não chama validators: a unicidade do nome
 * ({@code @CategoryCreateValid} / {@code @CategoryUpdateValid}) e o formato dos
 * campos são validados antes, na camada web. O banco também impõe nome único.
 * </p>
 *
 * <p>
 * <b>Relação com produtos:</b> este service não acessa produtos.
 * {@code Category.products} é o lado inverso do relacionamento (o dono é
 * {@code Product}), e nenhuma regra deste service verifica se a categoria está
 * em uso.
 * </p>
 *
 * <p>
 * <b>Transações:</b> todos os métodos públicos são transacionais (leituras com
 * {@code readOnly = true}); os métodos privados executam na transação do método
 * público que os chamou. <b>Autorização:</b> não é feita aqui, e sim nos
 * controllers ({@code @PreAuthorize}) e na configuração de segurança.
 * <b>Exceção lançada:</b> {@link ResourceNotFoundException}
 * ({@code error.category.notFound}).
 * </p>
 *
 * <p>
 * O indicador {@code active} é apenas gravado por {@link #create},
 * {@link #activate(Long)} e {@link #deactivate(Long)}; nenhuma consulta ou
 * regra deste service o utiliza para filtrar ou bloquear categorias.
 * </p>
 */
@Service
public class CategoryService {

  private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  /**
   * Constrói o serviço de categorias com suas dependências principais.
   *
   * @param categoryRepository repositório de categorias
   * @param categoryMapper     responsável pela conversão entre DTOs e entidades
   */
  public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
    this.categoryRepository = categoryRepository;
    this.categoryMapper = categoryMapper;
  }

  /**
   * Lista categorias de forma paginada, com filtro opcional por nome.
   *
   * <p>
   * O termo recebe {@code trim}; nulo, vazio ou só com espaços significa "sem
   * filtro" e usa {@code findAll(pageable)}. Com filtro, usa
   * {@code findByNameContainingIgnoreCase}. A resposta ({@code id} e
   * {@code name}) não acessa relacionamentos, então a conversão não dispara
   * consultas adicionais.
   * </p>
   *
   * @param name     termo procurado no nome (opcional)
   * @param pageable página, tamanho e ordenação
   * @return página de categorias
   */
  @Transactional(readOnly = true)
  public Page<CategoryResponse> search(String name, Pageable pageable) {

    String filter = StringUtils.hasText(name) ? name.trim() : null;

    logger.debug("Buscando categorias | filtro={} | page={} | size={} | sort={}", filter, pageable.getPageNumber(),
        pageable.getPageSize(), pageable.getSort());

    Page<Category> categoriesPage = (filter != null)
        ? categoryRepository.findByNameContainingIgnoreCase(filter, pageable)
        : categoryRepository.findAll(pageable);

    logger.debug("Busca concluída | total={}", categoriesPage.getTotalElements());

    return categoryMapper.toResponsePage(categoriesPage);
  }

  /**
   * Retorna os detalhes de uma categoria (nome, descrição e indicador
   * {@code active}).
   *
   * @param id identificador da categoria
   * @return detalhes da categoria
   * @throws ResourceNotFoundException ({@code error.category.notFound}) se a
   *                                   categoria não existir
   */
  @Transactional(readOnly = true)
  public CategoryDetailsResponse findById(Long id) {
    return categoryMapper.toDetailsResponse(findEntityById(id));
  }

  /**
   * Cria uma categoria <b>ativa</b>.
   *
   * <p>
   * O mapper copia nome e descrição do DTO; a categoria é marcada como ativa
   * e salva. A unicidade do nome é validada antes, por
   * {@code @CategoryCreateValid}; uma violação da restrição única do banco
   * ocorreria na gravação e não é tratada aqui.
   * </p>
   *
   * @param categoryCreateRequest dados da nova categoria
   * @return categoria criada ({@code id} e {@code name})
   */
  @Transactional
  public CategoryResponse create(CategoryCreateRequest categoryCreateRequest) {
    logger.debug("Inserindo nova categoria - dados: {}", categoryCreateRequest);
    Category entity = categoryMapper.toEntity(categoryCreateRequest);
    entity.setActive(true);
    entity = categoryRepository.save(entity);
    logger.info("Categoria criada com sucesso. id: {}", entity.getId());
    return categoryMapper.toResponse(entity);
  }

  /**
   * Atualiza nome e descrição de uma categoria.
   *
   * <p>
   * Usa {@code getReferenceById} (referência preguiçosa): a existência só é
   * verificada quando o proxy é acessado (pelo mapper ou por {@code save}),
   * dentro do bloco {@code try}; se a categoria não existir, o
   * {@code EntityNotFoundException} da JPA é convertido em
   * {@link ResourceNotFoundException}. O {@code CategoryMapper.updateEntity}
   * só sobrescreve campos não nulos do DTO. A unicidade do nome (excluindo a
   * própria categoria) é validada antes, por {@code @CategoryUpdateValid}. O
   * indicador {@code active} não é alterado.
   * </p>
   *
   * @param id                    identificador da categoria
   * @param categoryUpdateRequest dados da atualização
   * @return categoria atualizada ({@code id} e {@code name})
   * @throws ResourceNotFoundException se a categoria não existir
   */
  @Transactional
  public CategoryResponse update(Long id, CategoryUpdateRequest categoryUpdateRequest) {
    logger.debug("Atualizando categoria. id: {}", id);

    try {
      Category entity = categoryRepository.getReferenceById(id);
      categoryMapper.updateEntity(categoryUpdateRequest, entity);
      entity = categoryRepository.save(entity);

      logger.info("Categoria atualizada com sucesso. id: {}", id);
      return categoryMapper.toResponse(entity);

    } catch (EntityNotFoundException e) {
      logger.warn("Falha ao atualizar categoria. Categoria não encontrada. id: {}", id);
      throw new ResourceNotFoundException("error.category.notFound");
    }
  }

  /**
   * Marca a categoria como ativa. Operação idempotente: se já estiver ativa,
   * nada é alterado. Não chama {@code save}: a alteração da entidade
   * gerenciada é gravada no commit.
   *
   * @param id identificador da categoria
   * @throws ResourceNotFoundException se a categoria não existir
   */
  @Transactional
  public void activate(Long id) {
    changeStatus(id, true);
  }

  /**
   * Marca a categoria como inativa. Operação idempotente: se já estiver
   * inativa, nada é alterado. Não chama {@code save}: a alteração da entidade
   * gerenciada é gravada no commit. O indicador não oculta a categoria de
   * {@code search} nem afeta os produtos associados.
   *
   * @param id identificador da categoria
   * @throws ResourceNotFoundException se a categoria não existir
   */
  @Transactional
  public void deactivate(Long id) {
    changeStatus(id, false);
  }

  /**
   * Remove fisicamente uma categoria.
   *
   * <p>
   * Carrega a categoria com {@code findById} (404 se não existir) e chama
   * {@code delete}. Não há verificação de produtos vinculados: como
   * {@code Category} é o lado inverso do relacionamento com {@code Product}, a
   * JPA não remove os vínculos de {@code tb_product_category}. Se houver
   * produtos associados, a violação da chave estrangeira tende a ser detectada
   * no commit, depois do retorno do método, e propagada como
   * {@code DataIntegrityViolationException} (não tratada aqui; o handler
   * global a converte em resposta HTTP 409).
   * </p>
   *
   * @param id identificador da categoria
   * @throws ResourceNotFoundException se a categoria não existir
   */
  @Transactional
  public void delete(Long id) {
    logger.debug("Deletando categoria. id: {}", id);

    Category entity = findEntityById(id);
    categoryRepository.delete(entity);
    logger.info("Categoria deletada com sucesso. id: {}", id);
  }

  /**
   * Busca uma entidade {@link Category} por ID.
   *
   * <p>
   * Realiza consulta imediata e lança exceção caso não encontre a entidade.
   * </p>
   *
   * @param id identificador da categoria
   * @return entidade encontrada
   * @throws ResourceNotFoundException caso a categoria não exista
   */
  private Category findEntityById(Long id) {
    logger.debug("Buscando categoria por id: {}", id);

    return categoryRepository.findById(id).orElseThrow(() -> {
      logger.warn("Categoria não encontrada. id: {}", id);
      return new ResourceNotFoundException("error.category.notFound");
    });
  }

  /**
   * Define o indicador {@code active} da categoria, se for diferente do atual.
   *
   * <p>
   * Carrega a categoria com {@code findById}; se o valor já for o desejado,
   * apenas retorna. Não chama {@code save}: a mudança é gravada no commit.
   * </p>
   *
   * @param id     identificador da categoria
   * @param active valor desejado ({@code true} = ativa)
   * @throws ResourceNotFoundException se a categoria não existir
   */
  private void changeStatus(Long id, boolean active) {
    Category entity = findEntityById(id);

    if (entity.isActive() == active) {
      logger.debug("Status já definido | id={} | active={}", id, active);
      return;
    }

    entity.setActive(active);

    logger.info("Status alterado | id={} | active={}", id, active);
  }

}