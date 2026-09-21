package com.albertsilva.dev.dscatalog.validation.user.validator;

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.security.auth.AuthenticatedUserService;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmailForAuthenticatedUser;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link UniqueEmailForAuthenticatedUser}: garante que o e-mail
 * novo do <b>usuário autenticado</b> não pertença a outro usuário.
 *
 * <p>
 * <b>Comportamento:</b> {@code null} ou em branco ⇒ <b>válido</b>. Caso
 * contrário: normaliza o e-mail ({@code trim()} + {@code toLowerCase()}); obtém
 * o usuário autenticado com {@code AuthenticatedUserService.getAuthenticatedUser()}
 * (claim {@code userId} do JWT no {@code SecurityContext}, seguido de
 * {@code UserRepository.findById}); consulta
 * {@code existsByEmailIgnoreCaseAndIdNot(email, idDoUsuárioAutenticado)}. Se
 * outro usuário tiver o e-mail, registra {@code {user.email.unique}}.
 * </p>
 *
 * <p>
 * <b>Dependências:</b> {@code UserRepository} e {@code AuthenticatedUserService}
 * (pacote de segurança). <b>Diferença para os demais validators de unicidade
 * de atualização:</b> o id de referência <b>não vem da URL</b>, e sim do JWT.
 * Se não houver JWT, claim ou usuário, {@code getAuthenticatedUser()} lança
 * {@code AuthenticatedUserNotFoundException}, que este validator não captura; a
 * forma como essa exceção chega ao cliente depende do Bean Validation e do
 * handler de exceções (a confirmar na camada web). O mesmo aviso de
 * normalização de {@link UniqueEmailValidator} se aplica ao valor persistido.
 * </p>
 */
public class UniqueEmailForAuthenticatedUserValidator
    implements ConstraintValidator<UniqueEmailForAuthenticatedUser, String> {

  /**
   * Repositório utilizado para consulta de usuários.
   */
  private final UserRepository repository;

  /**
   * Serviço responsável por obter o usuário autenticado.
   */
  private final AuthenticatedUserService authenticatedUserService;

  /**
   * Construtor.
   *
   * @param repository               repositório de usuários
   * @param authenticatedUserService serviço do usuário autenticado
   */
  public UniqueEmailForAuthenticatedUserValidator(UserRepository repository,
      AuthenticatedUserService authenticatedUserService) {
    this.repository = repository;
    this.authenticatedUserService = authenticatedUserService;
  }

  /**
   * Verifica se o e-mail (normalizado) está livre ou pertence ao próprio usuário
   * autenticado.
   *
   * @param value   e-mail informado (pode ser {@code null})
   * @param context contexto usado para registrar a violação personalizada
   * @return {@code true} se for nulo/em branco, se não existir ou se pertencer
   *         ao usuário autenticado; {@code false} se pertencer a outro usuário
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    if (value == null || value.isBlank()) {
      return true;
    }

    String normalizedEmail = value.trim().toLowerCase();

    User authenticatedUser = authenticatedUserService.getAuthenticatedUser();

    boolean emailAlreadyExists = repository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, authenticatedUser.getId());

    if (!emailAlreadyExists) {
      return true;
    }

    context.disableDefaultConstraintViolation();

    context.buildConstraintViolationWithTemplate("{user.email.unique}").addConstraintViolation();

    return false;
  }

}