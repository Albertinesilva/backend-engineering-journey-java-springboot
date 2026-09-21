package com.albertsilva.dev.dscatalog.service;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.Role;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.dto.user.request.AuthenticatedUserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.PasswordUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.mapper.user.UserMapper;
import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.security.auth.AuthenticatedUserService;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.dscatalog.service.exception.PasswordUpdateException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;

/**
 * Serviço de aplicação dos fluxos de <b>conta do próprio usuário</b>, chamados
 * por {@code AccountController}. Os casos de uso administrativos sobre
 * usuários ficam em {@code UserService}.
 *
 * <p>
 * <b>Fluxos por e-mail (sem usuário autenticado):</b>
 * {@link #register(UserRegisterRequest)},
 * {@link #confirmEmail(String)}, {@link #resendActivationEmail(String)},
 * {@link #requestPasswordRecovery(String)} e
 * {@link #resetPassword(String, String)}. Coordenam {@code UserRepository},
 * {@code TokenService} (tokens de ativação/recuperação) e {@code EmailService}
 * (envio do e-mail).
 * </p>
 *
 * <p>
 * <b>Fluxos do usuário autenticado:</b>
 * {@link #updateAuthenticatedUser(AuthenticatedUserUpdateRequest)},
 * {@link #updatePassword(PasswordUpdateRequest)} e
 * {@link #getAuthenticatedUser()}. A identidade vem de
 * {@code AuthenticatedUserService}, que lê o claim {@code userId} do JWT
 * presente no {@code SecurityContext}; nenhum desses métodos recebe o id do
 * usuário como parâmetro.
 * </p>
 *
 * <p>
 * <b>Não implementado:</b> {@link #deactivateAccount()} apenas lança
 * {@link UnsupportedOperationException}.
 * </p>
 *
 * <p>
 * <b>Transações:</b> todos os métodos, exceto {@code deactivateAccount}, são
 * transacionais (somente {@code getAuthenticatedUser} é {@code readOnly}).
 * {@code TokenService} participa da mesma transação. Em {@code confirmEmail}
 * e {@code updatePassword}, as alterações em entidades gerenciadas são gravadas
 * no commit, sem {@code save} explícito.
 * </p>
 *
 * <p>
 * <b>Autorização:</b> não é feita aqui (controllers e configuração de
 * segurança). <b>E-mails:</b> o retorno dos métodos {@code ...Async} de
 * {@code EmailService} ({@code CompletableFuture}) é ignorado, então falhas de
 * envio não chegam a este service.
 * </p>
 */
@Service
public class AccountService {

  private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final EmailService emailService;
  private final AuthenticatedUserService authenticatedUserService;

