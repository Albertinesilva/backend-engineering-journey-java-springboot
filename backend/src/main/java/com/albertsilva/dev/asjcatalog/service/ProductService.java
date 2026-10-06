package com.albertsilva.dev.dscatalog.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.dscatalog.entity.Category;
import com.albertsilva.dev.dscatalog.entity.Product;
import com.albertsilva.dev.dscatalog.mapper.product.ProductMapper;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.albertsilva.dev.dscatalog.service.exception.DatabaseException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Serviço responsável pelas operações de negócio relacionadas à entidade
 * {@link Product}.
 *
 * <p>
 * Gerencia produtos e sua relação com categorias, centralizando
 * regras de negócio, validações e associações entre entidades.
 * </p>
 *
 * <p>
 * <b>Responsabilidades:</b>
 * </p>
 * <ul>
 * <li>Operações de CRUD de produtos</li>
 * <li>Gerenciamento de relacionamento entre produtos e categorias</li>
 * <li>Conversão entre entidades e DTOs</li>
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
 *          persistência com JPA e regras de negócio centralizadas.
 */
@Service
public class ProductService {

  private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final ProductMapper productMapper;

  /**
   * Constrói o serviço de produtos com suas dependências principais.
   *
   * @param productRepository  repositório de produtos
   * @param categoryRepository repositório de categorias
   * @param productMapper      responsável pela conversão entre DTOs e entidades
   */
  public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
      ProductMapper productMapper) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.productMapper = productMapper;
  }

  /**
   * Busca produtos com suporte a filtragem por nome e paginação.
   *
   * <p>
   * Permite buscar produtos cujo nome contenha o termo fornecido,
   * ignorando diferenças de maiúsculas/minúsculas.
   * </p>
   *
   * <p>
   * Se o parâmetro {@code name} for nulo ou vazio, retorna todos os produtos
   * paginados.
   * </p>
   *
   * @param name     termo de busca para o nome do produto (opcional)
   * @param pageable informações de paginação e ordenação
   * @return página de produtos que correspondem ao critério de busca
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
  public Page<ProductResponse> search(String name, Pageable pageable) {

    String filter = StringUtils.hasText(name) ? name.trim() : null;

    logger.debug("Buscando produtos | filtro={} | page={} | size={} | sort={}", filter, pageable.getPageNumber(),
        pageable.getPageSize(), pageable.getSort());

    Page<Product> productsPage = (filter != null) ? productRepository.findByNameContainingIgnoreCase(filter, pageable)
        : productRepository.findAll(pageable);

    logger.debug("Busca concluída | total={}", productsPage.getTotalElements());

    return productMapper.toResponsePage(productsPage);
  }

  /**
   * Busca um produto pelo seu identificador.
   *
   * <p>
   * Retorna os detalhes completos do produto,
   * incluindo categorias associadas.
   * </p>
   *
   * @param id identificador do produto
   * @return detalhes completos do produto
   * @throws ResourceNotFoundException caso o produto não exista
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
  public ProductDetailsResponse findById(Long id) {
    return productMapper.toDetailsResponse(findEntityById(id));
  }

  /**
   * Insere um novo produto no sistema.
   *
   * <p>
   * Além dos dados básicos, realiza o vínculo
   * com categorias utilizando seus respectivos IDs.
   * </p>
   *
   * <p>
   * O frontend envia apenas IDs das categorias,
   * enquanto o backend resolve o relacionamento completo.
   * </p>
   *
   * @param productCreateRequest dados para criação do produto
   * @return produto criado
   *
   * @implNote
   *           Utiliza conversão DTO → Entity e mapeamento controlado
   *           de categorias para garantir integridade relacional.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          DTO Pattern, relacionamento ManyToMany e persistência.
   */
  @Transactional
  public ProductResponse create(ProductCreateRequest productCreateRequest) {
    logger.debug("Inserindo novo produto - dados: {}", productCreateRequest);
    Product entity = productMapper.toEntity(productCreateRequest);
    entity.setActive(true);
    syncCategories(entity, productCreateRequest.categoryIds());
    entity = productRepository.save(entity);
    logger.info("Produto criado com sucesso. id: {}", entity.getId());
    return productMapper.toResponse(entity);
  }

  /**
   * Atualiza parcialmente um produto existente.
   *
   * <p>
   * Permite modificar atributos básicos e,
   * opcionalmente, substituir as categorias associadas.
   * </p>
   *
   * <p>
   * Quando {@code categoryIds} é informado,
   * as categorias atuais são removidas e substituídas
   * pelas novas categorias fornecidas.
   * </p>
   *
   * @param id  identificador do produto
   * @param dto dados para atualização parcial
   * @return produto atualizado
   * @throws ResourceNotFoundException caso o produto não exista
   *
   * @implNote
   *           Utiliza {@code getReferenceById(id)} para obter uma referência lazy
   *           (proxy) da entidade, evitando consulta imediata ao banco.
   *
   *           <p>
   *           O proxy é carregado apenas quando atributos da entidade
   *           são acessados, reduzindo consultas desnecessárias.
   *           </p>
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          JPA Proxy, Lazy Loading, Performance e Contexto de Persistência.
   */
  @Transactional
  public ProductResponse update(Long id, ProductUpdateRequest dto) {
    logger.debug("Atualizando produto. id: {}", id);

    try {
      Product entity = productRepository.getReferenceById(id);
      productMapper.updateEntity(dto, entity);

      if (dto.categoryIds() != null) {
        syncCategories(entity, dto.categoryIds());
        logger.debug("Categorias do produto atualizadas. id: {}", id);
      }

      entity = productRepository.save(entity);
      logger.info("Serviço Produto atualizado com sucesso. id: {}", id);
      return productMapper.toResponse(entity);

    } catch (EntityNotFoundException e) {
      logger.warn("Falha ao atualizar produto. Produto não encontrado. id: {}", id);
      throw new ResourceNotFoundException("Entity not found id: " + id);
    }
  }

  /**
   * Ativa um produto existente.
   *
   * <p>
   * Altera o status do produto para ativo,
   * permitindo que ele seja exibido e comercializado.
   * </p>
   *
   * @param id identificador do produto
   * @throws ResourceNotFoundException caso o produto não exista
   *
   * @implNote
   *           Realiza atualização parcial do status do produto,
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

  /**
   * Desativa um produto existente.
   *
   * <p>
   * Altera o status do produto para inativo,
   * ocultando-o de listagens e impedindo comercialização.
   * </p>
   *
   * @param id identificador do produto
   * @throws ResourceNotFoundException caso o produto não exista
   *
   * @implNote
   *           Realiza atualização parcial do status do produto,
   *           mantendo as demais informações inalteradas.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          atualização parcial, status de entidade e regras de negócio.
   */
  @Transactional
  public void deactivate(Long id) {
    changeStatus(id, false);
  }

  /**
   * Remove um produto existente do sistema.
   *
   * <p>
   * Valida previamente a existência da entidade
   * antes da exclusão.
   * </p>
   *
   * @param id identificador do produto
   * @throws ResourceNotFoundException caso o produto não exista
   * @throws DatabaseException         em caso de violação de integridade
   *
   * @implNote
   *           Garante segurança ao validar existência antes do delete
   *           e trata exceções de integridade referencial.
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          exclusão segura, integridade de dados e tratamento de exceções.
   */
  @Transactional
  public void delete(Long id) {
    logger.debug("Deletando produto. id: {}", id);

    Product entity = findEntityById(id);

    try {
      productRepository.delete(entity);
      logger.info("Produto deletado com sucesso. id: {}", id);

    } catch (DataIntegrityViolationException e) {
      logger.error("Erro de integridade ao deletar produto. id: {}", id);
      throw new DatabaseException("Integrity violation: cannot delete product with related entities");
    }
  }

  /**
   * Busca uma entidade {@link Product} por ID.
   *
   * <p>
   * Realiza consulta imediata e lança exceção caso não encontre a entidade.
   * </p>
   *
   * @param id identificador do produto
   * @return entidade encontrada
   * @throws ResourceNotFoundException caso o produto não exista
   */
  private Product findEntityById(Long id) {
    logger.debug("Buscando produto por id: {}", id);

    return productRepository.findById(id).orElseThrow(() -> {
      logger.warn("Produto não encontrado. id: {}", id);
      return new ResourceNotFoundException("Entity not found id: " + id);
    });
  }

  /**
   * Realiza o mapeamento entre produto e categorias.
   *
   * <p>
   * Remove categorias antigas e substitui
   * pelas categorias informadas.
   * </p>
   *
   * <p>
   * Valida se todos os IDs recebidos existem
   * antes de concluir a associação.
   * </p>
   *
   * @param entity      produto a ser associado
   * @param categoryIds lista de IDs de categorias
   * @throws ResourceNotFoundException caso alguma categoria não exista
   *
   * @implNote
   *           Utiliza {@code findAllById} para buscar todas as categorias
   *           em lote, evitando múltiplas consultas (N+1 problem).
   *
   *           <p>
   *           Essa abordagem melhora performance
   *           e garante consistência relacional.
   *           </p>
   *
   * @apiNote
   *          Esta implementação reforça conceitos importantes como:
   *          mapeamento de relacionamentos em JPA, performance JPA, N+1 queries e
   *          relacionamentos eficientes.
   */
  private void syncCategories(Product entity, List<Long> categoryIds) {
    entity.getCategories().clear();

    if (categoryIds == null || categoryIds.isEmpty()) {
      logger.debug("Nenhuma categoria fornecida para mapear ao produto. id: {}", entity.getId());
      return;
    }

    List<Category> categories = categoryRepository.findAllById(categoryIds);

    if (categories.size() != categoryIds.size()) {
      logger.warn("Uma ou mais categorias não foram encontradas. produtoId: {}", entity.getId());
      throw new ResourceNotFoundException("One or more categories not found");
    }

    entity.getCategories().addAll(categories);

    logger.debug("Categorias mapeadas ao produto. produtoId: {}, total: {}", entity.getId(), categories.size());
  }

  /**
   * Altera o status de um produto para ativo ou inativo.
   *
   * <p>
   * Realiza a mudança de status do produto, ativando ou desativando-o
   * conforme o parâmetro fornecido.
   * </p>
   *
   * @param id     identificador do produto
   * @param active novo status do produto (true para ativo, false para inativo)
   * @throws ResourceNotFoundException caso o produto não exista
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
    Product entity = findEntityById(id);

    if (entity.isActive() == active) {
      logger.debug("Status já definido | id={} | active={}", id, active);
      return;
    }

    entity.setActive(active);

    logger.info("Status alterado | id={} | active={}", id, active);
  }

}