package com.albertsilva.dev.asjcatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.albertsilva.dev.asjcatalog.validation.user.annotation.StrongPassword;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela
 * annotation {@link StrongPassword}.
 *
 * <p>
 * Este validator verifica se a senha atende
 * aos critérios mínimos de segurança definidos
 * pela aplicação.
 *
 * <p>
 * As seguintes regras são aplicadas:
 * <ul>
 * <li>Mínimo de caracteres</li>
 * <li>Presença de letra maiúscula</li>
 * <li>Presença de letra minúscula</li>
 * <li>Presença de número</li>
 * <li>Presença de caractere especial</li>
 * <li>Ausência de espaços em branco</li>
 * <li>Bloqueio de padrões de senhas comuns</li>
 * </ul>
 *
 * <p>
 * As mensagens de erro são adicionadas manualmente
 * ao contexto de validação utilizando
 * {@link ConstraintValidatorContext}.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    /**
     * Quantidade mínima de caracteres exigida
     * para a senha.
     */
    private static final int MIN_LENGTH = 10;

    /**
     * Expressão regular utilizada para validar
     * a presença de letras maiúsculas.
     */
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");

    /**
     * Expressão regular utilizada para validar
     * a presença de letras minúsculas.
     */
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");

    /**
     * Expressão regular utilizada para validar
     * a presença de números.
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d.*");

    /**
     * Expressão regular utilizada para validar
     * a presença de caracteres especiais.
     */
    private static final Pattern SPECIAL_CHARACTER_PATTERN = Pattern.compile(".*[^a-zA-Z0-9\\s].*");

    /**
     * Lista contendo padrões de senhas comuns
     * e inseguras bloqueadas pela aplicação.
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of("123456", "12345678", "password", "admin", "qwerty",
            "abc123", "111111", "123123");

    /**
     * Executa a validação completa da senha informada.
     *
     * <p>
     * Caso a senha seja {@code null}, a validação
     * será considerada válida, permitindo que a
     * obrigatoriedade seja tratada por outras
     * annotations como {@code @NotBlank}.
     *
     * <p>
     * Todas as inconsistências encontradas durante
     * a validação são armazenadas em uma lista de erros
     * e adicionadas posteriormente ao contexto de validação.
     *
     * @param value   senha informada
     * @param context contexto utilizado pelo Bean Validation
     *                para registrar erros personalizados
     * @return {@code true} caso a senha seja válida;
     *         {@code false} caso existam erros de validação
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        List<String> errors = new ArrayList<>();

        validateBlank(value, errors);

        if (!errors.isEmpty()) {
            addErrors(errors, context);
            return false;
        }

        validateWhitespace(value, errors);
        validateLength(value, errors);
        validateUppercase(value, errors);
        validateLowercase(value, errors);
        validateNumber(value, errors);
        validateSpecialCharacter(value, errors);
        validateCommonPasswords(value, errors);

        addErrors(errors, context);

        return errors.isEmpty();
    }

    /**
     * Valida se a senha está vazia.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateBlank(String password, List<String> errors) {

        if (password.isBlank()) {
            errors.add("Senha não pode ser vazia");
        }
    }

    /**
     * Valida se a senha contém espaços em branco.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateWhitespace(String password, List<String> errors) {

        boolean containsWhitespace = password.chars().anyMatch(Character::isWhitespace);

        if (containsWhitespace) {
            errors.add("Senha não pode conter espaços");
        }
    }

    /**
     * Valida se a senha possui a quantidade mínima
     * de caracteres exigida pela aplicação.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateLength(String password, List<String> errors) {

        if (password.length() < MIN_LENGTH) {
            errors.add(String.format("Senha deve possuir ao menos %d caracteres", MIN_LENGTH));
        }
    }

    /**
     * Valida se a senha contém ao menos
     * uma letra maiúscula.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateUppercase(String password, List<String> errors) {

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Senha deve conter ao menos uma letra maiúscula");
        }
    }

    /**
     * Valida se a senha contém ao menos
     * uma letra minúscula.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateLowercase(String password, List<String> errors) {

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("Senha deve conter ao menos uma letra minúscula");
        }
    }

    /**
     * Valida se a senha contém ao menos um número.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateNumber(String password, List<String> errors) {

        if (!NUMBER_PATTERN.matcher(password).matches()) {
            errors.add("Senha deve conter ao menos um número");
        }
    }

    /**
     * Valida se a senha contém ao menos
     * um caractere especial.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateSpecialCharacter(String password, List<String> errors) {

        if (!SPECIAL_CHARACTER_PATTERN.matcher(password).matches()) {
            errors.add("Senha deve conter ao menos um caractere especial");
        }
    }

    /**
     * Valida se a senha contém padrões conhecidos
     * de senhas comuns e inseguras.
     *
     * <p>
     * A comparação é realizada de forma
     * case-insensitive.
     *
     * @param password senha informada
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
     */
    private void validateCommonPasswords(String password, List<String> errors) {

        String normalizedPassword = password.toLowerCase();

        boolean containsForbiddenPattern = COMMON_PASSWORDS.stream().anyMatch(normalizedPassword::contains);

        if (containsForbiddenPattern) {
            errors.add("Senha contém padrões muito comuns e inseguros");
        }
    }

    /**
     * Adiciona ao contexto de validação todos os erros
     * encontrados durante o processo de validação.
     *
     * <p>
     * O método desabilita a mensagem padrão do Bean Validation
     * para permitir o registro de mensagens customizadas.
     *
     * @param errors  lista contendo os erros encontrados
     * @param context contexto utilizado para registrar
     *                as violações de validação
     */
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