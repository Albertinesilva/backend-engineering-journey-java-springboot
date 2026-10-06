package com.albertsilva.dev.dscatalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.dscatalog.entity.Product;

/**
 * Repositório responsável pelo acesso a dados da entidade {@link Product}.
 *
 * <p>
 * Fornece operações básicas de persistência utilizando Spring Data JPA.
 * </p>
 *
 * <p>
 * <b>Funcionalidades disponíveis:</b>
 * </p>
 * <ul>
 * <li>Salvar produto</li>
 * <li>Buscar por ID</li>
 * <li>Listar produtos</li>
 * <li>Excluir produto</li>
 * </ul>
 *
 * <p>
 * <b>Importante:</b>
 * </p>
 * <ul>
 * <li>Mesmo sem métodos personalizados, o repositório já é totalmente
 * funcional</li>
 * <li>Novos métodos podem ser criados apenas definindo seus nomes</li>
 * </ul>
 *
 * <p>
 * <b>Exemplo de método que poderia ser adicionado:</b>
 * </p>
 * 
 * <pre>
 * Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
 * </pre>
 *
 * <p>
 * O Spring Data JPA interpretaria automaticamente esse método e geraria a
 * query.
 * </p>
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

  Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}