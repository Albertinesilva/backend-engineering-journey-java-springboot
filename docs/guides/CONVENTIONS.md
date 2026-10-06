# Convenções

Este guia reúne as convenções seguidas no ASJCatalog no capítulo 02: nomes de classes e pacotes, organização e nomes dos testes, idioma, JavaDoc, mensagens de commit, branches e nomes de migrations.

## Sumário

1. [Pacotes](#1-pacotes)
2. [Nomes de classes](#2-nomes-de-classes)
3. [Organização dos testes](#3-organização-dos-testes)
4. [Nomes dos testes](#4-nomes-dos-testes)
5. [Idioma](#5-idioma)
6. [JavaDoc](#6-javadoc)
7. [Mensagens de commit](#7-mensagens-de-commit)
8. [Branches](#8-branches)
9. [Migrations](#9-migrations)
10. [Limitações conhecidas](#10-limitações-conhecidas)

## 1. Pacotes

- Pacote base: `com.albertsilva.dev.asjcatalog`. Nomes de pacote em minúsculas, sem separadores.
- O primeiro nível é a **camada** (`web`, `service`, `repository`, `entity`, `dto`, `mapper`, `config`). Dentro de `dto` e `mapper`, os subpacotes separam por **recurso** (`dto.category`, `mapper.product`).
- DTOs se dividem em `request` (entrada) e `response` (saída).
- Exceções de negócio ficam em `service.exception`; o tratamento de erros HTTP, em `web.exception`.

A árvore completa está em [ARCHITECTURE.md](ARCHITECTURE.md#2-pacotes).

## 2. Nomes de classes

| Tipo | Padrão | Exemplo |
| --- | --- | --- |
| Controller | `<Recurso>Controller` | `CategoryController` |
| Service | `<Recurso>Service` | `ProductService` |
| Repositório | `<Entidade>Repository` | `CategoryRepository` |
| Mapper | `<Entidade>Mapper` | `ProductMapper` |
| DTO de entrada | `<Entidade><Ação>Request` | `CategoryCreateRequest`, `ProductUpdateRequest` |
| DTO de saída | `<Entidade>Response` e `<Entidade>DetailsResponse` (versão com mais dados) | `ProductDetailsResponse` |
| Exceção | `<Situação>Exception` | `ResourceNotFoundException` |
| Configuração | `<Assunto>Config` | `SpringDocOpenApiConfig` |

DTOs são `record` (classes imutáveis do Java). Entidades são classes com construtor sem argumentos, *getters* e *setters*.

## 3. Organização dos testes

- Os testes ficam em `backend/src/test/java`, no mesmo pacote base. Os subpacotes espelham a camada testada: `entity`, `repository`, `service` e `web.controller`.
- Os testes de integração ficam separados, em `integrations.service` e `integrations.web.controller`.
- Os dados de teste reutilizáveis ficam em `factory`, uma factory por entidade.
- Cada classe de teste agrupa os métodos em classes internas `@Nested`, uma por operação (`FindByIdOperations`, `DeleteOperations`...), com `@DisplayName` em inglês.

| Tipo de teste | Sufixo da classe | Exemplo | Roda no `verify` |
| --- | --- | --- | --- |
| Unitário ou de fatia | `<Classe testada>Test` | `CategoryServiceTest`, `ProductRepositoryTest` | Sim |
| Integração | `<Classe testada>IT` | `CategoryControllerIT` | Não (veja [TESTING.md](TESTING.md#13-limitações-conhecidas)) |
| Teste de contexto | `<Aplicação>Tests` | `AsjcatalogApplicationTests` | Sim |
| Factory | `<Entidade>Factory` | `CategoryFactory` | Não é teste |

O mapa completo está em [TESTING.md](TESTING.md#3-mapa-dos-pacotes-de-teste).

## 4. Nomes dos testes

O padrão predominante dos métodos é `<ação>Should<resultado>When<cenário>`:

| Parte | Significado | Em `findByIdShouldReturnCategoryWhenIdExists` |
| --- | --- | --- |
| `<ação>` | Método ou operação testada | `findById` |
| `Should<resultado>` | Comportamento esperado | `ShouldReturnCategory` |
| `When<cenário>` | Condição do teste | `WhenIdExists` |

Variações existentes:

- testes de repositório começam por `should` (`shouldReturnEmptyOptionalWhenIdDoesNotExist`);
- testes de entidade começam pelo nome da entidade (`productHashCodeShouldBeBasedOnId`);
- alguns não têm a parte `When` (`createShouldSaveProduct`).

As constantes das factories seguem o padrão `MAIÚSCULAS_COM_SUBLINHADO` e descrevem o papel do dado: `EXISTING_ID`, `NON_EXISTING_ID`, `DEPENDENT_ID`, `COUNT_TOTAL_CATEGORIES`.

O corpo dos testes segue a ordem Arrange, Act, Assert, marcada por comentários na maioria das classes. Detalhes e exemplos em [TESTING.md](TESTING.md#11-padrões-de-escrita).

## 5. Idioma

| O quê | Idioma |
| --- | --- |
| Nomes no código (classes, métodos, variáveis, tabelas, colunas) | Inglês |
| Nomes e `@DisplayName` dos testes | Inglês |
| JavaDoc | Português |
| Mensagens de log | Português |
| Mensagens de erro da API (`ErrorType` e exceções) | Inglês |
| Documentação em `docs/` | Português |

## 6. JavaDoc

As classes de controller, service, repositório, mapper, DTO, entidade e exceção têm JavaDoc em português, descrevendo a responsabilidade de cada uma. As classes de teste e as factories não têm JavaDoc; nelas, o `@DisplayName` cumpre o papel de descrever cada teste. Recursos usados:

- `<p>` para parágrafos e `<b>` para destacar rótulos;
- `{@link Classe}` para referências a outras classes.

## 7. Mensagens de commit

Os commits mais recentes usam o formato:

```text
tipo(escopo): descrição
```

- **tipo** diz a natureza da mudança, por exemplo `refactor`;
- **escopo** é opcional e indica a área afetada;
- **descrição** em português, em letra minúscula e no presente (`renomeia`, `corrige`).

Exemplo real desta branch: `refactor: renomeia o pacote base e as referências de dscatalog para asjcatalog`.

## 8. Branches

O projeto é documentado por **capítulos**, e cada capítulo tem sua branch:

| Branch | Capítulo |
| --- | --- |
| `chapter-01-crud` | 01 — Operações CRUD |
| `chapter-02-tests` | 02 — Testes automatizados (esta branch) |
| `chapter-03-validation-security` | 03 — Validação e segurança |
| `chapter-04-domain-orm` | 04 — Domínio, ORM, casos de uso e acesso a dados |

Também existem `main` (a branch padrão do repositório remoto) e `develop`. Cada branch registra o projeto como estava ao fim do capítulo, e o `README.md` da raiz de cada uma apresenta aquele capítulo.

## 9. Migrations

- Nome no padrão do Flyway: `V<versão>__<descrição>.sql`, com dois sublinhados e descrição em minúsculas separada por `_`.
- Estrutura em `db/migration/schema`, com versões `V001` a `V004`.
- Dados de exemplo em `db/migration/data`, com versões `V100` a `V102`.

Detalhes em [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md).

## 10. Limitações conhecidas

- **Histórico de commits misto.** Dos 138 commits desta branch, só quatro seguem o formato `tipo(escopo): descrição`, e dois deles têm a descrição em inglês. Os demais usam mensagens livres, como `Update README.md` ou `Document`.
- **Constante com erro de digitação.** A enum `ErrorType` tem a constante `CONFLIT` (em vez de `CONFLICT`). Ela foi mantida no código deste capítulo; a enum inteira é substituída por `ApiErrorCode` no capítulo 04.
- **Nomes de teste fora do padrão.** Além das variações da seção 4, `deleteShouldReturnBadRequestWhenCategoryHasAssociatedProducts` diz `BadRequest` (400), mas o teste espera 409 (`Conflict`).
- **JavaDoc com informações que não correspondem ao código.** Os DTOs de entrada documentam um `@param active` inexistente, e o JavaDoc de `Product` cita uma regra de preço maior que zero que não é aplicada. Veja [API-ENDPOINTS.md](API-ENDPOINTS.md#6-limitações-conhecidas) e [DOMAIN-MODEL.md](DOMAIN-MODEL.md#6-limitações-conhecidas).
