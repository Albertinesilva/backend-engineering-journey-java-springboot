package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UserUpdateValid;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link UserUpdateValid}: duas verificações sobre
 * {@code UserUpdateRequest}.
 *
 * <ol>
 * <li><b>E-mail único, excluindo o próprio usuário:</b> lê o id de
 * {@code HttpServletRequest} (atributo de variáveis de URI do Spring MVC,
 * chave {@code id}). Se o e-mail for {@code null}/em branco, se o mapa ou a
 * chave {@code id} não existirem, ou se o id não for numérico
 * ({@code NumberFormatException} capturada), a verificação é <b>ignorada</b>
 * (sem violação). Caso contrário, normaliza o e-mail ({@code trim()} +
 * {@code toLowerCase()}) e consulta
 * {@code existsByEmailIgnoreCaseAndIdNot(email, id)}; violação em
 * {@code email} com {@code {user.email.unique}}. O id <b>não</b> é verificado
 * como existente: com id inexistente, qualquer e-mail já cadastrado é
 * considerado duplicado.</li>
 * <li><b>Senha sem dados pessoais:</b> só se a senha não for {@code null}
 * nem em branco; mesma regra de {@link PasswordPersonalDataValidator}
 * (substring, tokens de ao menos 3 caracteres, no
 * máximo uma violação em {@code password}).</li>
 * </ol>
 *
 * <p>
 * Dependências: {@code UserRepository} e {@code HttpServletRequest}
 * (injetado pelo Spring; o validator é criado como bean e recebe a requisição
 * corrente). A lógica de dados pessoais é uma cópia da de
 * {@link PasswordPersonalDataValidator}, e o e-mail original não é modificado.
 * </p>
 */
public class UserUpdateValidator implements ConstraintValidator<UserUpdateValid, UserUpdateRequest> {

  /**
   * Tamanho mínimo que um token (nome/sobrenome/email prefix)
   * deve ter para ser considerado na validação da senha.
   */
  private static final int MIN_TOKEN_LENGTH = 3;

  /**
   * Repositório utilizado para consultar dados
   * de usuários no banco de dados.
   */
  private final UserRepository repository;

  /**
   * Requisição HTTP atual, utilizada para extrair
   * o ID do usuário a ser atualizado a partir
   * das variáveis de template da URI.
   */
  private final HttpServletRequest request;

  /**
   * Construtor que recebe o repositório de usuários e
   * a requisição HTTP por injeção de dependência.
   *
   * @param repository repositório responsável pelas
   *                   operações de busca no banco de dados
   * @param request    requisição HTTP atual, utilizada para
   *                   extrair informações de contexto
   */
  public UserUpdateValidator(UserRepository repository, HttpServletRequest request) {
    this.repository = repository;
    this.request = request;
  }

  /**
   * Executa a verificação de e-mail único (excluindo o usuário da URL) e a de
   * dados pessoais na senha, acumulando todas as violações.
   *
   * @param dto     dados de atualização
   * @param context contexto usado para registrar as violações nos campos
   *                {@code email} e {@code password}
   * @return {@code true} se nenhuma violação for encontrada
   */
  @Override
  public boolean isValid(UserUpdateRequest dto, ConstraintValidatorContext context) {

    List<FieldMessage> errors = new ArrayList<>();

    validateUniqueEmail(dto, errors);
    validatePasswordDoesNotContainPersonalData(dto, errors);

    addErrors(errors, context);

    return errors.isEmpty();
  }

