package com.albertsilva.dev.dscatalog.validation.product.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.servlet.HandlerMapping;

import com.albertsilva.dev.dscatalog.dto.product.request.ProductUpdateRequest;
import com.albertsilva.dev.dscatalog.repository.CategoryRepository;
import com.albertsilva.dev.dscatalog.repository.ProductRepository;
import com.albertsilva.dev.dscatalog.validation.product.annotation.ProductUpdateValid;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link ProductUpdateValid}: duas verificações sobre
 * {@code ProductUpdateRequest}, sempre executadas (acumulam violações).
 *
 * <ol>
 * <li><b>Nome não pertencente a outro produto:</b> se o nome for
 * {@code null}/em branco, ou se {@code HttpServletRequest} não tiver o atributo
 * de variáveis de URI ou a chave {@code id}, <b>nada é verificado</b>. Caso
 * contrário, converte o {@code id} com {@code Long.parseLong} (<b>sem tratar
 * {@code NumberFormatException}</b>) e consulta
 * {@code existsByNameIgnoreCaseAndIdNot(nomeNormalizado, id)}; violação em
 * {@code name} com {@code {product.name.unique}}. O id não é verificado como
 * existente.</li>
 * <li><b>Categorias existentes:</b> idêntica à de {@link ProductCreateValidator}
 * (uma consulta {@code existsById} por id; violação em {@code categoryIds}).</li>
 * </ol>
 *
 * <p>
 * Depende de uma variável de caminho chamada {@code id} e de execução dentro de
 * uma requisição MVC. Consulta o banco. Usa {@code FieldMessage} (pacote de
 * exceções da camada web) como estrutura de acúmulo.
 * </p>
 */
public class ProductUpdateValidator implements ConstraintValidator<ProductUpdateValid, ProductUpdateRequest> {

  /** Repositório de produtos, usado na verificação de nome de outro produto. */
  private final ProductRepository productRepository;

  /** Repositório de categorias, usado na verificação de existência dos ids. */
  private final CategoryRepository categoryRepository;

  /** Requisição HTTP corrente, de onde se lê a variável de caminho {@code id}. */
  private final HttpServletRequest request;

  /**
   * Construtor para injeção de dependências.
   *
   * @param productRepository  repositório para acesso aos dados de produtos
   * @param categoryRepository repositório para acesso aos dados de categorias
   * @param request            objeto que representa a requisição HTTP atual,
   *                           utilizado para acessar os parâmetros da URI
   */
  public ProductUpdateValidator(ProductRepository productRepository, CategoryRepository categoryRepository,
      HttpServletRequest request) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.request = request;
  }

  /**
   * Verifica se o nome pertence a outro produto (id da URL) e se as categorias
   * existem, acumulando as violações.
   *
   * @param dto     dados de atualização
   * @param context contexto usado para registrar as violações nos campos
   *                {@code name} e {@code categoryIds}
   * @return {@code true} se nenhuma violação for encontrada
   */
  @Override
  public boolean isValid(ProductUpdateRequest dto, ConstraintValidatorContext context) {

    List<FieldMessage> errors = new ArrayList<>();

    validateUniqueName(dto, errors);
    validateCategories(dto, errors);

    addErrors(errors, context);

    return errors.isEmpty();
  }

  /**
   * Valida se já existe outro produto cadastrado
   * com o mesmo nome informado.
   *
   * <p>
   * Durante a verificação, o produto atualmente
   * sendo atualizado é desconsiderado na consulta.
   *
   * <p>
   * Antes da validação, o nome informado é normalizado:
   * <ul>
   * <li>{@code trim()} (remove espaços apenas nas extremidades)</li>
   * <li>Convertendo para letras minúsculas</li>
   * </ul>
   *
   * <p>
   * O identificador do produto é obtido a partir
   * dos parâmetros presentes na URI da requisição.
   *
   * @param dto    objeto contendo os dados do produto
   * @param errors lista responsável por armazenar
   *               os erros encontrados durante a validação
   */
  @SuppressWarnings("unchecked")
  private void validateUniqueName(ProductUpdateRequest dto, List<FieldMessage> errors) {

    if (dto.name() == null || dto.name().isBlank()) {
      return;
    }

    Map<String, String> uriVars = (Map<String, String>) request
        .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

    if (uriVars == null || !uriVars.containsKey("id")) {
      return;
    }

    Long productId = Long.parseLong(uriVars.get("id"));

    String normalizedName = dto.name().trim().toLowerCase();

    boolean productAlreadyExists = productRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, productId);

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
  private void validateCategories(ProductUpdateRequest dto, List<FieldMessage> errors) {

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