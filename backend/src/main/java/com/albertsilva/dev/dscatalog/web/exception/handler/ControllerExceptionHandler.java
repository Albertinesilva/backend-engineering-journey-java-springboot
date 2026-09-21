package com.albertsilva.dev.dscatalog.web.exception.handler;

import java.time.Instant;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.albertsilva.dev.dscatalog.service.exception.AuthenticatedUserNotFoundException;
import com.albertsilva.dev.dscatalog.service.exception.DatabaseException;
import com.albertsilva.dev.dscatalog.service.exception.InvalidTokenException;
import com.albertsilva.dev.dscatalog.service.exception.PasswordUpdateException;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;
import com.albertsilva.dev.dscatalog.web.exception.enums.ApiErrorCode;
import com.albertsilva.dev.dscatalog.web.exception.response.ProblemDetails;
import com.albertsilva.dev.dscatalog.web.exception.response.ValidationError;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tratamento global de exceções lançadas pelos controllers e pelas camadas
 * abaixo deles ({@code @RestControllerAdvice}). Converte cada exceção em uma
 * resposta JSON com {@link ProblemDetails} (ou {@link ValidationError}).
 *
 * <p>
 * <b>Formato:</b> {@code ProblemDetails} é uma classe <b>própria</b> do projeto
 * ({@code timestamp}, {@code status}, {@code code}, {@code error},
 * {@code message}, {@code path}); <b>não</b> é o
 * {@code org.springframework.http.ProblemDetail} e não segue o RFC 7807. Título
 * e detalhe são textos do {@code MessageSource} no idioma da requisição
 * ({@code Locale} do cabeçalho {@code Accept-Language}; padrão {@code pt_BR}).
 * O {@code code} é o {@link ApiErrorCode}, estável entre idiomas.
 * </p>
 *
 * <p>
 * <b>Exceções tratadas</b> (o Spring escolhe o handler pelo tipo de exceção
 * <em>mais específico</em>, não pela ordem de declaração):
 * </p>
 * <table>
 * <caption>Mapeamento exceção → resposta</caption>
 * <tr><th>Exceção</th><th>HTTP</th><th>{@code code}</th></tr>
 * <tr><td>{@code ResourceNotFoundException}</td><td>404</td><td>RESOURCE_NOT_FOUND</td></tr>
 * <tr><td>{@code NoResourceFoundException}</td><td>404</td><td>RESOURCE_NOT_FOUND</td></tr>
 * <tr><td>{@code DatabaseException}</td><td>400</td><td>DATABASE_ERROR</td></tr>
 * <tr><td>{@code DataIntegrityViolationException}</td><td>409</td><td>CONFLICT</td></tr>
 * <tr><td>{@code MethodArgumentNotValidException}</td><td>422</td><td>VALIDATION_ERROR (com {@code fieldErrors})</td></tr>
 * <tr><td>{@code AccessDeniedException}</td><td>403</td><td>ACCESS_DENIED</td></tr>
 * <tr><td>{@code InvalidTokenException}</td><td>400</td><td>INVALID_TOKEN</td></tr>
 * <tr><td>{@code AuthenticatedUserNotFoundException}</td><td>401</td><td>AUTHENTICATION_REQUIRED</td></tr>
 * <tr><td>{@code PasswordUpdateException}</td><td>422</td><td>PASSWORD_UPDATE_ERROR</td></tr>
 * <tr><td>{@code DisabledException}</td><td>403</td><td>ACCESS_DISABLED</td></tr>
 * <tr><td>{@code Exception} (qualquer outra)</td><td>500</td><td>INTERNAL_SERVER_ERROR</td></tr>
 * </table>
 *
 * <p>
 * <b>Consequências do handler genérico:</b> a classe não estende
 * {@code ResponseEntityExceptionHandler} e não há handler para
 * {@code HttpRequestMethodNotSupportedException}, {@code HttpMessageNotReadableException},
 * {@code MethodArgumentTypeMismatchException}, {@code MissingServletRequestParameterException},
 * {@code HttpMediaTypeNotSupportedException}, {@code ConstraintViolationException},
 * {@code AuthenticationException}, {@code UnsupportedOperationException} etc.:
 * todas caem em {@link #handleGeneric} e viram <b>{@code 500}</b> (em vez de
 * 405, 400, 415...). Isso foi observado nos logs do baseline (um {@code PATCH}
 * em rota {@code PUT} resultou em {@code 500}).
 * </p>
 *
 * <p>
 * <b>Escopo:</b> só exceções que chegam ao {@code DispatcherServlet}. Falhas de
 * autenticação do filtro de bearer token (token ausente, inválido ou expirado)
 * são respondidas antes, pela cadeia de segurança, sem passar por este
 * advice; já o {@code AccessDeniedException} de {@code @PreAuthorize} é lançado
 * dentro do controller e é tratado aqui.
 * </p>
 *
 * <p>
 * <b>Logs:</b> {@code WARN} para 404, 403 e 401 de identidade e token inválido;
 * {@code ERROR}, com a exceção completa (stack trace), para
 * {@code DatabaseException}, {@code DataIntegrityViolationException} e
 * {@code Exception}; {@code DEBUG} para recurso estático; nenhum log para
 * validação, {@code PasswordUpdateException} e {@code DisabledException}. O
 * corpo da resposta nunca contém stack trace nem a mensagem crua da exceção.
 * </p>
 */
