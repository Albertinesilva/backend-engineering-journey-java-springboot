package com.albertsilva.dev.dscatalog.web.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.product.response.ProductResponse;
import com.albertsilva.dev.dscatalog.service.ProductService;
import com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller responsável por expor os endpoints REST da entidade Product.
 *
 * <p>
 * Essa classe recebe requisições HTTP relacionadas a produtos e delega
 * o processamento para {@link ProductService}.
 * </p>
 *
 * <p>
 * <b>Base URL:</b> /api/v1/products
 * </p>
 *
 * <p>
 * <b>Responsabilidades:</b>
 * </p>
 * <ul>
 * <li>Gerenciar requisições de CRUD de produtos</li>
 * <li>Trabalhar com paginação</li>
 * <li>Controlar o fluxo HTTP (status codes)</li>
 * </ul>
 *
 * <p>
 * <b>Ponto crítico para iniciantes:</b>
 * </p>
 * <ul>
 * <li>O relacionamento com Category NÃO é enviado como objeto</li>
 * <li>O request envia apenas IDs (categoryIds)</li>
 * <li>O backend resolve o relacionamento</li>
 * </ul>
 */
@Tag(name = "Produtos", description = "Contém todas as operações aos recursos para cadastro, edição e leitura de um produto.")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

  private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

  private final ProductService productService;

  /**
   * Construtor para injeção de dependência do serviço de produtos.
   *
   * @param productService serviço responsável pelas regras de negócio
   */
  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  /**
   * Endpoint para criação de um novo produto.
   *
   * <p>
   * Recebe um JSON contendo os dados do produto, incluindo
   * a lista de IDs de categorias ({@code categoryIds}).
   * </p>
   *
   * <p>
   * <b>Exemplo de request:</b>
   * </p>
   * 
   * <pre>
   * {
   *   "name": "Produto X",
   *   "description": "Descrição",
   *   "price": 100.0,
   *   "categoryIds": [1, 2]
   * }
   * </pre>
   *
   * <p>
   * <b>Importante:</b>
   * </p>
   * <ul>
   * <li>Não enviar objetos de categoria</li>
   * <li>Apenas IDs</li>
   * </ul>
   *
   * <p>
   * <b>Resposta:</b>
   * </p>
   * <ul>
   * <li>HTTP 201 (Created)</li>
   * <li>Header Location com URI do recurso</li>
   * </ul>
   *
   * @param productCreateRequest dados do produto
   * @return produto criado
   */
  @Operation(summary = "Cria um novo produto", description = "Recurso para criar um produto no sistema.", responses = {
      @ApiResponse(responseCode = "201", description = "Produto criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @ApiResponse(responseCode = "400", description = "Dados inválidos ou campos obrigatórios ausentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Produto já existente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping
  public ResponseEntity<ProductResponse> create(@RequestBody ProductCreateRequest productCreateRequest) {
    logger.debug("Recebendo requisição para criar produto: {}", productCreateRequest);

    ProductResponse productResponse = productService.create(productCreateRequest);

    URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(productResponse.id())
        .toUri();

    logger.info("Produto criado com sucesso. id={}", productResponse.id());
    return ResponseEntity.created(uri).body(productResponse);
  }

  /**
   * Endpoint para listar produtos de forma paginada.
   *
   * <p>
   * Utiliza {@link Pageable}, permitindo paginação automática via parâmetros:
   * </p>
   *
   * <ul>
   * <li>page → número da página</li>
   * <li>size → quantidade por página</li>
   * <li>sort → ordenação (ex: name,asc)</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo:</b>
   * </p>
   * 
   * <pre>
   * GET /api/v1/products?page=0&size=10&sort=name,asc
   * </pre>
   *
   * @param pageable configuração de paginação automática
   * @return lista paginada de produtos
   */
  @Operation(summary = "Lista todos os produtos com paginação e filtragem", description = "Exige Bearer Token. Acesso restrito a ADMIN.", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "200", description = "Lista paginada de produtos", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class)))),
      @ApiResponse(responseCode = "403", description = "Usuário sem permissão para acessar este recurso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping
  public ResponseEntity<Page<ProductResponse>> findAll(@RequestParam(required = false) String name, Pageable pageable) {
    logger.debug("Buscando produtos paginados - name: {}, page: {}, size: {}, sort: {}",
        name,
        pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSort().isSorted() ? pageable.getSort() : "unsorted");

    Page<ProductResponse> response = productService.search(name, pageable);

    logger.debug("Produtos retornados: {}", response.getTotalElements());
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para buscar um produto pelo ID.
   *
   * <p>
   * Retorna os detalhes completos do produto, incluindo suas categorias.
   * </p>
   *
   * <p>
   * <b>Resposta:</b>
   * </p>
   * <ul>
   * <li>HTTP 200 → sucesso</li>
   * <li>HTTP 404 → não encontrado</li>
   * </ul>
   *
   * @param id identificador do produto
   * @return detalhes do produto
   */
  @Operation(summary = "Busca um produto pelo ID", description = "Recurso para obter detalhes completos de um produto pelo seu ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Produto encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductDetailsResponse.class))),
      @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping(value = "/{id}")
  public ResponseEntity<ProductDetailsResponse> findById(@PathVariable Long id) {
    logger.debug("Buscando produto por id: {}", id);

    ProductDetailsResponse response = productService.findById(id);

    logger.debug("Produto encontrado: id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para atualização parcial de um produto.
   *
   * <p>
   * Permite atualizar tanto os dados do produto quanto suas categorias.
   * </p>
   *
   * <p>
   * <b>Comportamento:</b>
   * </p>
   * <ul>
   * <li>Campos nulos NÃO são atualizados</li>
   * <li>Se categoryIds for informado → substitui categorias</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo:</b>
   * </p>
   * 
   * <pre>
   * {
   *   "name": "Novo nome",
   *   "categoryIds": [2, 3]
   * }
   * </pre>
   *
   * @param id                   identificador do produto
   * @param productUpdateRequest dados para atualização
   * @return produto atualizado
   */
  @Operation(summary = "Atualiza um produto", description = "Atualização parcial dos dados do produto, incluindo categorias.", responses = {
      @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping(value = "/{id}")
  public ResponseEntity<ProductResponse> update(@PathVariable Long id,
      @RequestBody ProductUpdateRequest productUpdateRequest) {

    logger.debug("Atualizando produto id={} com dados: {}", id, productUpdateRequest);

    ProductResponse response = productService.update(id, productUpdateRequest);

    logger.info("Produto atualizado com sucesso. id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para ativação de um produto.
   *
   * <p>
   * Altera o status do produto para ativo, permitindo sua exibição
   * e comercialização no sistema.
   * </p>
   *
   * <p>
   * <b>Respostas possíveis:</b>
   * </p>
   * <ul>
   * <li>204 → produto ativado com sucesso</li>
   * <li>404 → produto não encontrado</li>
   * </ul>
   *
   * @param id identificador do produto
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Ativa um produto", description = "Altera o status do produto para ativo.", responses = {
      @ApiResponse(responseCode = "204", description = "Produto ativado com sucesso"),
      @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/activate")
  public ResponseEntity<Void> activate(@PathVariable Long id) {

    logger.debug("Ativando produto id={}", id);

    productService.activate(id);

    logger.info("Produto ativado com sucesso. id={}", id);

    return ResponseEntity.noContent().build();
  }

  /**
   * Endpoint para desativação de um produto.
   *
   * <p>
   * Altera o status do produto para inativo, ocultando-o das listagens
   * e impedindo sua comercialização no sistema.
   * </p>
   *
   * <p>
   * <b>Respostas possíveis:</b>
   * </p>
   * <ul>
   * <li>204 → produto desativado com sucesso</li>
   * <li>404 → produto não encontrado</li>
   * </ul>
   *
   * @param id identificador do produto
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Desativa um produto", description = "Altera o status do produto para inativo.", responses = {
      @ApiResponse(responseCode = "204", description = "Produto desativado com sucesso"),
      @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {

    logger.debug("Desativando produto id={}", id);

    productService.deactivate(id);

    logger.info("Produto desativado com sucesso. id={}", id);

    return ResponseEntity.noContent().build();
  }

  /**
   * Endpoint para remoção de um produto.
   *
   * <p>
   * <b>Respostas possíveis:</b>
   * </p>
   * <ul>
   * <li>204 → removido com sucesso</li>
   * <li>404 → produto não encontrado</li>
   * <li>400 → erro de integridade</li>
   * </ul>
   *
   * @param id identificador do produto
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Remove um produto", description = "Exclui um produto pelo ID. Retorna erro se houver integridade referencial.", responses = {
      @ApiResponse(responseCode = "204", description = "Produto deletado com sucesso"),
      @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "400", description = "Violação de integridade - existem entidades relacionadas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @DeleteMapping(value = "/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    logger.debug("Deletando produto id={}", id);

    productService.delete(id);

    logger.info("Produto deletado com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }
}