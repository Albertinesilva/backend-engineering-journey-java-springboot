# Acesso a dados

Este guia explica como o ASJCatalog lê e grava no banco no capítulo 03: repositórios, consultas derivadas, a consulta nativa do login, paginação e transações.

## Sumário

1. [Repositórios](#1-repositórios)
2. [Consultas derivadas](#2-consultas-derivadas)
3. [Paginação e ordenação](#3-paginação-e-ordenação)
4. [Relacionamentos com categorias e roles](#4-relacionamentos-com-categorias-e-roles)
5. [Transações e open-in-view](#5-transações-e-open-in-view)
6. [A consulta do login](#6-a-consulta-do-login)
7. [Limitações conhecidas](#7-limitações-conhecidas)

## 1. Repositórios

Um **repositório** é a interface por onde o código acessa o banco. Há quatro, no pacote `repository`, e todos estendem `JpaRepository<Entidade, Long>`:

| Repositório | Entidade |
| --- | --- |
| `CategoryRepository` | `Category` |
| `ProductRepository` | `Product` |
| `UserRepository` | `User` |
| `RoleRepository` | `Role` |

O **Spring Data JPA** gera a implementação dessas interfaces quando a aplicação sobe. Só por estender `JpaRepository`, cada repositório já tem métodos prontos, como `findById`, `findAll(pageable)`, `save`, `delete`, `getReferenceById` e `findAllById`.

Os repositórios são usados pelos services e, neste capítulo, também pelos **validadores**: é assim que a validação confere se um nome ou e-mail já existe (veja [VALIDATION.md](VALIDATION.md#3-validadores-customizados)).

## 2. Consultas derivadas

Uma **consulta derivada** é um método cujo nome descreve a consulta. O Spring Data lê o nome e monta o SQL.

| Repositório | Método | Usado por |
| --- | --- | --- |
| `CategoryRepository`, `ProductRepository` | `findByNameContainingIgnoreCase(name, pageable)` | Busca por nome nas listagens |
| `CategoryRepository`, `ProductRepository` | `existsByNameIgnoreCase(name)` | Validadores de criação (nome único) |
| `CategoryRepository`, `ProductRepository` | `existsByNameIgnoreCaseAndIdNot(name, id)` | Validadores de atualização (nome único, exceto o próprio registro) |
| `UserRepository` | `findByFirstNameContainingIgnoreCase(firstName, pageable)` | Busca de usuários por primeiro nome |
| `UserRepository` | `findByEmail(email)` | Declarado; testado em `UserRepositoryTest` |
| `UserRepository` | `existsByEmailIgnoreCase(email)` | `@UniqueEmail` |
| `UserRepository` | `existsByEmailIgnoreCaseAndIdNot(email, id)` | `@UserUpdateValid` |

O nome se lê em partes. `existsByNameIgnoreCaseAndIdNot`: `existsBy` (existe algum) + `Name` + `IgnoreCase` (sem diferenciar maiúsculas) + `And` + `Id` + `Not` (com id diferente do informado).

Nos services, o método `search` escolhe a consulta conforme o filtro: se o parâmetro de nome veio preenchido, retira os espaços das pontas e usa a busca por nome; se veio vazio, usa `findAll(pageable)`. Nos usuários, o filtro é `firstName`.

## 3. Paginação e ordenação

As listagens (`GET /api/v1/categories`, `/api/v1/products` e `/api/v1/users`) recebem um `Pageable`, que o Spring preenche a partir de três parâmetros da URL:

| Parâmetro | Significado | Padrão |
| --- | --- | --- |
| `page` | Número da página, começando em 0 | `0` |
| `size` | Itens por página | `20` |
| `sort` | Campo e direção, como `sort=name,asc` | Sem ordenação |

**PowerShell**:

```powershell
curl.exe -s "http://localhost:8080/api/v1/products?page=0&size=5&sort=name,asc"
```

**bash**:

```bash
curl -s "http://localhost:8080/api/v1/products?page=0&size=5&sort=name,asc"
```

A resposta é um objeto com a lista em `content` e os dados da página em campos como `pageable`, `totalElements` e `totalPages`. Um exemplo completo está em [API-ENDPOINTS.md](API-ENDPOINTS.md#4-paginação-e-ordenação).

## 4. Relacionamentos com categorias e roles

**Produto e categorias.** O cliente informa as categorias só pelos ids, em `categoryIds`. O `ProductService` resolve o vínculo no método `syncCategories`: limpa as categorias atuais, busca as novas com `findAllById` e, se alguma não existir, lança `ResourceNotFoundException` (404). Neste capítulo, a validação já recusa `categoryIds` vazio ou com id inexistente antes de chegar ao service (422).

**Usuário e roles.** O `UserService` busca as roles com `roleRepository.findAllById(roleIds)` e lança `ResourceNotFoundException` se alguma não existir. Na atualização (`PUT`), o `UserMapper` **substitui** as roles do usuário pelas informadas.

## 5. Transações e open-in-view

**Transações.** A anotação `@Transactional` abre uma transação no início do método e a fecha no fim: confirma tudo (*commit*) se o método terminar bem, ou desfaz tudo (*rollback*) se sair uma exceção não verificada. No projeto, ela só aparece nos services:

| Uso | Métodos |
| --- | --- |
| `@Transactional(readOnly = true)` | `search` e `findById`. Avisa que não haverá escrita |
| `@Transactional` | `create`, `update`, `activate`, `deactivate` e `delete` |

**Open Session in View** (*open-in-view*) é um recurso do Spring que manteria a conexão com o banco aberta até o fim da requisição. O projeto o desliga no `application.properties` (`spring.jpa.open-in-view=false`). Consequência: os dados precisam ser carregados dentro do service, enquanto a transação está aberta, como faz `ProductService.findById` ao montar as categorias.

**Ativar e desativar** não chamam `save`: o service altera o campo `active` da entidade carregada, e o Hibernate grava a mudança ao confirmar a transação.

## 6. A consulta do login

No login, o Spring Security chama `UserService.loadUserByUsername(email)`, que usa uma **consulta nativa** (SQL escrito à mão, com `@Query(nativeQuery = true)`) de `UserRepository`:

```sql
SELECT tb_user.email AS username, tb_user.password, tb_role.id AS roleId, tb_role.authority
FROM tb_user
INNER JOIN tb_user_role ON tb_user.id = tb_user_role.user_id
INNER JOIN tb_role ON tb_role.id = tb_user_role.role_id
WHERE tb_user.email = :email
```

O resultado é lido pela interface `UserDetailsProjection`. Uma **projection** é uma interface com *getters* (`getUsername`, `getPassword`, `getRoleId`, `getAuthority`) que o Spring Data preenche com as colunas de mesmo nome. A consulta devolve uma linha por role; o service monta um `User` com o e-mail, a senha e todas as roles. O campo `active` não é lido.

## 7. Limitações conhecidas

- **Listagem, criação e atualização de produtos devolvem `categories` vazio.** `ProductMapper.toResponse` sempre monta a lista de categorias como `List.of()`. Só `GET /api/v1/products/{id}` mostra as categorias.
- **Usuário sem roles não consegue fazer login.** A consulta do login usa `INNER JOIN` com as roles; um usuário sem nenhuma role não é encontrado. A validação exige ao menos uma role na criação e na atualização, mas registros inseridos direto no banco podem não ter.
- **O login ignora o campo `active`.** Veja [AUTHENTICATION.md](AUTHENTICATION.md#11-limitações-conhecidas).
- **Sem ordenação padrão.** Sem o parâmetro `sort`, a ordem dos itens não é definida pela consulta.
- **Aviso na serialização das páginas.** As listagens devolvem o objeto `Page` do Spring Data diretamente, e o log mostra o aviso `Serializing PageImpl instances as-is is not supported`. Continua no capítulo 04.
