package com.albertsilva.dev.asjcatalog.web.controller;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.service.UserService;
import com.albertsilva.dev.asjcatalog.web.exception.response.ProblemDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controller responsável por expor os endpoints REST da entidade User.
 *
 * <p>
 * Esta classe recebe requisições HTTP, delega o processamento para a camada
 * de serviço ({@link UserService}) e retorna respostas padronizadas.
 * </p>
 *
 * <p>
 * <b>Base URL:</b> /api/v1/users
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
@Tag(name = "Usuários", description = "Contém todas as operações aos recursos para cadastro, edição e leitura de um usuário.")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  private final UserService userService;

  /**
   * Construtor para injeção de dependência do serviço.
   *
   * @param userService serviço de usuários
   */
  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Endpoint para criação de um novo usuário.
   *
   * <p>
   * Recebe um JSON contendo os dados do usuário e retorna o recurso criado.
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
   * @param userCreateRequest dados do usuário
   * @return usuário criado com status 201 e header Location
   */
  @Operation(summary = "Cria um novo usuário", description = "Requisição exige um Bearer Token. Acesso restrito a ADMIN logado", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "400", description = "Dados inválidos ou campos obrigatórios ausentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Usuário já existente", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
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
   * Endpoint para listar usuários com paginação e filtro opcional por nome.
   *
   * <p>
   * Permite filtrar por nome (parcial, case insensitive) e paginar resultados.
   * </p>
   *
   * <p>
   * <b>Exemplo de requisição:</b>
   * </p>
   *
   * <pre>
   * GET /api/v1/users?firstName=jo&page=0&size=10&sort=firstName,asc
   * </pre>
   *
   * @param firstName filtro opcional por nome
   * @param pageable  parâmetros de paginação e ordenação
   * @return lista paginada de usuários
   */
  @Operation(summary = "Lista usuários paginados com filtro opcional por nome", description = "Exige Bearer Token. Acesso restrito a ADMIN.", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "200", description = "Usuários consultados com sucesso", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserResponse.class)))),
      @ApiResponse(responseCode = "403", description = "Usuário sem permissão para acessar este recurso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
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
   * Endpoint para buscar um usuário pelo ID.
   *
   * <p>
   * Retorna HTTP 200 caso encontre, ou 404 caso não exista.
   * </p>
   *
   * @param id identificador do usuário
   * @return usuário encontrado
   */
  @Operation(summary = "Busca um usuário pelo ID", description = "Exige Bearer Token. Acesso restrito a ADMIN ou OPERATOR (apenas para seu próprio ID).", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDetailsResponse.class))),
      @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)")
  public ResponseEntity<UserDetailsResponse> findById(@PathVariable Long id) {
    logger.debug("Buscando usuário por id: {}", id);

    UserDetailsResponse response = userService.findById(id);

    logger.debug("Usuário encontrado: id={}", id);
    return ResponseEntity.ok(response);
  }

  /**
   * Endpoint para atualização completa de um usuário.
   *
   * <p>
   * Utiliza o método PUT, permitindo atualizar todos os campos do usuário.
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
   * @param id                identificador do usuário
   * @param userUpdateRequest dados para atualização
   * @return usuário atualizado
   */
  @Operation(summary = "Atualiza um usuário", description = "Exige Bearer Token. Acesso restrito a ADMIN ou OPERATOR (apenas para seu próprio ID).", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
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
   * Endpoint para ativação de um usuário.
   *
   * <p>
   * Altera o status do usuário para ativo, permitindo sua exibição
   * e utilização no sistema.
   * </p>
   *
   * @param id identificador do usuário
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Ativa um usuário", description = "Exige Bearer Token. Acesso restrito a ADMIN.", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "204", description = "Usuário ativado com sucesso"),
      @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
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
   * Endpoint para desativação de um usuário.
   *
   * <p>
   * Altera o status do usuário para inativo, ocultando-o das listagens
   * e impedindo sua utilização no sistema.
   * </p>
   *
   * @param id identificador do usuário
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Desativa um usuário", description = "Exige Bearer Token. Acesso restrito a ADMIN.", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "204", description = "Usuário desativado com sucesso"),
      @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
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
   * Endpoint para remoção de um usuário.
   *
   * <p>
   * Retorna HTTP 204 (No Content) em caso de sucesso.
   * </p>
   *
   * <p>
   * <b>Possíveis erros:</b>
   * </p>
   * <ul>
   * <li>404 → usuário não encontrado</li>
   * <li>409 → violação de integridade</li>
   * </ul>
   *
   * @param id identificador do usuário
   * @return resposta sem conteúdo
   */
  @Operation(summary = "Remove um usuário", description = "Exige Bearer Token. Acesso restrito a ADMIN.", security = @SecurityRequirement(name = "security"), responses = {
      @ApiResponse(responseCode = "204", description = "Usuário deletado com sucesso"),
      @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Violação de integridade - existem entidades relacionadas", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
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