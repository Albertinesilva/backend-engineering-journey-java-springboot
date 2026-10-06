# Configuração e perfis

Este guia explica como a aplicação é configurada no capítulo 02: os arquivos de configuração, os perfis `test`, `dev` e `prod`, os arquivos de log e as variáveis de ambiente.

## Sumário

1. [O que é um perfil](#1-o-que-é-um-perfil)
2. [Como o perfil é escolhido](#2-como-o-perfil-é-escolhido)
3. [Comparação entre os perfis](#3-comparação-entre-os-perfis)
4. [Arquivos de log](#4-arquivos-de-log)
5. [Variáveis de ambiente](#5-variáveis-de-ambiente)
6. [Limitações conhecidas](#6-limitações-conhecidas)

## 1. O que é um perfil

Um **perfil** do Spring é um conjunto de configurações que só vale em um ambiente. O projeto tem quatro arquivos em `backend/src/main/resources`:

| Arquivo | Quando vale |
| --- | --- |
| `application.properties` | Sempre, em todos os perfis |
| `application-test.properties` | Só no perfil `test` (banco H2 em memória) |
| `application-dev.properties` | Só no perfil `dev` (desenvolvimento, com PostgreSQL) |
| `application-prod.properties` | Só no perfil `prod` |

O Spring lê primeiro o `application.properties` e depois o arquivo do perfil ativo. Quando a mesma propriedade aparece nos dois, vale o valor do arquivo do perfil.

O `application.properties` tem só duas propriedades:

```properties
spring.profiles.active=test

spring.jpa.open-in-view=false
```

- `spring.profiles.active=test` define o perfil padrão. No capítulo 01 o padrão era `dev`; neste capítulo passou a ser `test`, e por isso a aplicação e os testes rodam sem PostgreSQL.
- `spring.jpa.open-in-view=false` desliga o *Open Session in View*, recurso que manteria a conexão com o banco aberta até o fim da requisição. Explicado em [DATA-ACCESS.md](DATA-ACCESS.md#5-transações-e-open-in-view).

Não existe `src/test/resources`: os testes usam esses mesmos arquivos e, portanto, o perfil `test`. Veja [TESTING.md](TESTING.md#3-mapa-dos-pacotes-de-teste).

## 2. Como o perfil é escolhido

O perfil padrão é `test`, escrito diretamente no arquivo. Para usar outro, passe o perfil como argumento na linha de comando, que tem prioridade sobre o arquivo.

**PowerShell**:

```powershell
cd backend
.\mvnw spring-boot:run '-Dspring-boot.run.arguments=--spring.profiles.active=dev'
```

**bash**:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

Com o `.jar` gerado pelo build (`backend/target/asjcatalog-0.0.1-SNAPSHOT.jar`), o argumento vai direto no comando (igual no PowerShell e no bash):

```powershell
java -jar target/asjcatalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

O perfil ativo aparece no início do log (`The following 1 profile is active: "test"`) e no banner dos perfis `test` e `dev` (`Environment: test`).

## 3. Comparação entre os perfis

| Aspecto | `test` (padrão) | `dev` | `prod` |
| --- | --- | --- | --- |
| Banco | H2 em memória (`jdbc:h2:mem:testdb`, usuário `sa`, sem senha) | PostgreSQL em `localhost:5432/asjcatalog` | Não configurado (veja Limitações conhecidas) |
| Credenciais do banco | Fixas no arquivo | `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` | — |
| Flyway | Desligado | Ligado: pastas `schema` e `data` | Não configurado |
| Quem cria as tabelas | Hibernate, e depois o `import.sql` insere os dados | Flyway | — |
| `ddl-auto` | Não definido (padrão do Spring Boot para banco em memória) | `none` | Não definido |
| SQL no console (`show-sql`) | Sim, formatado | Sim, formatado | Não definido |
| Console do H2 | Ligado em `/h2-console` | Não configurado | Não configurado |
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

- **`test`:** o JSON das respostas vem indentado (`spring.jackson.serialization.indent_output=true`), e os logs das requisições HTTP (`org.springframework.web`) ficam em `DEBUG`. O arquivo também traz, comentadas, as propriedades que geram o `create.sql` a partir das entidades.
- **`dev`:** `spring.web.locale-resolver=fixed` e `spring.web.locale=pt_BR` (idioma fixo em português do Brasil) e codificação UTF-8 nas requisições e respostas (`server.servlet.encoding.*`).

## 4. Arquivos de log

Além do console, cada perfil grava o log num arquivo. Os caminhos são relativos à pasta de onde a aplicação foi iniciada: com `cd backend`, ficam em `backend/logs/`, que o `.gitignore` não versiona (regra `*.log`).

| Perfil | Arquivo | Tamanho máximo por arquivo | Arquivos guardados |
| --- | --- | --- | --- |
| `test` | `logs/test/asjcatalog-test.log` | 5 MB | 5 |
| `dev` | `logs/dev/asjcatalog-dev.log` | 10 MB | 10 |
| `prod` | `logs/asjcatalog-prod.log` | 50 MB | 30 |

Quando o arquivo atinge o tamanho máximo, o Spring Boot (pelo Logback, a biblioteca de log que ele usa) o renomeia e começa outro; essa troca se chama **rotação**. As colunas de tamanho e de quantidade vêm das propriedades `logging.logback.rollingpolicy.max-file-size` e `max-history`.

Como os testes rodam no perfil `test`, o `./mvnw verify` também escreve em `backend/logs/test/asjcatalog-test.log`.

## 5. Variáveis de ambiente

| Variável | Propriedade | Perfil | Padrão | Para que serve |
| --- | --- | --- | --- | --- |
| `POSTGRES_DATASOURCE_USER` | `spring.datasource.username` | `dev` | Sem padrão (obrigatória) | Usuário do PostgreSQL |
| `POSTGRES_DATASOURCE_PASSWORD` | `spring.datasource.password` | `dev` | Sem padrão (obrigatória) | Senha do PostgreSQL |

Como definir no PowerShell e no bash: veja [GETTING-STARTED.md](GETTING-STARTED.md#4-caminho-completo-perfil-dev-com-postgresql).

Se uma delas faltar no perfil `dev`, a aplicação não sobe: o Spring usa o texto `${POSTGRES_DATASOURCE_USER}` literalmente como usuário, e o PostgreSQL recusa com `password authentication failed`.

## 6. Limitações conhecidas

- **O perfil `prod` está incompleto.** O `application-prod.properties` só configura logs. Ao subir com `java -jar target/asjcatalog-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod` (confirmado por execução):
  - sem banco configurado, o Spring Boot cria um banco H2 em memória, porque o H2 está no classpath, e os dados somem ao parar a aplicação;
  - o Flyway roda com o local padrão (`db/migration`) e aplica as pastas `schema` e `data`, inclusive os dados de exemplo: `GET /api/v1/products` devolveu os 25 produtos;
  - o Swagger fica nos caminhos padrão do springdoc (`/swagger-ui/index.html` e `/v3/api-docs`, ambos 200), e `/docs-asjcatalog.html` responde 500;
  - o console do H2 (`/h2-console`) respondeu 500.

  Um perfil `prod` com banco próprio e segredos obrigatórios chega no capítulo 04.
- **Perfil fixo no arquivo.** Não há variável de ambiente para escolher o perfil; a escolha por `APP_PROFILE` chega no capítulo 04.
- **Arquivo `application-test.properties` sem configuração de codificação para o `import.sql`.** Os acentos dos dados de exemplo aparecem corrompidos no perfil `test`; veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#8-limitações-conhecidas). Corrigido no capítulo 04.
- **Comentário desatualizado no `application-test.properties`.** O comentário do banner diz "ambiente de desenvolvimento", mas está no arquivo do perfil `test`; o mesmo vale para o comentário de `server.port`, que fala em "ambiente de testes" no `application-dev.properties`.