  /**
   * Valida se o email fornecido é único no banco de dados,
   * ignorando o usuário sendo atualizado.
   *
   * <p>
   * O método extrai o ID do usuário a ser atualizado
   * a partir das variáveis de template da URI da requisição HTTP.
   * Caso o ID não seja encontrado ou seja inválido, a validação
   * é ignorada.
   *
   * <p>
   * O email é normalizado para minúsculas e espaços em branco
   * são removidos antes da consulta.
   *
   * @param dto    objeto contendo os dados de atualização do usuário
   * @param errors lista responsável por armazenar
   *               os erros encontrados
   */
  private void validateUniqueEmail(UserUpdateRequest dto, List<FieldMessage> errors) {

    if (dto.email() == null || dto.email().isBlank()) {
      return;
    }

    @SuppressWarnings("unchecked")
    Map<String, String> uriVars = (Map<String, String>) request
        .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    if (uriVars == null || !uriVars.containsKey("id")) {
      return;
    }

    Long userId;

    try {
      userId = Long.parseLong(uriVars.get("id"));
    } catch (NumberFormatException e) {
      return;
    }

    String normalizedEmail = dto.email().trim().toLowerCase();

    boolean emailAlreadyExists = repository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, userId);

    if (emailAlreadyExists) {
      errors.add(new FieldMessage("email", "{user.email.unique}"));
    }
  }

  /**
   * Valida se a senha não contém dados pessoais do usuário
   * como nome, sobrenome ou parte local do email.
   *
   * <p>
   * Caso a senha seja {@code null} ou esteja em branco (vazia ou só com
   * espaços), a verificação é ignorada.
   *
   * @param dto    objeto contendo os dados de atualização do usuário
   * @param errors lista responsável por armazenar
   *               os erros encontrados
   */
  private void validatePasswordDoesNotContainPersonalData(UserUpdateRequest dto, List<FieldMessage> errors) {

    if (dto.password() == null || dto.password().isBlank()) {
      return;
    }

    String password = dto.password().trim().toLowerCase();

    validateToken(password, dto.firstName(), errors);
    validateToken(password, dto.lastName(), errors);

    if (dto.email() != null && dto.email().contains("@")) {

      String emailPrefix = dto.email().split("@")[0];

      validateToken(password, emailPrefix, errors);
    }
  }

  /**
   * Valida se a senha contém um token específico (nome, sobrenome ou
   * parte local do email) de forma case-insensitive.
   *
   * <p>
   * O token é considerado válido apenas se possuir um tamanho
   * mínimo definido por {@link #MIN_TOKEN_LENGTH}. Caso contrário,
   * a validação é ignorada.
   *
   * <p>
   * Se a senha contiver o token normalizado, um erro é adicionado
   * à lista de erros, evitando duplicatas.
   *
   * @param password senha normalizada em minúsculas
   * @param value    valor do token a ser verificado (nome, sobrenome, etc)
   * @param errors   lista responsável por armazenar
   *                 os erros encontrados
   */
  private void validateToken(String password, String value, List<FieldMessage> errors) {

    if (value == null) {
      return;
    }

    String normalized = value.trim().toLowerCase();

    if (normalized.length() < MIN_TOKEN_LENGTH) {
      return;
    }

    boolean alreadyExists = errors.stream().anyMatch(
        error -> error.fieldName().equals("password") && error.message().equals("{user.password.personalData}"));

    if (password.contains(normalized) && !alreadyExists) {
      errors.add(new FieldMessage("password", "{user.password.personalData}"));
    }
  }

  /**
   * Adiciona ao contexto de validação todos os erros
   * encontrados durante o processo de validação.
   *
   * <p>
   * O método desabilita a mensagem padrão do Bean Validation
   * para permitir o registro de mensagens customizadas e
   * associar cada erro ao campo específico via
   * {@link ConstraintValidatorContext#buildConstraintViolationWithTemplate(String)}.
   *
   * @param errors  lista contendo os erros encontrados
   * @param context contexto utilizado para registrar
   *                as violações de validação
   */
  private void addErrors(List<FieldMessage> errors, ConstraintValidatorContext context) {

    if (errors.isEmpty()) {
      return;
    }

    context.disableDefaultConstraintViolation();

    for (FieldMessage error : errors) {

      context.buildConstraintViolationWithTemplate(error.message()).addPropertyNode(error.fieldName())
          .addConstraintViolation();
    }
  }
}