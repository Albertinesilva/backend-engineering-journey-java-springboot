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
 * Validator de {@link CategoryCreateValid}: uma única verificação sobre
 * {@code CategoryCreateRequest}.
 *
 * <p>
 * Se o nome for {@code null} ou em branco, nada é verificado. Caso contrário,
 * normaliza o nome ({@code trim()} + {@code toLowerCase()}) e consulta
 * {@code CategoryRepository.existsByNameIgnoreCase}; se existir, registra
 * {@code {category.name.unique}} no campo {@code name}. O nome do DTO não é
 * modificado (o mapper o grava como recebido). Consulta o banco; não usa HTTP
 * nem autenticação. Usa {@code FieldMessage} (pacote de exceções da camada web)
 * como estrutura de acúmulo.
 * </p>
 */
public class CategoryCreateValidator implements ConstraintValidator<CategoryCreateValid, CategoryCreateRequest> {

  /** Repositório usado para verificar a existência de categoria com o mesmo nome. */
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
   * Verifica se já existe categoria com o nome informado.
   *
   * @param dto     dados de criação
   * @param context contexto usado para registrar a violação no campo
   *                {@code name}
   * @return {@code true} se o nome estiver livre (ou em branco)
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
   * <li>{@code trim()} (remove espaços apenas nas extremidades)</li>
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