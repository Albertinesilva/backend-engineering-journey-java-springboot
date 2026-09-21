package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.Hashtable;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import com.albertsilva.dev.dscatalog.validation.user.annotation.ValidEmail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link ValidEmail}: confere o <b>formato</b> do e-mail e a
 * <b>existência de registro MX</b> para o domínio, por consulta DNS.
 *
 * <p>
 * <b>Comportamento (em ordem):</b>
 * </p>
 * <ol>
 * <li>{@code null} ou em branco ({@code isBlank()}) ⇒ <b>válido</b> (a
 * obrigatoriedade é de {@code @NotBlank});</li>
 * <li>normaliza <em>apenas para validar</em>: {@code trim()} e
 * {@code toLowerCase()} (este sem {@code Locale});</li>
 * <li>a expressão regular {@code EMAIL_PATTERN} deve casar com o valor
 * inteiro; senão ⇒ inválido;</li>
 * <li>o domínio (texto após o {@code @}) deve ter ao menos um registro MX;
 * senão ⇒ inválido.</li>
 * </ol>
 *
 * <p>
 * Em qualquer falha registra a mensagem {@code {user.email.invalid}}, sem
 * distinguir "formato inválido" de "domínio sem MX" (nem de "DNS
 * indisponível"). O valor original do DTO <b>não</b> é alterado: quem persiste
 * o e-mail (mapper/service) recebe o texto como foi enviado, inclusive com
 * espaços nas pontas ou letras maiúsculas.
 * </p>
 *
 * <p>
 * <b>Efeito externo:</b> cada validação de e-mail que passa pelo formato faz
 * uma consulta DNS <b>síncrona</b>, na thread da requisição. Não há injeção de
 * dependências, banco de dados nem acesso a HTTP/autenticação.
 * </p>
 */
public class ValidEmailValidator implements ConstraintValidator<ValidEmail, String> {

  /**
   * Expressão regular do formato, aplicada ao e-mail já normalizado:
   * {@code ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$}.
   *
   * <p>
   * Aceita somente caracteres ASCII (sem IDN nem acentos); a parte local aceita
   * letras, dígitos e {@code + _ . -}, sem limite de tamanho e sem restrição
   * quanto a pontos consecutivos ou nas extremidades; o domínio aceita letras,
   * dígitos, {@code .} e {@code -}, e termina em um sufixo de pelo menos 2 letras.
   * </p>
   */
  private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

  /**
   * Valida o e-mail conforme descrito na documentação da classe.
   *
   * <p>
   * {@code null} e valores em branco são considerados válidos. A validação usa
   * {@code value.trim().toLowerCase()}, mas o valor original não é modificado.
   * Falha de formato e ausência de MX (ou falha na consulta) produzem a mesma
   * mensagem, {@code {user.email.invalid}}.
   * </p>
   *
   * @param value   e-mail informado (pode ser {@code null})
   * @param context contexto usado para registrar a violação personalizada
   * @return {@code true} se for nulo/em branco ou tiver formato válido e MX;
   *         {@code false} caso contrário
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
   * Consulta o DNS (via JNDI, provedor {@code com.sun.jndi.dns.DnsContextFactory},
   * sem servidor, timeout ou tentativas configurados no código) e verifica se o
   * domínio possui ao menos um registro do tipo MX.
   *
   * <p>
   * Só o tipo MX é considerado: não há alternativa para domínios que aceitam
   * e-mail apenas por registro A/AAAA. <b>Qualquer {@link Exception}</b> (domínio
   * inexistente, DNS indisponível, tempo esgotado etc.) resulta em {@code false},
   * o que faz uma falha de rede tornar inválido um e-mail que talvez fosse
   * válido. O contexto JNDI é fechado no bloco {@code finally}.
   * </p>
   *
   * @param domain domínio a consultar
   * @return {@code true} se houver ao menos um registro MX; {@code false} se não
   *         houver ou se ocorrer qualquer erro
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