# ASJCatalog Backend — B-2: Camada de Persistência (Repositories e Projections)

> **Fase:** B-2 — análise e documentação (JavaDoc) de `repository` e `projection`.
> **Pré-requisitos:** [B-0-BACKEND-INVENTORY.md](B-0-BACKEND-INVENTORY.md) e [B-1-DOMAIN-LAYER.md](B-1-DOMAIN-LAYER.md).
> **Escopo alterado:** somente comentários JavaDoc em `src/main/java/**/repository/**` e `src/main/java/**/projection/**`, mais esta documentação. Nenhuma query, mapping, assinatura, nome, parâmetro ou tipo de retorno foi alterado.

Rótulos: **[FATO]** confirmado diretamente no código; **[INFERÊNCIA]** conclusão razoável baseada em fatos/uso, sem execução dedicada; **[HIPÓTESE]** precisa de confirmação posterior. Quando uso "evidência de baseline", refiro-me a resultados **já registrados no B-0** (execução de `mvn test` e dos `*IT`); nenhum teste foi executado na B-2.

---

## 1. Objetivo da camada

A camada de persistência é a fronteira entre os services/validators e o banco. Ela expõe interfaces Spring Data JPA (`JpaRepository<Entidade, Long>`) para as 6 entidades de B-1 e duas *projections* que representam linhas de consultas nativas. Nada nesta camada contém lógica de negócio: há apenas consultas (derivadas, JPQL e SQL nativo) e o contrato de retorno delas.

## 2. Composição da camada [FATO]

| Pacote | Arquivo | Tipo |
|--------|---------|------|
| `repository` | `CategoryRepository`, `ProductRepository`, `UserRepository`, `RoleRepository`, `TokenRepository`, `EmailRepository` | 6 interfaces `@Repository extends JpaRepository<E, Long>` |
| `projection` | `ProductProjection`, `UserDetailsProjection` | 2 interfaces (projeções de interface) |

Confirmado por leitura: **não há** `@Param`, `@Modifying`, `@EntityGraph`, `@Lock`, `@Transactional`, `@QueryHints`, `@NamedQuery`, `Specification`, `Example`, `Slice`, nem implementações manuais (fragmentos) nesta camada. Total de métodos próprios: **16** (13 derivados + 3 com `@Query`).

## 3. Mapa dos repositories [FATO]

| Repository | Entidade | Métodos próprios | Herdados efetivamente usados em `src/main` |
|------------|----------|------------------|--------------------------------------------|
| `CategoryRepository` | `Category` | `findByNameContainingIgnoreCase(String, Pageable)`, `existsByNameIgnoreCase(String)`, `existsByNameIgnoreCaseAndIdNot(String, Long)` | `findAll(Pageable)`, `findById`, `findAllById`, `existsById`, `getReferenceById`, `save`, `delete` |
| `ProductRepository` | `Product` | `findByNameContainingIgnoreCase(String, Pageable)`, `searchProducts(List<Long>, String, Pageable)` (nativa), `searchProductsWithCategories(List<Long>)` (JPQL), `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot` | `findAll(Pageable)`, `findById`, `getReferenceById`, `save`, `delete` |
| `UserRepository` | `User` | `findByFirstNameContainingIgnoreCase(String, Pageable)`, `findByEmail(String)`, `existsByEmailIgnoreCase(String)`, `existsByEmailIgnoreCaseAndIdNot(String, Long)`, `searchUserAndRolesByEmail(String)` (nativa) | `findAll(Pageable)`, `findById`, `getReferenceById`, `save`, `delete` |
| `RoleRepository` | `Role` | `findByAuthority(String)` | `findAllById`, `existsById` |
| `TokenRepository` | `Token` | `findByToken(String)`, `findByUserAndTypeAndDisabledFalse(User, TokenType)` | `save` |
| `EmailRepository` | `Email` | — | `save` |

## 4. Mapa das projections [FATO]

