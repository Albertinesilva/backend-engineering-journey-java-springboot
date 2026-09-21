package com.albertsilva.dev.dscatalog.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.dscatalog.domain.recovery.Token;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.repository.TokenRepository;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

/**
 * Serviço que cria, valida e desabilita os {@link Token} usados nos fluxos de
 * ativação de conta e de recuperação de senha. É chamado apenas por
 * {@code AccountService}; usa somente {@code TokenRepository}.
 *
 * <p>
 * <b>Transações:</b> a anotação {@code @Transactional} está na classe, então
 * todos os métodos públicos são transacionais (escrita) e, quando chamados por
 * {@code AccountService}, participam da transação dele. Os métodos que
 * desabilitam tokens não chamam {@code save}: dependem da entidade
 * gerenciada ser gravada no commit.
 * </p>
 *
 * <p>
 * <b>Regras de validade:</b> a decisão de aceitar um token é do domínio
 * ({@code Token.validate(TokenType)}: tipo, desabilitado, expirado), e este
 * service apenas a aciona em {@link #findAndValidateToken(String, TokenType)}.
 * {@code Token.isValid()} não é usado por nenhum código de produção.
 * </p>
 *
 * <p>
 * <b>Configuração:</b> validade dos tokens lida das propriedades
 * {@code account.activation.token.hours} (ativação) e
 * {@code account.password-recovery.token.minutes} (recuperação).
 * </p>
 */
@Service
@Transactional
public class TokenService {

  @Value("${account.activation.token.hours}")
  private Long activationTokenExpirationHours;

  @Value("${account.password-recovery.token.minutes}")
  private Long passwordRecoveryTokenExpirationMinutes;

  private final TokenRepository tokenRepository;

  public TokenService(TokenRepository tokenRepository) {
    this.tokenRepository = tokenRepository;
  }

  /**
   * Cria e persiste um token de ativação para o usuário, com UUID aleatório e
   * validade de {@code account.activation.token.hours} horas
   * ({@code Token.activationToken}). Não desabilita tokens anteriores.
   *
   * @param user usuário dono do token (deve estar persistido)
   * @return token salvo
   */
  public Token createActivationToken(User user) {
    return tokenRepository.save(Token.activationToken(user, activationTokenExpirationHours));
  }

  /**
   * Cria e persiste um token de recuperação de senha para o usuário, com UUID
   * aleatório e validade de {@code account.password-recovery.token.minutes}
   * minutos ({@code Token.passwordRecoveryToken}). Não desabilita tokens
   * anteriores.
   *
   * @param user usuário dono do token (deve estar persistido)
   * @return token salvo
   */
  public Token createPasswordRecoveryToken(User user) {
    return tokenRepository.save(Token.passwordRecoveryToken(user, passwordRecoveryTokenExpirationMinutes));
  }

  /**
   * Desabilita todos os tokens de ativação do usuário que ainda não estão
   * desabilitados. A consulta não considera a expiração, então tokens vencidos
   * também são desabilitados. Chamado por
   * {@code AccountService.resendActivationEmail}.
   *
   * @param user usuário dono dos tokens
   */
  public void disableAllActivationTokens(User user) {

    List<Token> tokens = tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.ACTIVATION);

    tokens.forEach(Token::disable);
  }

  /**
   * Desabilita todos os tokens de recuperação de senha do usuário que ainda não
   * estão desabilitados (inclusive os vencidos). Atualmente nenhum código de
   * {@code src/main} chama este método.
   *
   * @param user usuário dono dos tokens
   */
  public void disableAllPasswordRecoveryTokens(User user) {

    List<Token> tokens = tokenRepository.findByUserAndTypeAndDisabledFalse(user, TokenType.PASSWORD_RECOVERY);

    tokens.forEach(Token::disable);
  }

  /**
   * Busca um token pelo valor e o valida para a finalidade informada.
   *
   * <p>
   * Sequência: {@code TokenRepository.findByToken}; se não existir, lança
   * {@link ResourceNotFoundException}; depois chama
   * {@code Token.validate(type)}, que recusa tipo diferente, token
   * desabilitado ou expirado. O método <b>não</b> altera o token: quem o consome
   * deve desabilitá-lo em seguida (é o que {@code AccountService} faz).
   * </p>
   *
   * @param tokenValue valor do token recebido do usuário
   * @param type       finalidade esperada
   * @return token válido (o usuário dono tem carregamento tardio, e só pode ser
   *         lido enquanto a transação estiver aberta)
   * @throws ResourceNotFoundException ({@code error.token.notFound}) se não
   *                                   existir token com esse valor
   * @throws com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException
   *         se o tipo for diferente, o token estiver desabilitado ou tiver
   *         expirado
   */
  public Token findAndValidateToken(String tokenValue, TokenType type) {

    Token token = tokenRepository.findByToken(tokenValue)
        .orElseThrow(() -> new ResourceNotFoundException("error.token.notFound"));
    token.validate(type);
    return token;
  }
}
