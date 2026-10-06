# Acesso a dados

Este guia explica como o ASJCatalog lê e grava no banco no capítulo 02: repositórios, consultas, paginação e transações.

## Sumário

1. [Repositórios](#1-repositórios)
2. [Consultas derivadas](#2-consultas-derivadas)
3. [Paginação e ordenação](#3-paginação-e-ordenação)
4. [Relacionamento com categorias](#4-relacionamento-com-categorias)
5. [Transações e open-in-view](#5-transações-e-open-in-view)
6. [Limitações conhecidas](#6-limitações-conhecidas)

## 1. Repositórios

Um **repositório** é a interface por onde o código acessa o banco. Há dois, no pacote `repository`, e os dois estendem `JpaRepository<Entidade, Long>`:

| Repositório | Entidade |
| --- | --- |
| `CategoryRepository` | `Category` |
| `ProductRepository` | `Product` |

O **Spring Data JPA** gera a implementação dessas interfaces quando a aplicação sobe. Só por estender `JpaRepository`, cada repositório já tem métodos prontos. Os que os services usam:

| Método | Uso |
| --- | --- |
| `findById(id)` | Buscar por id (devolve um `Optional`) |
| `findAll(pageable)` | Listar com paginação |
| `save(entidade)` | Inserir ou atualizar |
| `delete(entidade)` | Remover |
| `getReferenceById(id)` | Obter uma referência para atualizar sem buscar o registro antes |
| `findAllById(ids)` | Buscar várias categorias pelos ids (em `ProductService`) |

Os repositórios são testados com `@DataJpaTest`, contra o H2: `CategoryRepositoryTest` e `ProductRepositoryTest`. Veja [TESTING.md](TESTING.md#6-testes-de-repositório-com-datajpatest).

## 2. Consultas derivadas

Uma **consulta derivada** é um método cujo nome descreve a consulta. O Spring Data lê o nome e monta o SQL, sem que você escreva a consulta.

Os dois repositórios declaram a mesma consulta:

```java
Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
```

O nome se lê assim: `findBy` (buscar por) + `Name` (campo `name`) + `Containing` (contém o texto) + `IgnoreCase` (sem diferenciar maiúsculas).

Nos services, o método `search` escolhe a consulta conforme o filtro:

- se o parâmetro `name` veio preenchido, retira os espaços das pontas e usa `findByNameContainingIgnoreCase`;
- se veio vazio, só com espaços ou não veio, usa `findAll(pageable)`.

Cada um desses casos (vazio, nulo, só espaços, com espaços nas pontas) tem um teste em `CategoryServiceTest`.

Exemplo real: `GET /api/v1/products?name=tv` encontra o produto `Smart TV`.

## 3. Paginação e ordenação

As listagens (`GET /api/v1/categories` e `GET /api/v1/products`) recebem um `Pageable`, que o Spring preenche a partir de três parâmetros da URL:

| Parâmetro | Significado | Padrão |
| --- | --- | --- |
| `page` | Número da página, começando em 0 | `0` |
| `size` | Itens por página | `20` |
| `sort` | Campo e direção, como `sort=name,asc` ou `sort=name,desc` | Sem ordenação |

Os padrões são os do Spring Data: o projeto não usa `@PageableDefault`.

**PowerShell**:

```powershell
curl.exe -s "http://localhost:8080/api/v1/products?page=0&size=5&sort=name,asc"
```

**bash**:

```bash
curl -s "http://localhost:8080/api/v1/products?page=0&size=5&sort=name,asc"
```

A resposta é um objeto com a lista em `content` e os dados da página em campos como `pageable`, `totalElements` e `totalPages`. Um exemplo completo está em [API-ENDPOINTS.md](API-ENDPOINTS.md#4-paginação-e-ordenação).

## 4. Relacionamento com categorias

O cliente informa as categorias de um produto só pelos ids, no campo `categoryIds`. O `ProductService` resolve o vínculo no método `syncCategories`:

1. limpa as categorias atuais do produto;
2. se a lista de ids estiver vazia ou nula, para aqui;
3. busca as categorias com `findAllById`;
4. se alguma não existir, lança `ResourceNotFoundException("One or more categories not found")` (resposta 404);
5. adiciona as categorias encontradas ao produto.

Na criação, `syncCategories` sempre roda. Na atualização (`PATCH`), só roda quando `categoryIds` é enviado; nesse caso as categorias são **substituídas**.

## 5. Transações e open-in-view

**Transações.** A anotação `@Transactional` abre uma transação no início do método e a fecha no fim: confirma tudo (*commit*) se o método terminar bem, ou desfaz tudo (*rollback*) se sair uma exceção não verificada. No projeto, ela só aparece nos services:

| Uso | Métodos |
| --- | --- |
| `@Transactional(readOnly = true)` | `search` e `findById`. Avisa que não haverá escrita, o que permite otimizações do Hibernate |
| `@Transactional` | `create`, `update`, `activate`, `deactivate` e `delete` |

Se o método do service é chamado quando já existe uma transação aberta, ele participa dela em vez de abrir outra. É o que acontece nos testes de integração, em que a transação é aberta pelo próprio teste e desfeita no fim. Isso muda quando o SQL chega ao banco; veja [TESTING.md](TESTING.md#9-testes-de-integração-com-transactional).

**Open Session in View** (*open-in-view*) é um recurso do Spring que mantém a conexão com o banco aberta até o fim da requisição, inclusive enquanto o JSON é montado. O projeto o desliga no `application.properties`:

```properties
spring.jpa.open-in-view=false
```

Consequência prática: os dados precisam ser carregados dentro do service, enquanto a transação está aberta. É o que acontece em `ProductService.findById`, que monta o `ProductDetailsResponse`, incluindo as categorias, dentro do método transacional.

**Ativar e desativar** não chamam `save`: o service altera o campo `active` da entidade carregada, e o Hibernate grava a mudança ao confirmar a transação. Se o status já for o pedido, nada é alterado.

## 6. Limitações conhecidas

- **Listagem, criação e atualização de produtos devolvem `categories` vazio.** `ProductMapper.toResponse` sempre monta a lista de categorias como `List.of()`. Só `GET /api/v1/products/{id}` (que usa `toDetailsResponse`) mostra as categorias. As categorias são gravadas normalmente; o problema é só na resposta.
- **Enviar `categoryIds: []` num `PATCH` remove todas as categorias do produto**, porque `syncCategories` limpa a lista antes de verificar se ela está vazia (confirmado por execução).
- **Sem ordenação padrão.** Sem o parâmetro `sort`, a ordem dos itens não é definida pela consulta.
- **Aviso na serialização das páginas.** As listagens devolvem o objeto `Page` do Spring Data diretamente, e o log mostra o aviso `Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure`. O formato do JSON das páginas pode mudar em versões futuras do Spring Data. Esse comportamento continua no capítulo 04.
