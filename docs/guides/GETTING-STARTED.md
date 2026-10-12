# Primeiros passos

[🏠 Índice](../HOME.md) · Próximo: [Configuração e perfis](CONFIGURATION.md) ➡️

Este guia leva você do zero até a primeira requisição autenticada na API do ASJCatalog, rodando no seu computador.

## Sumário

1. [Pré-requisitos](#1-pré-requisitos)
   - [Baixar o repositório](#baixar-o-repositório)
2. [Criar o banco de dados](#2-criar-o-banco-de-dados)
3. [Definir as variáveis obrigatórias](#3-definir-as-variáveis-obrigatórias)
4. [Subir a aplicação](#4-subir-a-aplicação)
5. [Servidor de e-mail (SMTP)](#5-servidor-de-e-mail-smtp)
6. [Documentação interativa (Swagger)](#6-documentação-interativa-swagger)
7. [Usuários de exemplo](#7-usuários-de-exemplo)
8. [Primeira requisição autenticada](#8-primeira-requisição-autenticada)
9. [Problemas comuns](#9-problemas-comuns)
10. [Limitações conhecidas](#10-limitações-conhecidas)

## 1. Pré-requisitos

| Ferramenta | Para quê | Como conferir |
| --- | --- | --- |
| JDK 17 | Compilar e rodar a aplicação | `java -version` deve mostrar a versão 17 |
| PostgreSQL | Banco de dados do perfil `dev` | O serviço deve estar rodando em `localhost:5432` |
| Git | Baixar o repositório | `git --version` |
| curl | Fazer requisições pelo terminal | No Windows 10 ou superior, `curl.exe --version` |

Não é preciso instalar o Maven. O projeto traz o **Maven Wrapper** (`mvnw` e `mvnw.cmd`, dentro de `backend/`), um script que baixa a versão certa do Maven na primeira execução.

### Baixar o repositório

Clone o repositório e entre na branch do capítulo (os comandos são iguais no PowerShell e no bash):

```powershell
git clone https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot
git checkout chapter-04-domain-orm
```

**Os comandos deste guia partem da raiz do repositório**, a pasta `backend-engineering-journey-java-springboot` criada pelo `git clone`. O código da aplicação fica na subpasta `backend`, e é nela que o comando `cd backend` entra na [seção 4](#4-subir-a-aplicação).

## 2. Criar o banco de dados

A aplicação espera um banco chamado `asjcatalog` no PostgreSQL local. O endereço está fixo em `backend/src/main/resources/application-dev.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/asjcatalog
```

Crie o banco uma única vez, pelo `psql` ou pelo pgAdmin. Com o `psql` (o comando é igual no PowerShell e no bash):

```powershell
psql -U postgres -c "CREATE DATABASE asjcatalog;"
```

Não é preciso criar tabelas. Na primeira subida, o **Flyway** (ferramenta que aplica scripts SQL versionados, chamados *migrations*) cria as tabelas e insere os dados de exemplo. Os detalhes estão em [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md).

## 3. Definir as variáveis obrigatórias

Uma **variável de ambiente** é um valor definido no sistema operacional ou no terminal que a aplicação lê ao iniciar. Ela serve para manter senhas fora do código.

No perfil `dev` (o perfil padrão, explicado em [CONFIGURATION.md](CONFIGURATION.md)), duas variáveis não têm valor padrão e precisam ser definidas:

| Variável | Conteúdo |
| --- | --- |
| `POSTGRES_DATASOURCE_USER` | Usuário do PostgreSQL |
| `POSTGRES_DATASOURCE_PASSWORD` | Senha desse usuário |

**PowerShell**, válido só para o terminal atual:

```powershell
$env:POSTGRES_DATASOURCE_USER = 'postgres'
$env:POSTGRES_DATASOURCE_PASSWORD = 'sua_senha'
```

**PowerShell**, gravado para o seu usuário do Windows (vale nos terminais abertos depois do comando):

```powershell
[Environment]::SetEnvironmentVariable('POSTGRES_DATASOURCE_USER', 'postgres', 'User')
[Environment]::SetEnvironmentVariable('POSTGRES_DATASOURCE_PASSWORD', 'sua_senha', 'User')
```

**bash**:

```bash
export POSTGRES_DATASOURCE_USER=postgres
export POSTGRES_DATASOURCE_PASSWORD=sua_senha
```

As demais variáveis têm valores padrão para desenvolvimento. A lista completa está em [CONFIGURATION.md](CONFIGURATION.md#4-variáveis-de-ambiente).

## 4. Subir a aplicação

> **Antes de subir:** ao iniciar, a aplicação testa a conexão com o servidor de e-mail (SMTP). **Sem credenciais de e-mail válidas, a subida falha.** Escolha abaixo o comando de acordo com o seu caso. A explicação completa está na [seção 5](#5-servidor-de-e-mail-smtp).

Os comandos partem da raiz do repositório; `cd backend` entra na pasta do código.

**Sem credenciais de e-mail** (o caso mais comum em desenvolvimento): suba desligando o teste de conexão.

**PowerShell**:

```powershell
cd backend
.\mvnw spring-boot:run '-Dspring-boot.run.arguments=--spring.mail.test-connection=false'
```

**bash**:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.mail.test-connection=false
```

Nesse modo, os e-mails de ativação e de recuperação de senha não são enviados (veja o aviso na [seção 5](#5-servidor-de-e-mail-smtp)).

**Com credenciais de e-mail:** defina `MAIL_USERNAME` e `MAIL_PASSWORD` (e, se não usar o Gmail, `MAIL_HOST` e `MAIL_PORT`) e suba normalmente.

**PowerShell**:

```powershell
$env:MAIL_USERNAME = 'seu_email@gmail.com'
$env:MAIL_PASSWORD = 'sua_senha_smtp'
cd backend
.\mvnw spring-boot:run
```

**bash**:

```bash
export MAIL_USERNAME=seu_email@gmail.com
export MAIL_PASSWORD=sua_senha_smtp
cd backend
./mvnw spring-boot:run
```

Nos dois casos, a aplicação sobe na porta 8080 e está pronta quando o log mostra uma linha como esta:

```text
Started AsjcatalogApplication in 8.6 seconds (process running for 8.982)
```

## 5. Servidor de e-mail (SMTP)

**SMTP** é o protocolo usado para enviar e-mails. A aplicação envia e-mails de ativação de conta e de recuperação de senha.

O arquivo `application.properties`, que vale para todos os perfis, tem:

```properties
spring.mail.test-connection=true
```

Com isso, **a subida testa a conexão com o servidor de e-mail** (por padrão `smtp.gmail.com`, porta 587, com as credenciais `MAIL_USERNAME` e `MAIL_PASSWORD`). A exceção é o perfil `test`, usado pelos testes automatizados: o `application-test.properties` desliga esse teste, e por isso **os testes (`./mvnw verify`) não precisam de credenciais de e-mail**. Veja [TESTING.md](TESTING.md#8-dependências-externas).

Ao subir a aplicação no perfil `dev`, sem credenciais válidas, ela não sobe e o log mostra:

```text
Caused by: java.lang.IllegalStateException: Mail server is not available
Caused by: jakarta.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted.
```

Há dois caminhos, e os dois comandos estão na [seção 4](#4-subir-a-aplicação):

- **Com credenciais válidas:** defina `MAIL_USERNAME` e `MAIL_PASSWORD` (e, se não usar o Gmail, `MAIL_HOST` e `MAIL_PORT`). Para gravá-las de forma permanente, use os mesmos comandos da [seção 3](#3-definir-as-variáveis-obrigatórias).
- **Sem credenciais:** desligue o teste passando `--spring.mail.test-connection=false` na linha de comando.

O parâmetro `-Dspring-boot.run.arguments` repassa argumentos para a aplicação. No PowerShell, o argumento vai entre aspas simples por causa do ponto no nome. Para passar mais de um argumento, separe-os por espaço dentro das mesmas aspas:

```powershell
.\mvnw spring-boot:run '-Dspring-boot.run.arguments=--spring.mail.test-connection=false --server.port=8081'
```

> **Atenção:** ao subir sem credenciais válidas, **os e-mails de ativação e de recuperação de senha não são enviados**. As requisições de cadastro e de recuperação respondem normalmente; a falha aparece só no log (`Erro ao enviar email de ativação para ...`). O token gerado fica gravado na tabela `tb_token`, e a conta cadastrada continua inativa. Os fluxos de conta, e como ativar uma conta nesse modo, estão em [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#8-testar-os-fluxos-sem-servidor-de-e-mail).

## 6. Documentação interativa (Swagger)

Com a aplicação no ar, abra no navegador:

```text
http://localhost:8080/docs-asjcatalog.html
```

O endereço redireciona para `/swagger-ui/index.html`, que lista todos os endpoints e permite testá-los pelo navegador. A especificação OpenAPI em JSON fica em `/docs-asjcatalog`.

Para chamar endpoints protegidos pelo Swagger, obtenha um token (seção 8), clique em **Authorize** e cole o valor do `access_token`.

O Swagger existe nos perfis `dev` e `test` e é desativado no perfil `prod`.

## 7. Usuários de exemplo

As migrations de dados do perfil `dev` criam dois usuários:

| E-mail | Senha | Roles |
| --- | --- | --- |
| `albert@gmail.com` | `123456` | `ROLE_OPERATOR` |
| `maria@gmail.com` | `123456` | `ROLE_OPERATOR`, `ROLE_ADMIN` |

Uma **role** é um papel que define o que o usuário pode fazer. `ROLE_ADMIN` tem acesso administrativo aos usuários; `ROLE_OPERATOR` gerencia o catálogo.

> **Aviso:** esses usuários existem apenas para desenvolvimento e testes (pasta `db/migration/data` e arquivo `import.sql`). O perfil `prod` não executa esses scripts, então esses usuários não existem em produção. Como a senha é pública, nunca use esses dados em um ambiente real.

## 8. Primeira requisição autenticada

A API usa **OAuth2**, um padrão de autorização. Primeiro você troca e-mail e senha por um **token** (um texto assinado que prova quem você é) e depois envia esse token em cada requisição protegida.

O pedido de token vai para `POST /oauth2/token` e precisa de duas identificações:

- a do **cliente** (a aplicação que está pedindo), enviada por *HTTP Basic* com `-u`. Em desenvolvimento, os valores padrão são `myclientid` e `myclientsecret`;
- a do **usuário**, enviada no corpo com `grant_type=password`, `username` e `password`.

**PowerShell** (use `curl.exe`: no Windows PowerShell, `curl` sem `.exe` é um apelido de outro comando):

```powershell
$resp = curl.exe -s -X POST http://localhost:8080/oauth2/token -u 'myclientid:myclientsecret' -d 'grant_type=password' -d 'username=maria@gmail.com' -d 'password=123456'
$token = ($resp | ConvertFrom-Json).access_token
curl.exe -s http://localhost:8080/api/v1/accounts/me -H "Authorization: Bearer $token"
```

**bash**: rode o primeiro comando, copie o valor de `access_token` da resposta e use-o no segundo:

```bash
curl -s -X POST http://localhost:8080/oauth2/token -u myclientid:myclientsecret -d grant_type=password -d username=maria@gmail.com -d password=123456
TOKEN='cole_aqui_o_access_token'
curl -s http://localhost:8080/api/v1/accounts/me -H "Authorization: Bearer $TOKEN"
```

A resposta do token traz os campos `access_token`, `refresh_token`, `token_type` (`Bearer`) e `expires_in` (86400 segundos, ou seja, 24 horas). A chamada a `/api/v1/accounts/me` devolve o usuário dono do token:

```json
{"id":2,"firstName":"Maria","lastName":"Green","email":"maria@gmail.com","roles":[{"id":1,"authority":"ROLE_OPERATOR"},{"id":2,"authority":"ROLE_ADMIN"}]}
```

As listagens de produtos e de categorias são públicas e não exigem token:

```powershell
curl.exe -s "http://localhost:8080/api/v1/products?size=2"
```

A renovação do token e as regras de acesso de cada endpoint são explicadas em [AUTHENTICATION.md](AUTHENTICATION.md).

## 9. Problemas comuns

| Mensagem no log | Causa | Solução |
| --- | --- | --- |
| `Web server failed to start. Port 8080 was already in use.` | Outro processo, muitas vezes outra instância da própria aplicação, já usa a porta 8080 | Encerre o outro processo ou suba em outra porta com `'-Dspring-boot.run.arguments=--server.port=8081'`. No PowerShell, `Get-NetTCPConnection -LocalPort 8080` mostra o processo (`OwningProcess`) |
| `FATAL: database "asjcatalog" does not exist` | O banco não foi criado | Crie o banco ([seção 2](#2-criar-o-banco-de-dados)) |
| `FATAL: password authentication failed for user "${POSTGRES_DATASOURCE_USER}"` | A variável não está definida no terminal onde a aplicação foi iniciada. O Spring envia o texto `${...}` literalmente como nome de usuário | Defina as variáveis ([seção 3](#3-definir-as-variáveis-obrigatórias)) no mesmo terminal, ou abra um terminal novo se usou `SetEnvironmentVariable` |
| `Mail server is not available` e `AuthenticationFailedException: 535` | Credenciais de e-mail ausentes ou inválidas | Veja a [seção 5](#5-servidor-de-e-mail-smtp) |

## 10. Limitações conhecidas

- **Falha no envio de e-mail não chega ao cliente da API.** Este item trata só do caso em que o envio **falha**, por exemplo com o servidor SMTP fora do ar ou com credenciais inválidas; quando o envio funciona, o e-mail chega normalmente. Nesse caso de falha, o cadastro e a recuperação de senha respondem com sucesso mesmo assim, e o erro fica só no log.
  - **Na recuperação de senha**, responder sucesso é intencional: o endpoint sempre responde da mesma forma para não revelar se o e-mail está cadastrado.
  - **No cadastro**, a conta é criada inativa mesmo sem o e-mail. Depois que o envio voltar a funcionar, a pessoa pode pedir outro e-mail em `POST /api/v1/accounts/resend-activation` (veja [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md#3-reenvio-de-ativação)).
- **A variável ausente não é apontada pelo nome.** No perfil `dev`, esquecer `POSTGRES_DATASOURCE_USER` ou `POSTGRES_DATASOURCE_PASSWORD` não gera uma mensagem de "variável ausente": o erro aparece como falha de autenticação no PostgreSQL (veja a seção 9).

---

[🏠 Índice](../HOME.md) · Próximo: [Configuração e perfis](CONFIGURATION.md) ➡️
