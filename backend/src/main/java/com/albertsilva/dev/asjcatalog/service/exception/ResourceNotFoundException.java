package com.albertsilva.dev.dscatalog.service.exception;

/**
 * Exceção lançada quando um recurso não é encontrado no sistema.
 *
 * <p>
 * Utilizada principalmente em operações de busca por identificador,
 * quando o registro não existe na base de dados.
 * </p>
 *
 * <p>
 * <b>Uso típico:</b>
 * </p>
 * <ul>
 * <li>Busca por ID (findById)</li>
 * <li>Atualização de recurso inexistente</li>
 * <li>Exclusão de recurso inexistente</li>
 * </ul>
 *
 * <p>
 * <b>Exemplo de cenário:</b>
 * </p>
 * 
 * <pre>
 * Buscar um produto com ID que não existe
 * </pre>
 *
 * <p>
 * <b>Comportamento esperado:</b>
 * </p>
 * <ul>
 * <li>Mapeada para HTTP 404 (Not Found)</li>
 * </ul>
 */
public class ResourceNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Cria uma nova exceção de recurso não encontrado.
   *
   * @param msg mensagem descritiva do erro
   */
  public ResourceNotFoundException(String msg) {
    super(msg);
  }
}