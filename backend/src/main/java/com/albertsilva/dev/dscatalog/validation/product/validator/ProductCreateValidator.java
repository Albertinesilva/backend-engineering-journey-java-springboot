package com.albertsilva.dev.dscatalog.validation.product.validator;

import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductCreateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.albertsilva.dev.dscatalog.validation.product.annotation.ProductCreateValid;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa as regras de validação utilizadas durante
 * o processo de criação de produtos.
 *
 * <p>
 * Este validator é associado à annotation
 * {@link ProductCreateValid} e executa validações
 * contextuais em nível de classe.
 *
 * <p>
 * Atualmente, a validação garante:
 * <ul>
 * <li>Que não exista outro produto cadastrado
 * com o mesmo nome</li>
 * <li>Que todas as categorias informadas existam
 * na base de dados</li>
 * </ul>
 *
 * <p>
 * As mensagens de erro geradas durante a validação
 * são armazenadas em uma lista de {@link FieldMessage}
 * e adicionadas manualmente ao contexto de validação.
 */
public class ProductCreateValidator implements ConstraintValidator<ProductCreateValid, ProductCreateRequest> {

  // Repositórios utilizados para realizar consultas necessárias durante o
  // processo de validação
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  /**
   * Construtor utilizado para injetar as dependências
   * necessárias para a validação.
   *
   * @param productRepository  repositório de produtos, utilizado para
   *                           verificar a existência de produtos com
   *                           o mesmo nome
   * @param categoryRepository repositório de categorias, utilizado para
   *                           verificar a existência das categorias
   *                           informadas no DTO
   */
  public ProductCreateValidator(ProductRepository productRepository,
      CategoryRepository categoryRepository) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
  }

  /**
   * Executa as validações relacionadas ao processo
   * de criação de produtos.
   *
   * <p>
   * As regras de validação são aplicadas utilizando
   * os dados presentes no DTO informado.
   *
   * @param dto     objeto contendo os dados do produto
   *                que será validado
   * @param context contexto utilizado pelo Bean Validation
   *                para registrar erros personalizados
   * @return {@code true} caso nenhuma inconsistência seja encontrada;
   *         {@code false} caso existam erros de validação
   */
  @Override
  public boolean isValid(ProductCreateRequest dto, ConstraintValidatorContext context) {

    List<FieldMessage> errors = new ArrayList<>();

    validateUniqueName(dto, errors);
    validateCategories(dto, errors);

    addErrors(errors, context);

    return errors.isEmpty();
  }

  /**
   * Valida se já existe um produto cadastrado
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
   * @param dto    objeto contendo os dados do produto
   * @param errors lista responsável por armazenar
   *               os erros encontrados durante a validação
   */
  private void validateUniqueName(ProductCreateRequest dto, List<FieldMessage> errors) {

    if (dto.name() == null || dto.name().isBlank()) {
      return;
    }

    String normalizedName = dto.name().trim().toLowerCase();

    boolean productAlreadyExists = productRepository.existsByNameIgnoreCase(normalizedName);

    if (productAlreadyExists) {

      errors.add(new FieldMessage("name", "{product.name.unique}"));
    }
  }

  /**
   * Valida se todas as categorias informadas
   * no produto existem na base de dados.
   *
   * <p>
   * A validação percorre todos os identificadores
   * de categorias informados e verifica individualmente
   * sua existência.
   *
   * <p>
   * Caso ao menos uma categoria não exista,
   * uma mensagem de erro é adicionada à lista
   * de erros.
   *
   * @param dto    objeto contendo os dados do produto
   * @param errors lista responsável por armazenar
   *               os erros encontrados durante a validação
   */
  private void validateCategories(ProductCreateRequest dto, List<FieldMessage> errors) {

    if (dto.categoryIds() == null || dto.categoryIds().isEmpty()) {
      return;
    }

    boolean invalidCategory = dto.categoryIds().stream().anyMatch(id -> !categoryRepository.existsById(id));

    if (invalidCategory) {

      errors.add(new FieldMessage("categoryIds", "{product.categoryIds.invalid}"));
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