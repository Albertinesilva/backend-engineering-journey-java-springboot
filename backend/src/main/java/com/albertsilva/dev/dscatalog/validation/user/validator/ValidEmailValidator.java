package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.Hashtable;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import com.albertsilva.dev.dscatalog.validation.user.annotation.ValidEmail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela
 * annotation {@link ValidEmail}.
 *
 * <p>
 * Este validator verifica se o endereço de email possui
 * um formato válido e se o domínio possui registros
 * MX (Mail Exchange) válidos no servidor DNS.
 *
 * <p>
 * O processo de validação é realizado em duas etapas:
 * <ul>
 * <li>Validação de formato através de expressão regular</li>
 * <li>Validação da existência de registros MX do domínio</li>
 * </ul>
 *
 * <p>
 * Caso a validação de registros MX falhe por motivos
 * de conectividade ou DNS indisponível, a validação
 * retorna {@code false}. O validator também normaliza
 * o email para minúsculas e remove espaços em branco
 * antes de validar.
 */
public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

  /**
   * Expressão regular utilizada para validar
   * o formato básico do endereço de email.
   *
   * <p>
   * O padrão aceita:
   * <ul>
   * <li>Caracteres alfanuméricos, +, _, ., -</li>
   * <li>Um símbolo @</li>
   * <li>Domínio com caracteres alfanuméricos, ., -</li>
   * <li>Extensão com no mínimo 2 caracteres alfabéticos</li>
   * </ul>
   */
  private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

  /**
   * Executa a validação completa do endereço de email fornecido.
   *
   * <p>
   * Caso o email seja {@code null} ou vazio, a validação
   * será considerada válida, permitindo que a obrigatoriedade
   * seja tratada por outras annotations como {@code @NotBlank}.
   *
   * <p>
   * O processo de validação segue as etapas:
   * <ol>
   * <li>Normaliza o email para minúsculas</li>
   * <li>Valida o formato através de expressão regular</li>
   * <li>Extrai o domínio do email</li>
   * <li>Verifica a existência de registros MX para o domínio</li>
   * </ol>
   *
   * @param value   endereço de email a ser validado
   * @param context contexto utilizado pelo Bean Validation
   *                para registrar erros personalizados
   * @return {@code true} se o email possui formato válido
   *         e registros MX válidos; {@code false} caso contrário
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    if (value == null || value.isBlank()) {
      return true;
    }

    String email = value.trim().toLowerCase();

    if (!isValidEmailFormat(email)) {

      context.disableDefaultConstraintViolation();

      context.buildConstraintViolationWithTemplate("{user.email.invalid}").addConstraintViolation();

      return false;
    }

    String domain = extractDomain(email);

    if (!hasMxRecord(domain)) {

      context.disableDefaultConstraintViolation();

      context.buildConstraintViolationWithTemplate("{user.email.invalid}").addConstraintViolation();

      return false;
    }

    return true;
  }

  /**
   * Valida se o endereço de email está em conformidade
   * com a expressão regular definida.
   *
   * @param email endereço de email a ser validado
   * @return {@code true} se o email possui formato válido;
   *         {@code false} caso contrário
   */
  private boolean isValidEmailFormat(String email) {
    return email.matches(EMAIL_PATTERN);
  }

  /**
   * Extrai o domínio (parte após o @) do endereço de email.
   *
   * @param email endereço de email com formato válido
   * @return a porção de domínio do email
   */
  private String extractDomain(String email) {
    return email.substring(email.indexOf("@") + 1);
  }

  /**
   * Verifica a existência de registros MX (Mail Exchange) para
   * o domínio fornecido através de consulta ao servidor DNS.
   *
   * <p>
   * O método utiliza a API JNDI para realizar consultas DNS
   * e verifica se o domínio possui ao menos um registro MX válido.
   * Caso ocorra qualquer erro durante a consulta (DNS indisponível,
   * domínio inválido, etc.), o método retorna {@code false}.
   *
   * @param domain domínio a ser consultado no servidor DNS
   * @return {@code true} se o domínio possui registros MX;
   *         {@code false} caso não possua ou em caso de erro
   */
  private boolean hasMxRecord(String domain) {

    Hashtable<String, String> env = new Hashtable<>();

    env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

    InitialDirContext context = null;

    try {

      context = new InitialDirContext(env);

      Attributes attributes = context.getAttributes(domain, new String[] { "MX" });

      Attribute attribute = attributes.get("MX");

      return attribute != null && attribute.size() > 0;

    } catch (Exception e) {

      return false;

    } finally {
      closeContext(context);
    }
  }

  /**
   * Fecha o contexto JNDI de forma segura, capturando
   * qualquer exceção que possa ocorrer durante o encerramento.
   *
   * @param context contexto JNDI a ser fechado
   */
  private void closeContext(InitialDirContext context) {

    if (context == null) {
      return;
    }

    try {
      context.close();
    } catch (Exception e) {
      // ignora erro ao fechar
    }
  }
}