package com.albertsilva.dev.dscatalog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.entity.Category;
import com.albertsilva.dev.dscatalog.mapper.category.CategoryMapper;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Serviço responsável pelas operações de negócio relacionadas à entidade
 * {@link Category}.
 *
 * <p>
 * Gerencia categorias, centralizando regras de negócio,
 * validações, persistência e tratamento transacional.
 * </p>
 *
 * <p>
 * <b>Responsabilidades:</b>
 * </p>
 * <ul>
 * <li>Operações de CRUD de categorias</li>
 * <li>Paginação e filtros de busca</li>
 * <li>Conversão entre entidades e DTOs</li>
 * <li>Tratamento de exceções de negócio</li>
 * <li>Garantia de integridade e consistência dos dados</li>
 * </ul>
 *
 * @implNote
 *           Atua como camada de serviço (Service Layer), intermediando
 *           Controller, Repository e Mapper dentro da arquitetura Spring Boot.
 *
 * @apiNote
 *          Esta implementação exemplifica conceitos fundamentais de aplicações
 *          corporativas,
 *          como Service Layer, arquitetura em camadas, DTO Pattern,
 *          persistência com JPA, paginação e regras de negócio centralizadas.
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
   * Busca categorias com suporte a filtragem por nome e paginação.
   *
   * <p>
   * Permite buscar categorias cujo nome contenha o termo fornecido,
   * ignorando diferenças de maiúsculas/minúsculas.
   * </p>
   *
   * <p>
   * Se o parâmetro {@code name} for nulo ou vazio, retorna todas as categorias
   * paginadas.
   * </p>
   *
   * @param name     termo de busca para o nome da categoria (opcional)
   * @param pageable informações de paginação e ordenação
   * @return página de categorias que correspondem ao critério de busca
   *
   * @implNote
   *           Utiliza métodos específicos do repositório para otimizar a busca
   *           com filtro, evitando carregamento desnecessário de dados.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          filtragem eficiente, paginação, uso de Optional e boas práticas de
   *          consulta em Spring Data JPA.
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
   * Busca uma categoria pelo seu identificador.
   *
   * <p>
   * Retorna os dados completos da categoria,
   * garantindo validação segura da existência do registro.
   * </p>
   *
   * @param id identificador da categoria
   * @return dados da categoria
   * @throws ResourceNotFoundException caso a categoria não exista
   *
   * @implNote
   *           Utiliza {@code findById(id)}, realizando consulta imediata no
   *           banco.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          Optional, tratamento de exceções e busca segura de entidades.
   */
  @Transactional(readOnly = true)
  public CategoryResponse findById(Long id) {
    return categoryMapper.toResponse(findEntityById(id));
  }

  /**
   * Insere uma nova categoria no sistema.
   *
   * <p>
   * Converte o DTO de entrada em entidade
   * e persiste os dados no banco.
   * </p>
   *
   * @param categoryCreateRequest dados para criação da categoria
   * @return categoria criada
   *
   * @implNote
   *           Utiliza conversão DTO → Entity,
   *           garantindo separação entre camada de apresentação e persistência.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          DTO Pattern, persistência e criação de entidades em APIs RESTful.
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
   * Atualiza parcialmente uma categoria existente.
   *
   * <p>
   * Permite modificar apenas campos informados,
   * preservando dados não enviados.
   * </p>
   *
   * @param id                    identificador da categoria
   * @param categoryUpdateRequest dados para atualização parcial
   * @return categoria atualizada
   * @throws ResourceNotFoundException caso a categoria não exista
   *
   * @implNote
   *           Utiliza {@code getReferenceById(id)} para obter uma referência lazy
   *           (proxy) da entidade, evitando consulta imediata ao banco.
   *
   *           <p>
   *           O proxy será inicializado somente quando atributos forem acessados.
   *           </p>
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          JPA Proxy, Lazy Loading, atualização parcial e Contexto de
   *          Persistência.
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
      throw new ResourceNotFoundException("Entity not found id: " + id);
    }
  }

  /**
   * Ativa uma categoria existente.
   *
   * <p>
   * Altera o status da categoria para ativo,
   * permitindo que ela seja exibida e utilizada.
   * </p>
   *
   * @param id identificador da categoria
   * @throws ResourceNotFoundException caso a categoria não exista
   *
   * @implNote
   *           Realiza atualização parcial do status da categoria,
   *           mantendo as demais informações inalteradas.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          atualização parcial, status de entidade e regras de negócio.
   */
  @Transactional
  public void activate(Long id) {
    changeStatus(id, true);
  }

  /*
   * Desativa uma categoria existente.
   *
   * <p>
   * Altera o status da categoria para inativo,
   * ocultando-a de listagens e impedindo sua utilização.
   * </p>
   *
   * @param id identificador da categoria
   * 
   * @throws ResourceNotFoundException caso a categoria não exista
   *
   * @implNote
   * Realiza atualização parcial do status da categoria,
   * mantendo as demais informações inalteradas.
   *
   * @apiNote
   * Esta implementação reforça conceitos importantes como:
   * atualização parcial, status de entidade e regras de negócio.
   */
  @Transactional
  public void deactivate(Long id) {
    changeStatus(id, false);
  }

  /**
   * Remove uma categoria existente do sistema.
   *
   * <p>
   * Valida previamente a existência da entidade
   * antes da exclusão.
   * </p>
   *
   * <p>
   * Possíveis cenários de erro:
   * </p>
   * <ul>
   * <li>Categoria não encontrada →
   * {@link ResourceNotFoundException}</li>
   * <li>Violação de integridade referencial →
   * tratada globalmente via {@code @RestControllerAdvice}</li>
   * </ul>
   *
   * @param id identificador da categoria
   * @throws ResourceNotFoundException caso a categoria não exista
   *
   * @implNote
   *           Utiliza {@code findById(id)} para validar existência
   *           e carregar a entidade em uma única consulta,
   *           evitando redundância de operações como {@code existsById(id)}.
   *
   *           <p>
   *           Não utiliza {@code flush()} manual,
   *           permitindo sincronização natural com o banco
   *           durante o commit da transação.
   *           </p>
   *
   *           <p>
   *           Não utiliza {@code Propagation.SUPPORTS},
   *           pois operações de escrita devem ocorrer
   *           dentro de transação ativa para garantir
   *           consistência e integridade dos dados.
   *           </p>
   *
   *           <p>
   *           O tratamento de exceções como
   *           {@code DataIntegrityViolationException}
   *           permanece centralizado globalmente,
   *           garantindo padronização e confiabilidade
   *           nas respostas da API.
   *           </p>
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          exclusão segura, integridade de dados,
   *          controle transacional, otimização de consultas
   *          e tratamento centralizado de exceções.
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
      return new ResourceNotFoundException("Entity not found id: " + id);
    });
  }

  /**
   * Altera o status de uma categoria para ativo ou inativo.
   *
   * <p>
   * Realiza a mudança de status da categoria, ativando ou desativando-a
   * conforme o parâmetro fornecido.
   * </p>
   *
   * @param id     identificador da categoria
   * @param active novo status da categoria (true para ativo, false para inativo)
   * @throws ResourceNotFoundException caso a categoria não exista
   *
   * @implNote
   *           Centraliza a lógica de alteração de status em um método privado,
   *           evitando duplicação de código entre os métodos de ativação e
   *           desativação.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          centralização de lógica, DRY Principle e manutenção facilitada.
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