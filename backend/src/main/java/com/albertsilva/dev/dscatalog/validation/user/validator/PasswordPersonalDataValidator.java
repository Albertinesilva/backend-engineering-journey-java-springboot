package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.dscatalog.validation.user.annotation.PasswordPersonalData;
import com.albertsilva.dev.dscatalog.validation.user.contract.PasswordPersonalDataCandidate;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link PasswordPersonalData}: impede que a senha <em>contenha</em>
 * dados pessoais do próprio candidato a usuário.
 *
 * <p>
 * <b>Comportamento:</b> candidato {@code null} ou senha {@code null} ⇒
 * <b>válido</b>. A senha é normalizada ({@code trim()} + {@code toLowerCase()})
 * e comparada, por <b>substring</b> ({@code contains}), com três valores, também
 * normalizados: primeiro nome, sobrenome e a parte do e-mail antes do
 * <b>primeiro</b> {@code @} (só se o e-mail contiver {@code @}). Valores
 * {@code null} ou com menos de 3 caracteres são
 * ignorados. Nomes compostos (com espaço) são comparados inteiros e, como a senha
 * forte não aceita espaços, tendem a nunca casar.
 * </p>
 *
 * <p>
 * <b>Violação:</b> no máximo <b>uma</b>, {@code {user.password.personalData}},
 * associada ao campo {@code password} (mesmo que mais de um dado case). Não
 * consulta banco, HTTP nem autenticação. Usa {@code FieldMessage}, do pacote de
 * exceções da camada web, como estrutura de acúmulo.
 * </p>
 */
public class PasswordPersonalDataValidator
        implements ConstraintValidator<PasswordPersonalData, PasswordPersonalDataCandidate> {

    /**
     * Quantidade mínima de caracteres que um token deve possuir
     * para ser considerado durante a validação.
     */
    private static final int MIN_TOKEN_LENGTH = 3;

    /**
     * Verifica se a senha contém nome, sobrenome ou prefixo do e-mail do candidato.
     *
     * @param candidate dados do usuário (pode ser {@code null})
     * @param context   contexto usado para registrar a violação no campo
     *                  {@code password}
     * @return {@code true} se nenhum dado pessoal for encontrado na senha
     */
    @Override
    public boolean isValid(PasswordPersonalDataCandidate candidate, ConstraintValidatorContext context) {

        if (candidate == null) {
            return true;
        }

        List<FieldMessage> errors = new ArrayList<>();

        validatePasswordDoesNotContainPersonalData(candidate, errors);

        addErrors(errors, context);

        return errors.isEmpty();
    }

    /**
     * Valida se a senha não contém informações pessoais do usuário.
     *
     * @param candidate dados do usuário utilizados na validação
     * @param errors    lista de erros encontrados
     */
    private void validatePasswordDoesNotContainPersonalData(PasswordPersonalDataCandidate candidate,
            List<FieldMessage> errors) {

        if (candidate.password() == null) {
            return;
        }

        String normalizedPassword = candidate.password().trim().toLowerCase();

        validateToken(normalizedPassword, candidate.firstName(), errors);
        validateToken(normalizedPassword, candidate.lastName(), errors);

        if (candidate.email() != null && candidate.email().contains("@")) {

            String emailPrefix = candidate.email().split("@")[0];

            validateToken(normalizedPassword, emailPrefix, errors);
        }
    }

    /**
     * Verifica se um determinado dado pessoal está presente na senha.
     *
     * <p>
     * Apenas tokens com tamanho mínimo de
     * {@value #MIN_TOKEN_LENGTH} caracteres são considerados,
     * reduzindo falsos positivos durante a validação.
     * </p>
     *
     * @param password senha normalizada
     * @param value    dado pessoal a ser verificado
     * @param errors   lista de erros encontrados
     */
    private void validateToken(String password, String value, List<FieldMessage> errors) {

        if (value == null) {
            return;
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.length() < MIN_TOKEN_LENGTH) {
            return;
        }

        boolean alreadyExists = errors.stream().anyMatch(error -> error.fieldName().equals("password")
                && error.message().equals("{user.password.personalData}"));

        if (password.contains(normalized) && !alreadyExists) {

            errors.add(new FieldMessage("password", "{user.password.personalData}"));
        }
    }

    /**
     * Registra no contexto do Bean Validation todas as violações
     * encontradas durante a validação.
     *
     * <p>
     * Cada violação é associada ao atributo {@code password},
     * permitindo sua exibição correta no frontend e a resolução
     * automática da mensagem pelo {@code MessageSource}.
     * </p>
     *
     * @param errors  lista de erros encontrados
     * @param context contexto do Bean Validation
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