package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link StrongPassword}: aplica regras de composição à senha.
 *
 * <p>
 * <b>Comportamento:</b> {@code null} ⇒ <b>válido</b>. Qualquer outro valor,
 * <b>inclusive {@code ""} e texto só com espaços</b>, é avaliado. Todas as
 * regras são verificadas (não há interrupção na primeira falha) e <b>cada
 * regra violada gera uma violação própria</b>, com chave específica:
 * </p>
 * <ul>
 * <li>{@code user.password.whitespace}: contém algum espaço em branco
 * ({@code Character.isWhitespace});</li>
 * <li>{@code user.password.uppercase}: nenhuma letra A-Z;</li>
 * <li>{@code user.password.lowercase}: nenhuma letra a-z;</li>
 * <li>{@code user.password.number}: nenhum dígito 0-9;</li>
 * <li>{@code user.password.specialCharacter}: nenhum caractere fora de
 * A-Z, a-z, 0-9 e espaço — <b>letras acentuadas contam como "especial"</b>;</li>
 * <li>{@code user.password.common}: a senha (após {@code trim} e minúsculas) é
 * exatamente uma das 9 senhas comuns da lista;</li>
 * <li>{@code user.password.sequence}: os dígitos da senha, tomados em
 * conjunto, contêm 6 ou mais dígitos consecutivos crescentes ou decrescentes
 * (ver {@code validateNumericSequences}).</li>
 * </ul>
 *
 * <p>
 * <b>Não verifica tamanho</b> (mínimo/máximo ficam a cargo de {@code @Size} nos
 * DTOs, quando existe) <b>nem dados pessoais</b> (ver
 * {@link PasswordPersonalDataValidator}). Todas as verificações de letras e
 * dígitos são ASCII. Não há dependências, banco, HTTP ou rede.
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

    /**
     * Avalia todas as regras da senha e registra uma violação por regra violada.
     *
     * @param value   senha informada (pode ser {@code null})
     * @param context contexto usado para registrar as violações
     * @return {@code true} se for {@code null} ou não violar nenhuma regra
     */
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

    /**
     * Registra {@code user.password.whitespace} se a senha contiver qualquer
     * caractere para o qual {@code Character.isWhitespace} seja verdadeiro.
     */
    private void validateWhitespace(String password, List<String> errors) {

        boolean containsWhitespace = password.chars().anyMatch(Character::isWhitespace);

        if (containsWhitespace) {
            errors.add("{user.password.whitespace}");
        }
    }

    /** Registra {@code user.password.uppercase} se não houver letra A-Z. */
    private void validateUppercase(String password, List<String> errors) {

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.uppercase}");
        }
    }

    /** Registra {@code user.password.lowercase} se não houver letra a-z. */
    private void validateLowercase(String password, List<String> errors) {

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.lowercase}");
        }
    }

    /** Registra {@code user.password.number} se não houver dígito 0-9. */
    private void validateNumber(String password, List<String> errors) {

        if (!NUMBER_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.number}");
        }
    }

    /**
     * Registra {@code user.password.specialCharacter} se não houver nenhum caractere
     * fora de A-Z, a-z, 0-9 e espaço (letras acentuadas satisfazem esta regra).
     */
    private void validateSpecialCharacter(String password, List<String> errors) {

        if (!SPECIAL_CHARACTER_PATTERN.matcher(password).matches()) {
            errors.add("{user.password.specialCharacter}");
        }
    }

    /**
     * Registra {@code user.password.common} se a senha, após {@code trim()} e
     * {@code toLowerCase()}, for <b>exatamente igual</b> a uma das senhas da lista
     * interna (comparação de igualdade, não de conteúdo). Como todas as entradas
     * da lista são só dígitos ou só letras minúsculas, uma senha assim já viola
     * também as regras de maiúscula e de caractere especial.
     *
     * @param password senha informada
     * @param errors   lista que acumula as chaves de mensagem das violações
     */
    private void validateCommonPasswords(String password, List<String> errors) {

        String normalizedPassword = password.trim().toLowerCase();

        if (COMMON_PASSWORDS.contains(normalizedPassword)) {
            errors.add("{user.password.common}");
        }
    }

    /**
     * Detecta sequências numéricas previsíveis.
     *
     * <p>
     * Extrai <b>todos os dígitos da senha, concatenados</b> (descartando os demais
     * caracteres) e procura, nessa cadeia, {@value #MIN_NUMERIC_SEQUENCE_LENGTH}
     * ou mais dígitos consecutivos em que cada um é exatamente o anterior mais 1
     * (por exemplo, {@code 123456}) ou menos 1 (por exemplo, {@code 654321}). Como
     * os dígitos são concatenados, dígitos <em>separados</em> por outros
     * caracteres na senha também formam sequência (por exemplo, {@code a1b2c3d4e5f6}).
     * Se a cadeia tiver menos de 6 dígitos, nada é verificado. No máximo uma
     * violação {@code user.password.sequence} é registrada.
     * </p>
     *
     * @param password senha informada
     * @param errors   lista que acumula as chaves de mensagem das violações
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

    /**
     * Se houver erros, desabilita a violação padrão ({@code {user.password.strong}})
     * e registra cada chave acumulada como violação separada, sem nó de propriedade
     * (herda o campo anotado).
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