@RestControllerAdvice
public class ControllerExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

  private final MessageSource messageSource;

  /**
   * @param messageSource fonte das mensagens traduzidas (bean de {@code MessageSourceConfig})
   */
  public ControllerExceptionHandler(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * {@code ResourceNotFoundException} → {@code 404}, {@code RESOURCE_NOT_FOUND}.
   * O detalhe é a mensagem traduzida da <b>chave</b> carregada pela exceção (por
   * exemplo, {@code error.user.notFound}); se a chave não existir, usa
   * {@code error.resource.message}. Log {@code WARN}.
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetails> handleResourceNotFound(ResourceNotFoundException e,
      HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    logger.warn("ResourceNotFoundException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.RESOURCE_NOT_FOUND,
        "error.resource.title", e.getMessage(), "error.resource.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code DatabaseException} → {@code 400}, {@code DATABASE_ERROR}. Detalhe pela
   * chave da exceção, com padrão {@code error.database.message}. Log
   * {@code ERROR} com stack trace.
   */
  @ExceptionHandler(DatabaseException.class)
  public ResponseEntity<ProblemDetails> handleDatabase(DatabaseException e, HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    logger.error("DatabaseException - path: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.DATABASE_ERROR,
        "error.database.title", e.getMessage(), "error.database.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code DataIntegrityViolationException} → {@code 409}, {@code CONFLICT}, com
   * texto genérico ({@code error.conflict.message}); a mensagem da exceção
   * (que pode citar restrições do banco) só vai para o log {@code ERROR}, com
   * stack trace.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetails> handleDataIntegrity(DataIntegrityViolationException e,
      HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.CONFLICT;
    logger.error("DataIntegrityViolationException - path: {}, message: {}", request.getRequestURI(), e.getMessage(), e);
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.CONFLICT,
        "error.conflict.title", "error.conflict.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code NoResourceFoundException} (rota ou recurso estático inexistente) →
   * {@code 404}, {@code RESOURCE_NOT_FOUND}, texto genérico. Log {@code DEBUG}.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetails> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.NOT_FOUND;
    logger.debug("Static resource not found - path: {}", request.getRequestURI());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.RESOURCE_NOT_FOUND,
        "error.resource.title", "error.resource.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code MethodArgumentNotValidException} (falha do Bean Validation em
   * {@code @Valid @RequestBody}) → {@code 422}, {@code VALIDATION_ERROR}, com
   * {@link ValidationError}.
   *
   * <p>
   * Para cada <b>erro de campo</b> ({@code getFieldErrors()}) acrescenta um
   * {@code FieldMessage} (nome do campo e mensagem já interpolada). Vários erros
   * do mesmo campo geram várias entradas. Erros <b>globais</b>
   * ({@code getGlobalErrors()}) não são incluídos; hoje todos os validators de
   * classe associam a violação a um campo, então nada é perdido. Sem log.
   * </p>
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetails> validation(MethodArgumentNotValidException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
    ValidationError err = buildValidationError(status, ApiErrorCode.VALIDATION_ERROR,
        "error.validation.title", "error.validation.message", request.getRequestURI(), locale);
    e.getBindingResult().getFieldErrors().forEach(f -> err.addError(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code AccessDeniedException} → {@code 403}, {@code ACCESS_DENIED}. Trata a
   * negação de {@code @PreAuthorize} (lançada dentro do controller); inclui o
   * caso de usuário anônimo em rota liberada por URL mas protegida por método
   * (por exemplo, {@code GET /accounts/me}), que responde {@code 403} aqui e não
   * {@code 401}. Log {@code WARN}.
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetails> handleAccessDenied(AccessDeniedException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.FORBIDDEN;
    logger.warn("AccessDeniedException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.ACCESS_DENIED,
        "error.access.title", "error.access.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code InvalidTokenException} (token de ativação/recuperação de tipo errado,
   * desabilitado ou expirado) → {@code 400}, {@code INVALID_TOKEN}. Detalhe pela
   * chave da exceção ({@code error.token.type.invalid}, {@code error.token.disabled},
   * {@code error.token.expired}). Não se refere ao JWT. Log {@code WARN}.
   */
  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<ProblemDetails> handleInvalidToken(InvalidTokenException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.BAD_REQUEST;
    logger.warn("InvalidTokenException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.INVALID_TOKEN,
        "error.invalid.token.title", e.getMessage(), "error.invalid.token.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code AuthenticatedUserNotFoundException} (JWT sem principal válido, sem
   * claim {@code userId} ou com usuário inexistente) → {@code 401},
   * {@code AUTHENTICATION_REQUIRED}. Detalhe pela chave da exceção; a chave
   * {@code error.auth.userId.claim.notFound} só existe no idioma português, e nos
   * demais usa-se o texto padrão. Log {@code WARN}.
   */
  @ExceptionHandler(AuthenticatedUserNotFoundException.class)
  public ResponseEntity<ProblemDetails> handleAuthenticatedUserNotFound(AuthenticatedUserNotFoundException e,
      HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.UNAUTHORIZED;
    logger.warn("AuthenticatedUserNotFoundException - path: {}, message: {}", request.getRequestURI(), e.getMessage());
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.AUTHENTICATION_REQUIRED,
        "error.authentication.title", e.getMessage(), "error.authentication.message", request.getRequestURI(), locale);

    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code PasswordUpdateException} (confirmação divergente, senha atual
   * incorreta, nova senha igual à atual) → {@code 422},
   * {@code PASSWORD_UPDATE_ERROR}. Detalhe pela chave da exceção. Sem log.
   */
  @ExceptionHandler(PasswordUpdateException.class)
  public ResponseEntity<ProblemDetails> handlePasswordUpdate(PasswordUpdateException e, HttpServletRequest request,
      Locale locale) {

    HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.PASSWORD_UPDATE_ERROR, "error.password.update.title",
        e.getMessage(), "error.password.update.message", request.getRequestURI(), locale);

    return ResponseEntity.status(status).body(err);
  }

  /**
   * {@code DisabledException} → {@code 403}, {@code ACCESS_DISABLED}. Nenhum
   * código da aplicação lança essa exceção (o login com conta inativa é
   * tratado no provider OAuth2, que usa {@code OAuth2AuthenticationException}), e
   * não há provider padrão do Spring configurado; provavelmente o handler não é
   * alcançado hoje. Sem log.
   */
  @ExceptionHandler(DisabledException.class)
  public ResponseEntity<ProblemDetails> handleDisabledException(DisabledException e, HttpServletRequest request,
      Locale locale) {
    HttpStatus status = HttpStatus.FORBIDDEN;
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.ACCESS_DISABLED,
        "error.account.disabled.title", "error.account.disabled.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * Qualquer outra {@code Exception} → {@code 500}, {@code INTERNAL_SERVER_ERROR},
   * com texto genérico. Log {@code ERROR} com a exceção completa.
   *
   * <p>
   * Inclui exceções do próprio Spring MVC sem handler específico, como
   * {@code HttpRequestMethodNotSupportedException} (405), corpo JSON ilegível,
   * parâmetro obrigatório ausente e tipo de argumento inválido, e também
   * {@code UnsupportedOperationException} de {@code POST /accounts/deactivate}.
   * </p>
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetails> handleGeneric(Exception e, HttpServletRequest request, Locale locale) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    logger.error("Unexpected error - path: {}", request.getRequestURI(), e);
    ProblemDetails err = buildProblemDetails(status, ApiErrorCode.INTERNAL_SERVER_ERROR,
        "error.internal.title", "error.internal.message", request.getRequestURI(), locale);
    return ResponseEntity.status(status).body(err);
  }

  /**
   * Monta a resposta com título e detalhe obtidos de <b>duas chaves fixas</b> do
   * {@code MessageSource} (sem usar a mensagem da exceção); {@code timestamp} =
   * instante atual.
   */
  private ProblemDetails buildProblemDetails(HttpStatus status, ApiErrorCode code, String titleKey, String messageKey,
      String path, Locale locale) {
    return new ProblemDetails(Instant.now(), status.value(), code,
        getMessage(titleKey, locale), getMessage(messageKey, locale), path);
  }

  /**
   * Monta a resposta cujo detalhe vem da <b>chave carregada pela exceção</b>
   * ({@code detailCodeOrMessage}), com recuo para {@code fallbackMessageKey}.
   */
  private ProblemDetails buildProblemDetails(HttpStatus status, ApiErrorCode code, String titleKey,
      String detailCodeOrMessage, String fallbackMessageKey, String path, Locale locale) {
    return new ProblemDetails(Instant.now(), status.value(), code,
        getMessage(titleKey, locale), resolveMessage(detailCodeOrMessage, fallbackMessageKey, locale), path);
  }

  /**
   * Cria o {@link ValidationError} (lista de erros de campo vazia, a ser
   * preenchida pelo chamador).
   */
  private ValidationError buildValidationError(HttpStatus status, ApiErrorCode code, String titleKey, String messageKey,
      String path, Locale locale) {
    return new ValidationError(Instant.now(), status.value(), code,
        getMessage(titleKey, locale), getMessage(messageKey, locale), path);
  }

  /**
   * Traduz {@code codeOrMessage} como chave do {@code MessageSource}; se for
   * {@code null}, ou se a chave não existir, devolve a mensagem de
   * {@code fallbackKey}. Assim, uma exceção lançada com texto que não é chave não
   * vaza texto interno para o cliente.
   */
  private String resolveMessage(String codeOrMessage, String fallbackKey, Locale locale) {
    if (codeOrMessage == null) {
      return getMessage(fallbackKey, locale);
    }

    try {
      String message = messageSource.getMessage(codeOrMessage, null, locale);
      return message != null ? message : getMessage(fallbackKey, locale);
    } catch (NoSuchMessageException e) {
      return getMessage(fallbackKey, locale);
    }
  }

  /**
   * Busca a mensagem da chave no idioma informado; se a chave não existir,
   * devolve a própria chave como texto.
   */
  private String getMessage(String key, Locale locale) {
    return messageSource.getMessage(key, null, key, locale);
  }
}