package com.albertsilva.dev.asjcatalog.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.albertsilva.dev.asjcatalog.entity.Product;

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

 /**
   * Busca produtos cujo nome contenha o valor informado (ignorando
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
   * @param name     termo para busca no nome da produto
   * @param pageable configurações de paginação (página, tamanho, ordenação)
   * @return página contendo produtos encontrados
   */
  Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

  /**
   * Verifica se existe um produto com o nome informado (ignorando
   * maiúsculas/minúsculas).
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de dados antes de criar ou atualizar um produto</li>
   * <li>Evitar duplicidade de nomes no sistema</li>
   * </ul>
   *
   * @param name nome do produto a ser verificado
   * @return true se existir um produto com o nome informado, false caso contrário
   */
  boolean existsByNameIgnoreCase(String name);

  /**
   * Verifica se existe um produto com o nome informado (ignorando
   * maiúsculas/minúsculas) e com ID diferente do informado.
   *
   * <p>
   * <b>Uso típico:</b>
   * </p>
   * <ul>
   * <li>Validação de dados antes de atualizar um produto</li>
   * <li>Permitir que o produto atual mantenha seu nome, mas evitar que outro
   * produto tenha o mesmo nome</li>
   * </ul>
   *
   * @param name nome do produto a ser verificado
   * @param id   ID do produto a ser ignorado na verificação
   * @return true se existir um produto com o nome informado e ID diferente, false caso contrário
   */
  boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}