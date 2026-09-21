package com.albertsilva.dev.dscatalog.web.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import com.albertsilva.dev.dscatalog.web.exception.response.ValidationError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints de <b>produtos</b> ({@code /api/v1/products}), delegados a
 * {@code ProductService}.
 *
 * <p>
 * <b>Acesso:</b> {@code GET} em {@code /api/v1/products/**} é <b>público por
 * URL</b> e os métodos de leitura não têm {@code @PreAuthorize}; o OpenAPI, no
 * entanto, marca esses endpoints com exigência de Bearer. Escrita exige
 * autenticação (URL) e {@code ADMIN} ou {@code OPERATOR}.
 * </p>
 *
 * <p>
 * Verbo de atualização: <b>{@code PUT}</b> (o corpo exige nome e
 * {@code categoryIds}); alguns testes existentes usam {@code PATCH}, divergência
 * registrada na documentação de baseline.
 * </p>
 */
@Tag(name = "Produtos", description = "Operações para consulta, cadastro e gestão de produtos do catálogo.")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

  private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

  private final ProductService productService;

  /**
   * @param productService service de produtos
   */
  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  /**
   * <b>{@code POST /api/v1/products}</b> — cria um produto (ativo) e o vincula às
   * categorias.
   *
   * <ul>
   * <li><b>Acesso:</b> autenticado (URL) e {@code ADMIN} ou {@code OPERATOR}.</li>
   * <li><b>Entrada:</b> {@link ProductCreateRequest} com {@code @Valid} (nome,
   * {@code categoryIds} não vazio; {@code @ProductCreateValid}: nome único e
   * categorias existentes). O campo {@code date} é validado, mas descartado.</li>
   * <li><b>Fluxo:</b> {@code ProductService.create}.</li>
   * <li><b>Sucesso:</b> {@code 201} com {@link ProductResponse} e
   * {@code Location} = URL da requisição + {@code /{id}}.</li>
   * <li><b>Erros:</b> {@code 422} (validação); {@code 404} (categoria não
   * encontrada no service); {@code 403}; {@code 409}.</li>
   * </ul>
   *
   * @param productCreateRequest dados do produto
   * @return resposta {@code 201} com o produto criado
   */
  @Operation(summary = "Cria um novo produto", description = "Cria um novo produto no catálogo e retorna o recurso criado. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Produto criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest productCreateRequest) {
    logger.debug("Recebendo requisição para criar produto: {}", productCreateRequest);

    ProductResponse productResponse = productService.create(productCreateRequest);

    URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(productResponse.id())
        .toUri();

    logger.info("Produto criado com sucesso. id={}", productResponse.id());
    return ResponseEntity.created(uri).body(productResponse);
  }

  /**
   * <b>{@code GET /api/v1/products}</b> — lista paginada com filtros por nome e
   * por categorias.
   *
   * <ul>
   * <li><b>Acesso:</b> público por URL; sem {@code @PreAuthorize}.</li>
   * <li><b>Parâmetros:</b> {@code name} (padrão {@code ""}); {@code categoryIds}
   * (texto, padrão {@code "0"} = sem filtro; ids separados por vírgula); e
   * {@code page}, {@code size}, {@code sort} do Spring Data (a ordenação só
   * atinge as colunas {@code id} e {@code name}).</li>
   * <li><b>Fluxo:</b> {@code ProductService.findAllPaged} (consulta nativa +
   * {@code JOIN FETCH}); <b>não</b> usa {@code ProductService.search}.</li>
   * <li><b>Sucesso:</b> {@code 200} com {@code Page<ProductResponse>}.</li>
   * <li><b>Erros:</b> {@code categoryIds} não numérico gera
   * {@code NumberFormatException} ({@code 500}, sem handler específico).</li>
   * </ul>
   *
   * @param name        trecho do nome
   * @param categoryIds ids de categorias separados por vírgula, ou {@code "0"}
   * @param pageable    página, tamanho e ordenação
   * @return resposta {@code 200} com a página de produtos
   */
  @Operation(summary = "Lista produtos com paginação", description = "Retorna uma página de produtos filtrados por nome e categorias. Requer autenticação com Bearer Token.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista paginada de produtos", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ProductResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping
  public ResponseEntity<Page<ProductResponse>> findAll(
      @RequestParam(value = "name", defaultValue = "") String name,
      @RequestParam(value = "categoryIds", defaultValue = "0") String categoryIds,
      Pageable pageable) {

    logger.debug("Buscando produtos paginados - name: {}, page: {}, size: {}, sort: {}",
        name,
        pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSort().isSorted() ? pageable.getSort() : "unsorted");

    Page<ProductResponse> response = productService.findAllPaged(name, categoryIds, pageable);

    logger.debug("Produtos retornados: {}", response.getTotalElements());
    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code GET /api/v1/products/{id}}</b> — detalhes de um produto. Acesso:
   * público por URL; sem {@code @PreAuthorize}. Fluxo:
   * {@code ProductService.findById}. Sucesso: {@code 200} com
   * {@link ProductDetailsResponse}. Erros: {@code 404}; id não numérico vira
   * {@code 500}.
   *
   * @param id identificador do produto
   * @return resposta {@code 200} com os detalhes
   */
  @Operation(summary = "Busca um produto pelo ID", description = "Retorna os detalhes completos de um produto existente. Requer autenticação com Bearer Token.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Produto encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductDetailsResponse.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping(value = "/{id}")
  public ResponseEntity<ProductDetailsResponse> findById(@PathVariable Long id) {
    logger.debug("Buscando produto por id: {}", id);

    ProductDetailsResponse response = productService.findById(id);

    logger.debug("Produto encontrado: id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code PUT /api/v1/products/{id}}</b> — atualiza o produto e substitui suas
   * categorias.
   *
   * <ul>
   * <li><b>Acesso:</b> autenticado (URL) e {@code ADMIN} ou {@code OPERATOR}.</li>
   * <li><b>Entrada:</b> {@code @PathVariable Long id} e {@link ProductUpdateRequest}
   * com {@code @Valid} ({@code @ProductUpdateValid}: nome de outro produto, com o
   * {@code id} da URL; categorias existentes). Nome e {@code categoryIds} são
   * obrigatórios; descrição, preço e URL só sobrescrevem se não forem
   * nulos.</li>
   * <li><b>Fluxo:</b> {@code ProductService.update}.</li>
   * <li><b>Sucesso:</b> {@code 200} com {@link ProductResponse}.</li>
   * <li><b>Erros:</b> {@code 422}; {@code 404}; {@code 403}. Um {@code PATCH} para
   * esta rota não tem mapeamento: {@code 405} do Spring, que o handler genérico
   * converte em {@code 500}.</li>
   * </ul>
   *
   * @param id                   identificador do produto
   * @param productUpdateRequest novos dados
   * @return resposta {@code 200} com o produto atualizado
   */
  @Operation(summary = "Atualiza um produto", description = "Atualiza os dados de um produto existente. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PutMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<ProductResponse> update(@PathVariable Long id,
      @Valid @RequestBody ProductUpdateRequest productUpdateRequest) {

    logger.debug("Atualizando produto id={} com dados: {}", id, productUpdateRequest);

    ProductResponse response = productService.update(id, productUpdateRequest);

    logger.info("Produto atualizado com sucesso. id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code PATCH /api/v1/products/{id}/activate}</b> — marca o produto como
   * ativo (sem corpo). Acesso: autenticado (URL) e {@code ADMIN} ou
   * {@code OPERATOR}. Fluxo: {@code ProductService.activate}. Sucesso:
   * {@code 204}. Erros: {@code 404}, {@code 403}.
   *
   * @param id identificador do produto
   * @return resposta {@code 204}
   */
  @Operation(summary = "Ativa um produto", description = "Ativa um produto existente no catálogo. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Produto ativado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/activate")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<Void> activate(@PathVariable Long id) {
    logger.debug("Ativando produto id={}", id);

    productService.activate(id);

    logger.info("Produto ativado com sucesso. id={}", id);

    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code PATCH /api/v1/products/{id}/deactivate}</b> — marca o produto como
   * inativo (sem corpo). Acesso: autenticado (URL) e {@code ADMIN} ou
   * {@code OPERATOR}. Fluxo: {@code ProductService.deactivate}. Sucesso:
   * {@code 204}. Erros: {@code 404}, {@code 403}. Produtos inativos continuam
   * aparecendo na listagem.
   *
   * @param id identificador do produto
   * @return resposta {@code 204}
   */
  @Operation(summary = "Desativa um produto", description = "Desativa um produto existente no catálogo. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Produto desativado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/deactivate")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {
    logger.debug("Desativando produto id={}", id);

    productService.deactivate(id);

    logger.info("Produto desativado com sucesso. id={}", id);

    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code DELETE /api/v1/products/{id}}</b> — remove fisicamente o produto.
   * Acesso: autenticado (URL) e {@code ADMIN} ou {@code OPERATOR}. Fluxo:
   * {@code ProductService.delete}. Sucesso: {@code 204}. Erros: {@code 404};
   * {@code 400} ({@code DatabaseException}, quando a violação é detectada dentro
   * do método do service); {@code 409}; {@code 403}.
   *
   * @param id identificador do produto
   * @return resposta {@code 204}
   */
  @Operation(summary = "Remove um produto", description = "Remove um produto existente do catálogo. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Produto deletado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @DeleteMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    logger.debug("Deletando produto id={}", id);

    productService.delete(id);

    logger.info("Produto deletado com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }
}