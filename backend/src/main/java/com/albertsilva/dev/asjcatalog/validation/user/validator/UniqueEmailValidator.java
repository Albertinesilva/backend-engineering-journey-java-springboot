package com.albertsilva.dev.asjcatalog.validation.user.validator;

import com.albertsilva.dev.asjcatalog.repository.UserRepository;
import com.albertsilva.dev.asjcatalog.validation.user.annotation.UniqueEmail;

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
 *
 * <p>
 * O validator realiza uma consulta no repositório de usuários
 * utilizando a operação case-insensitive para garantir que
 * emails com variações de maiúsculas/minúsculas sejam
 * considerados iguais.
 *
 * <p>
 * Caso o email seja {@code null} ou vazio, a validação
 * será considerada válida, permitindo que a obrigatoriedade
 * seja tratada por outras annotations como {@code @NotBlank}.
 */
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    /**
     * Repositório utilizado para consultar dados
     * de usuários no banco de dados.
     */
    private final UserRepository repository;

    /**
     * Construtor que recebe o repositório de usuários
     * por injeção de dependência.
     *
     * @param repository repositório responsável pelas
     *                   operações de busca no banco de dados
     */
    public UniqueEmailValidator(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Executa a validação de unicidade do endereço de email.
     *
     * <p>
     * O método normaliza o email para minúsculas e remove
     * espaços em branco antes de consultar o banco de dados.
     * A verificação no repositório é case-insensitive,
     * garantindo que variações de maiúsculas/minúsculas
     * não resultem em duplicatas.
     *
     * @param value   endereço de email a ser validado
     * @param context contexto utilizado pelo Bean Validation
     *                para registrar erros personalizados
     * @return {@code true} se o email é único ou está vazio;
     *         {@code false} se o email já existe no banco
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        String normalizedEmail = value.trim().toLowerCase();

        return !repository.existsByEmailIgnoreCase(normalizedEmail);
    }
}