| Projection | Tipo | Campos (tipo) | Origem | Alimentada por | Consumidor |
|------------|------|---------------|--------|----------------|------------|
| `ProductProjection` | interface projection **fechada**; estende `Identifiable<Long>` | `getId(): Long` (do contrato), `getName(): String` | colunas `tb_product.id` e `tb_product.name` (sem alias) | `ProductRepository.searchProducts` | `ProductService.findAllPaged` |
| `UserDetailsProjection` | interface projection **fechada** | `getId(): Long`, `getUsername(): String`, `getPassword(): String`, `getRoleId(): Long`, `getAuthority(): String`, `getActive(): boolean` | aliases `id`, `username` (=`tb_user.email`), `password`, `roleId` (=`tb_role.id`), `authority`, `active` | `UserRepository.searchUserAndRolesByEmail` | `UserService.loadUserByUsername` |

Nenhuma das duas usa `@Value`/SpEL, *class-based DTO projection* ou `Tuple`. Não há projeções dinâmicas.

## 5. Consultas derivadas (13) [FATO]

| Método | Retorno | Comportamento | Chamadores |
|--------|---------|---------------|------------|
| `CategoryRepository.findByNameContainingIgnoreCase` | `Page<Category>` | `LIKE '%termo%'` em `name`, sem caixa; conta automática | `CategoryService.search` (só com filtro) |
| `CategoryRepository.existsByNameIgnoreCase` | `boolean` | existência por nome sem caixa | `CategoryCreateValidator` |
| `CategoryRepository.existsByNameIgnoreCaseAndIdNot` | `boolean` | idem, excluindo um `id` | `CategoryUpdateValidator` |
| `ProductRepository.findByNameContainingIgnoreCase` | `Page<Product>` | como a de categoria | `ProductService.search` (**sem chamadores** acima do service; ver §12) |
| `ProductRepository.existsByNameIgnoreCase` | `boolean` | | `ProductCreateValidator` |
| `ProductRepository.existsByNameIgnoreCaseAndIdNot` | `boolean` | | `ProductUpdateValidator` |
| `UserRepository.findByFirstNameContainingIgnoreCase` | `Page<User>` | `LIKE '%termo%'` em `firstName` (sobrenome não participa) | `UserService.search` (só com filtro) |
| `UserRepository.findByEmail` | `Optional<User>` | comparação **exata** (sem `IgnoreCase`, sem `trim`) | `AccountService.requestPasswordRecovery`, `AccountService.resendActivationEmail` |
| `UserRepository.existsByEmailIgnoreCase` | `boolean` | existência por e-mail sem caixa | `UniqueEmailValidator` |
| `UserRepository.existsByEmailIgnoreCaseAndIdNot` | `boolean` | idem, excluindo um `id` | `UserUpdateValidator`, `UniqueEmailForAuthenticatedUserValidator` |
| `RoleRepository.findByAuthority` | `Optional<Role>` | comparação exata | `AccountService.register` (`ROLE_OPERATOR`) |
| `TokenRepository.findByToken` | `Optional<Token>` | comparação exata; não verifica tipo/expiração/desabilitado | `TokenService.findAndValidateToken` |
| `TokenRepository.findByUserAndTypeAndDisabledFalse` | `List<Token>` | tokens do usuário e tipo com `disabled = false` (**inclui vencidos**) | `TokenService.disableAllActivationTokens` (← `AccountService.resendActivationEmail`); `disableAllPasswordRecoveryTokens` (sem chamadores) |

## 6. Consultas JPQL (1) [FATO]

**`ProductRepository.searchProductsWithCategories(List<Long> productsIds): List<Product>`**

- JPQL: `SELECT obj FROM Product obj JOIN FETCH obj.categories WHERE obj.id IN :productsIds`.
- Entidade `Product`, alias `obj`; parâmetro `:productsIds` associado pelo nome do parâmetro do método (sem `@Param`).
- Sem `ORDER BY`, sem paginação. Junção **interna** com fetch: produto sem categorias não retornaria.
- Chamador: `ProductService.findAllPaged` (segunda etapa da listagem).

## 7. Consultas SQL nativas (2) [FATO]

**7.1 `ProductRepository.searchProducts(List<Long> categoryIds, String name, Pageable): Page<ProductProjection>`**

