package com.albertsilva.dev.dscatalog.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.albertsilva.dev.dscatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.PasswordResetRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.PasswordUpdateRequest;
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

/**
 * Endpoints da <b>conta do próprio usuário</b> ({@code /api/v1/accounts}), todos
 * delegados a {@code AccountService}. O controller não usa mappers nem
 * repositories: recebe o DTO, chama o service e monta o {@code ResponseEntity}.
 *
 * <p>
 * <b>Acesso:</b> na cadeia de segurança, {@code GET} e {@code POST} em
 * {@code /api/v1/accounts/**} são <b>públicos por URL</b>; {@code PUT} e
 * {@code PATCH} exigem autenticação. Por isso, nas rotas públicas por URL que
 * precisam de login ({@code POST /deactivate}, {@code GET /me}) a proteção vem
 * <b>somente</b> do {@code @PreAuthorize} do método.
 * </p>
 *
 * <p>
 * <b>Documentação OpenAPI:</b> as anotações {@code @Operation}/{@code @ApiResponses}
 * descrevem os endpoints, exceto {@code PATCH /me/password}, que não tem
 * nenhuma; os códigos declarados nem sempre coincidem com os que o código
 * produz (ver documentação de cada método).
 * </p>
 */
@Tag(name = "Conta", description = "Operações públicas e autenticadas relacionadas ao ciclo de vida da conta do usuário.")
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

  private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

  private final AccountService accountService;

  /**
   * @param accountService service dos fluxos de conta
   */
  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  /**
   * <b>{@code POST /api/v1/accounts/register}</b> — registro público de conta.
   *
   * <ul>
   * <li><b>Acesso:</b> público (regra de URL); sem {@code @PreAuthorize}.</li>
   * <li><b>Entrada:</b> {@link UserRegisterRequest} com {@code @Valid} (Bean
   * Validation antes do método: e-mail válido por DNS e único, senha forte sem
   * dados pessoais, nomes).</li>
   * <li><b>Fluxo:</b> {@code AccountService.register}: usuário inativo com
   * {@code ROLE_OPERATOR}, token de ativação e e-mail de confirmação.</li>
   * <li><b>Sucesso:</b> {@code 201 Created} com {@link UserResponse}; <b>não</b>
   * envia cabeçalho {@code Location}.</li>
   * <li><b>Erros:</b> {@code 422} (validação); {@code 409} (violação de unicidade
   * no banco, por exemplo em corrida); {@code 500} (por exemplo, role padrão
   * ausente). O {@code 400} declarado no OpenAPI não é produzido por este
   * endpoint.</li>
   * </ul>
   *
   * @param userRegisterRequest dados do registro
   * @return resposta {@code 201} com o usuário criado (inativo)
   * @throws MessagingException declarada, mas não lançada pela implementação atual
   */
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

  /**
   * <b>{@code GET /api/v1/accounts/activate?token=...}</b> — confirma o e-mail e
   * ativa a conta (operação que <b>altera estado usando {@code GET}</b>).
   *
   * <ul>
   * <li><b>Acesso:</b> público (regra de URL).</li>
   * <li><b>Entrada:</b> parâmetro de consulta {@code token} obrigatório; sem
   * {@code @Valid}. Se o parâmetro faltar, o Spring lança uma exceção que,
   * sem handler específico, vira {@code 500} (ver
   * {@code ControllerExceptionHandler}).</li>
   * <li><b>Fluxo:</b> {@code AccountService.confirmEmail}.</li>
   * <li><b>Sucesso:</b> {@code 204 No Content}.</li>
   * <li><b>Erros:</b> {@code 404} (token inexistente); {@code 400} (token de
   * outro tipo, desabilitado ou expirado).</li>
   * </ul>
   *
   * @param token valor do token de ativação recebido por e-mail
   * @return resposta {@code 204}
   */
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

  /**
   * <b>{@code POST /api/v1/accounts/resend-activation}</b> — reenvia o e-mail de
   * ativação.
   *
   * <ul>
   * <li><b>Acesso:</b> público (regra de URL).</li>
   * <li><b>Entrada:</b> {@link UserEmailRequest} com {@code @Valid} (e-mail
   * obrigatório e válido por DNS).</li>
   * <li><b>Fluxo:</b> {@code AccountService.resendActivationEmail}.</li>
   * <li><b>Sucesso:</b> {@code 204} <b>sempre</b>, inclusive quando o e-mail não
   * existe ou a conta já está ativa (o service não faz nada nesses casos).</li>
   * <li><b>Erros:</b> {@code 422} (validação).</li>
   * </ul>
   *
   * @param request e-mail da conta
   * @return resposta {@code 204}
   * @throws MessagingException declarada, mas não lançada pela implementação atual
   */
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

  /**
   * <b>{@code POST /api/v1/accounts/password-recovery}</b> — solicita a
   * recuperação de senha.
   *
   * <ul>
   * <li><b>Acesso:</b> público (regra de URL).</li>
   * <li><b>Entrada:</b> {@link UserEmailRequest} com {@code @Valid}.</li>
   * <li><b>Fluxo:</b> {@code AccountService.requestPasswordRecovery}: se o usuário
   * existir, cria token de recuperação e envia e-mail.</li>
   * <li><b>Sucesso:</b> {@code 204} <b>sempre</b> (não revela se o e-mail
   * existe).</li>
   * <li><b>Erros:</b> {@code 422} (validação).</li>
   * </ul>
   *
   * @param request e-mail da conta
   * @return resposta {@code 204}
   */
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

  /**
   * <b>{@code POST /api/v1/accounts/reset-password}</b> — redefine a senha com o
   * token de recuperação.
   *
   * <ul>
   * <li><b>Acesso:</b> público (regra de URL).</li>
   * <li><b>Entrada:</b> {@link PasswordResetRequest} com {@code @Valid} (token
   * obrigatório; senha com {@code @StrongPassword}, sem {@code @Size}). O
   * controller passa {@code token()} e {@code password()} ao service; o DTO não
   * avança além do controller. O log deste método não registra os campos.</li>
   * <li><b>Fluxo:</b> {@code AccountService.resetPassword}.</li>
   * <li><b>Sucesso:</b> {@code 204}.</li>
   * <li><b>Erros:</b> {@code 422} (validação); {@code 404} (token inexistente);
   * {@code 400} (token inválido, desabilitado ou expirado).</li>
   * </ul>
   *
   * @param request token e nova senha
   * @return resposta {@code 204}
   */
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

  /**
   * <b>{@code POST /api/v1/accounts/deactivate}</b> — <b>não implementado</b>.
   *
   * <ul>
   * <li><b>Acesso:</b> a regra de URL é pública ({@code POST} em
   * {@code /api/v1/accounts/**}); a proteção vem só de
   * {@code @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")}. Sem token, o método
   * é negado (resposta esperada {@code 403}, e não {@code 401}, a confirmar).</li>
   * <li><b>Fluxo:</b> {@code AccountService.deactivateAccount}, que apenas lança
   * {@link UnsupportedOperationException}.</li>
   * <li><b>Resultado real para um usuário autenticado:</b> {@code 500}
   * (tratado pelo handler genérico), com a exceção registrada em log. Nenhum dado
   * é alterado. O {@code 204} descrito no OpenAPI nunca é produzido.</li>
   * </ul>
   *
   * @return nunca retorna normalmente: a chamada ao service lança exceção
   */
  @Operation(summary = "Desativa a conta do usuário", description = "Desativa a conta do usuário autenticado. A conta desativada não poderá mais ser utilizada para autenticação nem para acesso aos recursos protegidos.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Conta desativada com sucesso"),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PostMapping("/deactivate")
  @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
  public ResponseEntity<Void> deactivateAccount() {
    logger.info("Solicitação de desativação de conta recebida");

    accountService.deactivateAccount();

    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code PUT /api/v1/accounts/me}</b> — atualiza nome, sobrenome e e-mail do
   * usuário do token.
   *
   * <ul>
   * <li><b>Acesso:</b> autenticado por regra de URL ({@code PUT} não é público) e
   * {@code @PreAuthorize("isAuthenticated()")} (redundante).</li>
   * <li><b>Entrada:</b> {@link AuthenticatedUserUpdateRequest} com {@code @Valid}
   * (e-mail com {@code @UniqueEmailForAuthenticatedUser}, que usa o usuário do
   * JWT). O request não tem id: o usuário vem do claim {@code userId}.</li>
   * <li><b>Fluxo:</b> {@code AccountService.updateAuthenticatedUser} (único ponto
   * em que o e-mail é gravado normalizado).</li>
   * <li><b>Sucesso:</b> {@code 200} com {@link UserResponse}.</li>
   * <li><b>Erros:</b> {@code 422} (validação); {@code 401} se a identidade do JWT
   * não puder ser resolvida.</li>
   * </ul>
   *
   * @param request novos dados do perfil
   * @return resposta {@code 200} com o usuário atualizado
   */
  @Operation(summary = "Atualiza os dados do usuário autenticado", description = "Atualiza nome, sobrenome e e-mail da conta autenticada. Requer autenticação com Bearer Token.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "422", description = "Erro de validação", content = @Content(schema = @Schema(implementation = ValidationError.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(schema = @Schema(implementation = ProblemDetails.class)))
  })
  @PutMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserResponse> updateAuthenticatedUser(
      @Valid @RequestBody AuthenticatedUserUpdateRequest request) {

    logger.info("Atualizando perfil do usuário autenticado");

    UserResponse response = accountService.updateAuthenticatedUser(request);

    logger.info("Perfil atualizado com sucesso");

    return ResponseEntity.ok(response);
  }

  /**
   * <b>{@code PATCH /api/v1/accounts/me/password}</b> — troca a senha do usuário
   * do token. É o único endpoint deste controller <b>sem anotações OpenAPI</b> e
   * sem {@code @PreAuthorize}.
   *
   * <ul>
   * <li><b>Acesso:</b> autenticado apenas pela regra de URL ({@code PATCH} não é
   * público).</li>
   * <li><b>Entrada:</b> {@link PasswordUpdateRequest} com {@code @Valid} (nova
   * senha e confirmação de 10 a 72 caracteres; nova senha forte).</li>
   * <li><b>Fluxo:</b> {@code AccountService.updatePassword} (confirmação igual,
   * senha atual correta, nova diferente da atual).</li>
   * <li><b>Sucesso:</b> {@code 204}.</li>
   * <li><b>Erros:</b> {@code 422} (validação ou {@code PasswordUpdateException});
   * {@code 401} se a identidade do JWT não puder ser resolvida.</li>
   * </ul>
   *
   * @param request senha atual, nova senha e confirmação
   * @return resposta {@code 204}
   */
  @PatchMapping("/me/password")
  public ResponseEntity<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {

    accountService.updatePassword(request);

    return ResponseEntity.noContent().build();
  }

  /**
   * <b>{@code GET /api/v1/accounts/me}</b> — devolve o usuário do token.
   *
   * <ul>
   * <li><b>Acesso:</b> a regra de URL é <b>pública</b> ({@code GET} em
   * {@code /api/v1/accounts/**}); a proteção vem só de
   * {@code @PreAuthorize("isAuthenticated()")}. Sem token, o método é negado
   * (resposta esperada {@code 403}, e não {@code 401}, a confirmar).</li>
   * <li><b>Fluxo:</b> {@code AccountService.getAuthenticatedUser}.</li>
   * <li><b>Sucesso:</b> {@code 200} com {@link UserResponse} (sem senha nem
   * {@code active}).</li>
   * <li><b>Erros:</b> {@code 401} se a identidade do JWT não puder ser
   * resolvida.</li>
   * </ul>
   *
   * @return resposta {@code 200} com os dados do usuário autenticado
   */
  @Operation(summary = "Obtém o usuário autenticado", description = "Retorna os dados do usuário atualmente autenticado no sistema. Requer autenticação com Bearer Token.", security = @SecurityRequirement(name = "security"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Usuário retornado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))),
      @ApiResponse(responseCode = "401", description = "Usuário não autenticado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class))),
      @ApiResponse(responseCode = "500", description = "Erro interno do servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProblemDetails.class)))
  })
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserResponse> getAuthenticatedUser() {
    UserResponse response = accountService.getAuthenticatedUser();
    return ResponseEntity.ok(response);
  }

}
