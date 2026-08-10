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
import com.albertsilva.dev.dscatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.mapper.user.UserMapper;
import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.security.auth.AuthenticatedUserService;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.mail.MessagingException;

/**
 * Serviço responsável pelo gerenciamento do ciclo de vida das contas
 * de usuário.
 *
 * <p>
 * Centraliza os fluxos relacionados a registro de usuários,
 * ativação de conta por e-mail, reenvio de e-mail de ativação
 * e recuperação de senha.
 * </p>
 *
 * <p>
 * Também coordena a integração entre usuários, tokens de segurança
 * e serviços de envio de e-mail.
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
   * Registra uma nova conta de usuário.
   *
   * <p>
   * Durante o processo de registro:
   * </p>
   *
   * <ol>
   * <li>Obtém a role padrão do usuário;</li>
   * <li>Cria a entidade {@link User};</li>
   * <li>Codifica a senha utilizando o {@link PasswordEncoder};</li>
   * <li>Marca a conta como inativa;</li>
   * <li>Persiste o usuário no banco de dados;</li>
   * <li>Gera um token de ativação;</li>
   * <li>Envia um e-mail de confirmação da conta.</li>
   * </ol>
   *
   * @param request dados necessários para registro da conta
   * @return dados do usuário criado
   * @throws MessagingException    caso ocorra falha durante o envio do e-mail
   * @throws IllegalStateException caso a role padrão não esteja cadastrada
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
   * Confirma o cadastro de uma conta por meio de um token de ativação.
   *
   * <p>
   * O token informado deve:
   * </p>
   *
   * <ul>
   * <li>Existir no sistema;</li>
   * <li>Ser do tipo {@link TokenType#ACTIVATION};</li>
   * <li>Estar ativo e dentro do prazo de validade.</li>
   * </ul>
   *
   * <p>
   * Após a validação, a conta do usuário é ativada e o token é
   * invalidado para impedir reutilização.
   * </p>
   *
   * @param tokenValue valor do token recebido pelo usuário
   * @throws InvalidTokenException caso o token seja inválido,
   *                               expirado ou incompatível com o fluxo de
   *                               ativação
   */
  @Transactional
  public void confirmEmail(String tokenValue) {

    Token token = tokenService.findAndValidateToken(tokenValue, TokenType.ACTIVATION);

    User user = token.getUser();

    user.activate();

    token.disable();
  }

  /**
   * Solicita a recuperação de senha para uma conta existente.
   *
   * <p>
   * Caso o usuário seja encontrado:
   * </p>
   *
   * <ol>
   * <li>Um token de recuperação é gerado;</li>
   * <li>Um e-mail contendo instruções de recuperação é enviado.</li>
   * </ol>
   *
   * @param email endereço de e-mail associado à conta
   * @throws ResourceNotFoundException caso não exista usuário
   *                                   cadastrado com o e-mail informado
   */
  @Transactional
  public void requestPasswordRecovery(String email) {

    userRepository.findByEmail(email).ifPresent(user -> {

      Token token = tokenService.createPasswordRecoveryToken(user);
      emailService.sendPasswordRecoveryEmailAsync(user, token.getToken());
    });
  }

  /**
   * Reenvia um novo e-mail de ativação para uma conta ainda não ativada.
   *
   * <p>
   * Antes da geração de um novo token:
   * </p>
   *
   * <ul>
   * <li>Todos os tokens de ativação ativos do usuário são invalidados;</li>
   * <li>Um novo token de ativação é criado;</li>
   * <li>Um novo e-mail de confirmação é enviado.</li>
   * </ul>
   *
   * <p>
   * Caso o usuário não exista ou a conta já esteja ativada,
   * nenhuma ação será executada.
   * </p>
   *
   * @param email endereço de e-mail da conta
   * @throws MessagingException caso ocorra falha no envio do e-mail
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

  @Transactional
  public void resetPassword(String tokenValue, String password) {

    Token token = tokenService.findAndValidateToken(tokenValue, TokenType.PASSWORD_RECOVERY);

    User user = token.getUser();

    user.setPassword(passwordEncoder.encode(password));

    token.disable();

    userRepository.save(user);
  }

  @Transactional
  public UserResponse updateAuthenticatedUser(AuthenticatedUserUpdateRequest request) {

    User user = authenticatedUserService.getAuthenticatedUser();

    user.setFirstName(request.firstName().trim());
    user.setLastName(request.lastName().trim());
    user.setEmail(request.email().trim().toLowerCase());

    user = userRepository.save(user);

    return userMapper.toResponse(user);
  }

  @Transactional(readOnly = true)
  public UserResponse getAuthenticatedUser() {

    User user = authenticatedUserService.getAuthenticatedUser();

    return userMapper.toResponse(user);
  }

  public void deactivateAccount() {
    throw new UnsupportedOperationException("Unimplemented method 'deactivateAccount'");
  }
}
