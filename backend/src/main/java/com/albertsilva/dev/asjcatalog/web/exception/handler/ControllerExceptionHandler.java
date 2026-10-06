package com.albertsilva.dev.asjcatalog.web.exception.handler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.albertsilva.dev.asjcatalog.service.exception.DatabaseException;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.asjcatalog.web.exception.enums.ErrorType;
import com.albertsilva.dev.asjcatalog.web.exception.response.ProblemDetails;
import com.albertsilva.dev.asjcatalog.web.exception.response.ValidationError;

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
   * Trata exceções do tipo {@link NoResourceFoundException}.
   *
   * <p>
   * Essa exceção ocorre quando um recurso estático solicitado não é encontrado
   * pela aplicação, como por exemplo:
   * </p>
   * <ul>
   * <li>Requisição automática para <code>/favicon.ico</code></li>
   * <li>Arquivos CSS inexistentes</li>
   * <li>Arquivos JavaScript ausentes</li>
   * <li>Imagens ou assets não encontrados no diretório static/public</li>
   * </ul>
   *
   * <p>
   * Esse tratamento evita que recursos inexistentes sejam interpretados como
   * erro interno da aplicação.
   * </p>
   *
   * <p>
   * Retorna um erro HTTP 404 (Not Found).
   * </p>
   *
   * <p>
   * <b>Observação:</b>
   * </p>
   * O log é registrado em nível WARN, pois se trata de uma condição esperada
   * em alguns cenários de navegação.
   *
   * @param e       exceção lançada
   * @param request requisição HTTP atual
   * @return resposta padronizada contendo detalhes do erro
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetails> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    logger.debug("Static resource not found - path: {}", request.getRequestURI());
    ProblemDetails err = new ProblemDetails(Instant.now(), status.value(),
        "Resource not found",
        "Static resource not found", request.getRequestURI());
    return ResponseEntity.status(status).body(err);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetails> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
    HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
    ValidationError err = new ValidationError(Instant.now(), status.value(), "Validation Error",
        "One or more fields are invalid", request.getRequestURI());
    e.getBindingResult().getFieldErrors().forEach(f -> err.addError(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.status(status).body(err);
  }

  /**
   * Trata exceções de acesso negado do Spring Security.
   *
   * <p>
   * Essa exceção ocorre quando um usuário autenticado tenta acessar
   * um recurso sem possuir as permissões necessárias.
   * </p>
   *
   * <p>
   * Exemplos:
   * </p>
   * <ul>
   * <li>Usuário comum acessando endpoint ADMIN</li>
   * <li>Falha em regras do @PreAuthorize</li>
   * <li>Permissões insuficientes para operação</li>
   * </ul>
   *
   * <p>
   * Retorna HTTP 403 (Forbidden).
   * </p>
   *
   * @param e       exceção lançada
   * @param request requisição HTTP atual
   * @return resposta padronizada contendo detalhes do erro
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetails> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
    HttpStatus status = HttpStatus.FORBIDDEN;
    logger.warn("AccessDeniedException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = new ProblemDetails(Instant.now(), status.value(), "Access denied",
        "Você não possui permissão para acessar este recurso", request.getRequestURI());
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
  public ResponseEntity<ProblemDetails> handleGeneric(Exception e, HttpServletRequest request) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    logger.error("Unexpected error - path: {}", request.getRequestURI(), e);
    ProblemDetails err = new ProblemDetails(Instant.now(), status.value(), ErrorType.INTERNAL_SERVER_ERROR.getMessage(),
        ErrorType.UNEXPECTED_ERROR_OCCURRED.getMessage(), request.getRequestURI());
    return ResponseEntity.status(status).body(err);
  }
}