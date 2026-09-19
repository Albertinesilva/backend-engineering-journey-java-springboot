package com.albertsilva.dev.dscatalog.domain.recovery;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.albertsilva.dev.dscatalog.domain.recovery.enums.TokenType;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Token de uso pontual associado a um {@link User}, utilizado nos fluxos de
 * ativação de conta e de recuperação de senha.
 *
 * <p>
 * O valor do token ({@code token}) é um UUID aleatório que o sistema entrega ao
 * usuário e que depois é apresentado de volta para comprovar a posse do
 * e-mail. Cada token tem uma finalidade ({@link TokenType}), uma data de
 * expiração absoluta e um indicador de desabilitação.
 * </p>
 *
 * <p>
 * <b>Regras e invariantes:</b>
 * </p>
 * <ul>
 * <li>Um token é utilizável enquanto não estiver desabilitado e não tiver
 * expirado (ver {@link #isValid()} e {@link #validate(TokenType)})</li>
 * <li>A entidade não desabilita o token sozinha após o uso: quem o consome
 * deve chamar {@link #disable()}. Além desse método, os setters públicos
 * {@code setDisabled} e {@code setExpireDate} permitem alterar o estado
 * diretamente</li>
 * <li>As fábricas {@link #activationToken(User, long)} e
 * {@link #passwordRecoveryToken(User, long)} geram o UUID e calculam a
 * expiração; o construtor público aceita esses valores prontos</li>
 * <li>{@code createdAt} é definido no construtor e a coluna não é atualizável
 * ({@code updatable = false})</li>
 * <li>A identidade é definida somente pelo {@code id} (ver
 * {@link #equals(Object)})</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: {@code tb_token}; o valor do token é único na tabela</li>
 * <li>Muitos-para-um com {@link User}, carregamento tardio
 * ({@code FetchType.LAZY}); {@code Token} é o lado dono, e a coluna
 * {@code user_id} é obrigatória. O lado inverso é {@code User.tokens}</li>
 * <li>O tipo é persistido pelo nome da constante de {@link TokenType}</li>
 * </ul>
 */
@Entity
@Table(name = "tb_token")
public class Token implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Valor do token: texto opaco (UUID quando criado pelas fábricas), único na
   * tabela. É este valor, e não o {@code id}, que circula fora do sistema.
   */
  @Column(nullable = false, unique = true)
  private String token;

  /**
   * Usuário ao qual o token pertence (obrigatório). O carregamento é tardio: a
   * instância só é lida do banco no primeiro acesso, e isso exige uma sessão
   * JPA ainda aberta.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * Instante de criação, definido pelo construtor. A coluna não é atualizável.
   */
  @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant createdAt;

  /**
   * Instante absoluto de expiração (obrigatório). Ver {@link #isExpired()}.
   */
  @Column(nullable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant expireDate;

  /**
   * Indica que o token foi invalidado explicitamente (por exemplo, depois de ser
   * consumido). Um token desabilitado nunca é considerado válido. Alterado por
   * {@link #disable()}.
   */
  @Column(nullable = false)
  private boolean disabled;

  /**
   * Finalidade do token, conferida em {@link #validate(TokenType)}.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TokenType type;

  /**
   * Construtor sem argumentos reservado à JPA ({@code protected}, não deve ser
   * usado diretamente pelo código da aplicação). Deixa todos os campos com o
   * valor padrão.
   */
  protected Token() {
  }

  /**
   * Cria um token com os valores informados.
   *
   * <p>
   * Define {@code createdAt} como o instante atual e {@code disabled} como
   * {@code false}. Os argumentos não são validados; um {@code expireDate}
   * nulo, por exemplo, só se manifestaria depois, em {@link #isExpired()}. Para
   * criar tokens completos (com UUID e expiração calculada), preferir as
   * fábricas estáticas.
   * </p>
   *
   * @param token      valor do token (normalmente um UUID)
   * @param user       usuário dono do token
   * @param expireDate instante de expiração
   * @param type       finalidade do token
   */
  public Token(String token, User user, Instant expireDate, TokenType type) {
    this.token = token;
    this.user = user;
    this.createdAt = Instant.now();
    this.expireDate = expireDate;
    this.type = type;
    this.disabled = false;
  }

  /**
   * Indica se o token expirou: é {@code true} quando {@code expireDate} é
   * estritamente anterior ao instante atual. No exato instante da expiração o
   * token ainda não é considerado expirado.
   *
   * <p>
   * Considera apenas a data; não leva em conta se o token foi desabilitado.
   * Pressupõe {@code expireDate} não nulo (caso contrário, lança
   * {@link NullPointerException}).
   * </p>
   *
   * @return {@code true} se o token já expirou
   */
  public boolean isExpired() {
    return expireDate.isBefore(Instant.now());
  }

  /**
   * Indica se o token está utilizável: não desabilitado e não expirado.
   *
   * <p>
   * Não verifica a finalidade ({@link TokenType}) e não lança exceção; para
   * validar um token para uma finalidade específica, com motivo da recusa, ver
   * {@link #validate(TokenType)}.
   * </p>
   *
   * @return {@code true} se o token não está desabilitado e não expirou
   */
  public boolean isValid() {
    return !disabled && !isExpired();
  }

  /**
   * Marca o token como desabilitado ({@code disabled = true}). Operação
   * idempotente; o domínio não oferece método para reabilitá-lo.
   *
   * <p>
   * O método apenas altera o objeto em memória, sem acessar o banco: a mudança é
   * gravada quando a entidade gerenciada é sincronizada pela JPA.
   * </p>
   */
  public void disable() {
    this.disabled = true;
  }

  /**
   * Cria um token de ativação de conta.
   *
   * <p>
   * O valor é um UUID aleatório e a expiração é o instante atual acrescido de
   * {@code expirationHours} horas. O token é apenas construído, não persistido, e
   * os argumentos não são validados: um número de horas zero ou negativo produz
   * um token que já nasce (ou quase) expirado.
   * </p>
   *
   * @param user            usuário dono do token
   * @param expirationHours validade do token, em horas, a partir de agora
   * @return novo token do tipo {@link TokenType#ACTIVATION}
   */
  public static Token activationToken(User user, long expirationHours) {
    return new Token(UUID.randomUUID().toString(), user,
        Instant.now().plus(expirationHours, ChronoUnit.HOURS), TokenType.ACTIVATION);
  }

  /**
   * Cria um token de recuperação de senha.
   *
   * <p>
   * O valor é um UUID aleatório e a expiração é o instante atual acrescido de
   * {@code expirationMinutes} minutos. O token é apenas construído, não
   * persistido, e os argumentos não são validados: um número de minutos zero ou
   * negativo produz um token que já nasce (ou quase) expirado.
   * </p>
   *
   * @param user              usuário dono do token
   * @param expirationMinutes validade do token, em minutos, a partir de agora
   * @return novo token do tipo {@link TokenType#PASSWORD_RECOVERY}
   */
  public static Token passwordRecoveryToken(User user, long expirationMinutes) {
    return new Token(UUID.randomUUID().toString(), user,
        Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES), TokenType.PASSWORD_RECOVERY);
  }

  /**
   * Valida o token para a finalidade esperada, lançando exceção com o motivo da
   * recusa.
   *
   * <p>
   * As verificações são feitas nesta ordem, e a primeira que falhar interrompe a
   * validação:
   * </p>
   * <ol>
   * <li>tipo diferente do esperado: mensagem {@code error.token.type.invalid}</li>
   * <li>token desabilitado: mensagem {@code error.token.disabled}</li>
   * <li>token expirado: mensagem {@code error.token.expired}</li>
   * </ol>
   *
   * <p>
   * As mensagens são chaves de internacionalização, e não textos finais. O
   * método não altera o token e não examina o estado do usuário dono.
   * </p>
   *
   * @param expectedType finalidade para a qual o token está sendo apresentado
   * @throws InvalidTokenException se o token não for válido para
   *                               {@code expectedType}
   */
  public void validate(TokenType expectedType) {

    if (type != expectedType) {
      throw new InvalidTokenException("error.token.type.invalid");
    }

    if (disabled) {
      throw new InvalidTokenException("error.token.disabled");
    }

    if (isExpired()) {
      throw new InvalidTokenException("error.token.expired");
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpireDate() {
    return expireDate;
  }

  public void setExpireDate(Instant expireDate) {
    this.expireDate = expireDate;
  }

  /**
   * Acessor do indicador de desabilitação. Repare que o nome é
   * {@code getDisabled} (e não {@code isDisabled}). Não equivale a
   * {@link #isValid()}, que também considera a expiração.
   *
   * @return {@code true} se o token foi desabilitado
   */
  public boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public TokenType getType() {
    return type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  /**
   * Compara dois tokens pelo identificador ({@code id}), e não pelo valor do
   * campo {@code token}.
   *
   * <p>
   * Duas instâncias sem {@code id} (ainda não persistidas) são consideradas
   * iguais entre si, e {@link #hashCode()} depende apenas do {@code id}. A
   * comparação exige a mesma classe exata ({@code getClass()}), e não apenas
   * compatibilidade de tipo.
   * </p>
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Token other = (Token) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

}