package com.albertsilva.dev.asjcatalog.validation.user.validator;

import com.albertsilva.dev.asjcatalog.validation.user.annotation.ValidEmail;
import com.albertsilva.dev.asjcatalog.validation.user.dns.MxRecordLookup;

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
 * uma consulta de MX <b>síncrona</b>, na thread da requisição, por meio de
 * {@link MxRecordLookup}, recebido por injeção de dependência (em produção,
 * {@code JndiMxRecordLookup}, que consulta o DNS real). Não há acesso a banco
 * de dados nem a HTTP/autenticação.
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
   * Consulta de registro MX do domínio.
   */
  private final MxRecordLookup mxRecordLookup;

  /**
   * Construtor.
   *
   * @param mxRecordLookup consulta de registro MX do domínio
   */
  public ValidEmailValidator(MxRecordLookup mxRecordLookup) {
    this.mxRecordLookup = mxRecordLookup;
  }

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

    if (!mxRecordLookup.hasMxRecord(domain)) {

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
}