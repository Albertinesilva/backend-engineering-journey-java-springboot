# Convenções

⬅️ Anterior: [Testes](TESTING.md) · [🏠 Índice](../HOME.md) · Próximo: [Decisões técnicas](DESIGN-DECISIONS.md) ➡️

Este guia reúne as convenções seguidas no ASJCatalog: nomes de classes e pacotes, idioma, JavaDoc, testes, mensagens de commit, branches e numeração de migrations.

## Sumário

1. [Pacotes](#1-pacotes)
2. [Nomes de classes](#2-nomes-de-classes)
3. [Idioma](#3-idioma)
4. [JavaDoc](#4-javadoc)
5. [Testes](#5-testes)
6. [Mensagens de commit](#6-mensagens-de-commit)
7. [Branches](#7-branches)
8. [Migrations](#8-migrations)

## 1. Pacotes

- Pacote base: `com.albertsilva.dev.asjcatalog`. Nomes de pacote em minúsculas, sem separadores.
- O primeiro nível é a **camada** (`web`, `service`, `repository`, `domain`, `dto`, `mapper`, `validation`, `security`, `config`...). Dentro da camada, os subpacotes separam por **assunto** (`dto.category`, `validation.user`, `domain.catalog`).
- DTOs se dividem em `request` (entrada) e `response` (saída). Validações se dividem em `annotation` e `validator`.
- Os testes repetem o pacote da classe testada (`service/CategoryServiceTest` testa `service/CategoryService`). Os testes de integração ficam em `integrations/`, com subpacotes por assunto.

A árvore completa está em [ARCHITECTURE.md](ARCHITECTURE.md#2-pacotes).

## 2. Nomes de classes

| Tipo | Padrão | Exemplo |
| --- | --- | --- |
| Controller | `<Recurso>Controller` | `CategoryController` |
| Service | `<Recurso>Service` | `AccountService` |
| Repositório | `<Entidade>Repository` | `ProductRepository` |
| Mapper | `<Entidade>Mapper` | `UserMapper` |
| DTO de entrada | `<Entidade><Ação>Request` | `CategoryCreateRequest`, `PasswordResetRequest` |
| DTO de saída | `<Entidade>Response` e `<Entidade>DetailsResponse` (versão com mais campos) | `ProductDetailsResponse` |
| Projection | `<Assunto>Projection` | `UserDetailsProjection` |
| Anotação de validação de classe | `<Entidade><Ação>Valid` | `ProductCreateValid` |
| Anotação de validação de campo | Descreve a regra | `StrongPassword`, `UniqueEmail`, `ValidEmail` |
| Validator | `<NomeDaAnotação>Validator` | `StrongPasswordValidator` |
| Exceção | `<Situação>Exception` | `ResourceNotFoundException` |
| Configuração | `<Assunto>Config` | `MessageSourceConfig`, `ResourceServerConfig` |
| Teste de unidade | `<Classe>Test` | `TokenServiceTest` |
| Teste de integração | `<Classe ou assunto>IT` | `CategoryControllerIT`, `OAuth2TokenIT` |
| Fábrica de dados de teste | `<Entidade>Factory` | `UserFactory` |

DTOs são `record`. Entidades são classes com construtor sem argumentos, *getters* e *setters*.

## 3. Idioma

| O quê | Idioma |
| --- | --- |
| Nomes no código (classes, métodos, variáveis, tabelas, colunas) | Inglês |
| Nomes de testes e `@DisplayName` | Inglês |
| JavaDoc e comentários | Português |
| Mensagens de log | Português |
| Mensagens da API | Português, inglês e espanhol, pelos arquivos `messages_*.properties` |
| Documentação em `docs/` | Português |
| Mensagens de commit | Português |

## 4. JavaDoc

As classes de produção têm JavaDoc em português, escrito para explicar o **comportamento real**, inclusive limitações conhecidas (por exemplo, o JavaDoc do `EmailService` avisa que o `@Async` não tem efeito). Recursos usados:

- `<p>` para parágrafos e `<b>` para destacar rótulos;
- `{@code ...}` para trechos de código e nomes de propriedades;
- `{@link Classe}` para referências a outras classes.

Ao mudar um comportamento, atualize o JavaDoc junto.

## 5. Testes

- Nome do método no padrão `<ação>Should<resultado>When<condição>`, por exemplo `findByIdShouldReturnCategoryWhenIdExists` ou `deleteShouldReturnForbiddenWithoutWriteRole`.
- `@DisplayName` em inglês, descrevendo o cenário.
- `@Nested` agrupa os testes de um mesmo método ou endpoint.
- Dados de teste vêm das fábricas do pacote `factory`.
- Classes que sobem o contexto do Spring fixam o perfil com `@ActiveProfiles("test")`.

Como rodar e organizar: [TESTING.md](TESTING.md).

## 6. Mensagens de commit

Formato:

```text
tipo(escopo): descrição
```

- **tipo** diz a natureza da mudança;
- **escopo** é opcional e indica a área afetada;
- **descrição** em português, em letra minúscula, no presente e na terceira pessoa (`adiciona`, `corrige`, `remove`), sem ponto final.

Tipos usados no histórico:

| Tipo | Uso | Exemplo real |
| --- | --- | --- |
| `feat` | Funcionalidade nova | `feat(i18n): testa idioma por Accept-Language, remove config de locale sem efeito e atualiza banner` |
| `fix` | Correção | `fix(flyway): move roles para a pasta reference e aplica em todos os perfis` |
| `refactor` | Mudança de código sem mudar comportamento | `refactor: renomeia o pacote base de dscatalog para asjcatalog` |
| `test` | Testes | `test: amplia a cobertura com ITs de conta, autorização, CORS e consultas nativas` |
| `docs` | Documentação | `docs: reorganiza estrutura de documentação, move auditorias e adiciona licença MIT` |
| `build` | Build e dependências | `build: adiciona o maven-failsafe-plugin para executar os testes *IT` |

Escopos já usados: `i18n`, `flyway` e `config`.

Os commits mais antigos do repositório não seguem esse formato (há mensagens como `Update README.md`). A convenção vale para os commits novos.

## 7. Branches

O projeto é documentado por **capítulos**, e cada capítulo tem sua branch:

| Branch | Capítulo |
| --- | --- |
| `chapter-01-crud` | 01 — CRUD |
| `chapter-02-tests` | 02 — Testes automatizados |
| `chapter-03-validation-security` | 03 — Validação e segurança |
| `chapter-04-domain-orm` | 04 — Domínio, ORM, casos de uso e acesso a dados |

Também existem `main` (a branch padrão do repositório remoto) e `develop`. O trabalho de um capítulo é feito na branch dele, e o `README.md` da raiz de cada branch apresenta aquele capítulo.

## 8. Migrations

**Numeração global e sequencial.** A próxima migration recebe a **maior versão existente + 1**, **qualquer que seja a pasta**. Hoje a maior é `V105`, então a próxima é `V106`, depois `V107`, e assim por diante.

- **A pasta define em quais perfis a migration roda.** `schema` e `reference` rodam em `dev` e `prod`; `data` roda só em `dev`.
- **O número define a ordem.** O Flyway junta as pastas e ordena só pela versão.
- **Não use `outOfOrder`.** A configuração do projeto não o ativa, e ele não deve ser ativado para contornar a numeração.

**Por quê.** Um banco já migrado anota no histórico do Flyway a maior versão aplicada (hoje V105 em `dev` e V104 em `prod`). Uma migration nova com número menor, como uma `V012` em `schema`, fica "fora de ordem", e o Flyway recusa a subida com `Detected resolved migration not applied to database`. Com a regra "maior + 1", toda migration nova é sempre a última, em qualquer ambiente. A faixa `V001`–`V011` de `schema` e a faixa `V100`–`V105` de `data` são históricas e não indicam onde numerar a próxima.

Passo a passo para criar uma migration: [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#6-criar-uma-migration-nova).

---

⬅️ Anterior: [Testes](TESTING.md) · [🏠 Índice](../HOME.md) · Próximo: [Decisões técnicas](DESIGN-DECISIONS.md) ➡️
