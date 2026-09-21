package com.albertsilva.dev.dscatalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.dscatalog.domain.recovery.Email;

/**
 * Repositório Spring Data JPA da entidade {@link Email} (tabela
 * {@code tb_email}), com chave primária {@code Long}.
 *
 * <p>
 * Não declara nenhuma consulta própria: usa apenas as operações herdadas de
 * {@link JpaRepository}. No código atual, somente {@code save} é utilizado, por
 * {@code EmailService}, para gravar o registro de cada e-mail enviado.
 * </p>
 */
@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
  
}
