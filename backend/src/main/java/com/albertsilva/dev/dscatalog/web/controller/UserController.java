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

import com.albertsilva.dev.dscatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.service.UserService;
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
 * Endpoints <b>administrativos de usuários</b> ({@code /api/v1/users}),
 * delegados a {@code UserService}. Os fluxos da própria conta ficam em
 * {@code AccountController}.
 *
 * <p>
 * <b>Acesso:</b> nenhuma rota é pública: a regra de URL exige autenticação em
 * todas, e o {@code @PreAuthorize} restringe a {@code ADMIN}, exceto duas rotas
 * com regra por usuário para {@code OPERATOR}: {@code GET /{id}} (usa
 * {@code AuthenticatedUserService.isCurrentUser(#id)}, com o {@code userId} do
 * JWT) e {@code PUT /{id}} (usa {@code authentication.principal.id}; ver a
 * documentação do método).
 * </p>
 *
 * <p>
 * <b>Logs:</b> {@code create} e {@code update} registram, em nível
 * {@code DEBUG}, o DTO inteiro; como o {@code toString()} de um {@code record}
 * inclui todos os componentes, isso <b>inclui a senha em texto</b> quando
 * informada.
 * </p>
 */
@Tag(name = "Usuários", description = "Operações administrativas para gerenciamento de usuários do sistema.")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  private final UserService userService;

  /**
   * @param userService service de usuários
   */
  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * <b>{@code POST /api/v1/users}</b> — cria um usuário <b>ativo</b>.
   *
   * <ul>
   * <li><b>Acesso:</b> {@code ADMIN}.</li>
   * <li><b>Entrada:</b> {@link UserCreateRequest} com {@code @Valid} (e-mail
   * válido por DNS e único, senha forte de 10 a 72 caracteres sem dados
   * pessoais, {@code roleIds} existentes; {@code roleIds} nulo ou vazio cria
   * usuário sem roles, que não consegue autenticar).</li>
   * <li><b>Log:</b> {@code DEBUG} com o DTO inteiro, inclusive a senha.</li>
   * <li><b>Fluxo:</b> {@code UserService.create}.</li>
   * <li><b>Sucesso:</b> {@code 201} com {@link UserResponse} e {@code Location}
   * = URL da requisição + {@code /{id}}.</li>
   * <li><b>Erros:</b> {@code 422}; {@code 404} (role não encontrada no service);
   * {@code 403}; {@code 409}.</li>
   * </ul>
   *
   * @param userCreateRequest dados do usuário
   * @return resposta {@code 201} com o usuário criado
   */
  @Operation(summary = "Cria um novo usuário", description = "Cria um novo usuário no sistema e retorna o recurso criado. Requer autenticação com Bearer Token e permissão ADMIN.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    logger.debug("Recebendo requisição para criar usuário: {}", userCreateRequest);
    UserResponse response = userService.create(userCreateRequest);
    URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(response.id()).toUri();

    logger.info("Usuário criado com sucesso. id: {}", response.id());
    return ResponseEntity.created(uri).body(response);
  }

  /**
   * <b>{@code GET /api/v1/users}</b> — lista paginada de usuários, com filtro
   * opcional por primeiro nome. Acesso: {@code ADMIN}. Parâmetros:
   * {@code firstName} (opcional) e {@code page}/{@code size}/{@code sort}.
   * Fluxo: {@code UserService.search}. Sucesso: {@code 200} com
   * {@code Page<UserResponse>} (sem senha nem {@code active}). Erros:
   * {@code 403}.
   *
   * @param firstName termo procurado no primeiro nome (opcional)
   * @param pageable  página, tamanho e ordenação
   * @return resposta {@code 200} com a página de usuários
   */
  @Operation(summary = "Lista usuários com paginação", description = "Retorna uma página de usuários filtrados por nome. Requer autenticação com Bearer Token e permissão ADMIN.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Usuários consultados com sucesso", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Page<UserResponse>> findAll(@RequestParam(required = false) String firstName,
      Pageable pageable) {

    logger.debug("Buscando usuários - firstName: {}, page: {}, size: {}, sort: {}",
        firstName,
        pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSort().isSorted() ? pageable.getSort() : "unsorted");

    Page<UserResponse> response = userService.search(firstName, pageable);

    logger.debug("Usuários encontrados: {}", response.getTotalElements());

    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code GET /api/v1/users/{id}}</b> — detalhes de um usuário.
   *
   * <ul>
   * <li><b>Acesso:</b> {@code ADMIN}, ou {@code OPERATOR} <b>somente para o
   * próprio usuário</b>: {@code @authenticatedUserService.isCurrentUser(#id)}
   * compara {@code #id} com o claim {@code userId} do JWT (esta regra é
   * coerente com a identidade do token).</li>
   * <li><b>Fluxo:</b> {@code UserService.findById}.</li>
   * <li><b>Sucesso:</b> {@code 200} com {@link UserDetailsResponse} (inclui
   * {@code active}; sem senha).</li>
   * <li><b>Erros:</b> {@code 404}; {@code 403} (inclusive {@code OPERATOR}
   * consultando outro usuário); {@code 401} se o JWT não resolver um
   * usuário.</li>
   * </ul>
   *
   * @param id identificador do usuário
   * @return resposta {@code 200} com os detalhes
   */
  @Operation(summary = "Busca um usuário pelo ID", description = "Retorna os dados completos de um usuário existente. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR para o próprio usuário.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDetailsResponse.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') OR (hasRole('OPERATOR') AND @authenticatedUserService.isCurrentUser(#id))")
  public ResponseEntity<UserDetailsResponse> findById(@PathVariable Long id) {
    logger.debug("Buscando usuário por id: {}", id);

    UserDetailsResponse response = userService.findById(id);

    logger.debug("Usuário encontrado: id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code PUT /api/v1/users/{id}}</b> — atualiza nome, e-mail e, se
   * informados, senha e roles.
   *
   * <ul>
   * <li><b>Acesso:</b> {@code ADMIN}, ou {@code OPERATOR} quando
   * {@code #id == authentication.principal.id}.</li>
   * <li><b>Atenção ao segundo termo:</b> {@code authentication} é um
   * {@code JwtAuthenticationToken} e seu principal é o {@code Jwt}; a propriedade
   * {@code id} do principal é {@code Jwt.getId()}, que devolve o claim
   * <b>{@code jti}</b> (id do token, texto), e não o {@code userId}. Como
   * {@code #id} é {@code Long}, a comparação tende a ser sempre falsa: na
   * prática, <b>{@code OPERATOR} não consegue atualizar nenhum usuário, nem a si
   * mesmo</b> (resposta esperada {@code 403}, a confirmar em execução);
   * {@code ADMIN} não é afetado, pois {@code hasRole('ADMIN')} vem antes.</li>
   * <li><b>Ordem:</b> a validação do corpo ({@code @Valid}, com
   * {@code @UserUpdateValid}) ocorre <b>antes</b> do {@code @PreAuthorize}; um
   * {@code OPERATOR} com corpo inválido recebe {@code 422} em vez de
   * {@code 403}.</li>
   * <li><b>Entrada:</b> {@link UserUpdateRequest} ({@code roleIds} nulo mantém
   * as roles; conjunto vazio remove todas; {@code password} nulo mantém a
   * senha).</li>
   * <li><b>Log:</b> {@code DEBUG} com o DTO inteiro, inclusive a senha.</li>
   * <li><b>Fluxo/sucesso:</b> {@code UserService.update}; {@code 200} com
   * {@link UserResponse}. <b>Erros:</b> {@code 422}, {@code 404}, {@code 403}.</li>
   * </ul>
   *
   * @param id                identificador do usuário
   * @param userUpdateRequest novos dados
   * @return resposta {@code 200} com o usuário atualizado
   */
  @Operation(summary = "Atualiza os dados de um usuário", description = "Atualiza completamente os dados de um usuário existente. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR para o próprio usuário.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PutMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)")
  public ResponseEntity<UserResponse> update(@PathVariable Long id,
      @Valid @RequestBody UserUpdateRequest userUpdateRequest) {
    logger.debug("Atualizando usuário id={} com dados: {}", id, userUpdateRequest);

    UserResponse response = userService.update(id, userUpdateRequest);

    logger.info("Usuário atualizado com sucesso. id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code PATCH /api/v1/users/{id}/activate}</b> — ativa o usuário (sem
   * corpo). Acesso: {@code ADMIN}. Fluxo: {@code UserService.activate}. Sucesso:
   * {@code 204}. Erros: {@code 404}, {@code 403}.
   *
   * @param id identificador do usuário
   * @return resposta {@code 204}
   */
  @Operation(summary = "Ativa um usuário", description = "Ativa um usuário existente. Requer autenticação com Bearer Token e permissão ADMIN.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Usuário ativado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/activate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> activate(@PathVariable Long id) {
    logger.debug("Ativando usuário id={}", id);

    userService.activate(id);

    logger.info("Usuário ativado com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code PATCH /api/v1/users/{id}/deactivate}</b> — desativa o usuário (sem
   * corpo). Acesso: {@code ADMIN}. Fluxo: {@code UserService.deactivate}.
   * Sucesso: {@code 204}. Erros: {@code 404}, {@code 403}. Não revoga tokens já
   * emitidos.
   *
   * @param id identificador do usuário
   * @return resposta {@code 204}
   */
  @Operation(summary = "Desativa um usuário", description = "Desativa um usuário existente. Requer autenticação com Bearer Token e permissão ADMIN.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PatchMapping("/{id}/deactivate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deactivate(@PathVariable Long id) {
    logger.debug("Desativando usuário id={}", id);

    userService.deactivate(id);

    logger.info("Usuário desativado com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code DELETE /api/v1/users/{id}}</b> — remove fisicamente o usuário e seus
   * tokens de conta. Acesso: {@code ADMIN}. Fluxo: {@code UserService.delete}.
   * Sucesso: {@code 204}. Erros: {@code 404}, {@code 403}, {@code 409}.
   *
   * @param id identificador do usuário
   * @return resposta {@code 204}
   */
  @Operation(summary = "Remove um usuário", description = "Remove um usuário existente do sistema. Requer autenticação com Bearer Token e permissão ADMIN.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @DeleteMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    logger.debug("Deletando usuário id={}", id);

    userService.delete(id);

    logger.info("Usuário deletado com sucesso. id={}", id);
    return ResponseEntity.noContent().build();
  }
}