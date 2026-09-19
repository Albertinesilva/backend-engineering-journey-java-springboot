package com.albertsilva.dev.dscatalog.domain.recovery.enums;

/**
 * Finalidade de um {@link com.albertsilva.dev.dscatalog.domain.recovery.Token}.
 *
 * <p>
 * É persistido pelo nome da constante ({@code @Enumerated(EnumType.STRING)}) na
 * coluna {@code tb_token.type}, cuja restrição {@code check} (migration
 * {@code V009}) aceita exatamente os valores declarados aqui: uma nova
 * constante exigiria também uma nova migration.
 * </p>
 *
 * <p>
 * O tipo é conferido em {@code Token#validate(TokenType)}: um token só é aceito
 * para a finalidade para a qual foi emitido.
 * </p>
 */
public enum TokenType {
  /**
   * Token emitido para confirmar o e-mail e ativar a conta de um usuário recém
   * registrado.
   */
  ACTIVATION,
  /**
   * Token emitido para permitir a redefinição da senha de um usuário.
   */
  PASSWORD_RECOVERY
}
