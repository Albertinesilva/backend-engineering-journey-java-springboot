package com.albertsilva.dev.dscatalog.web.exception.handler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.albertsilva.dev.dscatalog.service.exception.DatabaseException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.dscatalog.web.exception.enums.ErrorType;
import com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Classe responsável pelo tratamento global de exceções da API.
 *
 * <p>
 * Utiliza a anotação {@link RestControllerAdvice} para interceptar exceções
 * lançadas em qualquer camada da aplicação (principalmente Service) e
 * convertê-las em respostas HTTP padronizadas.
 * </p>
 *
 * <p>
 * <b>Objetivo:</b>
 * </p>
 * <ul>
 * <li>Centralizar o tratamento de erros</li>
 * <li>Evitar duplicação de código nos controllers</li>
 * <li>Padronizar o retorno de erro da API</li>
 * </ul>
 *
 * <p>
 * <b>Formato de resposta:</b>
 * </p>
 * 
 * <pre>
 * {
 *   "timestamp": "...",
 *   "status": 404,
 *   "error": "Resource not found",
 *   "message": "...",
 *   "path": "/endpoint"
 * }
 * </pre>
 */
@RestControllerAdvice
public class ControllerExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

  /**
   * Trata exceções do tipo {@link ResourceNotFoundException}.
   *
   * <p>
   * Essa exceção ocorre quando um recurso não é encontrado no sistema,
   * como por exemplo:
   * </p>
   * <ul>
   * <li>Buscar um ID inexistente</li>
   * <li>Atualizar um recurso que não existe</li>
   * <li>Deletar um recurso inexistente</li>
   * </ul>
   *
   * <p>
   * Retorna um erro HTTP 404 (Not Found).
   * </p>
   *
   * @param e       exceção lançada
   * @param request requisição HTTP atual
   * @return resposta padronizada contendo detalhes do erro
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetails> handleResourceNotFound(ResourceNotFoundException e,
      HttpServletRequest request) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    logger.warn("ResourceNotFoundException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = new ProblemDetails(Instant.now(), status.value(), ErrorType.RESOURCE_NOT_FOUND.getMessage(),
        e.getMessage(),
        request.getRequestURI());
    return ResponseEntity.status(status).body(err);
  }

  /**
   * Trata exceções do tipo {@link DatabaseException}.
   *
   * <p>
   * Essa exceção representa erros relacionados ao banco de dados,
   * como por exemplo:
   * </p>
   * <ul>
   * <li>Violação de integridade referencial</li>
   * <li>Tentativa de deletar entidade com relacionamento</li>
   * </ul>
   *
   * <p>
   * Retorna um erro HTTP 400 (Bad Request).
   * </p>
   *
   * <p>
   * <b>Observação:</b>
   * </p>
   * O log inclui o stack trace completo para facilitar debugging.
   *
   * @param e       exceção lançada
   * @param request requisição HTTP atual
   * @return resposta padronizada contendo detalhes do erro
   */
  @ExceptionHandler(DatabaseException.class)
  public ResponseEntity<ProblemDetails> handleDatabase(DatabaseException e, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    logger.error("DatabaseException - path: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
    ProblemDetails err = new ProblemDetails(Instant.now(), status.value(), ErrorType.DATABASE_ERROR.getMessage(),
        e.getMessage(),
        request.getRequestURI());
    return ResponseEntity.status(status).body(err);
  }

  /**
   * Trata exceções do tipo {@link DataIntegrityViolationException}.
   *
   * <p>
   * Essa exceção ocorre quando há violação de integridade dos dados,
   * como por exemplo:
   * </p>
   * <ul>
   * <li>Tentar deletar um recurso que possui relacionamentos dependentes</li>
   * <li>Tentar inserir um registro com chave duplicada</li>
   * <li>Tentar atualizar um registro com valor inválido</li>
   * </ul>
   *
   * <p>
   * Retorna um erro HTTP 400 (Bad Request).
   * </p>
   *
   * @param e       exceção lançada
   * @param request requisição HTTP atual
   * @return resposta padronizada contendo detalhes do erro
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetails> handleDataIntegrity(DataIntegrityViolationException e,
      HttpServletRequest request) {
    HttpStatus status = HttpStatus.CONFLICT;
    logger.error("DataIntegrityViolationException - path: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
    ProblemDetails err = new ProblemDetails(Instant.now(), status.value(), ErrorType.CONFLIT.getMessage(),
        "Cannot delete resource because it has related entities",
        request.getRequestURI());
    return ResponseEntity.status(status).body(err);
  }

  /**
   * Trata exceções genéricas (qualquer outra exceção não tratada
   * especificamente).
   *
   * <p>
   * Essa é uma camada de segurança para garantir que erros inesperados
   * sejam capturados e retornem uma resposta consistente, ao invés de
   * expor detalhes sensíveis ou deixar a aplicação falhar sem controle.
   * </p>
   *
   * <p>
   * Retorna um erro HTTP 500 (Internal Server Error).
   * </p>
   *
   * <p>
   * O log inclui o stack trace completo para facilitar debugging.
   * </p>
   *
   * @param e       exceção lançada
   * @param request requisição HTTP atual
   * @return resposta padronizada contendo detalhes do erro
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetails> handleGeneric(Exception e,
      HttpServletRequest request) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    logger.error("Unexpected error - path: {}", request.getRequestURI(), e);
    ProblemDetails err = new ProblemDetails(Instant.now(), status.value(), ErrorType.INTERNAL_SERVER_ERROR.getMessage(),
        ErrorType.UNEXPECTED_ERROR_OCCURRED.getMessage(),
        request.getRequestURI());
    return ResponseEntity.status(status).body(err);
  }
}