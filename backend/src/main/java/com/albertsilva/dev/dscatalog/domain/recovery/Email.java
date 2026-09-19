package com.albertsilva.dev.dscatalog.domain.recovery;

import java.io.Serializable;
import java.time.Instant;

import com.albertsilva.dev.dscatalog.domain.recovery.enums.EmailStatus;
import com.albertsilva.dev.dscatalog.dto.email.request.EmailRegisterRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro persistido de um e-mail que o sistema enviou (ou tentou enviar).
 *
 * <p>
 * A entidade guarda apenas os dados do registro: remetente, destinatário,
 * conteúdo, data de criação e {@link EmailStatus}. Ela não realiza o envio, que
 * é responsabilidade de outra camada, e não possui relacionamento JPA com
 * outras entidades (o destinatário é guardado como texto, sem vínculo com
 * {@code User}).
 * </p>
 *
 * <p>
 * <b>Regras e invariantes:</b>
 * </p>
 * <ul>
 * <li>Remetente, destinatário, conteúdo, data de criação e status são
 * obrigatórios no mapeamento; a entidade não valida o formato dos endereços
 * (isso é feito no DTO {@code EmailRegisterRequest})</li>
 * <li>Um registro criado por {@link #Email(EmailRegisterRequest)} nasce com
 * {@code createdAt} igual ao instante atual e status
 * {@link EmailStatus#PENDING}</li>
 * <li>Não há callbacks JPA nem controle de transição de status: qualquer
 * mudança depende de chamada explícita a {@link #setStatus(EmailStatus)}</li>
 * <li>A identidade é definida somente pelo {@code id} (ver
 * {@link #equals(Object)})</li>
 * </ul>
 *
 * <p>
 * <b>Mapeamento:</b>
 * </p>
 * <ul>
 * <li>Tabela: {@code tb_email}; o conteúdo é uma coluna {@code TEXT}</li>
 * <li>O status é persistido pelo nome da constante de {@link EmailStatus}</li>
 * </ul>
 */
@Entity
@Table(name = "tb_email")
public class Email implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Endereço do remetente registrado (obrigatório). */
  @Column(nullable = false)
  private String sender;

  /** Endereço do destinatário registrado (obrigatório). */
  @Column(nullable = false)
  private String recipient;

  /**
   * Texto registrado para o e-mail (obrigatório, coluna {@code TEXT}). A
   * entidade não impõe formato nem tamanho.
   */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  /**
   * Instante de criação do registro (obrigatório). É definido apenas pelo
   * construtor {@link #Email(EmailRegisterRequest)}; não há callback JPA, então
   * uma instância criada de outro modo precisa recebê-lo por
   * {@link #setCreatedAt(Instant)} antes de ser persistida.
   */
  @Column(nullable = false, columnDefinition = "TIMESTAMP WITHOUT TIME ZONE")
  private Instant createdAt;

  /**
   * Situação do registro (obrigatória). Ver {@link EmailStatus}.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmailStatus status;

  /**
   * Cria um registro vazio, com todos os campos nulos. Construtor sem argumentos
   * exigido pela JPA: {@code createdAt} e {@code status}, que são obrigatórios,
   * não são preenchidos aqui.
   */
  public Email() {
  }

  /**
   * Cria um registro a partir dos dados de um {@link EmailRegisterRequest}.
   *
   * <p>
   * Copia remetente, destinatário e conteúdo; define {@code createdAt} como o
   * instante atual e o status como {@link EmailStatus#PENDING}. Os valores não
   * são validados por este construtor.
   * </p>
   *
   * @param request dados do e-mail a registrar
   * @throws NullPointerException se {@code request} for {@code null}
   */
  public Email(EmailRegisterRequest request) {
    this.sender = request.sender();
    this.recipient = request.recipient();
    this.content = request.content();
    this.createdAt = Instant.now();
    this.status = EmailStatus.PENDING;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public String getRecipient() {
    return recipient;
  }

  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public EmailStatus getStatus() {
    return status;
  }

  public void setStatus(EmailStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  /**
   * Compara dois registros pelo identificador ({@code id}).
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
    Email other = (Email) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

}
