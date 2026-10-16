# CLAUDE.md — ASJCatalog (backend)

Este arquivo dá ao Claude Code o contexto e as regras do projeto. Ele é versionado e vale para qualquer pessoa que use a ferramenta neste repositório. Leia-o inteiro antes de qualquer tarefa e siga as regras mesmo que o pedido não as repita.

Ele é um **mapa**: traz só as regras e os fatos que valem para toda tarefa. Os detalhes ficam nos guias listados na [seção 13](#13-índice-da-documentação).

---

## 1. O projeto

- **ASJCatalog**: API REST de um catálogo de produtos, em Java e Spring Boot. O nome vem das iniciais de **Albert Silva de Jesus**.
- Nasceu da base do **DSCatalog**, do curso DevSuperior, e evoluiu de forma independente; hoje é praticamente outro projeto.
- **Open source e sem fins comerciais**: laboratório de aprendizado, fonte de consulta futura e material para ajudar desenvolvedores júnior.
- A evolução é contada em **capítulos**, um por branch, cada um publicado no LinkedIn.
- Frontend independente, em outro repositório: https://github.com/Albertinesilva/asjcatalog-frontend (documentação própria em [`docs/`](https://github.com/Albertinesilva/asjcatalog-frontend/tree/main/docs)).
- O projeto **recebe contribuições externas por Pull Request** (por exemplo, o #42). O guia de contribuição e os modelos de issue e de PR ficam na branch `main`: [`CONTRIBUTING.md`](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/main/CONTRIBUTING.md), `CODE_OF_CONDUCT.md` e `.github/`.

## 2. Repositório e pastas

- GitHub: `Albertinesilva/backend-engineering-journey-java-springboot`
- Os caminhos deste arquivo partem da **raiz do repositório clonado**: a pasta criada pelo `git clone` (por exemplo, `backend-engineering-journey-java-springboot/`).

```
<raiz do repositório>/          ← comandos Git aqui
├── README.md                   ← README do capítulo (na main, página de entrada)
├── LICENSE                     ← MIT
├── CLAUDE.md
├── .github/workflows/ci.yml    ← pipeline de CI (seção 5)
├── docs/                       ← documentação técnica
│   ├── HOME.md                 ← índice
│   ├── REFERENCES.md
│   ├── SECURITY-CONTRACT.md
│   ├── guides/*.md             ← guias técnicos
│   ├── audits/*.md             ← registros históricos (não alterar)
│   └── assets/imgs/
└── backend/                    ← projeto Maven: comandos Maven aqui
    ├── pom.xml
    ├── mvnw / mvnw.cmd
    └── src/main/{java,resources}, src/test/java
```

- **Comandos do Maven**: sempre dentro de `backend/`, com o wrapper (`./mvnw` ou `.\mvnw`), nunca `mvn`.
- **Comandos do Git**: sempre na raiz. `git add .` dentro de `backend/` deixa de fora `docs/` e o `README.md`.
- Pacote base: `com.albertsilva.dev.asjcatalog`.

## 3. Stack

- Java 17, Spring Boot 3.5, Maven Wrapper.
- Spring Data JPA com Hibernate 6.6; PostgreSQL (`dev` e `prod`); H2 (só no perfil `test`).
- Flyway 11.7, com migrations em `backend/src/main/resources/db/migration/{schema,reference,data}`.
- Spring Security 6.5, Spring Authorization Server 1.5 e Resource Server: JWT (RS256), `grant_type=password` customizado, refresh token com rotação, roles `ROLE_OPERATOR` e `ROLE_ADMIN`.
- Bean Validation com validadores customizados.
- Internacionalização: `messages_pt_BR`, `messages_en` e `messages_es`, configurada em `config/i18n/MessageSourceConfig.java`.
- Spring Mail com Thymeleaf. O envio é síncrono: os métodos têm `@Async`, mas o projeto não tem `@EnableAsync`.
- springdoc-openapi 2.8: Swagger em `/docs-asjcatalog.html` (perfis `dev` e `test`).
- Spring Boot DevTools: reinicia a aplicação quando o código muda, e **cada reinício invalida todos os tokens**, porque a chave RSA e as autorizações ficam só em memória.
- Testes: JUnit 5, Mockito, MockMvc e AssertJ; Surefire e Failsafe; JaCoCo 0.8.
- As versões exatas estão no `backend/pom.xml` (as gerenciadas pelo Spring Boot vêm do parent `spring-boot-starter-parent`).

## 4. Perfis

| Perfil | Banco | O que lembrar |
|---|---|---|
| `dev` (padrão, `APP_PROFILE`) | PostgreSQL local `asjcatalog` | `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` obrigatórias. A subida testa o SMTP: sem `MAIL_USERNAME` e `MAIL_PASSWORD` válidas, suba com `--spring.mail.test-connection=false` |
| `test` | H2 em memória | Flyway desligado; tabelas criadas pelo Hibernate e dados do `import.sql`; sem teste de SMTP |
| `prod` | PostgreSQL por variáveis | Fail fast: segredos, credenciais e URLs sem valor padrão. Não roda a pasta `data`; `ddl-auto=validate`; Swagger e console do H2 desligados |

- Usuários de exemplo (só `dev` e `test`): `albert@gmail.com` (OPERATOR) e `maria@gmail.com` (OPERATOR + ADMIN), senha `123456`; cliente OAuth2 `myclientid` / `myclientsecret`.
- O banco `asjcatalog` tem o mesmo nome em todos os capítulos, mas estrutura diferente: ao alternar entre branches, recrie-o vazio ([DATABASE-MIGRATIONS.md, seção 7](docs/guides/DATABASE-MIGRATIONS.md#7-recriar-o-banco-de-dev-do-zero)).
- Todas as variáveis de ambiente: [CONFIGURATION.md](docs/guides/CONFIGURATION.md).

## 5. Testes, cobertura e CI

- `*Test` e `*Tests`: testes de unidade, executados pelo **Surefire** (`./mvnw test`).
- `*IT`: testes de integração, executados pelo **Failsafe** (`./mvnw verify`, que roda os dois).
- **Sempre** rode `./mvnw clean verify` em `backend/` depois de alterar código ou configuração.
- O JaCoCo gera três relatórios em `backend/target/site/`: `jacoco/` (unidade), `jacoco-it/` (integração) e `jacoco-merged/` (consolidado, o principal).
- **CI:** `.github/workflows/ci.yml` roda `./mvnw -B verify` (JDK 17 Temurin, `ubuntu-24.04`) em todo push e Pull Request para a `develop`.
- Os testes, inclusive no CI, dependem de DNS por causa da consulta MX do `@ValidEmail` (#20).
- Se o primeiro build falhar logo depois de mover muitos arquivos, pode ser a extensão Java do VS Code recompilando ao mesmo tempo, ou o Surefire com a pasta temporária em outro disco. Rode de novo antes de concluir que há erro.
- Comandos, infraestrutura de teste (`AbstractIT`, `TokenUtil`, fábricas) e o que cada grupo cobre: [TESTING.md](docs/guides/TESTING.md).

## 6. Princípios de arquitetura (não quebrar)

- **Camadas:** `web.controller` → `service` → `repository` → `domain`. Controllers não têm regra de negócio e **nunca recebem nem devolvem entidades**: entre as camadas circulam DTOs (`record`), convertidos pelos mappers de `mapper.<assunto>`.
- **Transações só nos services** (`@Transactional`). Com `spring.jpa.open-in-view=false`, tudo o que a resposta precisa é carregado dentro do service; acessar um relacionamento *lazy* fora da transação gera `LazyInitializationException`.
- **Exceções de negócio** (`service.exception`, todas `RuntimeException`) carregam a **chave** da mensagem, e não o texto; o `ControllerExceptionHandler` traduz.
- **Formato de erro próprio:** a classe `ProblemDetails` do projeto (não segue a RFC 7807); erros de validação (422) usam `ValidationError`, com `fieldErrors`. O campo `code` (enum `ApiErrorCode`) é estável e não muda com o idioma.
- **Validadores customizados tratam `null` como válido.** A obrigatoriedade vem de `@NotBlank`, `@NotEmpty` ou `@NotNull`.
- **Mensagens:** toda chave nova entra nos três arquivos `messages_*.properties`. Títulos (`*.title`) usam capitalização de frase e não têm ponto final; as demais mensagens terminam com um único ponto final. O `MessagesPropertiesTest` confere chaves, placeholders e pontuação.
- **Idioma:** nomes no código, nos testes e nos `@DisplayName` em inglês; JavaDoc, comentários, logs, documentação e commits em português.
- **JavaDoc** descreve o comportamento real, inclusive as limitações. Ao mudar um comportamento, atualize o JavaDoc junto.
- **Testes:** nome `<ação>Should<resultado>When<condição>`, `@Nested` por método ou endpoint, dados das fábricas do pacote `factory` e `@ActiveProfiles("test")` nas classes que sobem o contexto.
- Detalhes: [ARCHITECTURE.md](docs/guides/ARCHITECTURE.md), [CONVENTIONS.md](docs/guides/CONVENTIONS.md) e [DESIGN-DECISIONS.md](docs/guides/DESIGN-DECISIONS.md).

## 7. Segurança

- O [SECURITY-CONTRACT.md](docs/SECURITY-CONTRACT.md) registra o comportamento atual e funciona como contrato. Leia-o antes de qualquer tarefa que toque `security/`, regras de acesso ou tokens.
- Não mudam sem análise (seção 20 do contrato): `grant_type=password`, autenticação do cliente, `RegisteredClient`, `TokenSettings`, `tokenCustomizer` e os claims `authorities`, `userId` e `username`, refresh token, `OAuth2AuthorizationService`, `SecurityFilterChain`, `@PreAuthorize`, CORS e endpoints públicos.
- **Não corrija automaticamente um risco de segurança identificado** (seção 24 do contrato): ele vira issue e, quando for corrigido, a mudança é deliberada, isolada e testada.

## 8. Armadilhas conhecidas

- **SMTP no `dev`:** sem credenciais de e-mail válidas, a aplicação não sobe (seção 4).
- **DNS nos testes:** o `@ValidEmail` consulta o registro MX; sem rede, testes de usuário falham (#20).
- **Tokens se perdem ao reiniciar**, inclusive nos reinícios do DevTools: é preciso fazer login de novo.
- **`POST /oauth2/token`** responde no formato do OAuth2, com mensagens fixas em inglês, e não com `ProblemDetails` (#32). O 401 do Resource Server vem sem corpo (#22).
- **`spring.messages.*` e `spring.web.locale*` não têm efeito:** a configuração de idioma está em `MessageSourceConfig`.
- **`import.sql` é mantido à mão** e precisa continuar igual às pastas `data` e `reference`. Os vínculos usam ids fixos, e as fábricas de teste supõem totais fixos do seed (#21).
- **Flyway desligado nos testes:** um erro numa migration só aparece ao subir em `dev` ou `prod` (#19).
- **Exceções do Spring MVC sem handler** (verbo HTTP não suportado, JSON malformado, parâmetro ausente, tipo errado na URL) respondem 500 (#36).
- **`GET /products?sort=price` responde 500:** a listagem só ordena por `id` e `name` (#3).
- **A atualização de categoria é `PATCH`**; a de produto e a de usuário são `PUT` (#28).
- **PowerShell:** argumentos `-D` com ponto no nome entre aspas simples (`'-Dspring.profiles.active=test'`), `curl.exe` em vez de `curl`, e JSON no corpo por `Invoke-RestMethod` ou por arquivo (`-d '@arquivo.json'`).

## 9. Branches e Pull Requests

| Branch | Papel | Regra |
|---|---|---|
| `chapter-01-crud` a `chapter-04-domain-orm` | Registro histórico de cada capítulo publicado | Não recebem código novo nem PR. Não apagar |
| `chapter-05-jacoco` | Capítulo atual (cobertura de testes) | Trabalho atual |
| `develop` | Projeto completo e atualizado | Destino de todos os Pull Requests |
| `main` | Branch padrão e página de entrada (README, CONTRIBUTING, CODE_OF_CONDUCT, modelos em `.github/`) | Não recebe código do projeto até o deploy final |

- Falta um capítulo depois do 05: **Docker e CI/CD**. Não existe capítulo de "Consultas".
- **Pull Requests de contribuição:** partem da `develop`, voltam para ela e entram por **squash**. Se resolvem uma issue, o número vai no nome da branch (por exemplo, `fix/31-padroniza-mensagens`).
- **Integração de um capítulo na `develop`:** por Pull Request, com **merge commit** (`gh pr merge --merge`), nunca squash nem rebase. Se houver conflito, use uma branch temporária `integracao/<capitulo>-develop` para não alterar a branch do capítulo.

## 10. Regras de trabalho (obrigatórias)

1. **O Claude Code não faz commit nem push**: quem usa a ferramenta faz os commits. Ao fim de cada etapa, pare e sugira a mensagem de commit.
2. **Plano antes de agir.** Em tarefas com várias mudanças, mostre o plano e espere o "ok". Em auditorias, não altere nada.
3. **Não corrija bugs nem implemente melhorias enquanto valer a regra do capítulo atual** (seção 16). Tudo o que faltar para a produção vira uma issue (seção 12). Mostre a proposta da issue antes de criar.
4. **Não invente.** Toda afirmação sobre o código precisa ser conferida no código. Cite arquivo e linha. Se não conseguir confirmar, diga que é dúvida.
5. **Não escreva testes que congelem um comportamento errado** nem testes que dependam de um bug estar corrigido.
6. **Preserve os finais de linha.** A maioria dos arquivos usa CRLF; o `backend/.gitattributes` fixa `backend/mvnw` em LF e os `*.cmd` em CRLF. Ferramentas como `sed` convertem para LF; confira com `git ls-files --eol`.
7. **Renomeações e movimentações com `git mv`**, para preservar o histórico. O `git mv` já coloca a renomeação na área de commit, mas as edições de conteúdo não: avise para usar `git add .` na raiz.
8. **Migrations aplicadas nunca são alteradas** (o Flyway guarda o checksum). Migration nova usa a maior versão existente + 1, qualquer que seja a pasta (hoje a próxima é a V106). Não usar `outOfOrder`.
9. **Não altere as URLs externas** `devsuperior/dscatalog-resources` (imagens do seed) até a #29 ser resolvida, nem os arquivos de `docs/audits/`.
10. **Ao subir a aplicação**, use uma porta diferente de 8080 e encerre apenas os processos que você iniciou, conferindo a porta antes.
11. **Arquivos temporários** (corpos de issue, scripts) ficam fora do repositório e são apagados ao final.
12. **Nunca exponha segredos** (senhas reais, tokens, valores de variáveis de ambiente) em arquivos, issues ou respostas.
13. **Mudanças em segurança** seguem a seção 7.

## 11. Commits (para sugerir)

- Formato: `tipo(escopo): descrição`, em português, minúsculas, verbo no presente da 3ª pessoa, sem ponto final.
- Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `build` e `ci`.
- Exemplos: `test: adiciona ITs dos fluxos de conta`, `fix(i18n): alinha chaves entre idiomas`, `ci: adiciona workflow de build e testes`.
- **Sem linha `Co-Authored-By` (nem outra atribuição) de ferramentas de IA.** A regra vale para qualquer pessoa que use a ferramenta.
- Ao resolver uma issue, cite o número (`Refs #44`). O fechamento automático só acontece na branch padrão (`main`), então as issues são fechadas manualmente.

## 12. Issues

- Sempre use `--repo Albertinesilva/backend-engineering-journey-java-springboot` nos comandos `gh`.
- Antes de criar, confira se já existe uma issue igual (`gh issue list --state all`).
- Corpo em português, em markdown, com as seções:
  - `## Contexto`: o problema, com arquivo e linha;
  - `## O que fazer`: quando houver um caminho claro;
  - `## Impacto no frontend`: **obrigatória** quando a mudança exigir alteração ou verificação no `asjcatalog-frontend` (contrato da API, status HTTP, formato de erro, autenticação, tokens, filtros, fluxos de conta);
  - `## Critérios de aceite`: checklist com `- [ ]`. Quando houver impacto no frontend, inclua um item de verificação do frontend.
- **Links para o código:** permalinks com o SHA do commit (`.../blob/<sha>/backend/...#L10-L20`), para que a linha citada não mude.
- Passe o corpo por arquivo (`--body-file`), com o arquivo temporário fora do repositório.
- Labels: `bug`, `security`, `enhancement`, `tests`, `tech-debt`, `i18n`, `documentation`, `production-readiness`, `good first issue` e **`frontend`** (obrigatória quando houver impacto no frontend).
- Milestone **"Pronto para produção"** para o que precisa estar resolvido antes do deploy.

## 13. Índice da documentação

| Assunto | Onde |
|---|---|
| Índice geral e ordem de leitura | [docs/HOME.md](docs/HOME.md) |
| Ambiente, primeira subida, SMTP e problemas comuns | [docs/guides/GETTING-STARTED.md](docs/guides/GETTING-STARTED.md) |
| Perfis, variáveis de ambiente e fail fast | [docs/guides/CONFIGURATION.md](docs/guides/CONFIGURATION.md) |
| Camadas, pacotes e services | [docs/guides/ARCHITECTURE.md](docs/guides/ARCHITECTURE.md) |
| Entidades, relacionamentos e regras do domínio | [docs/guides/DOMAIN-MODEL.md](docs/guides/DOMAIN-MODEL.md) |
| Repositórios, consultas nativas, N+1, paginação e transações | [docs/guides/DATA-ACCESS.md](docs/guides/DATA-ACCESS.md) |
| Flyway, pastas, numeração, `import.sql` e recriação do banco | [docs/guides/DATABASE-MIGRATIONS.md](docs/guides/DATABASE-MIGRATIONS.md) |
| Endpoints, permissões, corpos e respostas | [docs/guides/API-ENDPOINTS.md](docs/guides/API-ENDPOINTS.md) |
| Regras de validação, senha forte e e-mail (MX) | [docs/guides/VALIDATION.md](docs/guides/VALIDATION.md) |
| Formato de erro, `ApiErrorCode` e status HTTP | [docs/guides/ERROR-HANDLING.md](docs/guides/ERROR-HANDLING.md) |
| Idiomas, arquivos de mensagens e como adicionar uma chave | [docs/guides/INTERNATIONALIZATION.md](docs/guides/INTERNATIONALIZATION.md) |
| Login, JWT, refresh token, roles, 401 e 403 | [docs/guides/AUTHENTICATION.md](docs/guides/AUTHENTICATION.md) |
| Cadastro, ativação, recuperação de senha e e-mails | [docs/guides/ACCOUNT-FLOWS.md](docs/guides/ACCOUNT-FLOWS.md) |
| Testes: tipos, comandos, infraestrutura e cobertura | [docs/guides/TESTING.md](docs/guides/TESTING.md) |
| Convenções de código, commits, branches e migrations | [docs/guides/CONVENTIONS.md](docs/guides/CONVENTIONS.md) |
| Decisões técnicas e trade-offs | [docs/guides/DESIGN-DECISIONS.md](docs/guides/DESIGN-DECISIONS.md) |
| Contrato de segurança | [docs/SECURITY-CONTRACT.md](docs/SECURITY-CONTRACT.md) |
| Referências oficiais e livros | [docs/REFERENCES.md](docs/REFERENCES.md) |
| Histórico de auditorias (não alterar) | [docs/audits/](docs/audits/) |
| Como contribuir | [CONTRIBUTING.md](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/main/CONTRIBUTING.md) (branch `main`) |
| Documentação do frontend | [asjcatalog-frontend/docs](https://github.com/Albertinesilva/asjcatalog-frontend/tree/main/docs) |

## 14. Escrita da documentação

- Público: desenvolvedor júnior. Explique cada termo técnico na primeira vez que aparecer.
- Guias em `docs/guides/`: formato técnico e enxuto, em português, sem badges, sem introdução em inglês e sem Autor/Contato. Título, uma frase de propósito e sumário se tiver mais de quatro seções.
- Cada guia tem uma linha de navegação no topo e no fim (`⬅️ Anterior · 🏠 Índice · Próximo ➡️`), na ordem de leitura definida em `docs/HOME.md`. Guia novo exige atualizar a navegação dos vizinhos e a tabela do `HOME.md`.
- Os guias que descrevem comportamento terminam com **"Limitações conhecidas"**, com o comportamento real e, quando for o caso, o capítulo em que o problema foi resolvido.
- `docs/guides/DESIGN-DECISIONS.md`: decisões no formato contexto → decisão → por quê → alternativas possíveis → consequências. Os campos **"a preencher pelo autor"** são respondidos só pelo mantenedor do projeto (#33): não deduza nem preencha.
- Comandos de exemplo para PowerShell e, quando diferente, para bash, um comando por linha (regras do PowerShell na seção 8).
- Links relativos dentro de `docs/`; links absolutos do GitHub quando o arquivo não existir na branch atual. Confira que todos os links e âncoras resolvem antes de concluir.
- README dos capítulos: preserve o padrão (título h1 centralizado, intro em inglês, badges terminando em `License` e `last-commit`, parágrafo em pt-BR, Sumário em tabela, seções de abertura e de fechamento, "🚀 Como Executar" e "📖 Documentação Técnica"). Badges só de tecnologias que existem na branch.

## 15. Comunicação

- Responda em português do Brasil, de forma clara e didática.
- Ao terminar uma etapa, mostre: o que mudou, o resultado do `./mvnw clean verify` (quando aplicável), o `git status` e a sugestão de commit.

## 16. Estado atual (atualizar a cada etapa)

Atualizado em 16/10/2026.

- Branch de trabalho: `chapter-05-jacoco`, criada a partir da `develop`. Nesta data, as duas apontam para o mesmo commit.
- **Regra do capítulo 05:** até o fim dos estudos de JaCoCo, nada de correções de bugs nem de melhorias (regra 3 da seção 10). Infraestrutura de testes, como a #20, continua no capítulo.
- Plano do capítulo 05:
  - Etapa 1: JaCoCo 0.8.15 com relatórios de unidade, integração e consolidado, e exclusões. **Concluída** (`b1cb5cf`).
  - Etapa 2: limite mínimo de cobertura (`check`) com 0,85 de linhas e 0,68 de branches no BUNDLE, sobre o consolidado. **Não iniciada: é a próxima.**
  - Etapa 3a: issue #20 (dependência de DNS nos testes), por ser infraestrutura de testes e pré-requisito do CI.
  - Etapa 3b: só testes novos para as lacunas de cobertura (handler de exceções, mappers, `Token.isValid`, provider de login, validador de e-mail da própria conta e contratos de `equals`/`hashCode`), seguidos de nova medição e reajuste dos limites com cerca de 3 pontos de folga.
  - Etapa 4: documentação e README do capítulo 05:
    - TESTING (cobertura, contagens e CI);
    - DESIGN-DECISIONS;
    - HOME (capítulo 05 e "Falta um capítulo");
    - CONVENTIONS (branch do capítulo 05 e tipo `ci`);
    - GETTING-STARTED (`git checkout` do capítulo);
    - REFERENCES;
    - exemplo de 404 do API-ENDPOINTS sem o ponto final da #31.
- Fora do plano, já na branch:
  - `ea58b16`: torna o `mvnw` executável;
  - `cedeba7` e `dd40232`: pipeline de CI;
  - `f35bfe5`: PR #42, contribuição externa;
  - `d998792`: PR #43, que padronizou as mensagens (#31).
- Issues abertas nesta etapa:
  - #44 (produto criado sem preço; label `frontend`);
  - #45 (coluna `price` NOT NULL, a ser feita junto com a #6).
  - A label `frontend` foi criada e aplicada às issues com impacto no frontend.
- PUT × PATCH de categoria: o frontend enviava `PUT` para `/api/v1/categories/{id}`, que só aceita `PATCH`. O problema está sendo corrigido diretamente no repositório do frontend, sem issue. No backend, fica o 405 que vira 500, registrado como cenário da #36.
- Confira no `git log` qual etapa já foi commitada antes de continuar.
