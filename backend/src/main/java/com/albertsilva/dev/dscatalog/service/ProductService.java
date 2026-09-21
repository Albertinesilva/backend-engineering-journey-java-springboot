package com.albertsilva.dev.dscatalog.service;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.albertsilva.dev.dscatalog.domain.catalog.Category;
import com.albertsilva.dev.dscatalog.domain.catalog.Product;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.dscatalog.mapper.product.ProductMapper;
import com.albertsilva.dev.dscatalog.projection.ProductProjection;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.albertsilva.dev.dscatalog.service.exception.DatabaseException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.dscatalog.util.IdentifiableUtils;

import jakarta.persistence.EntityNotFoundException;

/**
 * Serviço de aplicação dos produtos ({@link Product}): listagens paginadas,
 * consulta, criação, atualização, ativação/desativação e exclusão, incluindo o
 * vínculo do produto com categorias.
 *
 * <p>
 * <b>Dependências:</b> {@code ProductRepository}, {@code CategoryRepository}
 * (para resolver as categorias de um produto), {@code ProductMapper}
 * (conversão entre DTOs, entidade e respostas) e {@code IdentifiableUtils}.
 * Este service não chama validators: a validação dos DTOs de entrada
 * ({@code @Valid}), que inclui a unicidade do nome e a existência das
 * categorias, ocorre antes, na camada web.
 * </p>
 *
 * <p>
 * <b>Transações:</b> todos os métodos públicos são transacionais (leituras
 * com {@code readOnly = true}, escritas com transação de escrita). Os métodos
 * privados não têm anotação e executam dentro da transação do método público
 * que os chamou. Erros não tratados ({@code RuntimeException}) revertem a
 * transação.
 * </p>
 *
 * <p>
 * <b>Autorização:</b> não é feita aqui. As regras de acesso ficam nos
 * controllers ({@code @PreAuthorize}) e na configuração de segurança.
 * </p>
 *
 * <p>
 * <b>Exceções:</b> {@link ResourceNotFoundException} (produto ou categoria
 * inexistente) e {@link DatabaseException} (exclusão). A conversão em resposta
 * HTTP é feita por {@code ControllerExceptionHandler}.
 * </p>
 *
 * <p>
 * <b>Comportamento atual do indicador {@code active}:</b> é apenas gravado por
 * {@link #create}, {@link #activate(Long)} e {@link #deactivate(Long)};
 * nenhuma consulta ou regra deste service o utiliza para filtrar ou bloquear
 * produtos.
 * </p>
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
   * Lista produtos por consulta derivada do Spring Data, com filtro opcional
   * por nome.
   *
   * <p>
   * O termo recebe {@code trim}; nulo, vazio ou só com espaços significa "sem
   * filtro" e usa {@code findAll(pageable)}. Com filtro, usa
   * {@code findByNameContainingIgnoreCase}. Diferentemente de
   * {@link #findAllPaged(String, String, Pageable)}, não filtra por categoria,
   * inclui produtos sem categoria e aceita ordenação por qualquer propriedade
   * da entidade.
   * </p>
   *
   * <p>
   * A conversão para resposta acessa as categorias de cada produto, que são
   * carregadas sob demanda (padrão N+1 provável, dentro da transação de
   * leitura).
   * </p>
   *
   * <p>
   * <b>Uso atual:</b> nenhum controller nem outro componente de
   * {@code src/main} chama este método (apenas testes); a listagem HTTP de
   * produtos usa {@code findAllPaged}.
   * </p>
   *
   * @param name     termo procurado no nome (opcional)
   * @param pageable página, tamanho e ordenação
   * @return página de produtos convertidos para {@link ProductResponse}
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
   * Lista produtos com filtro por nome e por categorias e com paginação; é a
   * consulta usada pela listagem HTTP de produtos.
   *
   * <p>
   * <b>Sequência:</b>
   * </p>
   * <ol>
   * <li>converte {@code categoryId} em lista de ids: {@code "0"} resulta em
   * lista vazia (é o valor padrão do controller); qualquer outro valor é
   * dividido por vírgula e cada parte é convertida com
   * {@code Long.parseLong}</li>
   * <li>{@code searchProducts} (SQL nativo) devolve uma página de
   * {@link ProductProjection} (id e nome) já filtrada, ordenada e paginada</li>
   * <li>coleta os ids da página e chama {@code searchProductsWithCategories}
   * (JPQL com {@code JOIN FETCH}), que carrega os produtos com as
   * categorias</li>
   * <li>{@code IdentifiableUtils.reorderByReference} recoloca os produtos na
   * ordem da página; ids sem produto correspondente são descartados</li>
   * <li>converte cada produto com {@code productMapper.toResponse} e monta um
   * {@code PageImpl} com o total informado pela consulta nativa</li>
   * </ol>
   *
   * <p>
   * <b>Comportamento observado no código:</b>
   * </p>
   * <ul>
   * <li>{@code name} é repassado como recebido (sem {@code trim} nem
   * tratamento de nulo/vazio); o controller usa {@code ""} por padrão</li>
   * <li>não há filtro por {@code active}: produtos inativos são listados</li>
   * <li>só aparecem produtos com ao menos uma categoria (a consulta nativa usa
   * {@code INNER JOIN})</li>
   * <li>{@code searchProductsWithCategories} é chamado mesmo quando a página
   * não tem ids (lista vazia); não há tratamento específico desse caso</li>
   * <li>{@code categoryId} nulo causa {@link NullPointerException} e valor não
   * numérico causa {@link NumberFormatException}; nenhuma das duas é tratada
   * aqui</li>
   * <li>a ordenação do {@code Pageable} é aplicada pela consulta nativa, sobre
   * as colunas {@code id} e {@code name}</li>
   * </ul>
   *
   * @param name       trecho do nome do produto
   * @param categoryId ids de categorias separados por vírgula, ou {@code "0"}
   *                   para não filtrar por categoria
   * @param pageable   página, tamanho e ordenação
   * @return página de {@link ProductResponse}, com as categorias de cada
   *         produto
   */
  @Transactional(readOnly = true)
  public Page<ProductResponse> findAllPaged(String name, String categoryId, Pageable pageable) {

    List<Long> categoryIds = Arrays.asList();
    if (!"0".equals(categoryId)) {
      categoryIds = Arrays.asList(categoryId.split(",")).stream().map(Long::parseLong).toList();
    }

    Page<ProductProjection> page = productRepository.searchProducts(categoryIds, name, pageable);
    List<Long> productsIds = page.map(ProductProjection::getId).toList();

    List<Product> products = productRepository.searchProductsWithCategories(productsIds);
    products = IdentifiableUtils.reorderByReference(products, page.getContent());

    List<ProductResponse> responses = products.stream().map(productMapper::toResponse).toList();

    return new PageImpl<>(responses, pageable, page.getTotalElements());
  }

  /**
   * Retorna os detalhes de um produto, incluindo suas categorias.
   *
   * <p>
   * Carrega o produto com {@code findById} e converte com
   * {@code toDetailsResponse}; as categorias são carregadas sob demanda durante
   * a conversão, dentro da transação de leitura.
   * </p>
   *
   * @param id identificador do produto
   * @return detalhes do produto
   * @throws ResourceNotFoundException ({@code error.product.notFound}) se o
   *                                   produto não existir
   */
  @Transactional(readOnly = true)
  public ProductDetailsResponse findById(Long id) {
    return productMapper.toDetailsResponse(findEntityById(id));
  }

  /**
   * Cria um produto ativo e o vincula às categorias informadas.
   *
   * <p>
   * Sequência: o mapper converte o DTO (copia nome, descrição, preço e URL da
   * imagem); o produto é marcado como ativo; as categorias são resolvidas por
   * {@code syncCategories}; o produto é salvo. As datas são preenchidas pelos
   * callbacks JPA. O campo {@code date} do request não é usado.
   * </p>
   *
   * <p>
   * <b>Regras fora deste método:</b> a unicidade do nome e a existência das
   * categorias são validadas antes, pelo validator {@code ProductCreateValid}.
   * O banco também impõe nome único; uma violação ocorreria na gravação e
   * não é tratada aqui.
   * </p>
   *
   * @param productCreateRequest dados para criação do produto
   * @return produto criado, com suas categorias
   * @throws ResourceNotFoundException ({@code error.product.categories.notFound})
   *                                   se alguma categoria não existir
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
   * Atualiza um produto existente e, se informado, substitui suas categorias.
   *
   * <p>
   * Usa {@code getReferenceById}, que devolve uma referência preguiçosa: a
   * existência do produto só é verificada quando o proxy é acessado (pelo
   * mapper, por {@code syncCategories} ou por {@code save}), sempre dentro do
   * bloco {@code try}. Se o produto não existir, o
   * {@code EntityNotFoundException} da JPA é convertido em
   * {@link ResourceNotFoundException}.
   * </p>
   *
   * <ul>
   * <li>campos nulos do DTO não sobrescrevem os valores atuais (regra do
   * {@code ProductMapper.updateEntity})</li>
   * <li>{@code categoryIds} diferente de nulo: as categorias são substituídas
   * ({@code syncCategories}); uma lista vazia remove todas (o DTO exige lista
   * não vazia com {@code @NotEmpty}, mas este service não)</li>
   * <li>{@code categoryIds} nulo: as categorias atuais são mantidas</li>
   * </ul>
   *
   * <p>
   * A unicidade do nome é validada antes, por {@code ProductUpdateValid}. O
   * {@code save} é chamado mesmo sobre a entidade já gerenciada.
   * </p>
   *
   * @param id  identificador do produto
   * @param dto dados para atualização
   * @return produto atualizado
   * @throws ResourceNotFoundException se o produto ({@code error.product.notFound})
   *                                   ou alguma categoria
   *                                   ({@code error.product.categories.notFound})
   *                                   não existir
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
      throw new ResourceNotFoundException("error.product.notFound");
    }
  }

  /**
   * Marca o produto como ativo. Operação idempotente: se já estiver ativo,
   * nada é alterado.
   *
   * <p>
   * Não chama {@code save}: a alteração da entidade gerenciada é gravada no
   * commit da transação. O indicador é apenas armazenado; nenhuma listagem ou
   * regra deste service o considera.
   * </p>
   *
   * @param id identificador do produto
   * @throws ResourceNotFoundException se o produto não existir
   */
  @Transactional
  public void activate(Long id) {
    changeStatus(id, true);
  }

  /**
   * Marca o produto como inativo. Operação idempotente: se já estiver inativo,
   * nada é alterado.
   *
   * <p>
   * Não chama {@code save}: a alteração da entidade gerenciada é gravada no
   * commit da transação. O indicador é apenas armazenado; nenhuma listagem ou
   * regra deste service o considera (produtos inativos continuam aparecendo em
   * {@code findAllPaged}).
   * </p>
   *
   * @param id identificador do produto
   * @throws ResourceNotFoundException se o produto não existir
   */
  @Transactional
  public void deactivate(Long id) {
    changeStatus(id, false);
  }

  /**
   * Remove fisicamente um produto.
   *
   * <p>
   * Carrega o produto com {@code findById} (404 se não existir), remove-o com
   * {@code delete} e converte {@code DataIntegrityViolationException} em
   * {@link DatabaseException}. As linhas de {@code tb_product_category} desse
   * produto são removidas pela própria JPA, pois {@code Product} é o lado dono
   * do relacionamento.
   * </p>
   *
   * <p>
   * Atenção: o {@code try/catch} envolve apenas a chamada a {@code delete}.
   * Como a remoção costuma ser sincronizada com o banco no commit, depois do
   * retorno do método, uma violação de integridade nesse momento pode não
   * passar por este {@code catch}.
   * </p>
   *
   * @param id identificador do produto
   * @throws ResourceNotFoundException se o produto não existir
   * @throws DatabaseException         se a exclusão violar a integridade dos
   *                                   dados (quando detectada dentro do
   *                                   método)
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
      throw new DatabaseException("error.database.product.relatedEntities");
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
      return new ResourceNotFoundException("error.product.notFound");
    });
  }

  /**
   * Substitui as categorias do produto pelas informadas.
   *
   * <ol>
   * <li>limpa o conjunto atual de categorias ({@code clear()}), antes de
   * qualquer validação</li>
   * <li>se a lista for nula ou vazia, termina: o produto fica sem
   * categorias</li>
   * <li>busca todas as categorias com {@code findAllById} (uma consulta)</li>
   * <li>se a quantidade encontrada for diferente da quantidade de ids
   * recebidos, lança {@link ResourceNotFoundException}</li>
   * </ol>
   *
   * <p>
   * A verificação é por quantidade: {@code findAllById} tende a devolver cada
   * categoria uma única vez, então ids repetidos na lista resultariam em
   * exceção mesmo com todas as categorias existentes. Quando a exceção é
   * lançada, a transação é revertida, e o {@code clear()} não é persistido.
   * </p>
   *
   * @param entity      produto que receberá as categorias
   * @param categoryIds ids das categorias
   * @throws ResourceNotFoundException se alguma categoria não existir
   *                                   ({@code error.product.categories.notFound})
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
      throw new ResourceNotFoundException("error.product.categories.notFound");
    }

    entity.getCategories().addAll(categories);

    logger.debug("Categorias mapeadas ao produto. produtoId: {}, total: {}", entity.getId(), categories.size());
  }

  /**
   * Define o indicador {@code active} do produto, se for diferente do atual.
   *
   * <p>
   * Carrega o produto com {@code findById}; se o valor já for o desejado,
   * apenas registra em log e retorna. Não chama {@code save}: a mudança é
   * gravada no commit.
   * </p>
   *
   * @param id     identificador do produto
   * @param active valor desejado ({@code true} = ativo)
   * @throws ResourceNotFoundException se o produto não existir
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