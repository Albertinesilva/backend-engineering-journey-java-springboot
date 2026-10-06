# Configuração e perfis

Este guia explica como a aplicação é configurada no capítulo 03: os arquivos de configuração, os perfis `dev`, `test` e `prod`, as propriedades de segurança, os arquivos de log e as variáveis de ambiente.

## Sumário

1. [O que é um perfil](#1-o-que-é-um-perfil)
2. [Como o perfil é escolhido](#2-como-o-perfil-é-escolhido)
3. [Comparação entre os perfis](#3-comparação-entre-os-perfis)
4. [Propriedades de segurança](#4-propriedades-de-segurança)
5. [Variáveis de ambiente](#5-variáveis-de-ambiente)
6. [Arquivos de log](#6-arquivos-de-log)
7. [Limitações conhecidas](#7-limitações-conhecidas)

## 1. O que é um perfil

Um **perfil** do Spring é um conjunto de configurações que só vale em um ambiente. O projeto tem estes arquivos em `backend/src/main/resources`:

| Arquivo | Quando vale |
| --- | --- |
| `application.properties` | Sempre, em todos os perfis |
| `application-dev.properties` | Só no perfil `dev` (desenvolvimento, com PostgreSQL) |
| `application-test.properties` | Só no perfil `test` (banco H2 em memória) |
| `application-prod.properties` | Só no perfil `prod` |
| `ValidationMessages.properties` | Mensagens de validação (veja [VALIDATION.md](VALIDATION.md#7-de-onde-vêm-as-mensagens)) |
| `META-INF/additional-spring-configuration-metadata.json` | Declara as propriedades próprias do projeto (`security.*` e `cors.origins`) para que a IDE as reconheça; não muda o comportamento da aplicação |

O Spring lê primeiro o `application.properties` e depois o arquivo do perfil ativo. Quando a mesma propriedade aparece nos dois, vale o valor do arquivo do perfil.

O `application.properties`:

```properties
spring.profiles.active=dev

spring.jpa.open-in-view=false

security.client-id=${CLIENT_ID:myclientid}
security.client-secret=${CLIENT_SECRET:myclientsecret}

security.jwt.duration=${JWT_DURATION:86400}

cors.origins=${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
```

- `spring.profiles.active=dev` define o perfil padrão. No capítulo 02 o padrão era `test`; neste capítulo voltou a ser `dev`, e por isso a aplicação e o teste de contexto precisam do PostgreSQL quando o perfil não é informado.
- `spring.jpa.open-in-view=false` desliga o *Open Session in View*. Explicado em [DATA-ACCESS.md](DATA-ACCESS.md#5-transações-e-open-in-view).
- As propriedades de segurança estão na seção 4. A sintaxe `${VARIAVEL:padrão}` lê a variável de ambiente e, se ela não existir, usa o valor depois dos dois-pontos.

## 2. Como o perfil é escolhido

O perfil padrão é `dev`, escrito diretamente no arquivo. Para usar outro, passe o perfil como argumento na linha de comando, que tem prioridade sobre o arquivo.

**PowerShell**:

```powershell
cd backend
.\mvnw spring-boot:run '-Dspring-boot.run.arguments=--spring.profiles.active=test'
```

**bash**:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=test
```

Com o `.jar` gerado pelo build (`backend/target/asjcatalog-0.0.1-SNAPSHOT.jar`), o argumento vai direto no comando (igual no PowerShell e no bash):

```powershell
java -jar target/asjcatalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=test
```

Nos testes, o perfil é passado ao Maven: `'-Dspring.profiles.active=test'` (veja [TESTING.md](TESTING.md#1-como-rodar-os-testes)).

O perfil ativo aparece no início do log (`The following 1 profile is active: "test"`) e no banner dos perfis `dev` e `test` (`Environment: test`).

## 3. Comparação entre os perfis

| Aspecto | `dev` (padrão) | `test` | `prod` |
| --- | --- | --- | --- |
| Banco | PostgreSQL em `localhost:5432/asjcatalog` | H2 em memória (`jdbc:h2:mem:testdb`, usuário `sa`, sem senha) | Não configurado (veja Limitações conhecidas) |
| Credenciais do banco | `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` | Fixas no arquivo | — |
| Flyway | Ligado: pastas `schema` e `data` | Desligado | Não configurado |
| Quem cria as tabelas | Flyway | Hibernate, e depois o `import.sql` insere os dados | — |
| `ddl-auto` | `none` | Não definido (padrão do Spring Boot para banco em memória) | Não definido |
| SQL no console (`show-sql`) | Sim, formatado | Sim, formatado | Não definido |
| Console do H2 | Não configurado | Ligado em `/h2-console`, com cadeia de segurança própria | Não configurado |
| Swagger | `/docs-asjcatalog.html` | `/docs-asjcatalog.html` | Não configurado |
| Nível de log da aplicação | `DEBUG` | `DEBUG` | `INFO` |
| Banner | `banner-dev.txt` | `banner-dev.txt` | Padrão do Spring Boot |
| Porta | 8080 | 8080 | 8080 (padrão; não está no arquivo) |
| Nome da aplicação | `ASJCatalog` | `ASJCatalog` | Não definido |

Termos da tabela:

- **H2** é um banco de dados escrito em Java que pode rodar só na memória.
- **Flyway** aplica scripts SQL versionados (*migrations*). Veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md).
- **`ddl-auto`** diz ao Hibernate o que fazer com as tabelas ao subir: `none` não faz nada. No perfil `test`, sem valor definido e com o Flyway desligado, o Spring Boot usa o padrão para banco em memória, que cria as tabelas a partir das entidades.

Outras configurações por perfil:

- **`dev`:** `spring.web.locale-resolver=fixed` e `spring.web.locale=pt_BR` (idioma fixo em português do Brasil) e codificação UTF-8 nas requisições e respostas (`server.servlet.encoding.*`).
- **`test`:** o JSON das respostas vem indentado (`spring.jackson.serialization.indent_output=true`), e os logs das requisições HTTP (`org.springframework.web`) ficam em `DEBUG`.

## 4. Propriedades de segurança

Valem em todos os perfis, porque ficam no `application.properties`:

| Propriedade | Padrão | Para que serve |
| --- | --- | --- |
| `security.client-id` | `myclientid` | Identificador do client OAuth2 que pede tokens |
| `security.client-secret` | `myclientsecret` | Senha do client |
| `security.jwt.duration` | `86400` | Validade do access token, em segundos (24 horas) |
| `cors.origins` | `http://localhost:3000,http://localhost:5173` | Origens de navegador autorizadas a chamar a API |

O que cada uma faz na prática está em [AUTHENTICATION.md](AUTHENTICATION.md#10-client-oauth2-e-cors).

## 5. Variáveis de ambiente

| Variável | Propriedade | Perfil | Padrão | Para que serve |
| --- | --- | --- | --- | --- |
| `POSTGRES_DATASOURCE_USER` | `spring.datasource.username` | `dev` | Sem padrão (obrigatória) | Usuário do PostgreSQL |
| `POSTGRES_DATASOURCE_PASSWORD` | `spring.datasource.password` | `dev` | Sem padrão (obrigatória) | Senha do PostgreSQL |
| `CLIENT_ID` | `security.client-id` | Todos | `myclientid` | Id do client OAuth2 |
| `CLIENT_SECRET` | `security.client-secret` | Todos | `myclientsecret` | Secret do client OAuth2 |
| `JWT_DURATION` | `security.jwt.duration` | Todos | `86400` | Validade do token, em segundos |
| `CORS_ORIGINS` | `cors.origins` | Todos | `http://localhost:3000,http://localhost:5173` | Origens permitidas, separadas por vírgula |

Como definir no PowerShell e no bash: veja [GETTING-STARTED.md](GETTING-STARTED.md#4-caminho-completo-perfil-dev-com-postgresql).

Se uma das variáveis do banco faltar no perfil `dev`, a aplicação não sobe: o Spring usa o texto `${POSTGRES_DATASOURCE_USER}` literalmente como usuário, e o PostgreSQL recusa com `password authentication failed`.

## 6. Arquivos de log

Além do console, cada perfil grava o log num arquivo. Os caminhos são relativos à pasta de onde a aplicação foi iniciada: com `cd backend`, ficam em `backend/logs/`, que o `.gitignore` não versiona (regra `*.log`).

| Perfil | Arquivo | Tamanho máximo por arquivo | Arquivos guardados |
| --- | --- | --- | --- |
| `dev` | `logs/dev/asjcatalog-dev.log` | 10 MB | 10 |
| `test` | `logs/test/asjcatalog-test.log` | 5 MB | 5 |
| `prod` | `logs/asjcatalog-prod.log` | 50 MB | 30 |

Quando o arquivo atinge o tamanho máximo, o Spring Boot (pelo Logback, a biblioteca de log que ele usa) o renomeia e começa outro; essa troca se chama **rotação**.

## 7. Limitações conhecidas

- **O perfil `prod` está incompleto.** O `application-prod.properties` só configura logs: não define banco, Flyway, Swagger nem segredos do client. Um perfil `prod` com banco próprio e com `CLIENT_ID` e `CLIENT_SECRET` obrigatórios chega no capítulo 04.
- **Perfil fixo no arquivo.** Não há variável de ambiente para escolher o perfil; a escolha por `APP_PROFILE` chega no capítulo 04.
- **Segredos com valor padrão.** Sem `CLIENT_ID` e `CLIENT_SECRET`, qualquer perfil usa `myclientid` e `myclientsecret`, que estão no próprio arquivo.
- **Arquivo `application-test.properties` sem configuração de codificação para o `import.sql`.** Os acentos dos dados de exemplo aparecem corrompidos no perfil `test`; veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#8-limitações-conhecidas). Corrigido no capítulo 04.
- **Descrições genéricas no `additional-spring-configuration-metadata.json`.** As quatro propriedades têm descrições como `A description for 'security.client-id'`.
