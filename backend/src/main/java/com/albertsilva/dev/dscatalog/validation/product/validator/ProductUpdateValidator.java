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
 * Implementa as regras de validação utilizadas durante
 * o processo de atualização de produtos.
 *
 * <p>
 * Este validator é associado à annotation
 * {@link ProductUpdateValid} e executa validações
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
 * Durante a atualização, o produto atualmente
 * sendo editado é desconsiderado na verificação
 * de unicidade do nome.
 *
 * <p>
 * O identificador do produto é obtido diretamente
 * da URI da requisição HTTP através do
 * {@link HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE}.
 *
 * <p>
 * As mensagens de erro geradas durante a validação
 * são armazenadas em uma lista de {@link FieldMessage}
 * e adicionadas manualmente ao contexto de validação.
 */
public class ProductUpdateValidator implements ConstraintValidator<ProductUpdateValid, ProductUpdateRequest> {

  // Repositórios necessários para as validações
  // O HttpServletRequest é utilizado para acessar os parâmetros da URI durante a
  // validação
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
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
   * Executa as validações relacionadas ao processo
   * de atualização de produtos.
   *
   * <p>
   * As regras de validação são aplicadas utilizando
   * os dados presentes no DTO e informações obtidas
   * da requisição HTTP.
   *
   * @param dto     objeto contendo os dados do produto
   *                que será validado
   * @param context contexto utilizado pelo Bean Validation
   *                para registrar erros personalizados
   * @return {@code true} caso nenhuma inconsistência seja encontrada;
   *         {@code false} caso existam erros de validação
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
   * <li>Removendo espaços extras</li>
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