package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela annotation
 * {@link StrongPassword}.
 *
 * <p>
 * Este validator é responsável exclusivamente pelas regras de segurança
 * da senha que não são cobertas pelas constraints padrão do Bean Validation.
 * </p>
 *
 * <p>
 * As seguintes regras são aplicadas:
 * </p>
 * <ul>
 * <li>Ausência de espaços em branco</li>
 * <li>Presença de letra maiúscula</li>
 * <li>Presença de letra minúscula</li>
 * <li>Presença de número</li>
 * <li>Presença de caractere especial</li>
 * <li>Bloqueio de senhas comuns e padrões numéricos previsíveis</li>
 * </ul>
 *
 * <p>
 * A obrigatoriedade da senha e a validação do tamanho mínimo e máximo
 * são delegadas às constraints padrão do Bean Validation, como
 * {@link jakarta.validation.constraints.NotBlank} e
 * {@link jakarta.validation.constraints.Size}.
 * </p>
 *
 * <p>
 * Todas as violações são registradas utilizando chaves do
 * {@code MessageSource}, permitindo internacionalização automática.
 * </p>
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");

    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");

    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d.*");

    private static final Pattern SPECIAL_CHARACTER_PATTERN = Pattern.compile(".*[^a-zA-Z0-9\\s].*");

    /**
     * Senhas amplamente conhecidas por serem inseguras.
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of("123456", "1234567", "12345678", "password", "admin",
            "qwerty", "abc123", "111111", "123123");

    /**
     * Comprimento mínimo de uma sequência numérica considerada insegura.
     */
    private static final int MIN_NUMERIC_SEQUENCE_LENGTH = 6;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        List<String> errors = new ArrayList<>();

        validateWhitespace(value, errors);
        validateUppercase(value, errors);
        validateLowercase(value, errors);
        validateNumber(value, errors);
        validateSpecialCharacter(value, errors);
        validateCommonPasswords(value, errors);
        validateNumericSequences(value, errors);

        addErrors(errors, context);

        return errors.isEmpty();
    }

    private void validateWhitespace(String password, List<String> errors) {

        boolean containsWhitespace = password.chars().anyMatch(Character::isWhitespace);

        if (containsWhitespace) {
            errors.add("{user.password.whitespace}");
        }
    }

    private void validateUppercase(String password, List<String> errors) {

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.uppercase}");
        }
    }

    private void validateLowercase(String password, List<String> errors) {

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.lowercase}");
        }
    }

    private void validateNumber(String password, List<String> errors) {

        if (!NUMBER_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.number}");
        }
    }

    private void validateSpecialCharacter(String password, List<String> errors) {

        if (!SPECIAL_CHARACTER_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.specialCharacter}");
        }
    }

    /**
     * Valida se a senha corresponde exatamente a uma senha
     * amplamente conhecida por ser insegura.
     *
     * <p>
     * A comparação é realizada após normalização para letras
     * minúsculas, permitindo bloquear variações como
     * {@code PASSWORD}, {@code Password} e {@code password}.
     * </p>
     *
     * @param password senha informada
     * @param errors   lista de erros encontrados
     */
    private void validateCommonPasswords(String password, List<String> errors) {

        String normalizedPassword = password.trim().toLowerCase();

        if (COMMON_PASSWORDS.contains(normalizedPassword)) {
            errors.add("{user.password.common}");
        }
    }

    /**
     * Valida se a senha contém sequências numéricas
     * crescentes ou decrescentes.
     *
     * <p>
     * São consideradas inseguras sequências com pelo menos
     * {@value #MIN_NUMERIC_SEQUENCE_LENGTH} dígitos consecutivos,
     * como:
     * </p>
     *
     * <ul>
     * <li>123456</li>
     * <li>1234567</li>
     * <li>654321</li>
     * <li>987654</li>
     * </ul>
     *
     * @param password senha informada
     * @param errors   lista de erros encontrados
     */
    private void validateNumericSequences(String password, List<String> errors) {

        String digits = password.replaceAll("\\D", "");

        if (digits.length() < MIN_NUMERIC_SEQUENCE_LENGTH) {
            return;
        }

        int ascending = 1;
        int descending = 1;

        for (int i = 1; i < digits.length(); i++) {

            int previous = digits.charAt(i - 1);
            int current = digits.charAt(i);

            if (current == previous + 1) {
                ascending++;
            } else {
                ascending = 1;
            }

            if (current == previous - 1) {
                descending++;
            } else {
                descending = 1;
            }

            if (ascending >= MIN_NUMERIC_SEQUENCE_LENGTH || descending >= MIN_NUMERIC_SEQUENCE_LENGTH) {

                errors.add("{user.password.sequence}");
                return;
            }
        }
    }

    private void addErrors(List<String> errors, ConstraintValidatorContext context) {

        if (errors.isEmpty()) {
            return;
        }

        context.disableDefaultConstraintViolation();

        for (String error : errors) {
            context.buildConstraintViolationWithTemplate(error).addConstraintViolation();
        }
    }
}