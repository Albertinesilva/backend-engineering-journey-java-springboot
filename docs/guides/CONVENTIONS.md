# Convenções

Este guia reúne as convenções seguidas no ASJCatalog no capítulo 01: nomes de classes e pacotes, idioma, JavaDoc, mensagens de commit, branches e nomes de migrations.

## Sumário

1. [Pacotes](#1-pacotes)
2. [Nomes de classes](#2-nomes-de-classes)
3. [Idioma](#3-idioma)
4. [JavaDoc](#4-javadoc)
5. [Mensagens de commit](#5-mensagens-de-commit)
6. [Branches](#6-branches)
7. [Migrations](#7-migrations)
8. [Limitações conhecidas](#8-limitações-conhecidas)

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

## 3. Idioma

| O quê | Idioma |
| --- | --- |
| Nomes no código (classes, métodos, variáveis, tabelas, colunas) | Inglês |
| JavaDoc | Português |
| Mensagens de log | Português |
| Mensagens de erro da API (`ErrorType` e exceções) | Inglês |
| Documentação em `docs/` | Português |

## 4. JavaDoc

As classes de controller, service, repositório, mapper, DTO, entidade e exceção têm JavaDoc em português, descrevendo a responsabilidade de cada uma. Recursos usados:

- `<p>` para parágrafos e `<b>` para destacar rótulos;
- `{@link Classe}` para referências a outras classes.

## 5. Mensagens de commit

Os commits mais recentes usam o formato:

```text
tipo(escopo): descrição
```

- **tipo** diz a natureza da mudança, por exemplo `refactor`;
- **escopo** é opcional e indica a área afetada;
- **descrição** em português, em letra minúscula e no presente (`renomeia`, `corrige`).

Exemplo real: `refactor: renomeia o pacote base e as referências de dscatalog para asjcatalog`.

## 6. Branches

O projeto é documentado por **capítulos**, e cada capítulo tem sua branch:

| Branch | Capítulo |
| --- | --- |
| `chapter-01-crud` | 01 — Operações CRUD (esta branch) |
| `chapter-02-tests` | 02 — Testes automatizados |
| `chapter-03-validation-security` | 03 — Validação e segurança |
| `chapter-04-domain-orm` | 04 — Domínio, ORM, casos de uso e acesso a dados |

Também existem `main` (a branch padrão do repositório remoto) e `develop`. Cada branch registra o projeto como estava ao fim do capítulo, e o `README.md` da raiz de cada uma apresenta aquele capítulo.

## 7. Migrations

- Nome no padrão do Flyway: `V<versão>__<descrição>.sql`, com dois sublinhados e descrição em minúsculas separada por `_`.
- Estrutura em `db/migration/schema`, com versões `V001` a `V004`.
- Dados de exemplo em `db/migration/data`, com versões `V100` a `V102`.

Detalhes em [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md).

## 8. Limitações conhecidas

- **Histórico de commits misto.** Dos commits desta branch, só três seguem o formato `tipo(escopo): descrição`, e dois deles têm a descrição em inglês. Os demais usam mensagens livres, como `Update README.md` ou `JavaDocs`.
- **Constante com erro de digitação.** A enum `ErrorType` tem a constante `CONFLIT` (em vez de `CONFLICT`). Ela foi mantida no código deste capítulo; a enum inteira é substituída por `ApiErrorCode` no capítulo 04.
