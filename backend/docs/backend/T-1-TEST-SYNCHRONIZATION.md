# T-1 — Sincronização e recuperação da suíte de testes

> Fase de **sincronização**: o código de produção atual é a referência; somente `src/test/**` foi alterado. Nenhuma decisão de contrato foi tomada e nenhum comportamento de produção foi tocado.
>
> Rótulos: **[FATO]** verificado por leitura ou execução nesta fase; **[INFERÊNCIA]**; **[HIPÓTESE]**.
> Base: [T-0-TEST-BASELINE.md](T-0-TEST-BASELINE.md) (mantido imutável).

---

## 1. Objetivo

Recuperar a confiança na suíte: eliminar os 10 failures do T-0 alinhando os testes ao comportamento já implementado, e acrescentar somente a cobertura mínima dos caminhos de produção que o T-0 apontou sem teste.

---

## 2. Estado inicial

| Item | Valor |
|------|-------|
| `git status --short` | apenas `?? docs/backend/T-0-TEST-BASELINE.md` (não rastreado) |
| Baseline `mvn test` | 283 testes, 5 failures |
| Baseline `mvn verify` | 283 testes, 5 failures (os `*IT` não entram) |
| Baseline `*IT` | 93 testes, 5 failures |
| Total de failures | 10 |

---

## 3. Failures tratados

Cada correção mudou **só o teste**; a coluna "referência de produção" é o comportamento que passou a ser validado.

| # | Teste (T-0) | Alteração | Referência de produção |
|---|-------------|-----------|------------------------|
| F-1 | `ProductTest.productShouldInstantiateCorrectly` | Removidas as asserções de `createdAt`/`updatedAt` desse teste; criado `productAuditDatesShouldBeNullBeforePersistence` (ambos `null` em instância nova). O preenchimento passa a ser validado em `ProductRepositoryTest` (§4). | `Product`: `createdAt`/`updatedAt` só pelo `@PrePersist` (privado); Javadoc: "permanece `null` em instâncias ainda não persistidas" |
| F-2, F-3 | `ProductControllerTest.findAllShouldReturnPage` / `findAllShouldReturnFilteredPage` | Mock e `verify` de `productService.search(...)` trocados por `findAllPaged(eq(""), eq("0"), any(Pageable))` e `findAllPaged(eq("pc"), eq("0"), any(Pageable))`. Os valores `""` e `"0"` são os padrões do controller. | `ProductController.findAll` → `productService.findAllPaged(name, categoryIds, pageable)` |
| F-4, F-5 | `ProductControllerTest.update*` | `patch` → `put`; `@DisplayName` do `@Nested` de `PATCH /products/{id}` para `PUT /products/{id}`; import de `patch` trocado por `put`. | `@PutMapping("/{id}")` em `ProductController.update` |
| F-9, F-10 | `ProductControllerIT.update*` | `patch` → `put` e `@DisplayName`s correspondentes (o import `patch` permanece: `activate`/`deactivate` continuam `PATCH`). | idem |
| F-6, F-7 | `CategoryControllerIT.insert…` / `update…` | `$.description` deixou de ser esperado na resposta: `jsonPath("$.description").doesNotExist()`. A descrição passou a ser verificada **no banco** (`categoryRepository.findById(...).getDescription()` igual ao do request). | `CategoryResponse(id, name)` |
| F-8 | `ProductControllerIT.insertShouldCreateProductWithValidData` | `$.date` deixou de ser esperado: `jsonPath("$.date").doesNotExist()`. Novo teste `insertShouldDiscardDateAndFillAuditDatesOnPersistence` (§4). Nada foi feito em `date` no DTO/mapper/resposta. | `ProductResponse` sem `date`; `ProductCreateRequest.date` aceito/validado e descartado; `createdAt`/`updatedAt` pelo `@PrePersist` |

Resultado: **10/10 failures tratados**, sem exceção pendente.

---

## 4. Testes novos (mínimos)

| Teste | Arquivo | O que valida (comportamento atual) |
|-------|---------|------------------------------------|
| `insertShouldDiscardDateAndFillAuditDatesOnPersistence` | `ProductControllerIT` | POST com `date` responde 201; o produto persistido tem `createdAt`/`updatedAt` preenchidos e iguais, `createdAt` ≠ `date` do request (descartado) e `active = true`. |
| `productAuditDatesShouldBeNullBeforePersistence` | `ProductTest` | instância nova tem `createdAt`/`updatedAt` nulos. |
| `shouldFillCreatedAtAndUpdatedAtWhenProductIsPersisted` | `ProductRepositoryTest` (`@DataJpaTest`, novo `@Nested` "Audit Dates Operations") | `@PrePersist` real: as duas datas preenchidas, com o mesmo instante. |
| `shouldRefreshOnlyUpdatedAtWhenPersistedProductIsModified` | `ProductRepositoryTest` | `@PreUpdate` real: `createdAt` intacto, `updatedAt` posterior (há um `Thread.sleep(20)` para garantir instante distinto). |
| `findAllShouldForwardCategoryIdsWhenInformed` | `ProductControllerTest` | `?categoryIds=1,3` chega ao service como `"1,3"`. |
| 3 testes em `FindAllPagedOperations` (novo `@Nested`) | `ProductServiceTest` (Mockito) | `"0"` → lista vazia de categorias; `"1,3"` → `[1L, 3L]`; a ordem da página nativa prevalece sobre a ordem do `JOIN FETCH` e o total vem da consulta nativa. |
| 6 testes em `FindAllPagedRealFlowOperations` (novo `@Nested`) | `ProductServiceIT` (H2, fluxo real) | sem filtro (total 25, categorias carregadas, "Macbook Pro" com Electronics + Computers); só uma categoria; sem repetição de produto em 2 categorias (`"1,3"` → 23); nome + categoria; ordem `DESC`; página vazia sem correspondência. |