- Tabelas: `tb_product` ⨝ `tb_product_category` (`INNER JOIN`). Colunas: `id`, `name`, `product_id`, `category_id`.
- Filtros: `(:categoryIds IS NULL OR category_id IN :categoryIds)` e `LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))`.
- `SELECT DISTINCT id, name` dentro de `SELECT * FROM (...) AS tb_result`; `countQuery` explícita com o mesmo miolo dentro de `COUNT(*)`.
- Retorna `ProductProjection` (id, name). Chamador: `ProductService.findAllPaged`.

**7.2 `UserRepository.searchUserAndRolesByEmail(String email): List<UserDetailsProjection>`**

- Tabelas: `tb_user` ⨝ `tb_user_role` ⨝ `tb_role` (duas `INNER JOIN`), `WHERE tb_user.email = :email`.
- Colunas/aliases: `id`, `username` (=email), `password`, `active`, `roleId`, `authority`.
- Uma linha por role; sem paginação/ordenação; não filtra por `active`. Chamador: `UserService.loadUserByUsername` (login).

## 8. Paginação e ordenação [FATO / INFERÊNCIA]

- **Mecanismos existentes:** apenas `Pageable`/`Page` do Spring Data (4 métodos: as três `findByNameContaining…`/`findByFirstNameContaining…` e `searchProducts`) e `findAll(Pageable)` herdado. **Não existe** `Slice`, `Sort` avulso, `@PageableDefault` nem configuração própria de web-paginação [FATO].
- **Origem do `Pageable`:** os controllers recebem `Pageable` como argumento; valem os padrões do Spring Data Web (página 0, tamanho 20, limite de tamanho do framework) [INFERÊNCIA — padrões do framework, não configurados no projeto].
- **Ordenação:** só a que o cliente enviar em `sort`. As consultas próprias **não** têm `ORDER BY` fixo; sem `sort`, a ordem não é definida pela consulta [FATO].
- **Consultas derivadas paginadas:** o Spring Data gera a consulta de contagem automaticamente [FATO]; a ordenação usa nomes de propriedades da entidade.
- **`searchProducts` (nativa):** a paginação/ordenação são aplicadas ao `SELECT *` **externo**, que só expõe `id` e `name`; `countQuery` explícita [FATO]. Ordenar por outra propriedade (ex.: `price`) não corresponde a coluna do resultado externo [FATO sobre a forma da SQL]; o comportamento em execução dessa situação **não foi verificado** [HIPÓTESE: erro de SQL/HTTP 500].
- Evidência de baseline (B-0): `ProductControllerIT` (`GET /products?sort=name,asc&size=12`) passou em H2 verificando `totalElements` e a ordem dos três primeiros nomes.
- **Consulta de contagem por chamada:** `findAllPaged` executa até 3 consultas (página, contagem — que o Spring Data pode dispensar quando a primeira página não está cheia [INFERÊNCIA] — e o `JOIN FETCH`).

## 9. JOIN / JOIN FETCH [FATO]

| Consulta | Junção | Efeito |
|----------|--------|--------|
| `searchProducts` | `INNER JOIN tb_product_category` | exclui produtos sem categoria; `DISTINCT` evita repetição quando o produto tem várias categorias filtradas |
| `searchProductsWithCategories` | `JOIN FETCH obj.categories` (interno) | inicializa `Product.categories` na mesma instrução; evita N+1 ao mapear a resposta |
| `searchUserAndRolesByEmail` | `INNER JOIN tb_user_role` e `INNER JOIN tb_role` | uma linha por role; usuário sem role → 0 linhas |

Nenhuma consulta usa `LEFT JOIN`. Nos dados iniciais (V102), todos os 25 produtos possuem categoria [FATO], então o `INNER JOIN` não oculta produtos semeados.

## 10. Projections e seus usos [FATO]

- **`ProductProjection`** — `ProductService.findAllPaged` (1) `page.map(ProductProjection::getId)` para obter os ids; (2) passa `page.getContent()` como referência de ordem a `IdentifiableUtils.reorderByReference` (aceita `List<? extends Identifiable<ID>>`, por isso a projection estende `Identifiable<Long>`).
- **`UserDetailsProjection`** — `UserService.loadUserByUsername`: usa a primeira linha para `id`, e-mail (`getUsername`), senha e `active` de um `User` parcial (nome/sobrenome nulos) e percorre todas as linhas criando `new Role(roleId, authority)` para `addRole`.
- Nenhum outro service/controller consome projections.

