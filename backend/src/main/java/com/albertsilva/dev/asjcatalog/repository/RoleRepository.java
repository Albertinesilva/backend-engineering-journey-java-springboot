package com.albertsilva.dev.asjcatalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.asjcatalog.entity.Role;

/**
 * Repositório responsável pelo acesso a dados da entidade {@link Role}.
 *
 * <p>
 * Esta interface utiliza o Spring Data JPA para fornecer operações
 * de persistência de forma automática, sem necessidade de implementação manual.
 * </p>
 *
 * <p>
 * <b>Funcionalidades fornecidas automaticamente:</b>
 * </p>
 * <ul>
 * <li>Salvar um papel (save)</li>
 * <li>Buscar por ID (findById)</li>
 * <li>Listar todas (findAll)</li>
 * <li>Deletar (delete)</li>
 * </ul>
 *
 * <p>
 * <b>Importante para júnior:</b>
 * </p>
 * <ul>
 * <li>Você não implementa essa interface</li>
 * <li>O Spring gera tudo em tempo de execução</li>
 * <li>O nome dos métodos define a query automaticamente</li>
 * </ul>
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

}
