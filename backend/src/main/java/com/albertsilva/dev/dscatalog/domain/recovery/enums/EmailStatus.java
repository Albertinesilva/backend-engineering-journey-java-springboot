package com.albertsilva.dev.dscatalog.domain.recovery.enums;

/**
 * Situação de um registro de e-mail
 * ({@link com.albertsilva.dev.dscatalog.domain.recovery.Email}).
 *
 * <p>
 * É persistido pelo nome da constante ({@code @Enumerated(EnumType.STRING)}) na
 * coluna {@code tb_email.status}, cuja restrição {@code check} (migration
 * {@code V011}) aceita exatamente os valores declarados aqui.
 * </p>
 *
 * <p>
 * O enum apenas descreve os estados possíveis; nenhuma regra de transição entre
 * eles é imposta pelo domínio.
 * </p>
 */
public enum EmailStatus {

  /**
   * Email criado e aguardando envio.
   */
  PENDING,

  /**
   * Email enviado com sucesso.
   */
  SENT,

  /**
   * Falha durante o envio.
   */
  ERROR
}
