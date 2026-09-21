package com.albertsilva.dev.dscatalog.dto.email.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO <b>interno</b> com os dados de um e-mail a registrar em {@code tb_email};
 * não é corpo de nenhum endpoint.
 *
 * <p>
 * É criado por {@code EmailService.registerEmailLog} (após o envio) e consumido
 * pelo construtor {@code Email(EmailRegisterRequest)}, que copia os três campos
 * e define a data de criação e o status {@code PENDING}. Não há mapper.
 * </p>
 *
 * <p>
 * As anotações de Bean Validation ({@code @NotBlank}, {@code @Email}) estão
 * declaradas, mas nenhum ponto do código aciona a validação sobre este objeto
 * (ele é instanciado internamente, sem {@code @Valid}). Atualmente o
 * {@code content} recebido é um rótulo fixo ("Confirmação de Cadastro" ou
 * "Redefinição de Senha"), e não o corpo do e-mail. O record não possui campo de
 * assunto.
 * </p>
 *
 * @param sender    endereço do remetente registrado
 * @param recipient endereço do destinatário
 * @param content   texto a registrar
 */
public record EmailRegisterRequest(

  @NotBlank(message = "{email.sender.notBlank}")
  @Email(message = "{email.sender.invalid}")
  String sender,

  @NotBlank(message = "{email.recipient.notBlank}")
  @Email(message = "{email.recipient.invalid}")
  String recipient,

  @NotBlank(message = "{email.content.notBlank}")
  String content) {
    
}
