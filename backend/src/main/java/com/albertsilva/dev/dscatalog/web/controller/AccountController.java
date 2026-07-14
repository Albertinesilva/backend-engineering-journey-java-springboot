package com.albertsilva.dev.dscatalog.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.albertsilva.dev.dscatalog.dto.user.request.PasswordResetRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserEmailRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.service.AccountService;
import com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails;
import com.albertsilva.dev.dscatalog.web.exception.response.ValidationError;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;

@Tag(name = "Conta", description = "Operações públicas e autenticadas relacionadas ao ciclo de vida da conta do usuário.")
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

  private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @Operation(summary = "Registra um novo usuário", description = "Cria uma nova conta de usuário, envia o e-mail de confirmação e mantém a conta desativada até a confirmação do token recebido por e-mail.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "409", description = "Conflito de dados", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRegisterRequest userRegisterRequest)
      throws MessagingException {

    logger.debug("Recebendo requisição de registro. email={}", userRegisterRequest.email());
    UserResponse response = accountService.register(userRegisterRequest);

    logger.info("Usuário criado com sucesso. id: {}", response.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "Ativa uma conta de usuário", description = "Ativa a conta do usuário a partir do token recebido por e-mail. Se o token for inválido ou expirado, a operação retorna erro.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Conta ativada com sucesso"),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping("/activate")
  public ResponseEntity<Void> activateAccount(@RequestParam String token) {
    accountService.confirmEmail(token);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Reenvia o e-mail de ativação", description = "Gera um novo token de ativação e envia um novo e-mail de confirmação para usuários que ainda não ativaram a conta.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "E-mail de ativação reenviado com sucesso"),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping("/resend-activation")
  public ResponseEntity<Void> resendActivationEmail(@Valid @RequestBody UserEmailRequest request)
      throws MessagingException {

    logger.info("Solicitação de reenvio de ativação recebida. email={}", request.email());
    accountService.resendActivationEmail(request.email());
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Solicita recuperação de senha", description = "Gera um token de recuperação de senha e envia as instruções para o e-mail informado.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Solicitação de recuperação recebida com sucesso"),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping("/password-recovery")
  public ResponseEntity<Void> requestPasswordRecovery(@Valid @RequestBody UserEmailRequest request) {
    logger.info("Solicitação de recuperação de senha recebida. email={}", request.email());

    accountService.requestPasswordRecovery(request.email());

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Redefine a senha", description = "Redefine a senha de um usuário a partir do token de recuperação enviado por e-mail.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Senha redefinida com sucesso"),
      @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
    logger.info("Solicitação de redefinição de senha recebida");

    accountService.resetPassword(request.token(), request.password());

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Desativa a conta do usuário", description = "Desativa a conta do usuário autenticado. A conta desativada não poderá mais ser utilizada para autenticação nem para acesso aos recursos protegidos.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Conta desativada com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
  @PostMapping("/deactivate")
  public ResponseEntity<Void> deactivateAccount() {
    logger.info("Solicitação de desativação de conta recebida");

    accountService.deactivateAccount();

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Obtém o usuário autenticado", description = "Retorna os dados do usuário atualmente autenticado no sistema. Requer autenticação com Bearer Token.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Usuário retornado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getAuthenticatedUser() {
    UserResponse response = accountService.getAuthenticatedUser();
    return ResponseEntity.ok(response);
  }

}
