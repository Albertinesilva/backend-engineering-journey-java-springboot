package com.albertsilva.dev.dscatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.dscatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UserCreateValid;
import com.albertsilva.dev.dscatalog.web.exception.response.FieldMessage;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela
 * annotation {@link UserCreateValid}.
 *
 * <p>
 * Este validator verifica se os dados fornecidos para
 * criação de usuários atendem aos critérios de segurança
 * definidos pela aplicação.
 * </p>
 *
 * <p>
 * As mensagens de validação são registradas através de
 * chaves do {@code MessageSource}, permitindo
 * internacionalização automática.
 * </p>
 */
public class UserCreateValidator implements ConstraintValidator<UserCreateValid, UserCreateRequest> {

    /**
     * Tamanho mínimo de um token considerado
     * durante a validação.
     */
    private static final int MIN_TOKEN_LENGTH = 3;

    @Override
    public boolean isValid(UserCreateRequest dto, ConstraintValidatorContext context) {

        List<FieldMessage> errors = new ArrayList<>();

        validatePasswordDoesNotContainPersonalData(dto, errors);

        addErrors(errors, context);

        return errors.isEmpty();
    }

    /**
     * Valida se a senha não contém dados pessoais.
     */
    private void validatePasswordDoesNotContainPersonalData(UserCreateRequest dto, List<FieldMessage> errors) {

        if (dto.password() == null) {
            return;
        }

        String password = dto.password().trim().toLowerCase();

        validateToken(password, dto.firstName(), errors);
        validateToken(password, dto.lastName(), errors);

        if (dto.email() != null && dto.email().contains("@")) {

            String emailPrefix = dto.email().split("@")[0];

            validateToken(password, emailPrefix, errors);
        }
    }

    /**
     * Valida um token (nome, sobrenome ou prefixo do email)
     * contra a senha informada.
     */
    private void validateToken(String password, String value, List<FieldMessage> errors) {

        if (value == null) {
            return;
        }

        String normalized = value.trim().toLowerCase();

        if (normalized.length() < MIN_TOKEN_LENGTH) {
            return;
        }

        boolean alreadyExists = errors.stream().anyMatch(
                error -> error.fieldName().equals("password") && error.message().equals("{user.password.personalData}"));

        if (password.contains(normalized) && !alreadyExists) {
            errors.add(new FieldMessage("password", "{user.password.personalData}"));
        }
    }

    /**
     * Adiciona ao contexto do Bean Validation
     * todas as violações encontradas.
     *
     * <p>
     * As mensagens registradas correspondem às chaves
     * definidas no {@code MessageSource}.
     * </p>
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