## 11. Relação Repository → Service (e demais consumidores) [FATO]

```
Controller ─▶ Service ─▶ Repository ─▶ Entidade/Tabela            (CRUD e listagens)
Validators (DTO) ─────▶ Repository                                 (unicidade/existência)
Security: AuthenticatedUserService ─▶ UserRepository.findById      (usuário do claim userId)
          UserService.loadUserByUsername ─▶ UserRepository.searchUserAndRolesByEmail ─▶ UserDetailsProjection
```

| Repository | Consumidores diretos |
|------------|----------------------|
| `CategoryRepository` | `CategoryService`; `ProductService` (`findAllById`); `CategoryCreateValidator`, `CategoryUpdateValidator`, `ProductCreateValidator`/`ProductUpdateValidator` (`existsById`) |
| `ProductRepository` | `ProductService`; `ProductCreateValidator`, `ProductUpdateValidator` |
| `UserRepository` | `UserService`; `AccountService`; `AuthenticatedUserService`; `UniqueEmailValidator`, `UserUpdateValidator`, `UniqueEmailForAuthenticatedUserValidator` |
| `RoleRepository` | `UserService` (`findAllById`), `AccountService` (`findByAuthority`), `ValidRolesValidator` (`existsById`) |
| `TokenRepository` | `TokenService` (único) |
| `EmailRepository` | `EmailService.registerEmailLog` (somente `save`) |

Os repositories **não** declaram transação; os limites transacionais estão nos services. Com `spring.jpa.open-in-view=false` (B-0), o carregamento tardio só é seguro dentro de um método transacional [INFERÊNCIA].

## 12. Observações de performance

| # | Observação | Rótulo |
|---|------------|--------|
| P-1 | `findAllPaged` evita N+1 de categorias: primeiro pagina só ids/nomes (projeção), depois carrega as entidades com `JOIN FETCH` numa consulta única. | FATO (desenho); efeito em execução INFERÊNCIA |
| P-2 | `ProductService.search` (consulta derivada + `ProductMapper.toResponse`, que percorre `getCategories()`) tem o padrão N+1 de categorias por produto da página. Não é acionado por endpoint, pois o controller usa `findAllPaged`. | FATO (caminho de código); N+1 em execução INFERÊNCIA |
| P-3 | `UserService.search` → `UserMapper.toResponse` percorre `getRoles()` de cada usuário da página: padrão N+1 de roles. | FATO (caminho); N+1 INFERÊNCIA |
| P-4 | `ProductService.findById` e `AccountService.getAuthenticatedUser` acessam coleções tardias (categorias/roles) após um `findById`: uma consulta extra por chamada. | INFERÊNCIA |
| P-5 | Projections usadas para não carregar entidades: `ProductProjection` (id, nome) e `UserDetailsProjection` (credenciais + role). | FATO |
| P-6 | `searchProducts`: `LOWER(name) LIKE '%…%'` não usa índice comum (não há índices além de PK/UNIQUE — B-0). Possível otimização: apenas registrada, **não** aplicada. | INFERÊNCIA |
| P-7 | Nas duas consultas de nome (derivada × nativa), o tratamento do termo difere: a derivada é gerada pelo Spring Data (que, segundo o framework, escapa curingas do termo); a nativa concatena o termo cru, então `%`/`_` viram curingas. | FATO (nativa); escape da derivada INFERÊNCIA |
| P-8 | `searchProductsWithCategories` não tem limite próprio; o volume é igual ao número de ids recebidos (ids de uma página). | FATO |

## 13. Fatos, inferências e hipóteses

