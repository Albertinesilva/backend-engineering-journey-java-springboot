package com.albertsilva.dev.dscatalog.validation.user.validator;

import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a lógica de validação utilizada pela
 * annotation {@link UniqueEmail}.
 *
 * <p>
 * Este validator verifica se o endereço de email fornecido
 * é único no banco de dados e não está registrado por
 * outro usuário da aplicação.
 * </p>
 *
 * <p>
 * O validator realiza uma consulta no repositório de usuários
 * utilizando operação case-insensitive para garantir que
 * emails com variações de maiúsculas/minúsculas sejam
 * considerados iguais.
 * </p>
 *
 * <p>
 * As mensagens de validação são registradas através de chaves
 * do {@code MessageSource}, permitindo internacionalização
 * automática conforme o header {@code Accept-Language}.
 * </p>
 */
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    /**
     * Repositório utilizado para consultar dados
     * de usuários.
     */
    private final UserRepository repository;

    /**
     * Construtor.
     *
     * @param repository repositório de usuários
     */
    public UniqueEmailValidator(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Executa a validação de unicidade do endereço de email.
     *
     * @param value   endereço de email informado
     * @param context contexto do Bean Validation
     * @return {@code true} caso o email seja válido;
     *         {@code false} caso já exista
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        String normalizedEmail = value.trim().toLowerCase();

        boolean emailAlreadyExists = repository.existsByEmailIgnoreCase(normalizedEmail);

        if (!emailAlreadyExists) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate("{user.email.unique}").addConstraintViolation();

        return false;
    }
}