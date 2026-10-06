# Primeiros passos

Este guia leva você do zero até um login e as primeiras chamadas à API do ASJCatalog no capítulo 03, rodando no seu computador.

## Sumário

1. [Pré-requisitos](#1-pré-requisitos)
2. [Baixar o repositório](#2-baixar-o-repositório)
3. [Caminho rápido: perfil test, sem dependências](#3-caminho-rápido-perfil-test-sem-dependências)
4. [Caminho completo: perfil dev, com PostgreSQL](#4-caminho-completo-perfil-dev-com-postgresql)
5. [Primeiras chamadas públicas](#5-primeiras-chamadas-públicas)
6. [Obter um token e chamar uma rota protegida](#6-obter-um-token-e-chamar-uma-rota-protegida)
7. [Documentação interativa (Swagger)](#7-documentação-interativa-swagger)
8. [Console do H2](#8-console-do-h2)
9. [Rodar os testes](#9-rodar-os-testes)
10. [Problemas comuns](#10-problemas-comuns)
11. [Limitações conhecidas](#11-limitações-conhecidas)

## 1. Pré-requisitos

| Ferramenta | Para quê | Como conferir |
| --- | --- | --- |
| JDK 17 | Compilar e rodar a aplicação e os testes | `java -version` deve mostrar a versão 17 |
| Git | Baixar o repositório | `git --version` |
| curl | Fazer requisições pelo terminal | No Windows 10 ou superior, `curl.exe --version` |
| PostgreSQL | **Só no perfil `dev`**, que é o padrão (seção 4) | O serviço deve estar rodando em `localhost:5432` |
| Acesso à internet | A validação de e-mail consulta o DNS ao criar usuários ([VALIDATION.md](VALIDATION.md#6-validação-de-e-mail)) | — |

Não é preciso instalar o Maven. O projeto traz o **Maven Wrapper** (`mvnw` e `mvnw.cmd`, dentro de `backend/`), um script que baixa a versão certa do Maven na primeira execução.

## 2. Baixar o repositório

Cada capítulo do projeto fica numa branch. Clone o repositório e entre na branch deste capítulo (os comandos são iguais no PowerShell e no bash):

```powershell
git clone https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot
git checkout chapter-03-validation-security
```

Os comandos deste guia partem da raiz do repositório. O código da aplicação fica na subpasta `backend`.

## 3. Caminho rápido: perfil test, sem dependências

Um **perfil** do Spring é um conjunto de configurações para um ambiente (veja [CONFIGURATION.md](CONFIGURATION.md)). Neste capítulo, o perfil padrão é o **`dev`**, que exige PostgreSQL. Para rodar sem instalar nada além do JDK, use o perfil **`test`**, que usa o **H2**, um banco de dados que roda na memória, é criado vazio a cada subida e apagado quando a aplicação para.

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

O parâmetro `-Dspring-boot.run.arguments` repassa argumentos para a aplicação. No PowerShell, ele vai entre aspas simples por causa do ponto no nome.

A aplicação sobe na porta 8080 e está pronta quando o log mostra uma linha como esta:

```text
Started AsjcatalogApplication in 10.836 seconds
```

Ao subir, o Hibernate cria as tabelas a partir das entidades e executa o arquivo `import.sql`, que insere 15 categorias, 25 produtos, 2 usuários e 2 roles de exemplo.

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

As variáveis de segurança (`CLIENT_ID`, `CLIENT_SECRET`, `JWT_DURATION` e `CORS_ORIGINS`) são opcionais: sem elas, valem os padrões descritos em [CONFIGURATION.md](CONFIGURATION.md#5-variáveis-de-ambiente).

**3. Suba a aplicação.** Como o perfil `dev` é o padrão, não é preciso informá-lo:

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

Na primeira subida, o Flyway cria as tabelas e insere os mesmos dados de exemplo do perfil `test`, inclusive os usuários.

## 5. Primeiras chamadas públicas

As listagens e buscas de categorias e produtos (`GET`) não exigem token.

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

No perfil `test`, o JSON das respostas vem formatado com quebras de linha. A lista completa de rotas está em [API-ENDPOINTS.md](API-ENDPOINTS.md).

## 6. Obter um token e chamar uma rota protegida

As demais rotas exigem um **token**: um texto que a API entrega depois do login e que o cliente envia em cada requisição. Os usuários de exemplo, **só para desenvolvimento**, são `albert@gmail.com` (OPERATOR) e `maria@gmail.com` (ADMIN e OPERATOR), os dois com a senha `123456`.

**PowerShell**:

```powershell
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('myclientid:myclientsecret'))
$body = @{ grant_type = 'password'; username = 'maria@gmail.com'; password = '123456' }
$token = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/oauth2/token -Headers @{ Authorization = "Basic $basic" } -Body $body).access_token
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users?size=2" -Headers @{ Authorization = "Bearer $token" }
```

**bash**:

```bash
TOKEN=$(curl -s -u myclientid:myclientsecret -d "grant_type=password&username=maria@gmail.com&password=123456" http://localhost:8080/oauth2/token | sed -E 's/.*"access_token":"([^"]+)".*/\1/')
curl -s "http://localhost:8080/api/v1/users?size=2" -H "Authorization: Bearer $TOKEN"
```

Sem o token, `GET /api/v1/users` responde 401; com o token de `albert@gmail.com`, que não é ADMIN, responde 403. O funcionamento completo do login, do token e das permissões está em [AUTHENTICATION.md](AUTHENTICATION.md).

## 7. Documentação interativa (Swagger)

Com a aplicação no ar (no perfil `test` ou `dev`), abra no navegador:

```text
http://localhost:8080/docs-asjcatalog.html
```

O endereço é público e redireciona para `/swagger-ui/index.html`, que lista os 21 endpoints. Para testar as rotas protegidas pelo navegador, clique em **Authorize** e cole o token obtido na seção 6. A especificação OpenAPI em JSON fica em `/docs-asjcatalog`.

## 8. Console do H2

Só no perfil `test`. O console é uma página web para consultar o banco em memória:

1. Abra `http://localhost:8080/h2-console`.
2. Em **JDBC URL**, use `jdbc:h2:mem:testdb`.
3. Em **User Name**, use `sa`, e deixe a senha vazia.
4. Clique em **Connect**. As tabelas são `TB_CATEGORY`, `TB_PRODUCT`, `TB_PRODUCT_CATEGORY`, `TB_USER`, `TB_ROLE` e `TB_USER_ROLE`.

## 9. Rodar os testes

Como o perfil padrão é `dev`, informe o perfil `test` para rodar a suíte sem PostgreSQL:

**PowerShell**:

```powershell
cd backend
.\mvnw verify '-Dspring.profiles.active=test'
```

**bash**:

```bash
cd backend
./mvnw verify -Dspring.profiles.active=test
```

Resultado esperado: `Tests run: 134, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`. Os 82 testes de integração (classes `*IT`) rodam à parte; como rodá-los e o que cada teste verifica está em [TESTING.md](TESTING.md).

## 10. Problemas comuns

| Mensagem no log | Causa | Solução |
| --- | --- | --- |
| `Web server failed to start. Port 8080 was already in use.` | Outro processo já usa a porta 8080 | Encerre o outro processo ou suba em outra porta, acrescentando `--server.port=8081` aos argumentos |
| `FATAL: database "asjcatalog" does not exist` | O banco não foi criado (perfil `dev`, o padrão) | Crie o banco (seção 4) ou use o perfil `test` (seção 3) |
| `FATAL: password authentication failed for user "${POSTGRES_DATASOURCE_USER}"` | A variável não está definida no terminal onde a aplicação foi iniciada; o Spring envia o texto `${...}` literalmente | Defina as variáveis (seção 4) no mesmo terminal |
| Falha do Flyway ao subir no perfil `dev` | O banco `asjcatalog` foi criado por outro capítulo, com outra estrutura | Recrie o banco vazio (seção 4) |
| Rota protegida responde 401 depois de reiniciar a aplicação | A chave que assina os tokens é recriada a cada subida | Faça login de novo ([AUTHENTICATION.md](AUTHENTICATION.md#5-a-chave-de-assinatura-muda-a-cada-subida)) |

Para vários argumentos, separe-os por espaço dentro das mesmas aspas: `'-Dspring-boot.run.arguments=--spring.profiles.active=test --server.port=8081'`.

## 11. Limitações conhecidas

- **O perfil é fixo no arquivo.** Não há variável de ambiente para escolher o perfil; é preciso passá-lo na linha de comando. A escolha por variável (`APP_PROFILE`) chega no capítulo 04.
- **O `verify` falha no perfil padrão sem PostgreSQL.** Veja [TESTING.md](TESTING.md#1-como-rodar-os-testes).
- **Acentos corrompidos no perfil `test`.** As descrições em português dos produtos aparecem com caracteres trocados. Detalhes em [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#8-limitações-conhecidas).
- **O endereço `/swagger-ui.html` responde 401.** Use `/docs-asjcatalog.html` ou `/swagger-ui/index.html`.