### 13.1 Fatos relevantes
1. Inventário: 6 repositories, 2 projections, 13 consultas derivadas, 1 JPQL, 2 nativas; nenhuma `@Param`; nomes de parâmetros dependem de `-parameters` (configurado no `pom.xml`).
2. `searchProducts` faz `INNER JOIN`; produtos sem categoria não são listados por `GET /api/v1/products`, ao contrário de `ProductService.search`.
3. O `ProductService` envia **lista vazia** (não `null`) em `categoryIds` quando o controller recebe `categoryIds=0` (padrão); o ramo `IS NULL` da SQL, portanto, não é o que trata "sem filtro" no fluxo atual.
4. `searchUserAndRolesByEmail` usa `INNER JOIN` com roles: usuário sem roles → lista vazia → `loadUserByUsername` lança `UsernameNotFoundException` (e o provider do grant `password` responde `invalid_grant`, "Invalid credentials"). `UserService.create` aceita `roleIds` nulo/vazio (`findRolesByIdsOrThrow` devolve conjunto vazio), portanto o código permite criar usuário sem roles [FATO do caminho de código]; que ele não consiga autenticar é INFERÊNCIA.
5. Comparação de e-mail inconsistente entre consultas: `existsByEmailIgnoreCase*` ignoram a caixa, mas `findByEmail` e `searchUserAndRolesByEmail` comparam o texto exato. `UserMapper.toEntity` grava o e-mail sem `lowercase`/`trim` (B-0). Consequência provável: login e recuperação de senha exigem a mesma caixa usada no cadastro [INFERÊNCIA].
6. `findByToken` não verifica tipo/expiração/desabilitado; `findByUserAndTypeAndDisabledFalse` inclui tokens vencidos ainda não desabilitados.
7. `RoleRepository.findByAuthority` retorna `Optional`; a coluna `authority` não é `UNIQUE` (V005).
8. `UserRepository.findByEmail` e `TokenRepository.findByToken` retornam `Optional` — o JavaDoc anterior dizia "ou `null`" (corrigido).
9. Três consultas próprias não têm teste direto no nível de repository: `searchProducts`, `searchProductsWithCategories` e `searchUserAndRolesByEmail` (busca por seus nomes em `src/test` não encontrou chamadas). São exercitadas indiretamente: `ProductControllerIT` (`GET /products`) e `OAuth2TokenIT` (login), ambos verdes no baseline do B-0.
10. `TokenService.disableAllPasswordRecoveryTokens` (e, portanto, seu uso de `findByUserAndTypeAndDisabledFalse` com `PASSWORD_RECOVERY`) não tem chamadores em `src/main`.
11. O `javadoc` do JDK rejeita as tags `@apiNote`/`@implNote` sem configuração `-tag` (3 erros em `UserDetailsProjection`, preexistentes). O `pom.xml` usa `failOnError=false` no `maven-javadoc-plugin`. Provavelmente outras classes usam essas tags (ex.: `ProductService`).

### 13.2 Inferências
- Padrões do Spring Data Web para `Pageable` (§8); escape de curingas nas consultas derivadas (§12 P-7); N+1 em `search`/`findById` (§12); dispensa de contagem pelo Spring Data em primeira página não cheia; `LazyInitializationException` seria o resultado de acessar coleções tardias fora de transação.
- O `DISTINCT` + `JOIN FETCH` de coleção: em Hibernate 6 os resultados duplicados por junção de coleção são consolidados; de todo modo `IdentifiableUtils.reorderByReference` indexa por id, então duplicatas colapsam [FATO no service; consolidação do Hibernate INFERÊNCIA].

### 13.3 Hipóteses (confirmar depois)
1. **`categoryIds` vazio em PostgreSQL.** No H2, a evidência de baseline (`ProductControllerIT`) indica que lista vazia retorna todos os produtos com categoria (o `IN` é expandido pelo Hibernate). O mesmo caminho **não foi verificado no PostgreSQL** (perfil `dev`). *(B-3: o `ProductService` só envia lista vazia quando recebe `"0"`, nunca `null` — confirmado; o efeito no PostgreSQL segue hipótese.)*
2. **`searchProductsWithCategories` com lista vazia.** Se `page` estiver vazia (página além do fim), `productsIds` é vazio e o `IN :productsIds` recebe lista vazia. Não há teste desse caminho em `findAllPaged` (o teste de "página inexistente" de `ProductServiceIT` chama `ProductService.search`, isto é, a consulta derivada). *(B-3: o service chama `searchProductsWithCategories` sem guarda para lista vazia — confirmado; o efeito segue hipótese.)*
3. **Ordenação por coluna inexistente** em `searchProducts` (ex.: `sort=price,asc`) → falha em tempo de execução (§8).
4. **Aliases da nativa em PostgreSQL** (`roleId` sem aspas vira `roleid`): o mapeamento por nome funcionou em H2 (`OAuth2TokenIT`); no PostgreSQL não foi executado nesta documentação.
5. **Proxy do Hibernate e `equals`** (B-1 §6 item 4) — relevante para `getReferenceById` nos services; fora desta camada.
6. `IncorrectResultSizeDataAccessException` em `findByAuthority` se houver `authority` duplicada no banco.

