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
 * Implementa a lógica de validação utilizada pela
 * annotation {@link UserUpdateValid}.
 *
 * <p>
 * Este validator verifica se os dados fornecidos para
 * a atualização de um usuário existente atendem aos critérios
 * de segurança definidos pela aplicação.
 *
 * <p>
 * As validações realizadas incluem:
 * <ul>
 * <li>Verifica se o email é único na base de dados (ignorando o usuário
 * atual)</li>
 * <li>Verifica se a senha (se fornecida) não contém o primeiro nome do
 * usuário</li>
 * <li>Verifica se a senha (se fornecida) não contém o sobrenome do usuário</li>
 * <li>Verifica se a senha (se fornecida) não contém a parte local do email
 * (antes do @)</li>
 * </ul>
 *
 * <p>
 * As mensagens de erro são adicionadas manualmente
 * ao contexto de validação utilizando
 * {@link ConstraintValidatorContext}.
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
   * Executa a validação completa dos dados de atualização de usuário.
   *
   * <p>
   * O método valida se os dados fornecidos no DTO de atualização
   * atendem aos critérios de segurança, incluindo verificações de
   * unicidade de email e se a senha não contém informações pessoais.
   *
   * @param dto     objeto contendo os dados de atualização do usuário
   * @param context contexto utilizado pelo Bean Validation
   *                para registrar erros personalizados
   * @return {@code true} se todos os dados são válidos;
   *         {@code false} caso existam erros de validação
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
   * Caso a senha seja {@code null} ou vazia, a validação é ignorada,
   * permitindo que a obrigatoriedade seja tratada por outras
   * annotations como {@code @NotBlank}.
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