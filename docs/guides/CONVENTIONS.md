# Convenções

Este guia reúne as convenções seguidas no ASJCatalog no capítulo 03: nomes de classes e pacotes, validação, segurança, testes, idioma, JavaDoc, mensagens de commit, branches e migrations.

## Sumário

1. [Pacotes](#1-pacotes)
2. [Nomes de classes](#2-nomes-de-classes)
3. [Validação](#3-validação)
4. [Segurança](#4-segurança)
5. [Testes](#5-testes)
6. [Idioma](#6-idioma)
7. [JavaDoc](#7-javadoc)
8. [Mensagens de commit](#8-mensagens-de-commit)
9. [Branches](#9-branches)
10. [Migrations](#10-migrations)
11. [Limitações conhecidas](#11-limitações-conhecidas)

## 1. Pacotes

- Pacote base: `com.albertsilva.dev.asjcatalog`. Nomes de pacote em minúsculas, sem separadores.
- O primeiro nível é a **camada** ou a **responsabilidade** (`web`, `service`, `repository`, `entity`, `dto`, `mapper`, `projection`, `validation`, `security`, `config`). Dentro de `dto`, `mapper` e `validation`, os subpacotes separam por **recurso** (`dto.user`, `validation.product`).
- DTOs se dividem em `request` (entrada) e `response` (saída).
- Exceções de negócio ficam em `service.exception`; o tratamento de erros HTTP, em `web.exception`.

A árvore completa está em [ARCHITECTURE.md](ARCHITECTURE.md#2-pacotes).

## 2. Nomes de classes

| Tipo | Padrão | Exemplo |
| --- | --- | --- |
| Controller | `<Recurso>Controller` | `UserController` |
| Service | `<Recurso>Service` | `ProductService` |
| Repositório | `<Entidade>Repository` | `RoleRepository` |
| Mapper | `<Entidade>Mapper` | `UserMapper` |
| DTO de entrada | `<Entidade><Ação>Request` | `UserCreateRequest`, `ProductUpdateRequest` |
| DTO de saída | `<Entidade>Response` e `<Entidade>DetailsResponse` (versão com mais dados) | `UserDetailsResponse` |
| Projection | `<Assunto>Projection` | `UserDetailsProjection` |
| Exceção | `<Situação>Exception` | `ResourceNotFoundException` |
| Configuração | `<Assunto>Config` | `ResourceServerConfig` |

DTOs são `record` (classes imutáveis do Java). Entidades são classes com construtor sem argumentos, *getters* e *setters*.

## 3. Validação

| Tipo | Padrão | Exemplo |
| --- | --- | --- |
| Anotação de classe (regras que envolvem o DTO inteiro) | `<Entidade><Ação>Valid` | `CategoryCreateValid`, `UserUpdateValid` |
| Anotação de campo | `Valid<Coisa>`, `Unique<Coisa>` ou `<Qualidade><Coisa>` | `ValidRoles`, `UniqueEmail`, `StrongPassword` |
| Classe com a regra | `<Anotação>Validator` | `CategoryCreateValidator`, `StrongPasswordValidator` |

- Anotações em `validation.<recurso>.annotation` e validadores em `validation.<recurso>.validator`.
- Mensagens das anotações padrão como chaves do `ValidationMessages.properties`, no formato `<recurso>.<campo>.<regra>` (`product.price.positive`).
- Erros de um validador de classe são associados a um campo com `addPropertyNode`, para aparecerem em `fieldErrors` com o nome do campo.

Detalhes em [VALIDATION.md](VALIDATION.md).

## 4. Segurança

- Roles gravadas com o prefixo `ROLE_` (`ROLE_ADMIN`) e referenciadas sem ele no `@PreAuthorize` (`hasRole('ADMIN')`).
- Regras de acesso por rota no `@PreAuthorize` de cada método do controller; rotas públicas e caminhos do Swagger em constantes do `ResourceServerConfig` (`PUBLIC_GET_ENDPOINTS`, `DOCUMENTATION_OPENAPI`).
- Valores de segurança em propriedades `security.*`, com variáveis de ambiente e valores padrão (`${CLIENT_ID:myclientid}`).

Detalhes em [AUTHENTICATION.md](AUTHENTICATION.md).

## 5. Testes

- Testes em `backend/src/test/java`, no mesmo pacote base, com subpacotes que espelham a camada testada; integração em `integrations`.
- Sufixo `Test` para testes unitários e de fatia, que rodam no `verify`; sufixo `IT` para os de integração, que rodam à parte.
- Uma factory por entidade em `factory`, com constantes em `MAIÚSCULAS_COM_SUBLINHADO` (`EXISTING_ID`, `COUNT_TOTAL_USERS`).
- Métodos agrupados em classes `@Nested` por operação, com `@DisplayName` em inglês e nomes no padrão `<ação>Should<resultado>When<cenário>`.

Detalhes em [TESTING.md](TESTING.md).

## 6. Idioma

| O quê | Idioma |
| --- | --- |
| Nomes no código (classes, métodos, variáveis, tabelas, colunas) | Inglês |
| Nomes e `@DisplayName` dos testes | Inglês |
| JavaDoc | Português |
| Mensagens de log | Português |
| Mensagens de validação dos campos e mensagem do 403 | Português |
| Títulos dos erros (`error`) e demais mensagens de erro | Inglês |
| Documentação em `docs/` | Português |

## 7. JavaDoc

As classes de controller, service, repositório, mapper, DTO, entidade, exceção, validação e segurança têm JavaDoc em português, descrevendo a responsabilidade de cada uma. Recursos usados: `<p>` para parágrafos, `<b>` para destacar rótulos e `{@link Classe}` para referências.

## 8. Mensagens de commit

Os commits mais recentes usam o formato:

```text
tipo(escopo): descrição
```

- **tipo** diz a natureza da mudança, por exemplo `refactor`;
- **escopo** é opcional e indica a área afetada;
- **descrição** em português, em letra minúscula e no presente (`renomeia`, `corrige`).

Exemplo real desta branch: `refactor: renomeia o pacote base e as referências de dscatalog para asjcatalog`.

## 9. Branches

O projeto é documentado por **capítulos**, e cada capítulo tem sua branch:

| Branch | Capítulo |
| --- | --- |
| `chapter-01-crud` | 01 — Operações CRUD |
| `chapter-02-tests` | 02 — Testes automatizados |
| `chapter-03-validation-security` | 03 — Validação e segurança (esta branch) |
| `chapter-04-domain-orm` | 04 — Domínio, ORM, casos de uso e acesso a dados |

Também existem `main` (a branch padrão do repositório remoto) e `develop`. Cada branch registra o projeto como estava ao fim do capítulo, e o `README.md` da raiz de cada uma apresenta aquele capítulo.

## 10. Migrations

- Nome no padrão do Flyway: `V<versão>__<descrição>.sql`, com dois sublinhados e descrição em minúsculas separada por `_`.
- Estrutura em `db/migration/schema`, com versões `V001` a `V008`.
- Dados de exemplo em `db/migration/data`, com versões `V100` a `V105`.

Detalhes em [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md).

## 11. Limitações conhecidas

- **Histórico de commits misto.** Dos 191 commits desta branch, só três seguem o formato `tipo(escopo): descrição`, e dois deles têm a descrição em inglês. Os demais usam mensagens livres, como `Update README.md`.
- **Constante com erro de digitação.** A enum `ErrorType` tem a constante `CONFLIT` (em vez de `CONFLICT`). Ela foi mantida no código deste capítulo; a enum inteira é substituída por `ApiErrorCode` no capítulo 04.
- **Mensagens de validação fora do arquivo de mensagens.** Os validadores de classe, `@StrongPassword` e `@ValidRoles` usam textos fixos no código, e 14 chaves do `ValidationMessages.properties` não são usadas. Veja [VALIDATION.md](VALIDATION.md#9-limitações-conhecidas).
- **Nomes de teste fora do padrão.** Além das variações descritas em [TESTING.md](TESTING.md#10-padrões-de-escrita), o teste comentado `deleteShouldReturnBadRequestWhenCategoryHasAssociatedProducts` diz `BadRequest` (400), mas esperava 409 (`Conflict`).
