package com.albertsilva.dev.dscatalog.service.exception;

/**
 * Exceção lançada quando ocorre um erro relacionado ao banco de dados.
 *
 * <p>
 * Esta exceção é utilizada para encapsular problemas que acontecem
 * durante operações de persistência, como:
 * </p>
 *
 * <ul>
 * <li>Violação de integridade (ex: chave estrangeira)</li>
 * <li>Falha ao deletar registros vinculados</li>
 * <li>Erros de constraint no banco</li>
 * </ul>
 *
 * <p>
 * <b>Uso típico:</b>
 * </p>
 * <ul>
 * <li>Camada de serviço (Service)</li>
 * <li>Ao capturar exceções do JPA/Hibernate</li>
 * </ul>
 *
 * <p>
 * <b>Exemplo de cenário:</b>
 * </p>
 * 
 * <pre>
 * Tentativa de deletar uma categoria que está vinculada a produtos
 * </pre>
 *
 * <p>
 * <b>Comportamento esperado:</b>
 * </p>
 * <ul>
 * <li>Geralmente mapeada para HTTP 400 ou 409</li>
 * </ul>
 */
public class DatabaseException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Cria uma nova exceção de erro de banco de dados.
   *
   * @param msg mensagem descritiva do erro
   */
  public DatabaseException(String msg) {
    super(msg);
  }
}