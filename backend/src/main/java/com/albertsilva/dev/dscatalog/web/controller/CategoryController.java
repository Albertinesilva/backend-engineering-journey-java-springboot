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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.category.response.CategoryResponse;
import com.albertsilva.dev.dscatalog.service.CategoryService;
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
 * Endpoints de <b>categorias</b> ({@code /api/v1/categories}), delegados a
 * {@code CategoryService}.
 *
 * <p>
 * <b>Acesso:</b> {@code GET} em {@code /api/v1/categories/**} é <b>público por
 * URL</b> e os métodos de leitura não têm {@code @PreAuthorize}, portanto
 * listar e consultar categorias <b>não exige token</b>, embora o OpenAPI
 * marque esses endpoints com exigência de Bearer e declare
 * {@code 401}/{@code 403}. Os métodos de escrita exigem autenticação (URL) e
 * {@code ADMIN} ou {@code OPERATOR} ({@code @PreAuthorize}).
 * </p>
 *
 * <p>
 * Verbo de atualização: {@code PATCH} (diferente de produtos e usuários, que
 * usam {@code PUT}).
 * </p>
 */
@Tag(name = "Categorias", description = "Operações para gestão de categorias do catálogo.")
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

  private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

  private final CategoryService categoryService;

  /**
   * @param categoryService service de categorias
   */
  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  /**
   * <b>{@code POST /api/v1/categories}</b> — cria uma categoria (ativa).
   *
   * <ul>
   * <li><b>Acesso:</b> autenticado (URL) e {@code ADMIN} ou {@code OPERATOR}.</li>
   * <li><b>Entrada:</b> {@link CategoryCreateRequest} com {@code @Valid}
   * (nome 3 a 80 caracteres e único, via {@code @CategoryCreateValid}).</li>
   * <li><b>Fluxo:</b> {@code CategoryService.create} (mapper e {@code save}).</li>
   * <li><b>Sucesso:</b> {@code 201} com {@link CategoryResponse} e
   * {@code Location} = URL da requisição + {@code /{id}}.</li>
   * <li><b>Erros:</b> {@code 422} (validação); {@code 403}; {@code 409}
   * (unicidade no banco).</li>
   * </ul>
   *
   * @param categoryCreateRequest dados da categoria
   * @return resposta {@code 201} com a categoria criada
   */
  @Operation(summary = "Cria uma nova categoria", description = "Cria uma nova categoria no catálogo e retorna o recurso criado. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest categoryCreateRequest) {
    logger.debug("Recebendo requisição para criar categoria: {}", categoryCreateRequest);

    CategoryResponse response = categoryService.create(categoryCreateRequest);

    URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();

    logger.info("Categoria criada com sucesso. id: {}", response.id());
    return ResponseEntity.created(uri).body(response);
  }

  /**
   * <b>{@code GET /api/v1/categories}</b> — lista paginada, com filtro opcional
   * por nome.
   *
   * <ul>
   * <li><b>Acesso:</b> público por URL; sem {@code @PreAuthorize}.</li>
   * <li><b>Parâmetros:</b> {@code name} (opcional) e os de paginação do Spring
   * Data ({@code page}, {@code size}, {@code sort}); sem {@code @PageableDefault}
   * (valem os padrões do Spring Data Web).</li>
   * <li><b>Fluxo:</b> {@code CategoryService.search}.</li>
   * <li><b>Sucesso:</b> {@code 200} com {@code Page<CategoryResponse>} (JSON com
   * {@code content}, {@code totalElements} etc.).</li>
   * <li><b>Erros:</b> propriedade de ordenação inexistente tende a gerar exceção
   * não tratada especificamente ({@code 500}).</li>
   * </ul>
   *
   * @param name     termo procurado no nome (opcional)
   * @param pageable página, tamanho e ordenação
   * @return resposta {@code 200} com a página de categorias
   */
  @Operation(summary = "Lista categorias com paginação", description = "Retorna uma página de categorias filtradas por nome. Requer autenticação com Bearer Token.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista paginada de categorias", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
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
   * <b>{@code GET /api/v1/categories/{id}}</b> — detalhes de uma categoria.
   *
   * <ul>
   * <li><b>Acesso:</b> público por URL; sem {@code @PreAuthorize}.</li>
   * <li><b>Fluxo:</b> {@code CategoryService.findById}.</li>
   * <li><b>Sucesso:</b> {@code 200} com {@link CategoryDetailsResponse}.</li>
   * <li><b>Erros:</b> {@code 404} (categoria inexistente); id não numérico falha
   * na conversão do {@code @PathVariable} e vira {@code 500} (sem handler
   * específico).</li>
   * </ul>
   *
   * @param id identificador da categoria
   * @return resposta {@code 200} com os detalhes
   */
  @Operation(summary = "Busca uma categoria pelo ID", description = "Retorna os detalhes completos de uma categoria existente. Requer autenticação com Bearer Token.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Categoria encontrada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryDetailsResponse.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping(value = "/{id}")
  public ResponseEntity<CategoryDetailsResponse> findById(@PathVariable Long id) {
    logger.debug("Buscando categoria por id: {}", id);

    CategoryDetailsResponse response = categoryService.findById(id);

    logger.debug("Categoria encontrada: id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code PATCH /api/v1/categories/{id}}</b> — atualiza nome e descrição.
   *
   * <ul>
   * <li><b>Acesso:</b> autenticado (URL) e {@code ADMIN} ou {@code OPERATOR}.</li>
   * <li><b>Entrada:</b> {@code @PathVariable Long id} e {@link CategoryUpdateRequest}
   * com {@code @Valid} ({@code @CategoryUpdateValid}: nome de outra categoria,
   * usando o {@code id} da URL). Semântica: só campos não nulos do DTO
   * sobrescrevem; o nome é obrigatório.</li>
   * <li><b>Fluxo:</b> {@code CategoryService.update}.</li>
   * <li><b>Sucesso:</b> {@code 200} com {@link CategoryResponse}.</li>
   * <li><b>Erros:</b> {@code 422} (validação, inclusive nome duplicado ou id
   * inexistente com nome já usado); {@code 404}; {@code 403}.</li>
   * </ul>
   *
   * @param id                    identificador da categoria
   * @param categoryUpdateRequest novos dados
   * @return resposta {@code 200} com a categoria atualizada
   */
  @Operation(summary = "Atualiza uma categoria", description = "Atualiza os dados de uma categoria existente. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Categoria atualizada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class))),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
      @Valid @RequestBody CategoryUpdateRequest categoryUpdateRequest) {

    logger.debug("Atualizando categoria id={} com dados: {}", id, categoryUpdateRequest);

    CategoryResponse response = categoryService.update(id, categoryUpdateRequest);

    logger.info("Categoria atualizada com sucesso. id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code PATCH /api/v1/categories/{id}/activate}</b> — marca a categoria
   * como ativa (sem corpo). Acesso: autenticado (URL) e {@code ADMIN} ou
   * {@code OPERATOR}. Fluxo: {@code CategoryService.activate}. Sucesso:
   * {@code 204}. Erros: {@code 404}, {@code 403}. O indicador apenas é gravado;
   * nenhuma listagem o usa.
   *
   * @param id identificador da categoria
   * @return resposta {@code 204}
   */
  @Operation(summary = "Ativa uma categoria", description = "Ativa uma categoria existente no catálogo. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Categoria ativada com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/activate")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<Void> activate(@PathVariable Long id) {
    logger.debug("Ativando categoria id={}", id);

    categoryService.activate(id);

    logger.info("Categoria ativada com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code PATCH /api/v1/categories/{id}/deactivate}</b> — marca a categoria
   * como inativa (sem corpo). Acesso: autenticado (URL) e {@code ADMIN} ou
   * {@code OPERATOR}. Fluxo: {@code CategoryService.deactivate}. Sucesso:
   * {@code 204}. Erros: {@code 404}, {@code 403}.
   *
   * @param id identificador da categoria
   * @return resposta {@code 204}
   */
  @Operation(summary = "Desativa uma categoria", description = "Desativa uma categoria existente no catálogo. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Categoria desativada com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/deactivate")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {
    logger.debug("Desativando categoria id={}", id);

    categoryService.deactivate(id);

    logger.info("Categoria desativada com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code DELETE /api/v1/categories/{id}}</b> — remove fisicamente a
   * categoria. Acesso: autenticado (URL) e {@code ADMIN} ou {@code OPERATOR}.
   * Fluxo: {@code CategoryService.delete}. Sucesso: {@code 204}. Erros:
   * {@code 404}; {@code 409} quando há produtos vinculados (violação de chave
   * estrangeira detectada no commit e convertida pelo handler); {@code 403}.
   *
   * @param id identificador da categoria
   * @return resposta {@code 204}
   */
  @Operation(summary = "Remove uma categoria", description = "Remove uma categoria existente do catálogo. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Categoria deletada com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @DeleteMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    logger.debug("Deletando categoria id={}", id);

    categoryService.delete(id);

    logger.info("Categoria deletada com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }
}