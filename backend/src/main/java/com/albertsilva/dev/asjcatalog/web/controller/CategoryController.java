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

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.service.CategoryService;
import com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller responsável por expor os endpoints REST da entidade Category.
 *
 * <p>
 * Esta classe recebe requisições HTTP, delega o processamento para a camada
 * de serviço ({@link CategoryService}) e retorna respostas padronizadas.
 * </p>
 *
 * <p>
 * <b>Base URL:</b> /api/v1/categories
 * </p>
 *
 * <p>
 * <b>Responsabilidades:</b>
 * </p>
 * <ul>
 * <li>Receber requisições HTTP</li>
 * <li>Validar e mapear parâmetros</li>
 * <li>Delegar regras de negócio para o Service</li>
 * <li>Retornar respostas HTTP apropriadas</li>
 * </ul>
 *
 * <p>
 * <b>Padrões REST utilizados:</b>
 * </p>
 * <ul>
 * <li>POST → criação</li>
 * <li>GET → consulta</li>
 * <li>PATCH → atualização parcial</li>
 * <li>DELETE → remoção</li>
 * </ul>
 */
@Tag(name = "Categorias", description = "Contém todas as operações aos recursos para cadastro, edição e leitura de uma categoria.")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

  private final CategoryService categoryService;

  /**
   * Construtor para injeção de dependência do serviço.
   *
   * @param categoryService serviço de categorias
   */
  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  /**
   * Endpoint para criação de uma nova categoria.
   *
   * <p>
   * Recebe um JSON contendo os dados da categoria e retorna o recurso criado.
   * </p>
   *
   * <p>
   * <b>Fluxo:</b>
   * </p>
   * <ol>
   * <li>Recebe o request</li>
   * <li>Delega para o Service</li>
   * <li>Gera a URI do recurso criado</li>
   * <li>Retorna HTTP 201 (Created)</li>
   * </ol>
   *
   * @param categoryCreateRequest dados da categoria
   * @return categoria criada com status 201 e header Location
   */
  @Operation(summary = "Cria uma nova categoria", description = "Recurso para criar uma nova categoria no sistema.", responses = {
      @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
      @ApiResponse(responseCode = "400", description = "Dados inválidos ou campos obrigatórios ausentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Categoria já existente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping
  public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest categoryCreateRequest) {
    logger.debug("Recebendo requisição para criar categoria: {}", categoryCreateRequest);

    CategoryResponse response = categoryService.create(categoryCreateRequest);

    URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();

    logger.info("Categoria criada com sucesso. id: {}", response.id());
    return ResponseEntity.created(uri).body(response);
  }

  /**
   * Endpoint para listar categorias com paginação.
   *
   * <p>
   * <b>Parâmetros suportados:</b>
   * </p>
   * <ul>
   * <li>page → número da página</li>
   * <li>linesPerPage → quantidade de registros por página</li>
   * <li>orderBy → campo para ordenação</li>
   * <li>direction → direção (ASC ou DESC)</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo:</b>
   * </p>
   * 
   * <pre>
   * GET /api/v1/categories?page=0&linesPerPage=10&direction=ASC&orderBy=name
   * </pre>
   *
   * @return lista paginada de categorias
   */
  @Operation(summary = "Lista todas as categorias com paginação e filtro por nome", description = "Exige Bearer Token. Acesso restrito a ADMIN.", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "200", description = "Lista paginada de categorias", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)))),
      @ApiResponse(responseCode = "403", description = "Usuário sem permissão para acessar este recurso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping
  public ResponseEntity<Page<CategoryResponse>> findAll(@RequestParam(required = false) String name,
      Pageable pageable) {

    logger.debug("Buscando categorias - name: {}, page: {}, size: {}, sort: {}", name, pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSort().isSorted() ? pageable.getSort() : "unsorted");
    Page<CategoryResponse> response = categoryService.search(name, pageable);

    logger.debug("Categorias retornadas: {}", response.getTotalElements());
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para buscar uma categoria pelo ID.
   *
   * <p>
   * Retorna HTTP 200 caso encontre, ou 404 caso não exista.
   * </p>
   *
   * @param id identificador da categoria
   * @return categoria encontrada
   */
  @Operation(summary = "Busca uma categoria pelo ID", description = "Recurso para obter detalhes de uma categoria específica pelo seu ID.", responses = {
      @ApiResponse(responseCode = "200", description = "Categoria encontrada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
      @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping(value = "/{id}")
  public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
    logger.debug("Buscando categoria por id: {}", id);

    CategoryResponse response = categoryService.findById(id);

    logger.debug("Categoria encontrada: id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para atualização parcial de uma categoria.
   *
   * <p>
   * Utiliza o método PATCH, permitindo atualizar apenas os campos enviados.
   * </p>
   *
   * <p>
   * <b>Importante:</b>
   * </p>
   * <ul>
   * <li>Campos nulos NÃO são atualizados</li>
   * <li>Apenas campos informados são modificados</li>
   * </ul>
   *
   * @param id                    identificador da categoria
   * @param categoryUpdateRequest dados para atualização
   * @return categoria atualizada
   */
  @Operation(summary = "Atualiza uma categoria", description = "Atualização parcial dos dados da categoria. Apenas campos enviados são alterados.", responses = {
      @ApiResponse(responseCode = "200", description = "Categoria atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
      @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping(value = "/{id}")
  public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
      @Valid @RequestBody CategoryUpdateRequest categoryUpdateRequest) {

    logger.debug("Atualizando categoria id={} com dados: {}", id, categoryUpdateRequest);

    CategoryResponse response = categoryService.update(id, categoryUpdateRequest);

    logger.info("Categoria atualizada com sucesso. id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para ativação de uma categoria.
   *
   * <p>
   * Altera o status da categoria para ativo, permitindo sua exibição
   * e utilização no sistema.
   * </p>
   *
   * @param id identificador da categoria
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Ativa uma categoria", description = "Altera o status da categoria para ativo.", responses = {
      @ApiResponse(responseCode = "204", description = "Categoria ativada com sucesso"),
      @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/activate")
  public ResponseEntity<Void> activate(@PathVariable Long id) {

    logger.debug("Ativando categoria id={}", id);

    categoryService.activate(id);

    logger.info("Categoria ativada com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Endpoint para desativação de uma categoria.
   *
   * <p>
   * Altera o status da categoria para inativo, ocultando-a das listagens
   * e impedindo sua utilização no sistema.
   * </p>
   *
   * @param id identificador da categoria
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Desativa uma categoria", description = "Altera o status da categoria para inativo.", responses = {
      @ApiResponse(responseCode = "204", description = "Categoria desativada com sucesso"),
      @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {

    logger.debug("Desativando categoria id={}", id);

    categoryService.deactivate(id);

    logger.info("Categoria desativada com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Endpoint para remoção de uma categoria.
   *
   * <p>
   * Retorna HTTP 204 (No Content) em caso de sucesso.
   * </p>
   *
   * <p>
   * <b>Possíveis erros:</b>
   * </p>
   * <ul>
   * <li>404 → categoria não encontrada</li>
   * <li>409 → violação de integridade</li>
   * </ul>
   *
   * @param id identificador da categoria
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Remove uma categoria", description = "Exclui uma categoria pelo ID. Retorna erro se houver integridade referencial.", responses = {
      @ApiResponse(responseCode = "204", description = "Categoria deletada com sucesso"),
      @ApiResponse(responseCode = "404", description = "Categoria não encontrada", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Violação de integridade - existem entidades relacionadas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @DeleteMapping(value = "/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    logger.debug("Deletando categoria id={}", id);

    categoryService.delete(id);

    logger.info("Categoria deletada com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }
}