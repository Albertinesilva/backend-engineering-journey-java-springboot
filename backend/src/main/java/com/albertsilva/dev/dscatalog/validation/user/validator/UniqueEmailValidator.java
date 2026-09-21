package com.albertsilva.dev.dscatalog.validation.user.validator;

import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator de {@link UniqueEmail}: rejeita e-mail já cadastrado.
 *
 * <p>
 * <b>Comportamento:</b> {@code null} ou em branco ⇒ <b>válido</b>. Caso
 * contrário, normaliza o valor com {@code trim()} e {@code toLowerCase()} (sem
 * {@code Locale}) e consulta {@code UserRepository.existsByEmailIgnoreCase}; se
 * existir, registra {@code {user.email.unique}} e retorna {@code false}.
 * </p>
 *
 * <p>
 * <b>Limites conhecidos:</b> a consulta compara com o e-mail <em>gravado</em>
 * ignorando a caixa, mas não remove espaços do valor gravado; e o mapper grava o
 * e-mail como recebido. Portanto um e-mail persistido com espaços nas pontas
 * não seria encontrado por esta verificação. A checagem e a gravação não são
 * atômicas (a restrição {@code UNIQUE} do banco, sensível à caixa, é a garantia
 * final). Não considera o usuário autenticado nem a URL; por isso é usado só
 * em fluxos de criação.
 * </p>
 *
 * <p>
 * <b>Efeitos:</b> consulta o banco; sem acesso a HTTP, autenticação ou rede.
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
     * Verifica se o e-mail (normalizado) ainda não existe na tabela de usuários.
     *
     * @param value   e-mail informado (pode ser {@code null})
     * @param context contexto usado para registrar a violação personalizada
     * @return {@code true} se for nulo/em branco ou ainda não estiver cadastrado;
     *         {@code false} se já existir (mensagem {@code {user.email.unique}})
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