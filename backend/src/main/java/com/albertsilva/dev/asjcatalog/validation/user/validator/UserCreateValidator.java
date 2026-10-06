package com.albertsilva.dev.asjcatalog.validation.user.validator;

import java.util.ArrayList;
import java.util.List;

import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.validation.user.annotation.UserCreateValid;
import com.albertsilva.dev.asjcatalog.web.exception.response.FieldMessage;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela
 * annotation {@link UserCreateValid}.
 *
 * <p>
 * Este validator verifica se os dados fornecidos para
 * a criação de um novo usuário atendem aos critérios
 * de segurança definidos pela aplicação.
 *
 * <p>
 * As validações realizadas incluem:
 * <ul>
 * <li>Verifica se a senha não contém o primeiro nome do usuário</li>
 * <li>Verifica se a senha não contém o sobrenome do usuário</li>
 * <li>Verifica se a senha não contém a parte local do email (antes do @)</li>
 * </ul>
 *
 * <p>
 * As mensagens de erro são adicionadas manualmente
 * ao contexto de validação utilizando
 * {@link ConstraintValidatorContext}.
 */
public class UserCreateValidator implements ConstraintValidator<UserCreateValid, UserCreateRequest> {

    /**
     * Tamanho mínimo que um token (nome/sobrenome/email prefix)
     * deve ter para ser considerado na validação da senha.
     */
    private static final int MIN_TOKEN_LENGTH = 3;

    /**
     * Executa a validação completa dos dados de criação de usuário.
     *
     * <p>
     * O método valida se os dados fornecidos no DTO de criação
     * atendem aos critérios de segurança, particularmente verificando
     * se a senha não contém informações pessoais do usuário.
     *
     * @param dto     objeto contendo os dados de criação do usuário
     * @param context contexto utilizado pelo Bean Validation
     *                para registrar erros personalizados
     * @return {@code true} se todos os dados são válidos;
     *         {@code false} caso existam erros de validação
     */
    @Override
    public boolean isValid(UserCreateRequest dto, ConstraintValidatorContext context) {

        List<FieldMessage> errors = new ArrayList<>();

        validatePasswordDoesNotContainPersonalData(dto, errors);

        addErrors(errors, context);

        return errors.isEmpty();
    }

    /**
     * Valida se a senha não contém dados pessoais do usuário
     * como nome, sobrenome ou parte local do email.
     *
     * <p>
     * Caso a senha seja {@code null}, a validação é ignorada,
     * permitindo que a obrigatoriedade seja tratada por outras
     * annotations como {@code @NotBlank}.
     *
     * @param dto    objeto contendo os dados de criação do usuário
     * @param errors lista responsável por armazenar
     *               os erros encontrados
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
     * Valida se a senha contém um token específico (nome, sobrenome ou
     * parte local do email) de forma case-insensitive.
     *
     * <p>
     * O token é considerado válido apenas se possuir um tamanho
     * mínimo definido por {@link #MIN_TOKEN_LENGTH}. Caso contrário,
     * a validação é ignorada.
     *
     * <p>
     * Se a senha contiver o token normalizado, um erro é adicionado
     * à lista de erros, evitando duplicatas.
     *
     * @param password senha normalizada em minúsculas
     * @param value    valor do token a ser verificado (nome, sobrenome, etc)
     * @param errors   lista responsável por armazenar
     *                 os erros encontrados
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
                && error.message().equals("Senha não pode conter dados pessoais"));

        if (password.contains(normalized) && !alreadyExists) {
            errors.add(new FieldMessage("password", "Senha não pode conter dados pessoais"));
        }
    }

    /**
     * Adiciona ao contexto de validação todos os erros
     * encontrados durante o processo de validação.
     *
     * <p>
     * O método desabilita a mensagem padrão do Bean Validation
     * para permitir o registro de mensagens customizadas e
     * associar cada erro ao campo específico via
     * {@link ConstraintValidatorContext#buildConstraintViolationWithTemplate(String)}.
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