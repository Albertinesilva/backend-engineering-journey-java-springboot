package com.albertsilva.dev.dscatalog.validation.category.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryUpdateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.validation.category.annotation.CategoryUpdateValid;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link CategoryUpdateValid}: verifica se o nome já pertence a
 * <b>outra</b> categoria.
 *
 * <p>
 * Se o nome for {@code null}/em branco, ou se {@code HttpServletRequest} não
 * tiver o atributo de variáveis de URI ou a chave {@code id}, <b>nada é
 * verificado</b> (resultado válido). Caso contrário, converte o {@code id} com
 * {@code Long.parseLong} — <b>sem tratar {@code NumberFormatException}</b> — e
 * consulta {@code existsByNameIgnoreCaseAndIdNot(nomeNormalizado, id)}
 * ({@code trim()} + {@code toLowerCase()}); violação em {@code name} com
 * {@code {category.name.unique}}. O id não é verificado como existente: com id
 * inexistente, qualquer nome já cadastrado é considerado duplicado.
 * </p>
 *
 * <p>
 * Depende do formato atual das rotas (uma variável de caminho chamada
 * {@code id}) e de estar sendo executado dentro de uma requisição MVC. Se o
 * {@code id} vier não numérico, é provável que o Spring já tenha rejeitado a
 * requisição ao converter o {@code @PathVariable}, antes deste validator (a
 * confirmar na camada web).
 * </p>
 */
public class CategoryUpdateValidator implements ConstraintValidator<CategoryUpdateValid, CategoryUpdateRequest> {

  /** Repositório usado para verificar a existência de outra categoria com o mesmo nome. */
  private final CategoryRepository repository;

  /** Requisição HTTP corrente, de onde se lê a variável de caminho {@code id}. */
  private final HttpServletRequest request;

  /**
   * Construtor para injeção de dependências.
   *
   * @param repository repositório de categorias utilizado
   *                   para consultas durante a validação
   * @param request    objeto representando a requisição HTTP
   *                   atual, utilizado para acessar parâmetros
   *                   da URI durante a validação
   */
  public CategoryUpdateValidator(CategoryRepository repository,
      HttpServletRequest request) {
    this.repository = repository;
    this.request = request;
  }

  /**
   * Verifica se o nome já pertence a outra categoria (o id vem da URL).
   *
   * @param dto     dados de atualização
   * @param context contexto usado para registrar a violação no campo
   *                {@code name}
   * @return {@code true} se não houver duplicidade ou se a verificação for
   *         ignorada
   */
  @Override
  public boolean isValid(CategoryUpdateRequest dto, ConstraintValidatorContext context) {

    List<FieldMessage> errors = new ArrayList<>();

    validateUniqueName(dto, errors);

    addErrors(errors, context);

    return errors.isEmpty();
  }

  /**
   * Valida se já existe outra categoria cadastrada
   * com o mesmo nome informado.
   *
   * <p>
   * Durante a verificação, a categoria atualmente
   * sendo atualizada é desconsiderada na consulta.
   *
   * <p>
   * Antes da consulta, o nome informado é normalizado:
   * <ul>
   * <li>{@code trim()} (remove espaços apenas nas extremidades)</li>
   * <li>{@code toLowerCase()} (sem {@code Locale})</li>
   * </ul>
   *
   * <p>
   * O identificador da categoria é obtido a partir
   * dos parâmetros presentes na URI da requisição.
   *
   * @param dto    objeto contendo os dados da categoria
   * @param errors lista responsável por armazenar
   *               os erros encontrados durante a validação
   */
  @SuppressWarnings("unchecked")
  private void validateUniqueName(CategoryUpdateRequest dto, List<FieldMessage> errors) {

    if (dto.name() == null || dto.name().isBlank()) {
      return;
    }

    Map<String, String> uriVars = (Map<String, String>) request
        .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    if (uriVars == null || !uriVars.containsKey("id")) {
      return;
    }

    Long categoryId = Long.parseLong(uriVars.get("id"));

    String normalizedName = dto.name().trim().toLowerCase();

    boolean categoryAlreadyExists = repository.existsByNameIgnoreCaseAndIdNot(normalizedName, categoryId);

    if (categoryAlreadyExists) {

      errors.add(new FieldMessage("name", "{category.name.unique}"));
    }
  }

  /**
   * Adiciona ao contexto de validação todos os erros
   * encontrados durante o processo de validação.
   *
   * <p>
   * O método desabilita a mensagem padrão do Bean Validation
   * para permitir o registro de mensagens customizadas
   * associadas a campos específicos.
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