Contagem: `mvn test` 283 → **290** (+7); `*IT` 93 → **100** (+7).

Observações:
- Os testes de `search` (`ProductServiceTest`, `ProductServiceIT`) **foram mantidos**: `ProductService.search` continua público e seus testes continuam válidos; apenas não é usado pelo controller. Os `@DisplayName` de `ProductServiceIT` "findAllPaged should …" descrevem, na verdade, chamadas a `search` (rótulo enganoso, **não alterado** nesta fase).
- **Correção ao T-0 (G-1):** o T-0 afirmou que `findAllPaged` "não é chamado por nenhum teste". A afirmação valia para chamadas **diretas**; `GET /products` em `ProductControllerIT` já exercitava o fluxo por HTTP (consulta nativa + `JOIN FETCH`) e passava. O que faltava era cobertura direta do service e do parsing/ordenação, agora adicionada.

---

## 5. Gaps deliberadamente deixados

| Gap | Motivo |
|-----|--------|
| `CategoryControllerTest` exclui `SecurityAutoConfiguration` e não exercita autorização | Exigiria trocar a configuração de todo o `@WebMvcTest` (mudança estrutural). Registrado para T-2. |
| `findAllPaged` com `categoryIds` não numérico (`NumberFormatException` → 500) ou `null` | Comportamento problemático de produção; fixá-lo em teste seria consagrar o defeito. Registrado para fase de produção. |
| `PATCH /products/{id}` → 500 em vez de 405 | Comportamento do `ControllerExceptionHandler`; sem teste de propósito. |
| `ProductCreateRequest.date` aceito e descartado | Dívida arquitetural. Os testes **descrevem** o comportamento atual; se for alterado, `doesNotExist()` em `$.date` e o novo IT terão de ser revistos. |
| Resumo × detalhe em `CategoryResponse`/`ProductResponse` | Idem: `doesNotExist()` em `$.description` documenta o contrato atual (só `id`,`name`). |
| Página vazia com `IN ()` no PostgreSQL | O teste do IT roda em H2 e passa; o comportamento em PostgreSQL continua sem verificação (B-9 F). |
| Validators, OAuth2, `AccountService`, `EmailService`, CORS, templates, `@Async`, resto do backlog B-9 §17 | Fora do escopo de T-1. |
| `*IT` fora do build padrão (sem Failsafe) | Exigiria alterar `pom.xml`. |

---

## 6. Resultados

| Comando | Total | Falhas | Erros | Skipped | Resultado | Tempo |
|---------|------:|-------:|------:|--------:|-----------|------:|
| `mvn -B test` | 290 | **0** | 0 | 0 | BUILD SUCCESS | 1:09 |
| `mvn -B verify` (sem flags) | 290 | **0** | 0 | 0 | BUILD SUCCESS | 1:02 |
| `mvn -B test -Dtest='*IT' -Dsurefire.failIfNoSpecifiedTests=false` | 100 | **0** | 0 | 0 | BUILD SUCCESS | 1:28 |
| `mvn -B test -Dtest=ProductControllerIT` (reexecução após ajuste de fim de linha) | 15 | 0 | 0 | 0 | BUILD SUCCESS | — |

Total da suíte: **390** testes (290 + 100), todos passando. Como antes, `mvn verify` **não** executa os `*IT` (não há Failsafe); por isso eles foram rodados explicitamente.

`git diff --check`: **sem saída** (exit 0).

---

## 7. Arquivos modificados

Somente `src/test/**` (7 arquivos, +330 / −17):

- `domain/catalog/ProductTest.java`
- `integrations/service/ProductServiceIT.java`
- `integrations/web/controller/CategoryControllerIT.java`
- `integrations/web/controller/ProductControllerIT.java`
- `repository/ProductRepositoryTest.java`
- `service/ProductServiceTest.java`
- `web/controller/ProductControllerTest.java`

Arquivos criados: `docs/backend/T-0-TEST-BASELINE.md` (fase anterior, ainda não versionado) e este documento.

Nota de processo: o `ProductControllerIT` chegou a ficar com fim de linha LF por causa de um script de edição; o CRLF original do repositório foi restaurado e o arquivo foi reexecutado (15 testes verdes). O diff final é apenas de conteúdo.

---

## 8. Produção

```text
src/main/**:            INTACTO
pom.xml:                INTACTO
src/main/resources/**:  INTACTO
create.sql:             INTACTO
```

Nenhum DTO, entidade, service, controller, repository, configuração ou endpoint foi alterado; nenhum comportamento antigo foi restaurado.

---

## 9. Git

```text
commit criado: NÃO
git add: NÃO
```
