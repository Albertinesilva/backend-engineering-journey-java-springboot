package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.dscatalog.validation.user.annotation.PasswordPersonalData;
import com.albertsilva.dev.dscatalog.validation.user.contract.PasswordPersonalDataCandidate;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela annotation
 * {@link PasswordPersonalData}.
 *
 * <p>
 * Este validator garante que a senha informada não contenha dados
 * pessoais do próprio usuário, reduzindo o uso de senhas previsíveis
 * e fáceis de serem descobertas.
 * </p>
 *
 * <p>
 * Atualmente são analisados:
 * </p>
 * <ul>
 * <li>Primeiro nome</li>
 * <li>Sobrenome</li>
 * <li>Parte local do email (antes do "@")</li>
 * </ul>
 *
 * <p>
 * A validação é aplicada a qualquer DTO que implemente o contrato
 * {@link PasswordPersonalDataCandidate}, tornando a regra reutilizável
 * em diferentes fluxos da aplicação.
 * </p>
 *
 * <p>
 * As violações são registradas diretamente no atributo
 * {@code password}, permitindo que o Bean Validation resolva as
 * mensagens através do {@code MessageSource}, respeitando o locale
 * da requisição.
 * </p>
 */
public class PasswordPersonalDataValidator
        implements ConstraintValidator<PasswordPersonalData, PasswordPersonalDataCandidate> {

    /**
     * Quantidade mínima de caracteres que um token deve possuir
     * para ser considerado durante a validação.
     */
    private static final int MIN_TOKEN_LENGTH = 3;

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