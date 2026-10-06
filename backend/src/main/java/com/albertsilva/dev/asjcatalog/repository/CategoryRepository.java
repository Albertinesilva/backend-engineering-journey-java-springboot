package com.albertsilva.dev.asjcatalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.asjcatalog.entity.Category;

/**
 * Repositório responsável pelo acesso a dados da entidade {@link Category}.
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
 * <li>Salvar uma categoria (save)</li>
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
public interface CategoryRepository extends JpaRepository<Category, Long> {

  /**
   * Busca categorias cujo nome contenha o valor informado (ignorando
   * maiúsculas/minúsculas).
   *
   * <p>
   * <b>Como o Spring interpreta esse método:</b>
   * </p>
   * <ul>
   * <li><b>findBy</b> → operação de busca</li>
   * <li><b>Name</b> → campo da entidade</li>
   * <li><b>Containing</b> → busca parcial (LIKE %valor%)</li>
   * <li><b>IgnoreCase</b> → ignora diferenças entre maiúsculas/minúsculas</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo prático:</b>
   * </p>
   * 
   * <pre>
   * Busca por "ele" pode retornar:
   * - Eletrônicos
   * - Eletrodomésticos
   * </pre>
   *
   * <p>
   * <b>Paginação:</b>
   * </p>
   * <ul>
   * <li>O resultado é paginado usando {@link Pageable}</li>
   * <li>Evita retorno de grandes volumes de dados</li>
   * </ul>
   *
   * @param name     termo para busca no nome da categoria
   * @param pageable configurações de paginação (página, tamanho, ordenação)
   * @return página contendo categorias encontradas
   */
  Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

  /**
   * Verifica se existe uma categoria com o nome informado (ignorando
   * maiúsculas/minúsculas).
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de unicidade antes de criar ou atualizar uma categoria</li>
   * </ul>
   *
   * @param name nome da categoria a ser verificada
   * @return {@code true} se existir uma categoria com o nome, {@code false} caso
   *         contrário
   */
  boolean existsByNameIgnoreCase(String name);

  /**
   * Verifica se existe uma categoria com o nome informado (ignorando
   * maiúsculas/minúsculas) e ID diferente do fornecido.
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de unicidade antes de atualizar uma categoria, ignorando a
   * própria categoria sendo atualizada</li>
   * </ul>
   *
   * @param name nome da categoria a ser verificada
   * @param id   ID da categoria que está sendo atualizada (a ser ignorada na
   *             verificação)
   * @return {@code true} se existir outra categoria com o mesmo nome,
   *         {@code false}
   *         caso contrário
   */
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

}
