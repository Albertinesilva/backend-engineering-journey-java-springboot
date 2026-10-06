# Primeiros passos

Este guia leva você do zero até as primeiras chamadas à API do ASJCatalog e à execução dos testes no capítulo 02, rodando no seu computador.

## Sumário

1. [Pré-requisitos](#1-pré-requisitos)
2. [Baixar o repositório](#2-baixar-o-repositório)
3. [Caminho rápido: perfil test, sem dependências](#3-caminho-rápido-perfil-test-sem-dependências)
4. [Caminho completo: perfil dev, com PostgreSQL](#4-caminho-completo-perfil-dev-com-postgresql)
5. [Documentação interativa (Swagger)](#5-documentação-interativa-swagger)
6. [Console do H2](#6-console-do-h2)
7. [Primeiras chamadas](#7-primeiras-chamadas)
8. [Rodar os testes](#8-rodar-os-testes)
9. [Problemas comuns](#9-problemas-comuns)
10. [Limitações conhecidas](#10-limitações-conhecidas)

## 1. Pré-requisitos

| Ferramenta | Para quê | Como conferir |
| --- | --- | --- |
| JDK 17 | Compilar e rodar a aplicação e os testes | `java -version` deve mostrar a versão 17 |
| Git | Baixar o repositório | `git --version` |
| curl | Fazer requisições pelo terminal | No Windows 10 ou superior, `curl.exe --version` |
| PostgreSQL | **Só no perfil `dev`** (seção 4) | O serviço deve estar rodando em `localhost:5432` |

Não é preciso instalar o Maven. O projeto traz o **Maven Wrapper** (`mvnw` e `mvnw.cmd`, dentro de `backend/`), um script que baixa a versão certa do Maven na primeira execução.

## 2. Baixar o repositório

Cada capítulo do projeto fica numa branch. Clone o repositório e entre na branch deste capítulo (os comandos são iguais no PowerShell e no bash):

```powershell
git clone https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot
git checkout chapter-02-tests
```

Os comandos deste guia partem da raiz do repositório. O código da aplicação fica na subpasta `backend`.

## 3. Caminho rápido: perfil test, sem dependências

Um **perfil** do Spring é um conjunto de configurações para um ambiente (veja [CONFIGURATION.md](CONFIGURATION.md)). Neste capítulo, o perfil padrão é o `test`, definido no `application.properties` (`spring.profiles.active=test`). Ele usa o **H2**, um banco de dados que roda na memória, é criado vazio a cada subida e apagado quando a aplicação para. Por isso não é preciso instalar nada além do JDK, nem informar o perfil.

**PowerShell**:

```powershell
cd backend
.\mvnw spring-boot:run
```

**bash**:

```bash
cd backend
./mvnw spring-boot:run
```

A aplicação sobe na porta 8080 e está pronta quando o log mostra uma linha como esta:

```text
Started AsjcatalogApplication in 10.774 seconds
```

Ao subir, o Hibernate cria as tabelas a partir das entidades e executa o arquivo `import.sql`, que insere 15 categorias e 25 produtos de exemplo.

## 4. Caminho completo: perfil dev, com PostgreSQL

O perfil `dev` usa um banco PostgreSQL persistente, e as tabelas são criadas pelo Flyway (veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md)).

**1. Crie o banco.** O endereço está fixo em `backend/src/main/resources/application-dev.properties` (`jdbc:postgresql://localhost:5432/asjcatalog`). Com o `psql` (o comando é igual no PowerShell e no bash):

```powershell
psql -U postgres -c "CREATE DATABASE asjcatalog;"
```

> **Atenção:** o banco `asjcatalog` tem o **mesmo nome** nos outros capítulos, mas a **estrutura é diferente** em cada um. Ao alternar entre branches, recrie o banco vazio antes de subir no perfil `dev`:
>
> ```powershell
> psql -U postgres -c "DROP DATABASE asjcatalog;"
> psql -U postgres -c "CREATE DATABASE asjcatalog;"
> ```
>
> Isso apaga todos os dados do banco. Pare a aplicação antes: o PostgreSQL não apaga um banco com conexões abertas.

**2. Defina as variáveis de ambiente.** Uma **variável de ambiente** é um valor definido no terminal que a aplicação lê ao iniciar; aqui ela guarda o usuário e a senha do banco fora do código.

**PowerShell** (vale só para o terminal atual):

```powershell
$env:POSTGRES_DATASOURCE_USER = 'postgres'
$env:POSTGRES_DATASOURCE_PASSWORD = 'sua_senha'
```

**bash**:

```bash
export POSTGRES_DATASOURCE_USER=postgres
export POSTGRES_DATASOURCE_PASSWORD=sua_senha
```

**3. Suba a aplicação no perfil `dev`.** Como o padrão é `test`, o perfil `dev` precisa ser informado:

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

O parâmetro `-Dspring-boot.run.arguments` repassa argumentos para a aplicação. No PowerShell, ele vai entre aspas simples por causa do ponto no nome.

Na primeira subida, o Flyway cria as tabelas e insere os mesmos dados de exemplo do perfil `test`.

## 5. Documentação interativa (Swagger)

Com a aplicação no ar (no perfil `test` ou `dev`), abra no navegador:

```text
http://localhost:8080/docs-asjcatalog.html
```

O endereço redireciona para `/swagger-ui/index.html`, que lista os 14 endpoints e permite testá-los pelo navegador. A especificação OpenAPI em JSON fica em `/docs-asjcatalog`.

## 6. Console do H2

Só no perfil `test`. O console é uma página web para consultar o banco em memória:

1. Abra `http://localhost:8080/h2-console`.
2. Em **JDBC URL**, use `jdbc:h2:mem:testdb`.
3. Em **User Name**, use `sa`, e deixe a senha vazia.
4. Clique em **Connect**. As tabelas são `TB_CATEGORY`, `TB_PRODUCT` e `TB_PRODUCT_CATEGORY`.

## 7. Primeiras chamadas

Todas as rotas são públicas: este capítulo não tem autenticação.

**PowerShell** (use `curl.exe`: no Windows PowerShell, `curl` sem `.exe` é um apelido de outro comando):

```powershell
curl.exe -s "http://localhost:8080/api/v1/categories?size=3"
curl.exe -s http://localhost:8080/api/v1/products/1
```

**bash**:

```bash
curl -s "http://localhost:8080/api/v1/categories?size=3"
curl -s http://localhost:8080/api/v1/products/1
```

Resposta real de `GET /api/v1/categories/2`:

```json
{ "id" : 2, "name" : "Electronics", "description" : "Electronic devices and gadgets", "active" : true }
```

No perfil `test`, o JSON das respostas vem formatado com quebras de linha. A lista completa de rotas, com exemplos, está em [API-ENDPOINTS.md](API-ENDPOINTS.md).

## 8. Rodar os testes

Como o perfil padrão é `test`, a suíte de testes também não precisa do PostgreSQL:

**PowerShell**:

```powershell
cd backend
.\mvnw clean verify
```

**bash**:

```bash
cd backend
./mvnw clean verify
```

Resultado esperado: `Tests run: 75, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`. Os testes de integração (classes `*IT`) rodam à parte. Como rodá-los, rodar uma classe ou um método e o que cada teste verifica está em [TESTING.md](TESTING.md).

## 9. Problemas comuns

| Mensagem no log | Causa | Solução |
| --- | --- | --- |
| `Web server failed to start. Port 8080 was already in use.` | Outro processo já usa a porta 8080 | Encerre o outro processo ou suba em outra porta, acrescentando `--server.port=8081` aos argumentos |
| `FATAL: database "asjcatalog" does not exist` | O banco não foi criado (perfil `dev`) | Crie o banco (seção 4) ou use o perfil `test` (seção 3) |
| `FATAL: password authentication failed for user "${POSTGRES_DATASOURCE_USER}"` | A variável não está definida no terminal onde a aplicação foi iniciada; o Spring envia o texto `${...}` literalmente | Defina as variáveis (seção 4) no mesmo terminal |
| Falha do Flyway ao subir no perfil `dev` | O banco `asjcatalog` foi criado por outro capítulo, com outra estrutura | Recrie o banco vazio (seção 4) |

Para trocar a porta no perfil `test`: `'-Dspring-boot.run.arguments=--server.port=8081'`. Para vários argumentos, separe-os por espaço dentro das mesmas aspas: `'-Dspring-boot.run.arguments=--spring.profiles.active=dev --server.port=8081'`.

## 10. Limitações conhecidas

- **O perfil é fixo no arquivo.** Não há variável de ambiente para escolher o perfil; é preciso passá-lo na linha de comando. A escolha por variável (`APP_PROFILE`) chega no capítulo 04, com `dev` como padrão.
- **Acentos corrompidos no perfil `test`.** As descrições em português dos produtos aparecem com caracteres trocados (`ClÃ¡ssico` em vez de `Clássico`). Detalhes em [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#8-limitações-conhecidas).
- **O endereço `/swagger-ui.html` responde 500.** Use `/docs-asjcatalog.html` ou `/swagger-ui/index.html`.
- **Os testes de integração não rodam no `verify`.** Veja [TESTING.md](TESTING.md#13-limitações-conhecidas).
