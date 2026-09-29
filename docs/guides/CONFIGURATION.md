# Configuração e perfis

Este guia explica como a aplicação é configurada em cada ambiente (perfis `dev`, `test` e `prod`) e lista todas as variáveis de ambiente que ela lê.

## Sumário

1. [O que é um perfil](#1-o-que-é-um-perfil)
2. [Como o perfil é escolhido](#2-como-o-perfil-é-escolhido)
3. [Comparação entre os perfis](#3-comparação-entre-os-perfis)
4. [Variáveis de ambiente](#4-variáveis-de-ambiente)
5. [Fail fast no perfil prod](#5-fail-fast-no-perfil-prod)
6. [Internacionalização](#6-internacionalização)
7. [Limitações conhecidas](#7-limitações-conhecidas)

## 1. O que é um perfil

Um **perfil** do Spring é um conjunto de configurações que só vale em um ambiente. O projeto tem quatro arquivos em `backend/src/main/resources`:

| Arquivo | Quando vale |
| --- | --- |
| `application.properties` | Sempre, em todos os perfis |
| `application-dev.properties` | Só no perfil `dev` (desenvolvimento local, com PostgreSQL) |
| `application-test.properties` | Só no perfil `test` (banco H2 em memória) |
| `application-prod.properties` | Só no perfil `prod` (produção) |

O Spring lê primeiro o `application.properties` e depois o arquivo do perfil ativo. Quando a mesma propriedade aparece nos dois, vale o valor do arquivo do perfil.

## 2. Como o perfil é escolhido

O `application.properties` define:

```properties
spring.profiles.active=${APP_PROFILE:dev}
```

A sintaxe `${NOME:padrão}` lê a variável de ambiente `NOME` e usa `padrão` quando ela não existe. Ou seja: **sem a variável `APP_PROFILE`, o perfil é `dev`**.

Para rodar com outro perfil, defina `APP_PROFILE` antes de subir.

**PowerShell**:

```powershell
$env:APP_PROFILE = 'test'
.\mvnw spring-boot:run
```

**bash**:

```bash
APP_PROFILE=test ./mvnw spring-boot:run
```

O perfil ativo aparece no início do log (`The following 1 profile is active: "dev"`) e no banner dos perfis `dev` e `test` (`Environment: dev`).

Os testes automatizados não dependem de `APP_PROFILE`: as classes de teste fixam o perfil `test` com `@ActiveProfiles("test")`.

## 3. Comparação entre os perfis

| Aspecto | `dev` | `test` | `prod` |
| --- | --- | --- | --- |
| Banco | PostgreSQL em `localhost:5432/asjcatalog` | H2 em memória (`jdbc:h2:mem:testdb`, usuário `sa`, sem senha) | PostgreSQL no endereço de `POSTGRES_DATASOURCE_URL` |
| Credenciais do banco | `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` | Fixas no arquivo | `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` |
| Pool de conexões | Padrão do Spring Boot | Padrão do Spring Boot | Até `DB_POOL_SIZE` conexões (padrão 10) |
| Flyway | Ligado: pastas `schema`, `reference` e `data` | Desligado | Ligado: pastas `schema` e `reference` |
| Quem cria as tabelas | Flyway | Hibernate, e depois o `import.sql` insere os dados | Flyway |
| `ddl-auto` | `none` | Não definido (padrão do Spring Boot para banco em memória) | `validate` |
| SQL no console (`show-sql`) | Sim, formatado | Sim, formatado | Não |
| Console do H2 | Desligado | Ligado em `/h2-console` | Desligado |
| Swagger | `/docs-asjcatalog.html` | `/docs-asjcatalog.html` | Desativado |
| Nível de log da aplicação | `DEBUG` | `DEBUG` | `INFO` |
| Outros logs | Spring `INFO`, SQL `DEBUG`, parâmetros do SQL `TRACE`, Flyway `DEBUG` | Spring Web `DEBUG`, SQL `DEBUG`, parâmetros do SQL `TRACE` | Spring Web `WARN`; SQL e parâmetros desligados |
| Arquivo de log | `logs/dev/asjcatalog-dev.log` (até 10 MB por arquivo) | `logs/test/asjcatalog-test.log` (até 5 MB) | `logs/asjcatalog-prod.log` (até 50 MB) |
| Banner | `banner-dev.txt` | `banner-dev.txt` | Padrão do Spring Boot |
| Porta | 8080 | 8080 | 8080 (padrão; não está no arquivo) |
| Cliente OAuth2 | Padrão `myclientid` / `myclientsecret` | Padrão `myclientid` / `myclientsecret` | `CLIENT_ID` e `CLIENT_SECRET` obrigatórias |

Termos da tabela:

- **H2** é um banco de dados escrito em Java que pode rodar só na memória. Ele é criado vazio a cada subida e apagado quando a aplicação para.
- **Flyway** aplica scripts SQL versionados (*migrations*). Veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md).
- **`ddl-auto`** diz ao Hibernate o que fazer com as tabelas ao subir: `none` não faz nada; `validate` confere se as entidades batem com as tabelas e impede a subida se não baterem. No perfil `test`, sem valor definido e com o Flyway desligado, o Spring Boot usa o padrão para banco em memória, que cria as tabelas a partir das entidades.
- **Console do H2** é uma página web para consultar o banco em memória. No perfil `test`, acesse `http://localhost:8080/h2-console` com a URL `jdbc:h2:mem:testdb` e o usuário `sa`.
- Os **caminhos de log** são relativos à pasta de onde a aplicação foi iniciada; com `cd backend`, ficam em `backend/logs/`.

O perfil `test` também devolve o JSON das respostas com indentação (`spring.jackson.serialization.indent_output=true`).

## 4. Variáveis de ambiente

Todas as variáveis que a aplicação lê. "Obrigatória em prod" significa que o `application-prod.properties` não tem valor padrão para ela.

| Variável | Propriedade | Padrão (dev e test) | Obrigatória em prod | Para que serve |
| --- | --- | --- | --- | --- |
| `APP_PROFILE` | `spring.profiles.active` | `dev` | Defina como `prod` | Escolhe o perfil |
| `POSTGRES_DATASOURCE_URL` | `spring.datasource.url` | Não usada (dev tem URL fixa; test usa H2) | Sim | Endereço JDBC do banco em produção |
| `POSTGRES_DATASOURCE_USER` | `spring.datasource.username` | Sem padrão (**obrigatória também em dev**) | Sim | Usuário do PostgreSQL |
| `POSTGRES_DATASOURCE_PASSWORD` | `spring.datasource.password` | Sem padrão (**obrigatória também em dev**) | Sim | Senha do PostgreSQL |
| `DB_POOL_SIZE` | `spring.datasource.hikari.maximum-pool-size` | Só existe em prod: `10` | Não | Máximo de conexões abertas com o banco |
| `CLIENT_ID` | `security.client-id` | `myclientid` | Sim | Identificação do cliente OAuth2 que pede tokens |
| `CLIENT_SECRET` | `security.client-secret` | `myclientsecret` | Sim | Senha do cliente OAuth2 |
| `JWT_DURATION` | `security.jwt.duration` | `86400` (segundos, 24 horas) | Não | Validade do token de acesso |
| `CORS_ORIGINS` | `cors.origins` | `http://localhost:3000,http://localhost:5173` | Sim | Origens de navegador autorizadas a chamar a API, separadas por vírgula |
| `BACKEND_URL` | `backend.url` | `http://localhost:8080` | Sim | Hoje não é usada (veja Limitações conhecidas) |
| `FRONTEND_URL` | `frontend.url` | `http://localhost:5173` | Sim | Base dos links enviados por e-mail (`/activate-account?token=` e `/reset-password?token=`) |
| `MAIL_HOST` | `spring.mail.host` | `smtp.gmail.com` | Não | Servidor SMTP |
| `MAIL_PORT` | `spring.mail.port` | `587` | Não | Porta do servidor SMTP |
| `MAIL_USERNAME` | `spring.mail.username` | `test@gmail.com` (fictício) | Sim | Usuário do servidor SMTP |
| `MAIL_PASSWORD` | `spring.mail.password` | Valor fictício | Sim | Senha do servidor SMTP |
| `ACTIVATION_TOKEN_HOURS` | `account.activation.token.hours` | `24` | Não | Validade, em horas, do token de ativação de conta |
| `PASSWORD_RECOVER_TOKEN_MINUTES` | `account.password-recovery.token.minutes` | `30` | Não | Validade, em minutos, do token de recuperação de senha |

**CORS** (*Cross-Origin Resource Sharing*) é a regra do navegador que bloqueia chamadas de uma página hospedada em um endereço para uma API em outro endereço, a menos que a API autorize aquela origem.

Como definir variáveis no PowerShell e no bash: veja [GETTING-STARTED.md](GETTING-STARTED.md#3-definir-as-variáveis-obrigatórias).

**Propriedades fixas que também valem em todos os perfis** (não vêm de variável):

| Propriedade | Valor | Efeito |
| --- | --- | --- |
| `spring.jpa.open-in-view` | `false` | Desliga o *Open Session in View*; explicado em [DATA-ACCESS.md](DATA-ACCESS.md#7-open-in-view-e-transações) |
| `spring.mail.test-connection` | `true` | A subida testa a conexão com o SMTP; veja [GETTING-STARTED.md](GETTING-STARTED.md#5-servidor-de-e-mail-smtp) |
| `spring.mail.properties.mail.smtp.auth` e `...starttls.enable` | `true` | Autenticação e criptografia STARTTLS no SMTP |

## 5. Fail fast no perfil prod

*Fail fast* significa falhar logo na subida em vez de rodar com uma configuração errada. No `application-prod.properties`, os segredos, as credenciais e os endereços não têm valor padrão, e **a aplicação não sobe se algum deles faltar**. O erro aparece de duas formas, conforme a propriedade:

| Tipo de propriedade | Exemplos | O que aparece no log |
| --- | --- | --- |
| Lida pelo código com `@Value` | `security.client-id`, `security.client-secret`, `cors.origins`, `frontend.url`, `backend.url` | `Could not resolve placeholder 'NOME_DA_VARIAVEL' in value "${NOME_DA_VARIAVEL}"` |
| Lida pela configuração automática do Spring | `spring.datasource.*`, `spring.mail.*` | O texto `${...}` é usado literalmente, e a falha aparece ao conectar. Exemplo: `password authentication failed for user "${POSTGRES_DATASOURCE_USER}"` |

O perfil `prod` também:

- não roda a pasta `db/migration/data`, então os usuários e produtos de exemplo não existem em produção;
- usa `ddl-auto=validate`, que impede a subida se as entidades não baterem com as tabelas;
- desativa o Swagger (`springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false`) e o console do H2.

## 6. Internacionalização

A configuração de idiomas fica na classe `config/i18n/MessageSourceConfig.java`, e não em arquivos `.properties`:

- as mensagens são lidas de `messages_pt_BR.properties`, `messages_en.properties` e `messages_es.properties`, em UTF-8;
- o idioma de cada resposta vem do cabeçalho HTTP `Accept-Language`;
- o idioma padrão é português do Brasil (`pt_BR`).

> **Atenção:** como a classe declara os beans `messageSource` e `localeResolver`, as propriedades `spring.messages.*` e `spring.web.locale*` **não têm efeito** neste projeto. Para mudar o comportamento de idioma, altere a classe `MessageSourceConfig`.

Os detalhes estão em [INTERNATIONALIZATION.md](INTERNATIONALIZATION.md).

## 7. Limitações conhecidas

- **`BACKEND_URL` não é usada.** A propriedade `backend.url` é injetada no `EmailService`, mas nenhum código usa o valor. Mesmo assim, ela é obrigatória em prod.
- **Variável ausente em dev aparece como erro de senha.** Veja a tabela da seção 5 e os problemas comuns em [GETTING-STARTED.md](GETTING-STARTED.md#9-problemas-comuns).
- **O teste de conexão SMTP vale em todos os perfis.** `spring.mail.test-connection=true` está no `application.properties` e nenhum perfil o desliga.
- **Espaços em `CORS_ORIGINS` não são removidos.** O valor é dividido por vírgula sem retirar espaços; escreva a lista sem espaço depois da vírgula.
- **Console do H2 sem autenticação no perfil `test`.** Quando o console está ligado, a aplicação cria uma cadeia de segurança própria para ele, sem regras de acesso. Não use o perfil `test` em um servidor acessível por outras pessoas.
- **Segredos padrão versionados.** Os valores padrão de `CLIENT_ID`, `CLIENT_SECRET`, `MAIL_USERNAME` e `MAIL_PASSWORD` estão no `application.properties`, que é público. Eles servem só para desenvolvimento; em prod, as variáveis são obrigatórias.
