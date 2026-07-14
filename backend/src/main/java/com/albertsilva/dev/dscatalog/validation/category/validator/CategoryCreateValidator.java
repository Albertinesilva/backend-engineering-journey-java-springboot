package com.albertsilva.dev.dscatalog.validation.category.validator;

import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.dscatalog.dto.category.request.CategoryCreateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.validation.category.annotation.CategoryCreateValid;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa as regras de validação utilizadas durante
 * o processo de criação de categorias.
 *
 * <p>
 * Este validator é associado à annotation
 * {@link CategoryCreateValid} e executa validações
 * contextuais em nível de classe.
 *
 * <p>
 * Atualmente, a validação garante que não exista
 * outra categoria cadastrada com o mesmo nome.
 *
 * <p>
 * As mensagens de erro geradas durante a validação
 * são armazenadas em uma lista de {@link FieldMessage}
 * e adicionadas manualmente ao contexto de validação.
 */
public class CategoryCreateValidator implements ConstraintValidator<CategoryCreateValid, CategoryCreateRequest> {

  /*
   * Repositório utilizado para verificar a existência
   * de categorias com o mesmo nome durante a validação.
   */
  private final CategoryRepository repository;

  /**
   * Construtor que recebe o repositório de categorias
   * para realizar as validações necessárias.
   *
   * @param repository repositório utilizado para verificar
   *                   a existência de categorias com o mesmo nome
   */
  public CategoryCreateValidator(CategoryRepository repository) {
    this.repository = repository;
  }

  /**
   * Executa as validações relacionadas ao processo
   * de criação de categorias.
   *
   * <p>
   * As regras de validação são aplicadas de forma
   * contextual utilizando os dados presentes no DTO.
   *
   * @param dto     objeto contendo os dados da categoria
   *                que será validada
   * @param context contexto utilizado pelo Bean Validation
   *                para registrar erros personalizados
   * @return {@code true} caso nenhuma inconsistência seja encontrada;
   *         {@code false} caso existam erros de validação
   */
  @Override
  public boolean isValid(CategoryCreateRequest dto, ConstraintValidatorContext context) {

    List<FieldMessage> errors = new ArrayList<>();

    validateUniqueName(dto, errors);

    addErrors(errors, context);

    return errors.isEmpty();
  }

  /**
   * Valida se já existe uma categoria cadastrada
   * com o mesmo nome informado.
   *
   * <p>
   * Antes da verificação, o nome é normalizado:
   * <ul>
   * <li>Removendo espaços extras</li>
   * <li>Convertendo para letras minúsculas</li>
   * </ul>
   *
   * <p>
   * Caso o nome já exista, uma mensagem de erro
   * é adicionada à lista de erros.
   *
   * @param dto    objeto contendo os dados da categoria
   * @param errors lista responsável por armazenar
   *               os erros encontrados durante a validação
   */
  private void validateUniqueName(CategoryCreateRequest dto, List<FieldMessage> errors) {

    if (dto.name() == null || dto.name().isBlank()) {
      return;
    }

    String normalizedName = dto.name().trim().toLowerCase();

    boolean categoryAlreadyExists = repository.existsByNameIgnoreCase(normalizedName);

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