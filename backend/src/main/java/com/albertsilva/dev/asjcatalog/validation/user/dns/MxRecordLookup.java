package com.albertsilva.dev.asjcatalog.validation.user.dns;

/**
 * Consulta se um domínio tem registro MX (<em>Mail Exchanger</em>) no DNS,
 * isto é, se há um servidor que recebe e-mails para ele.
 *
 * <p>
 * Usada por {@code ValidEmailValidator}, que recebe esta interface por
 * injeção de dependência. Em produção, a implementação é
 * {@link JndiMxRecordLookup}, que consulta o DNS real; nos testes, ela é
 * substituída por uma implementação controlada, para que a suíte não dependa
 * de acesso à internet.
 * </p>
 */
public interface MxRecordLookup {

  /**
   * Verifica se o domínio tem ao menos um registro MX.
   *
   * @param domain domínio a consultar (por exemplo, {@code gmail.com})
   * @return {@code true} se houver ao menos um registro MX; {@code false} se
   *         não houver ou se a consulta falhar
   */
  boolean hasMxRecord(String domain);
}