## 14. Pontos a aprofundar nas próximas fases

1. **Services (candidata a B-3):** usam todos os métodos acima e definem as fronteiras transacionais; é ali que os riscos N+1, o uso de `getReferenceById`, a lista vazia de `categoryIds`, a criação de usuário sem roles e o papel de `findAllPaged` × `search` se resolvem.
2. Mappers: momentos em que coleções tardias são percorridas (P-2 a P-4).
3. Validators: dependência de `HttpServletRequest` para o `id` da URI e das consultas `existsBy…`.
4. Segurança: `loadUserByUsername` (usuário parcial), claim `userId`, e diferenças de caixa no e-mail.
5. Testes (fase futura): ausência de testes diretos das 3 consultas customizadas e do comportamento de lista vazia.

## 15. JavaDoc adicionado ou ajustado

| Arquivo | Alterações |
|---------|-----------|
| `CategoryRepository` | Nova visão geral (derivadas, consumidores); reescrita do JavaDoc de `findByNameContainingIgnoreCase` (paginação, contagem, carregamento, uso); notas de uso/caixa nas duas `existsBy…`. |
| `ProductRepository` | Visão geral reescrita (a anterior exemplificava um método que já existe); **novos** JavaDocs de `searchProducts` (SQL nativa: dependências do schema, regras, paginação/ordenação, parâmetros por nome, lista vazia) e `searchProductsWithCategories` (JPQL, `JOIN FETCH`, ordem/paginação); `findByNameContainingIgnoreCase` com nota de N+1; notas de uso nas `existsBy…`; typo "nome da produto" removido. |
| `UserRepository` | Visão geral; `findByFirstNameContainingIgnoreCase` (N+1 de roles); `findByEmail` (exato; `Optional`, não `null`); notas em `existsBy…`; **novo** JavaDoc de `searchUserAndRolesByEmail` (SQL nativa, uma linha por role, `INNER JOIN`, sem filtro de `active`). |
| `RoleRepository` | Visão geral; **novo** JavaDoc de `findByAuthority` (`authority` não único). |
| `TokenRepository` | **Novo** JavaDoc de tipo; `findByToken` e `findByUserAndTypeAndDisabledFalse` reescritos (`Optional`; "ativos" → "não desabilitados", incluindo vencidos). |
| `EmailRepository` | **Novo** JavaDoc de tipo (sem consultas próprias). |
| `ProductProjection` | **Novo** JavaDoc (interface fechada, origem, não contém, uso) e de `getName()`. |
| `UserDetailsProjection` | JavaDoc de tipo reescrito: removidos trechos incorretos (exemplo de query com tabelas inexistentes/`LEFT JOIN`/`@Projection`, "type-safe/compile-time", `ROLE_USER`); documentada a cardinalidade (uma linha por role) e a origem de cada campo; ajustes em `getUsername`, `getAuthority` e `getActive`. |

Verificação: comparando cada arquivo com o HEAD após remover comentários, o código é idêntico nos 8 arquivos; `git diff --check` limpo (ver relatório da fase).

## 16. O que **não** foi feito

Nenhuma alteração de queries, mappings, assinaturas, nomes, parâmetros, paginação ou tipos de retorno; nenhum método criado; nenhuma dependência adicionada; nenhum arquivo em `src/test/**`, `domain`, `service`, `controller`, `dto`, `mapper`, `security`, `config`, `validation`, migrations ou `application*.properties` alterado; nenhum teste executado; nenhum commit.