  /**
   * Construtor para injeção de dependências.
   *
   * @param userRepository  repositório de usuários
   * @param roleRepository  repositório de roles
   * @param userMapper      responsável pela conversão entre DTOs e entidades
   * @param passwordEncoder responsável pela codificação de senhas
   * @param tokenService    serviço de gerenciamento de tokens
   * @param emailService    serviço de envio de e-mails
   */
  public AccountService(UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper,
      PasswordEncoder passwordEncoder, TokenService tokenService, EmailService emailService,
      AuthenticatedUserService authenticatedUserService) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.emailService = emailService;
    this.authenticatedUserService = authenticatedUserService;
  }

  /**
   * Registra uma conta de usuário comum, inativa até a confirmação do e-mail.
   *
   * <p>
   * Sequência:
   * </p>
   * <ol>
   * <li>busca a role fixa {@code ROLE_OPERATOR};</li>
   * <li>o mapper cria o {@link User} a partir do request, apenas com essa role
   * (nome e e-mail como recebidos, sem normalização);</li>
   * <li>codifica a senha e chama {@code deactivate()};</li>
   * <li>salva o usuário;</li>
   * <li>cria um token de tipo {@link TokenType#ACTIVATION} por
   * {@code TokenService};</li>
   * <li>chama {@code EmailService.sendActivationEmailAsync} (nome, e-mail e
   * valor do token) e descarta o {@code CompletableFuture} retornado;</li>
   * <li>devolve o {@link UserResponse}.</li>
   * </ol>
   *
   * <p>
   * <b>Falhas no envio do e-mail:</b> são capturadas por {@code EmailService}
   * e devolvidas apenas no futuro descartado; não interrompem o registro nem
   * revertem a transação. Apesar de {@code throws MessagingException} na
   * assinatura, nada no corpo do método lança essa exceção verificada (o método
   * de envio chamado não a declara). Como não há {@code @EnableAsync} no
   * projeto, o envio provavelmente ocorre na própria thread e dentro da
   * transação, antes do commit.
   * </p>
   *
   * <p>
   * Validações fora deste método: unicidade e formato do e-mail (inclui
   * consulta DNS), força da senha e dados pessoais na senha, feitas nos
   * validators do DTO.
   * </p>
   *
   * @param request dados do registro
   * @return dados do usuário criado (inativo)
   * @throws MessagingException    declarada na assinatura, mas não lançada pela
   *                               implementação atual
   * @throws IllegalStateException se a role {@code ROLE_OPERATOR} não existir
   */
  @Transactional
  public UserResponse register(UserRegisterRequest request) throws MessagingException {

    logger.debug("Registrando novo usuário. email: {}", request.email());

    Role operator = roleRepository.findByAuthority("ROLE_OPERATOR")
        .orElseThrow(() -> new IllegalStateException("Role 'ROLE_OPERATOR' não encontrada no banco de dados"));
    User user = userMapper.toEntity(request, Set.of(operator));
    user.setPassword(passwordEncoder.encode(request.password()));
    user.deactivate();

    user = userRepository.save(user);

    Token activationToken = tokenService.createActivationToken(user);

    emailService.sendActivationEmailAsync(user.getFirstName(), user.getEmail(), activationToken.getToken());

    logger.info("Usuário registrado com sucesso. id: {}", user.getId());

    return userMapper.toResponse(user);
  }

  /**
   * Ativa a conta associada a um token de ativação.
   *
   * <p>
   * Sequência: {@code TokenService.findAndValidateToken(valor, ACTIVATION)};
   * obtém o usuário do token (carregamento tardio, dentro da transação);
   * {@code user.activate()}; {@code token.disable()}. Não há {@code save}
   * explícito: as alterações são gravadas no commit. O método não verifica se a
   * conta já estava ativa.
   * </p>
   *
   * @param tokenValue valor do token recebido pelo usuário
   * @throws ResourceNotFoundException se não existir token com esse valor
   *                                   ({@code error.token.notFound})
   * @throws InvalidTokenException     se o token não for de ativação, estiver
   *                                   desabilitado ou tiver expirado (lançada
   *                                   por {@code Token.validate})
   */
  @Transactional
  public void confirmEmail(String tokenValue) {

    Token token = tokenService.findAndValidateToken(tokenValue, TokenType.ACTIVATION);

    User user = token.getUser();

    user.activate();

    token.disable();
  }

  /**
   * Inicia a recuperação de senha sem revelar se o e-mail existe.
   *
   * <p>
   * Busca o usuário por {@code findByEmail} (comparação exata). Se existir,
   * cria um token de tipo {@link TokenType#PASSWORD_RECOVERY} e chama
   * {@code EmailService.sendPasswordRecoveryEmailAsync} (retorno descartado). Se
   * não existir, <b>nada acontece</b> e nenhuma exceção é lançada, de modo que o
   * chamador observa o mesmo resultado nos dois casos.
   * </p>
   *
   * <p>
   * O método não verifica se a conta está ativa e não desabilita tokens de
   * recuperação anteriores ({@code TokenService.disableAllPasswordRecoveryTokens}
   * existe, mas não é chamado): várias solicitações deixam vários tokens
   * utilizáveis ao mesmo tempo.
   * </p>
   *
   * @param email e-mail da conta, comparado exatamente como gravado
   */
  @Transactional
  public void requestPasswordRecovery(String email) {

    userRepository.findByEmail(email).ifPresent(user -> {

      Token token = tokenService.createPasswordRecoveryToken(user);
      emailService.sendPasswordRecoveryEmailAsync(user, token.getToken());
    });
  }

  /**
   * Reenvia o e-mail de ativação para uma conta ainda inativa.
   *
   * <p>
   * Busca o usuário por {@code findByEmail} (comparação exata). Se não
   * existir, ou se já estiver ativo, retorna sem fazer nada e sem exceção.
   * Caso contrário: desabilita os tokens de ativação ainda não desabilitados
   * do usuário (inclusive vencidos), cria um novo token de ativação e chama
   * {@code EmailService.sendActivationEmailAsync} (retorno descartado).
   * </p>
   *
   * @param email e-mail da conta, comparado exatamente como gravado
   * @throws MessagingException declarada na assinatura, mas não lançada pela
   *                            implementação atual
   */
  @Transactional
  public void resendActivationEmail(String email) throws MessagingException {

    Optional<User> entity = userRepository.findByEmail(email);

    if (entity.isEmpty()) {
      return;
    }

    User user = entity.get();

    if (user.isActive()) {
      return;
    }

    tokenService.disableAllActivationTokens(user);

    Token token = tokenService.createActivationToken(user);

    emailService.sendActivationEmailAsync(user.getFirstName(), user.getEmail(), token.getToken());
  }

  /**
   * Redefine a senha do usuário a partir de um token de recuperação.
   *
   * <p>
   * Sequência: {@code TokenService.findAndValidateToken(valor,
   * PASSWORD_RECOVERY)}; obtém o usuário do token; grava a senha codificada;
   * desabilita o token; {@code userRepository.save(user)}.
   * </p>
   *
   * <p>
   * O método não ativa a conta, não desabilita outros tokens de recuperação do
   * mesmo usuário e não interage com tokens OAuth2 já emitidos. A força da
   * senha é validada no DTO; não há comparação com a senha atual.
   * </p>
   *
   * @param tokenValue valor do token recebido pelo usuário
   * @param password   nova senha (em texto)
   * @throws ResourceNotFoundException se não existir token com esse valor
   * @throws InvalidTokenException     se o token não for de recuperação de senha,
   *                                   estiver desabilitado ou tiver expirado
   */
  @Transactional
  public void resetPassword(String tokenValue, String password) {

    Token token = tokenService.findAndValidateToken(tokenValue, TokenType.PASSWORD_RECOVERY);

    User user = token.getUser();

    user.setPassword(passwordEncoder.encode(password));

    token.disable();

    userRepository.save(user);
  }

  /**
   * Atualiza nome, sobrenome e e-mail do usuário autenticado.
   *
   * <p>
   * <b>Identidade:</b> vem de {@code AuthenticatedUserService
   * .getAuthenticatedUser()}, que lê o claim {@code userId} do JWT no
   * {@code SecurityContext} e carrega o usuário por id. O método não recebe um
   * id, então só atua sobre o usuário do token.
   * </p>
   *
   * <p>
   * Aplica {@code trim} ao nome e ao sobrenome e {@code trim} + minúsculas ao
   * e-mail (é o único fluxo do sistema em que o e-mail é normalizado ao gravar),
   * salva e devolve o {@link UserResponse}. A unicidade do novo e-mail é
   * validada antes, pelo DTO ({@code @UniqueEmailForAuthenticatedUser}). Não há
   * confirmação do novo e-mail, o indicador {@code active} não muda e nenhum
   * token é reemitido; portanto o claim {@code username} de um JWT já emitido
   * continua com o e-mail antigo.
   * </p>
   *
   * @param request novos dados do perfil
   * @return dados atualizados do usuário
   * @throws com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException
   *         se não houver JWT válido, claim {@code userId} ou usuário
   *         correspondente
   */
  @Transactional
  public UserResponse updateAuthenticatedUser(AuthenticatedUserUpdateRequest request) {

    User user = authenticatedUserService.getAuthenticatedUser();

    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setEmail(request.email().trim().toLowerCase());

    user = userRepository.save(user);

    return userMapper.toResponse(user);
  }

  /**
   * Altera a senha do usuário autenticado.
   *
   * <p>
   * Passos, nesta ordem (as verificações de senha lançam
   * {@link PasswordUpdateException}):
   * </p>
   * <ol>
   * <li>{@code newPassword} igual a {@code confirmPassword}
   * ({@code error.account.password.confirmationMismatch});</li>
   * <li>usuário autenticado carregado por {@code AuthenticatedUserService};</li>
   * <li>{@code currentPassword} confere com o hash atual
   * ({@code error.account.password.currentInvalid});</li>
   * <li>a nova senha é diferente da atual
   * ({@code error.account.password.sameAsCurrent}).</li>
   * </ol>
   *
   * <p>
   * Em seguida grava a senha codificada, sem {@code save} explícito (a alteração
   * da entidade gerenciada é gravada no commit). Tamanho e força da senha são
   * validados antes, no DTO. Tokens OAuth2 já emitidos não são afetados por este
   * método.
   * </p>
   *
   * @param request senha atual, nova senha e confirmação
   * @throws PasswordUpdateException se alguma verificação falhar
   * @throws com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException
   *         se não houver JWT válido, claim {@code userId} ou usuário
   *         correspondente
   */
  @Transactional
  public void updatePassword(PasswordUpdateRequest request) {

    validatePasswordConfirmation(request);

    User user = authenticatedUserService.getAuthenticatedUser();

    validateCurrentPassword(request.currentPassword(), user);

    validateNewPassword(request.newPassword(), user);

    user.setPassword(passwordEncoder.encode(request.newPassword()));
  }

  /**
   * Devolve os dados do usuário autenticado (o do JWT da requisição), incluindo
   * suas roles. As roles são carregadas sob demanda durante a conversão, dentro
   * da transação de leitura.
   *
   * @return dados do usuário autenticado
   * @throws com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException
   *         se não houver JWT válido, claim {@code userId} ou usuário
   *         correspondente
   */
  @Transactional(readOnly = true)
  public UserResponse getAuthenticatedUser() {

    User user = authenticatedUserService.getAuthenticatedUser();

    return userMapper.toResponse(user);
  }

  /**
   * <b>Não implementado.</b> Sempre lança {@link UnsupportedOperationException}
   * (mensagem "Unimplemented method 'deactivateAccount'"), sem transação e sem
   * efeito sobre os dados. É chamado por {@code AccountController}, que publica o
   * endpoint {@code POST /api/v1/accounts/deactivate}.
   *
   * @throws UnsupportedOperationException sempre
   */
  public void deactivateAccount() {
    throw new UnsupportedOperationException("Unimplemented method 'deactivateAccount'");
  }

  /**
   * Exige que {@code newPassword} e {@code confirmPassword} sejam iguais.
   *
   * @throws PasswordUpdateException ({@code error.account.password.confirmationMismatch})
   */
  private void validatePasswordConfirmation(PasswordUpdateRequest request) {

    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new PasswordUpdateException("error.account.password.confirmationMismatch");
    }
  }

  /**
   * Exige que a senha informada confira, via {@code PasswordEncoder.matches},
   * com o hash guardado no usuário.
   *
   * @throws PasswordUpdateException ({@code error.account.password.currentInvalid})
   */
  private void validateCurrentPassword(String currentPassword, User user) {

    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new PasswordUpdateException("error.account.password.currentInvalid");
    }
  }

  /**
   * Exige que a nova senha seja diferente da atual (comparada com o hash por
   * {@code PasswordEncoder.matches}).
   *
   * @throws PasswordUpdateException ({@code error.account.password.sameAsCurrent})
   */
  private void validateNewPassword(String newPassword, User user) {

    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      throw new PasswordUpdateException("error.account.password.sameAsCurrent");
    }
  }

}
