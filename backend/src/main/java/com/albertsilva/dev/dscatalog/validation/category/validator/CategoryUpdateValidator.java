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
 * Implementa as regras de validação utilizadas durante
 * o processo de atualização de categorias.
 *
 * <p>
 * Este validator é associado à annotation
 * {@link CategoryUpdateValid} e executa validações
 * contextuais em nível de classe.
 *
 * <p>
 * Durante a atualização, a validação garante que
 * não exista outra categoria cadastrada com o mesmo nome,
 * desconsiderando a própria categoria que está sendo atualizada.
 *
 * <p>
 * O identificador da categoria é obtido diretamente
 * da URI da requisição HTTP através do
 * {@link HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE}.
 *
 * <p>
 * As mensagens de erro geradas durante a validação
 * são armazenadas em uma lista de {@link FieldMessage}
 * e adicionadas manualmente ao contexto de validação.
 */
public class CategoryUpdateValidator implements ConstraintValidator<CategoryUpdateValid, CategoryUpdateRequest> {

  // Repositório utilizado para realizar consultas
  // relacionadas à entidade de categoria durante a validação.
  private final CategoryRepository repository;
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
   * Executa as validações relacionadas ao processo
   * de atualização de categorias.
   *
   * <p>
   * As regras são aplicadas utilizando os dados
   * presentes no DTO e informações da requisição HTTP.
   *
   * @param dto     objeto contendo os dados da categoria
   *                que será validada
   * @param context contexto utilizado pelo Bean Validation
   *                para registrar erros personalizados
   * @return {@code true} caso nenhuma inconsistência seja encontrada;
   *         {@code false} caso existam erros de validação
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
   * Antes da validação, o nome informado é normalizado:
   * <ul>
   * <li>Removendo espaços extras</li>
   * <li>Convertendo para letras minúsculas</li>
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