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
 * Controller responsável pelas operações administrativas de usuários da API.
 */
@Tag(name = "Usuários", description = "Operações administrativas para gerenciamento de usuários do sistema.")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

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

  @Operation(summary = "Busca um usuário pelo ID", description = "Retorna os dados completos de um usuário existente. Requer autenticação com Bearer Token e permissão ADMIN ou OPERATOR para o próprio usuário.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Usuário encontrado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDetailsResponse.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "404", description = "Recurso não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping(value = "/{id}")
  @PreAuthorize("hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)")
  public ResponseEntity<UserDetailsResponse> findById(@PathVariable Long id) {
    logger.debug("Buscando usuário por id: {}", id);

    UserDetailsResponse response = userService.findById(id);

    logger.debug("Usuário encontrado: id={}", id);
    return ResponseEntity.ok(response);
  }

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