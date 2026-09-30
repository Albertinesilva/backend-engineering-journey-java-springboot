# Configuração e perfis

Este guia explica como a aplicação é configurada no capítulo 01: os arquivos de configuração, os perfis `dev`, `test` e `prod` e as variáveis de ambiente.

## Sumário

1. [O que é um perfil](#1-o-que-é-um-perfil)
2. [Como o perfil é escolhido](#2-como-o-perfil-é-escolhido)
3. [Comparação entre os perfis](#3-comparação-entre-os-perfis)
4. [Variáveis de ambiente](#4-variáveis-de-ambiente)
5. [Limitações conhecidas](#5-limitações-conhecidas)

## 1. O que é um perfil

Um **perfil** do Spring é um conjunto de configurações que só vale em um ambiente. O projeto tem quatro arquivos em `backend/src/main/resources`:

| Arquivo | Quando vale |
| --- | --- |
| `application.properties` | Sempre, em todos os perfis |
| `application-dev.properties` | Só no perfil `dev` (desenvolvimento, com PostgreSQL) |
| `application-test.properties` | Só no perfil `test` (banco H2 em memória) |
| `application-prod.properties` | Só no perfil `prod` |

O Spring lê primeiro o `application.properties` e depois o arquivo do perfil ativo. Quando a mesma propriedade aparece nos dois, vale o valor do arquivo do perfil.

O `application.properties` tem só duas linhas:

```properties
spring.profiles.active=dev
spring.jpa.open-in-view=false
```

- `spring.profiles.active=dev` define o perfil padrão.
- `spring.jpa.open-in-view=false` desliga o *Open Session in View*, recurso que manteria a conexão com o banco aberta até o fim da requisição. Explicado em [DATA-ACCESS.md](DATA-ACCESS.md#5-transações-e-open-in-view).

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

O perfil ativo aparece no início do log (`The following 1 profile is active: "test"`) e no banner dos perfis `dev` e `test` (`Environment: test`).

## 3. Comparação entre os perfis

| Aspecto | `dev` | `test` | `prod` |
| --- | --- | --- | --- |
| Banco | PostgreSQL em `localhost:5432/asjcatalog` | H2 em memória (`jdbc:h2:mem:testdb`, usuário `sa`, sem senha) | Não configurado (veja Limitações conhecidas) |
| Credenciais do banco | `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD` | Fixas no arquivo | — |
| Flyway | Ligado: pastas `schema` e `data` | Desligado | Não configurado |
| Quem cria as tabelas | Flyway | Hibernate, e depois o `import.sql` insere os dados | — |
| `ddl-auto` | `none` | Não definido (padrão do Spring Boot para banco em memória) | Não definido |
| SQL no console (`show-sql`) | Sim, formatado | Sim, formatado | Não definido |
| Console do H2 | Não configurado | Ligado em `/h2-console` | Não configurado |
| Swagger | `/docs-asjcatalog.html` | `/docs-asjcatalog.html` | Não configurado |
| Nível de log da aplicação | `DEBUG` | `DEBUG` | `INFO` |
| Arquivo de log | `logs/dev/asjcatalog-dev.log` (até 10 MB por arquivo) | `logs/test/asjcatalog-test.log` (até 5 MB) | `logs/asjcatalog-prod.log` (até 50 MB) |
| Banner | `banner-dev.txt` | `banner-dev.txt` | Padrão do Spring Boot |
| Porta | 8080 | 8080 | 8080 (padrão; não está no arquivo) |
| Nome da aplicação | `ASJCatalog` | `ASJCatalog` | Não definido |

Termos da tabela:

- **H2** é um banco de dados escrito em Java que pode rodar só na memória.
- **Flyway** aplica scripts SQL versionados (*migrations*). Veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md).
- **`ddl-auto`** diz ao Hibernate o que fazer com as tabelas ao subir: `none` não faz nada. No perfil `test`, sem valor definido e com o Flyway desligado, o Spring Boot usa o padrão para banco em memória, que cria as tabelas a partir das entidades.
- Os **caminhos de log** são relativos à pasta de onde a aplicação foi iniciada; com `cd backend`, ficam em `backend/logs/`.

Outras configurações por perfil:

- **`dev`:** `spring.web.locale-resolver=fixed` e `spring.web.locale=pt_BR` (idioma fixo em português do Brasil) e codificação UTF-8 nas requisições e respostas (`server.servlet.encoding.*`).
- **`test`:** o JSON das respostas vem indentado (`spring.jackson.serialization.indent_output=true`). O arquivo também traz, comentadas, as propriedades que geram o `create.sql` a partir das entidades.

## 4. Variáveis de ambiente

| Variável | Propriedade | Perfil | Padrão | Para que serve |
| --- | --- | --- | --- | --- |
| `POSTGRES_DATASOURCE_USER` | `spring.datasource.username` | `dev` | Sem padrão (obrigatória) | Usuário do PostgreSQL |
| `POSTGRES_DATASOURCE_PASSWORD` | `spring.datasource.password` | `dev` | Sem padrão (obrigatória) | Senha do PostgreSQL |

Como definir no PowerShell e no bash: veja [GETTING-STARTED.md](GETTING-STARTED.md#4-caminho-completo-perfil-dev-com-postgresql).

Se uma delas faltar, a aplicação não sobe: o Spring usa o texto `${POSTGRES_DATASOURCE_USER}` literalmente como usuário, e o PostgreSQL recusa com `password authentication failed`.

## 5. Limitações conhecidas

- **O perfil `prod` está incompleto.** O `application-prod.properties` só configura logs. Ao subir com `--spring.profiles.active=prod` (confirmado por execução com `spring-boot:run`):
  - sem banco configurado, o Spring Boot cria um banco H2 em memória, porque o H2 está no classpath, e os dados somem ao parar a aplicação;
  - o Flyway roda com o local padrão (`db/migration`) e aplica as pastas `schema` e `data`, inclusive os dados de exemplo;
  - o Swagger fica nos caminhos padrão (`/swagger-ui/index.html` e `/v3/api-docs`);
  - o console do H2 (`/h2-console`) também respondeu.

  Um perfil `prod` com banco próprio e segredos obrigatórios chega no capítulo 04.
- **Perfil fixo no arquivo.** Não há variável de ambiente para escolher o perfil; a escolha por `APP_PROFILE` chega no capítulo 04.
- **Arquivo `application-test.properties` sem configuração de codificação para o `import.sql`.** Os acentos dos dados de exemplo aparecem corrompidos no perfil `test`; veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#8-limitações-conhecidas). Corrigido no capítulo 04.
