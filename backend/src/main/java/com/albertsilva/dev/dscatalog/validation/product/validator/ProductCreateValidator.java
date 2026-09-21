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
 * Validator de {@link ProductCreateValid}: duas verificações sobre
 * {@code ProductCreateRequest}, sempre executadas (acumulam violações).
 *
 * <ol>
 * <li><b>Nome único:</b> se o nome não for {@code null}/em branco, normaliza
 * ({@code trim()} + {@code toLowerCase()}) e consulta
 * {@code ProductRepository.existsByNameIgnoreCase}; violação em {@code name}
 * com {@code {product.name.unique}}.</li>
 * <li><b>Categorias existentes:</b> se {@code categoryIds} não for
 * {@code null} nem vazio, chama {@code CategoryRepository.existsById} para
 * cada id (uma consulta por id, interrompendo na primeira ausente); violação
 * em {@code categoryIds} com {@code {product.categoryIds.invalid}}. Ids
 * repetidos passam (cada um existe) e um elemento {@code null} na lista faria
 * {@code existsById} lançar exceção (a confirmar em testes).</li>
 * </ol>
 *
 * <p>
 * Consulta o banco; não usa HTTP nem autenticação. Usa {@code FieldMessage}
 * (pacote de exceções da camada web) como estrutura de acúmulo. A obrigação de
 * {@code categoryIds} não vazio é de {@code @NotEmpty} no DTO.
 * </p>
 */
public class ProductCreateValidator implements ConstraintValidator<ProductCreateValid, ProductCreateRequest> {

  /** Repositório de produtos, usado na verificação de nome duplicado. */
  private final ProductRepository productRepository;

  /** Repositório de categorias, usado na verificação de existência dos ids. */
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
   * Verifica nome único e existência das categorias, acumulando as violações.
   *
   * @param dto     dados de criação
   * @param context contexto usado para registrar as violações nos campos
   *                {@code name} e {@code categoryIds}
   * @return {@code true} se nenhuma violação for encontrada
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
   * <li>{@code trim()} (remove espaços apenas nas extremidades)</li>
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