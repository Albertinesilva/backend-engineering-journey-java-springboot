<h1 align="center">🛡️Capítulo 03 — Validation & Security with Spring Boot | OAuth2 | JWT</h1>

<p align="justify">
<em>
This chapter focuses on building secure, production-ready backend APIs using <strong>Spring Security</strong>, <strong>OAuth2</strong>, <strong>JWT</strong>, and <strong>Bean Validation</strong>, applying modern authentication, authorization, and data validation strategies aligned with real-world enterprise applications.
</em>
</p>

<p align="center">

<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&logoColor=white" />

<img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

<img src="https://img.shields.io/badge/Security-Spring_Security-red?style=for-the-badge" />

<img src="https://img.shields.io/badge/Auth-OAuth2%20%7C%20JWT-critical?style=for-the-badge" />

<img src="https://img.shields.io/badge/Validation-Bean_Validation-blue?style=for-the-badge" />

<img src="https://img.shields.io/badge/Authorization-RBAC-success?style=for-the-badge" />

<img src="https://img.shields.io/badge/API_Security-Resource_Server-informational?style=for-the-badge" />

<img src="https://img.shields.io/badge/Architecture-Authorization_Server-black?style=for-the-badge" />

<img src="https://img.shields.io/badge/Testing-Spring_Security_Test-yellow?style=for-the-badge" />

<img src="https://img.shields.io/badge/Documentation-Swagger%20%7C%20OpenAPI-85EA2D?style=for-the-badge" />

<a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" /></a>

<img src="https://img.shields.io/github/last-commit/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

</p>

<p align="justify">
<em>
Neste capítulo, o projeto <strong>ASJCatalog</strong> evolui para um cenário muito mais próximo de aplicações corporativas reais, incorporando mecanismos robustos de autenticação, autorização e validação de dados utilizando o ecossistema moderno do <strong>Spring Security 6</strong>. O nome ASJCatalog vem das iniciais de Albert Silva de Jesus: o projeto nasceu da base do DSCatalog, do curso DevSuperior, e evoluiu de forma independente.

Além da proteção de endpoints REST, foram aplicados conceitos fundamentais de segurança backend moderna, incluindo <strong>OAuth2 Authorization Server</strong>, <strong>Resource Server</strong>, autenticação stateless com <strong>JWT</strong>, controle de acesso baseado em roles, tratamento global de erros de validação e integração segura com Swagger/OpenAPI.
</em>

</p>

---

# 📑 Sumário

> Navegação do capítulo.

---

| 🧩 Module                                                                     | ⚡ Description                                           |
| ----------------------------------------------------------------------------- | ------------------------------------------------------- |
| [📚 Contexto do Projeto](#-contexto-do-projeto)                               | Evolução arquitetural da API ASJCatalog                 |
| [🎯 Objetivos do Capítulo](#-objetivos-do-capítulo)                           | Estratégias e metas técnicas implementadas              |
| [🧠 Conceitos Fundamentais Trabalhados](#-conceitos-fundamentais-trabalhados) | Authentication, Authorization, JWT, OAuth2 e Validation |
| [🏛️ Arquitetura Geral de Segurança](#️-arquitetura-geral-de-segurança)       | Authorization Server + Resource Server                  |
| [🛠️ Tecnologias Utilizadas](#️-tecnologias-utilizadas)                       | Stack de segurança, validação e testes                  |
| [📦 Dependências Adicionadas](#-dependências-adicionadas)                     | Ecossistema Spring Security e OAuth2                    |
| [👥 Modelo de Usuários e Perfis](#-modelo-de-usuários-e-perfis)               | RBAC, roles e permissões                                |
| [🔐 Fluxo de Autenticação](#-fluxo-de-autenticação)                           | Processo de autenticação JWT                            |
| [⚙️ Configurações da Aplicação](#️-configurações-da-aplicação)                | Security properties e variáveis externas                |
| [🧾 Bean Validation](#-bean-validation)                                       | Validações declarativas e customizadas                  |
| [📂 Organização dos Packages](#-organização-dos-packages)                     | Estrutura modular da aplicação                          |
| [📊 Integração com Swagger/OpenAPI](#-integração-com-swaggeropenapi)          | Documentação autenticada via JWT                        |
| [🧪 Testes de Segurança](#-testes-de-segurança)                               | Testes de autenticação e autorização                    |
| [🧱 Boas Práticas Aplicadas](#-boas-práticas-aplicadas)                       | Estratégias modernas de segurança backend               |
| [🚀 Evolução Arquitetural do Projeto](#-evolução-arquitetural-do-projeto)     | Crescimento técnico da arquitetura                      |
| [🚧 Principais Desafios Encontrados](#-principais-desafios-encontrados)       | Complexidades enfrentadas durante implementação         |
| [🧠 Aprendizados Consolidados](#-aprendizados-consolidados)                   | Conhecimentos adquiridos neste capítulo                 |
| [🚧 Melhorias Futuras](#-melhorias-futuras)                                   | Próximas evoluções da arquitetura                       |
| [💼 Competências Demonstradas](#-competências-demonstradas)                   | Skills backend e segurança aplicadas                    |
| [🎓 Conclusão](#-conclusão)                                                   | Consolidação técnica do capítulo                        |
| [📚 Referências Técnicas](#-referências-técnicas)                             | Materiais e documentações utilizadas                    |
| [🚀 Como Executar](#-como-executar)                                           | Subida local, login de exemplo e testes                 |
| [📖 Documentação Técnica](#-documentação-técnica)                             | Guias detalhados em `docs/`                             |

---

# 📚 Contexto do Projeto

Após a consolidação da arquitetura em camadas e da estratégia de testes automatizados nos capítulos anteriores, o projeto evolui para uma nova etapa focada em autenticação, autorização e validação robusta.

Neste capítulo, a API ASJCatalog evolui para suportar aplicações robustas, incorporando mecanismos modernos de segurança utilizando Spring Security 6, OAuth2, JWT e Bean Validation, aproximando a aplicação de cenários reais utilizados em ambientes corporativos.

---

# 🎯 Objetivos do Capítulo

Este capítulo tem como objetivo transformar a API ASJCatalog em uma aplicação backend preparada para APIs escaláveis, autenticação moderna e sistemas seguros.

Para atingir esse objetivo, foram implementados os seguintes pilares:

---

## 1. Aplicar validação robusta com Bean Validation

A aplicação passou a utilizar validações declarativas para garantir integridade e previsibilidade dos dados recebidos pela API.

### Principais validações aplicadas

- `@NotBlank`
- `@Size`
- `@Pattern`
- `@Positive`
- `@PastOrPresent`
- `@NotEmpty`
- Validações customizadas de campo: `@ValidEmail`, `@UniqueEmail`, `@StrongPassword` e `@ValidRoles`
- Validações customizadas de classe: `@CategoryCreateValid`, `@ProductCreateValid`, `@UserCreateValid` e as equivalentes de atualização
- Integração com banco de dados
- Mensagens personalizadas: as anotações padrão usam o `ValidationMessages.properties`, e os validadores customizados usam textos definidos no código
- Tratamento global de erros

### Benefícios

- Integridade dos dados
- Contratos HTTP previsíveis
- Redução de inconsistências
- Segurança contra entradas inválidas
- Melhor experiência para consumidores da API

### ⚠️ Observações e recomendações — Validação

- Centralizar mensagens em `ValidationMessages.properties`
- Remover mensagens hardcoded
- Padronizar chaves de validação
- Preparar estrutura para i18n
- Adicionar testes para validadores customizados

---

## 2. Implementar autenticação moderna com OAuth2 e JWT

A autenticação da aplicação foi construída utilizando:

- Spring Security 6
- OAuth2 Authorization Server
- JWT
- BCrypt Password Encoder

### Recursos implementados

- Geração segura de tokens JWT
- Assinatura de tokens
- Expiração configurável
- Login via OAuth2 Password Flow
- Registro de aplicações clientes
- Controle de acesso baseado em roles

### ⚠️ Observações e recomendações — Segurança

- Preferir Authorization Code + PKCE em aplicações públicas
- Utilizar secret manager para chaves RSA
- Persistir tokens em produção
- Restringir CORS adequadamente
- Externalizar segredos
- Implementar políticas adicionais de segurança

---

## 3. Implementar controle de acesso e proteção de endpoints

A API passou a possuir controle de acesso baseado em autenticação e autorização utilizando Spring Security e RBAC.

### Estratégia aplicada

- Rotas públicas
- Rotas autenticadas
- Controle por roles
- Segurança em nível de método
- Autorização granular

### Estratégia de acesso aplicada

| Tipo de rota                                 | Acesso            |
| -------------------------------------------- | ----------------- |
| Swagger/OpenAPI                              | Público           |
| Categorias e produtos (GET)                  | Público           |
| Escrita de categorias e produtos             | ADMIN ou OPERATOR |
| Usuários                                     | ADMIN             |
| Demais endpoints                             | Autenticado       |

### Segurança em nível de método

```java
// Escrita de categorias e produtos
@PreAuthorize("hasRole('ADMIN') or hasRole('OPERATOR')")

// Usuários
@PreAuthorize("hasRole('ADMIN')")
```

`hasRole('ADMIN')` confere a permissão `ROLE_ADMIN`: o Spring acrescenta o prefixo `ROLE_` na comparação. A tabela completa de permissões está em [Autenticação e Autorização](docs/guides/AUTHENTICATION.md#9-quem-pode-fazer-o-quê).

### 📌 Exemplo de configuração de autorização

```java
.requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
.requestMatchers(DOCUMENTATION_OPENAPI).permitAll()
.anyRequest().authenticated()
```

### 📌 Fluxo de autorização da requisição

1. Cliente envia Bearer Token
2. Resource Server intercepta a requisição
3. JWT é validado
4. Roles do usuário são verificadas
5. Endpoint é liberado ou bloqueado

---

## 4. Aplicar boas práticas modernas de segurança

Foram implementadas estratégias utilizadas em APIs REST modernas e ambientes corporativos.

### 📌 Práticas aplicadas

- Controle de acesso por roles
- Configuração de CORS
- Desabilitação controlada de CSRF
- Separação entre Authorization Server e Resource Server
- Configuração por perfis (`dev`, `test`, `prod`)
- Documentação Swagger pública, com autorização via Bearer Token para testar as rotas protegidas
- Externalização de variáveis sensíveis

---

# 🧠 Conceitos Fundamentais Trabalhados

| Conceito             | Objetivo                                 |
| -------------------- | ---------------------------------------- |
| Authentication       | Verificar identidade do usuário          |
| Authorization        | Verificar permissões do usuário          |
| OAuth2               | Protocolo de autorização                 |
| JWT                  | Token seguro para autenticação stateless |
| Bean Validation      | Validação declarativa de dados           |
| Method Security      | Proteção em nível de métodos             |
| Resource Server      | Proteção dos recursos da API             |
| Authorization Server | Emissão e gerenciamento de tokens        |

---

## 🏛️ Arquitetura Geral de Segurança

A estratégia de segurança da aplicação foi construída utilizando uma arquitetura baseada em separação clara entre autenticação, autorização e proteção de recursos, seguindo padrões modernos utilizados em APIs escaláveis e arquiteturas desacopladas.

A aplicação foi dividida em dois componentes principais:

- **Authorization Server**
- **Resource Server**

Essa separação permite maior desacoplamento, escalabilidade e aderência ao ecossistema OAuth2 moderno.

---

### 📌 Estrutura Arquitetural

```text
┌──────────────────────┐
│      Client App      │
│  Frontend / Postman  │
└──────────┬───────────┘
           │
           │ Login Request
           ▼
┌────────────────────────────┐
│   Authorization Server     │
│ OAuth2 + Spring Security   │
│ JWT Generation             │
└──────────┬─────────────────┘
           │
           │ JWT Token
           ▼
┌────────────────────────────┐
│      Resource Server       │
│ JWT Validation             │
│ Route Protection           │
│ Method Security            │
└──────────┬─────────────────┘
           │
           ▼
┌────────────────────────────┐
│     Protected Endpoints    │
│ Products | Categories      │
│ Users                      │
└────────────────────────────┘
```

### 🔐 Fluxo de autenticação e autorização

```mermaid
sequenceDiagram
    autonumber

    participant Client
    participant AuthorizationServer
    participant ResourceServer

    Client->>AuthorizationServer: Authentication Request
    activate AuthorizationServer

    AuthorizationServer-->>Client: JWT Access Token
    deactivate AuthorizationServer

    Client->>ResourceServer: Request + Bearer Token

    activate ResourceServer
    ResourceServer->>ResourceServer: Validate JWT Signature & Claims
    ResourceServer-->>Client: Protected Resource Response
    deactivate ResourceServer
```

---

### 📌 Benefícios da Arquitetura

- Separação entre autenticação e autorização
- Maior modularidade
- Suporte a ambientes distribuídos
- Melhor manutenção evolutiva
- Estrutura preparada para arquiteturas desacopladas

---

## 🛠️ Tecnologias Utilizadas

## 🔐 Segurança

| Tecnologia                  | Função                                     |
| --------------------------- | ------------------------------------------ |
| Spring Security             | Framework principal de segurança           |
| OAuth2 Authorization Server | Emissão de tokens JWT                      |
| OAuth2 Resource Server      | Validação de tokens e proteção de recursos |
| JWT                         | Autenticação stateless baseada em token    |
| BCryptPasswordEncoder       | Criptografia segura de senhas              |

---

## 🧾 Validação

| Tecnologia          | Função                                         |
| ------------------- | ---------------------------------------------- |
| Bean Validation     | Validação declarativa                          |
| Hibernate Validator | Implementação da especificação Bean Validation |
| Jakarta Validation  | API padrão de validação                        |

---

## 📄 Documentação

| Tecnologia        | Função                      |
| ----------------- | --------------------------- |
| SpringDoc OpenAPI | Documentação automática     |
| Swagger UI        | Interface interativa da API |

---

## 🧪 Testes

| Tecnologia           | Função                             |
| -------------------- | ---------------------------------- |
| Spring Security Test | Testes de autenticação/autorização |
| MockMvc              | Testes de endpoints protegidos     |
| JUnit 5              | Testes automatizados               |
| Mockito              | Mocking de dependências            |

---

# 📦 Dependências Adicionadas

## 🔐 Spring Security

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## 🎫 OAuth2 Authorization Server

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-authorization-server</artifactId>
</dependency>
```

## 🛡️ OAuth2 Resource Server

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

---

## 🧪 Spring Security Test

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 👥 Modelo de Usuários e Perfis

O projeto passou a possuir um modelo de autenticação baseado em:

- Usuários
- Perfis (roles)
- Relacionamentos entre usuários e permissões

## 📌 Perfis utilizados

| Perfil        | Responsabilidade         |
| ------------- | ------------------------ |
| ROLE_ADMIN    | Controle total da API    |
| ROLE_OPERATOR | Operações intermediárias |

---

## 🖼️ Modelagem de Usuários e Perfis

<img src="docs/assets/imgs/modelo-conceitual.png" width="80%">

## 🔐 Fluxo de Autenticação

### Processo de Login

1. Cliente envia credenciais
2. Authorization Server autentica usuário
3. Token JWT é gerado
4. Cliente recebe token
5. Requisições utilizam Bearer Token
6. Resource Server valida JWT
7. Spring Security verifica permissões
8. API libera ou bloqueia acesso

---

### Requisição de Login

`POST /oauth2/token`

### Authorization

```text
Type: Basic Auth
Username: myclientid
Password: myclientsecret
```

O client id e o secret vêm de `security.client-id` e `security.client-secret` (variáveis `CLIENT_ID` e `CLIENT_SECRET`); os valores acima são os padrões.

### Body (x-www-form-urlencoded)

```text
grant_type=password
username=albert@gmail.com
password=123456
```

> [!WARNING]
> Os usuários de exemplo `albert@gmail.com` (OPERATOR) e `maria@gmail.com` (ADMIN e OPERATOR), ambos com a senha `123456`, são dados apenas de desenvolvimento, inseridos pelas migrations e pelo `import.sql`.

Resposta: `{"access_token":"eyJ...","token_type":"Bearer","expires_in":86399}`. Comandos prontos para PowerShell e bash estão em [Autenticação e Autorização](docs/guides/AUTHENTICATION.md#3-login).

---

### Estrutura JWT

Os tokens JWT são assinados com **RS256** (chave RSA gerada quando a aplicação sobe) e carregam informações como:

- Client que pediu o token
- Usuário autenticado
- Authorities/Roles
- Tempo de expiração
- Issuer
- Audience

### 📄 Exemplo de Payload JWT

```json
{
  "sub": "myclientid",
  "aud": "myclientid",
  "nbf": 1779662147,
  "iss": "http://localhost:8080",
  "exp": 1779748547,
  "iat": 1779662147,
  "jti": "c86d494c-43b0-4ce4-83bf-e3f6355da3bb",
  "authorities": ["ROLE_OPERATOR"],
  "username": "albert@gmail.com"
}
```

### 📌 Significado dos campos

| Campo         | Descrição                                   |
| ------------- | ------------------------------------------- |
| `sub`         | Client id (`myclientid`), não o usuário     |
| `aud`         | Client para o qual o token foi emitido      |
| `username`    | E-mail do usuário autenticado               |
| `authorities` | Roles/permissões do usuário                 |
| `iat`         | Data de emissão do token                    |
| `exp`         | Data de expiração                           |
| `iss`         | Emissor do token                            |

As claims `username` e `authorities` são acrescentadas pelo projeto, no `tokenCustomizer` do `AuthorizationServerConfig`.

---

## ⚙️ Configurações da Aplicação

### 📌 Properties de Segurança

```properties
security.client-id=${CLIENT_ID:myclientid}
security.client-secret=${CLIENT_SECRET:myclientsecret}
security.jwt.duration=${JWT_DURATION:86400}
cors.origins=${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
```

---

## 🛡️ Authorization Server

O Authorization Server é responsável por:

- Autenticar usuários
- Gerar tokens JWT
- Assinar tokens
- Registrar aplicações clientes
- Gerenciar autenticação OAuth2

### Responsabilidades implementadas

- Habilitação do Authorization Server
- Configuração de assinatura JWT
- Configuração de Password Encoder
- Registro de client OAuth2
- Configuração de token JWT
- Definição de duração do token

---

### Resource Server

O Resource Server é responsável por:

- Validar assinatura e expiração do JWT
- Converter claims em authorities do Spring Security
- Integrar autenticação stateless ao Security Filter Chain
- Aplicar políticas de autorização definidas no ResourceServerConfig

### Configurações aplicadas

- Controle de acesso por rota
- Configuração de CORS
- Configuração de CSRF
- Validação JWT
- Liberação do Swagger/OpenAPI
- Liberação do H2 Console em ambiente de teste

---

## 🔒 Exemplo de Requisição Autenticada

### 📌 Endpoint protegido

```http
GET /api/v1/users/2
Authorization: Bearer eyJraWQiOi...
```

A rota exige a role ADMIN; o token acima é o de `maria@gmail.com`. Sem token, a resposta é `401`; com o token de `albert@gmail.com` (só OPERATOR), é `403`.

---

### 📌 Exemplo de resposta

```json
{
  "id" : 2,
  "firstName" : "Maria",
  "lastName" : "Green",
  "email" : "maria@gmail.com",
  "roles" : [ {
    "id" : 1,
    "authority" : "ROLE_OPERATOR"
  }, {
    "id" : 2,
    "authority" : "ROLE_ADMIN"
  } ],
  "active" : true
}
```

---

### 📌 Fluxo da requisição

1. Cliente envia Bearer Token
2. Resource Server intercepta requisição
3. JWT é validado
4. Roles do usuário são verificadas
5. Endpoint é liberado ou negado

---

## 🧾 Bean Validation

A aplicação utiliza Bean Validation para garantir integridade, consistência e previsibilidade dos dados recebidos pela API.

A estratégia adotada combina validações declarativas, validações customizadas e integração com regras de negócio da aplicação, permitindo respostas padronizadas e maior segurança no processamento das requisições.

---

### Estratégia aplicada

- Validações declarativas em DTOs
- Integração com Hibernate Validator
- Validações customizadas por domínio
- Mensagens das anotações padrão no `ValidationMessages.properties`; mensagens dos validadores customizados definidas no código
- Integração com banco de dados
- Tratamento global de exceções
- Respostas padronizadas para erros de validação

---

### 📌 Principais validações utilizadas

| Validação         | Objetivo                                                     |
| ----------------- | ------------------------------------------------------------ |
| `@NotBlank`       | Garantir campos textuais obrigatórios                        |
| `@Size`           | Restringir tamanho mínimo e máximo                           |
| `@Pattern`        | Restringir o formato com expressão regular (nomes, URL)      |
| `@Positive`       | Garantir valores numéricos positivos                         |
| `@PastOrPresent`  | Validar datas no passado ou no presente                      |
| `@NotEmpty`       | Exigir ao menos um item (`categoryIds`, `roleIds`)           |
| Custom Validators | Regras específicas da aplicação                              |

---

### Exemplos de validação

Trecho real de `CategoryCreateRequest`: as anotações de campo usam chaves do `ValidationMessages.properties`, e a anotação de classe confere se o nome já existe.

```java
@CategoryCreateValid
public record CategoryCreateRequest(

    @NotBlank(message = "{category.name.notBlank}")
    @Size(min = 3, max = 80, message = "{category.name.size}")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s]+$", message = "{category.name.pattern}")
    String name,

    @Pattern(regexp = "^$|^.{3,255}$", message = "{category.description.size}")
    String description) {
}
```

---

### 📌 Validações customizadas implementadas

Além das validações padrão da especificação Bean Validation, a aplicação também implementa validações customizadas integradas às regras de negócio.

### Exemplos

| Anotação                                         | Regra                                                              |
| ------------------------------------------------ | ------------------------------------------------------------------ |
| `@StrongPassword`                                | Senha com 10+ caracteres, maiúscula, minúscula, número e especial  |
| `@ValidEmail`                                    | Formato do e-mail e registro MX do domínio (consulta ao DNS)       |
| `@UniqueEmail`                                   | E-mail ainda não cadastrado                                        |
| `@ValidRoles`                                    | Todas as roles informadas existem                                  |
| `@CategoryCreateValid` / `@CategoryUpdateValid`  | Nome de categoria único                                            |
| `@ProductCreateValid` / `@ProductUpdateValid`    | Nome de produto único e categorias existentes                      |
| `@UserCreateValid` / `@UserUpdateValid`          | Senha sem dados pessoais; na atualização, e-mail único             |

Essas validações permitem aplicar regras mais complexas, incluindo integração com banco de dados e validações contextuais da aplicação. As regras de cada DTO e exemplos reais de resposta estão em [Validação](docs/guides/VALIDATION.md).

---

### Benefícios da abordagem

- Maior integridade dos dados
- Redução de inconsistências
- Contratos HTTP mais previsíveis
- Melhor experiência para consumidores da API
- Centralização das regras de validação
- Melhor rastreabilidade de erros

---

## 🔄 Tratamento Global de Validações

A aplicação possui um mecanismo centralizado para tratamento de exceções e erros de validação.

### Benefícios

- Padronização das respostas
- Melhor integração frontend/backend
- Clareza de erros
- Rastreabilidade
- Melhor experiência de consumo da API

---

### 📄 Exemplo de resposta de validação

```json
{
  "timestamp": "2026-05-24T19:40:35.468801500Z",
  "status": 422,
  "error": "Validation Error",
  "message": "One or more fields are invalid",
  "path": "/api/v1/users",
  "fieldErrors": [
    {
      "fieldName": "firstName",
      "message": "Primeiro nome deve ter entre 2 e 80 caracteres"
    },
    {
      "fieldName": "password",
      "message": "Senha contém padrões muito comuns e inseguros"
    },
    {
      "fieldName": "firstName",
      "message": "Primeiro nome é obrigatório"
    },
    {
      "fieldName": "password",
      "message": "Senha deve possuir ao menos 10 caracteres"
    },
    {
      "fieldName": "password",
      "message": "Senha deve conter ao menos uma letra minúscula"
    },
    {
      "fieldName": "email",
      "message": "Email já existente"
    },
    {
      "fieldName": "roleIds",
      "message": "Usuário deve possuir ao menos uma role"
    },
    {
      "fieldName": "lastName",
      "message": "Sobrenome deve ter entre 2 e 80 caracteres"
    },
    {
      "fieldName": "lastName",
      "message": "Sobrenome é obrigatório"
    },
    {
      "fieldName": "password",
      "message": "Senha deve conter ao menos uma letra maiúscula"
    },
    {
      "fieldName": "password",
      "message": "Senha deve conter ao menos um caractere especial"
    }
  ]
}
```

O formato das respostas de erro (422, 403, 404, 409 e o 401 sem corpo) está em [Tratamento de Erros](docs/guides/ERROR-HANDLING.md).

---

## 📂 Organização dos Packages

A estrutura da camada de segurança foi organizada seguindo princípios de separação de responsabilidades, modularidade e baixo acoplamento, permitindo maior clareza arquitetural, facilidade de manutenção e escalabilidade evolutiva.

O objetivo foi estruturar cada responsabilidade de segurança de forma isolada e coesa, aproximando a aplicação de arquiteturas desacopladas utilizadas em projetos modernos com Spring Security.

---

## 🖼️ Organização da Camada de Segurança

```text
📦 com.albertsilva.dev.asjcatalog
┣ 📂 config
┃ ┗ 📄 SpringDocOpenApiConfig.java
┃
┣ 📂 dto
┃ ┣ 📂 category
┃ ┃ ┣ 📂 request
┃ ┃ ┃ ┣ 📄 CategoryCreateRequest.java
┃ ┃ ┃ ┗ 📄 CategoryUpdateRequest.java
┃ ┃ ┗ 📂 response
┃ ┃ ┃ ┗ 📄 CategoryResponse.java
┃ ┃
┃ ┣ 📂 product
┃ ┃ ┣ 📂 request
┃ ┃ ┃ ┣ 📄 ProductCreateRequest.java
┃ ┃ ┃ ┗ 📄 ProductUpdateRequest.java
┃ ┃ ┗ 📂 response
┃ ┃ ┃ ┣ 📄 ProductDetailsResponse.java
┃ ┃ ┃ ┗ 📄 ProductResponse.java
┃ ┃
┃ ┣ 📂 role
┃ ┃ ┗ 📂 response
┃ ┃ ┃ ┗ 📄 RoleResponse.java
┃ ┃
┃ ┗ 📂 user
┃   ┣ 📂 request
┃   ┃ ┣ 📄 UserCreateRequest.java
┃   ┃ ┗ 📄 UserUpdateRequest.java
┃   ┗ 📂 response
┃     ┣ 📄 UserDetailsResponse.java
┃     ┗ 📄 UserResponse.java
┃
┣ 📂 entity
┃ ┣ 📄 Category.java
┃ ┣ 📄 Product.java
┃ ┣ 📄 Role.java
┃ ┗ 📄 User.java
┃
┣ 📂 mapper
┃ ┣ 📂 category
┃ ┃ ┗ 📄 CategoryMapper.java
┃ ┣ 📂 product
┃ ┃ ┗ 📄 ProductMapper.java
┃ ┗ 📂 user
┃   ┗ 📄 UserMapper.java
┃
┣ 📂 projection
┃ ┗ 📄 UserDetailsProjection.java
┃
┣ 📂 repository
┃ ┣ 📄 CategoryRepository.java
┃ ┣ 📄 ProductRepository.java
┃ ┣ 📄 RoleRepository.java
┃ ┗ 📄 UserRepository.java
┃
┣ 📂 security
┃ ┣ 📂 config
┃ ┃ ┗ 📄 SecurityBeansConfig.java
┃ ┃
┃ ┣ 📂 oauth2
┃ ┃ ┣ 📂 authorization
┃ ┃ ┃ ┗ 📂 config
┃ ┃ ┃   ┗ 📄 AuthorizationServerConfig.java
┃ ┃ ┃
┃ ┃ ┣ 📂 grant
┃ ┃ ┃ ┗ 📂 password
┃ ┃ ┃   ┣ 📄 CustomPasswordAuthenticationConverter.java
┃ ┃ ┃   ┣ 📄 CustomPasswordAuthenticationProvider.java
┃ ┃ ┃   ┗ 📄 CustomPasswordAuthenticationToken.java
┃ ┃ ┃
┃ ┃ ┗ 📂 resource
┃ ┃   ┗ 📄 ResourceServerConfig.java
┃ ┃
┃ ┗ 📂 userdetails
┃   ┗ 📄 AuthenticatedUser.java
┃
┣ 📂 service
┃ ┣ 📂 exception
┃ ┃ ┣ 📄 DatabaseException.java
┃ ┃ ┗ 📄 ResourceNotFoundException.java
┃ ┣ 📄 CategoryService.java
┃ ┣ 📄 ProductService.java
┃ ┗ 📄 UserService.java
┃
┣ 📂 validation
┃ ┣ 📂 category
┃ ┃ ┣ 📂 annotation
┃ ┃ ┗ 📂 validator
┃ ┃
┃ ┣ 📂 product
┃ ┃ ┣ 📂 annotation
┃ ┃ ┗ 📂 validator
┃ ┃
┃ ┣ 📂 role
┃ ┃ ┣ 📂 annotation
┃ ┃ ┗ 📂 validator
┃ ┃
┃ ┗ 📂 user
┃   ┣ 📂 annotation
┃   ┗ 📂 validator
┃
┣ 📂 web
┃ ┣ 📂 controller
┃ ┃ ┣ 📄 CategoryController.java
┃ ┃ ┣ 📄 ProductController.java
┃ ┃ ┗ 📄 UserController.java
┃ ┃
┃ ┗ 📂 exception
┃   ┣ 📂 enums
┃   ┃ ┗ 📄 ErrorType.java
┃   ┣ 📂 handler
┃   ┃ ┗ 📄 ControllerExceptionHandler.java
┃   ┗ 📂 response
┃     ┣ 📄 FieldMessage.java
┃     ┣ 📄 ProblemDetails.java
┃     ┗ 📄 ValidationError.java
┃
┣ 📄 AsjcatalogApplication.java
┃
┗ 📂 resources
  ┣ 📂 db
  ┃ ┗ 📂 migration
  ┃   ┣ 📂 schema
  ┃   ┗ 📂 data
  ┣ 📂 META-INF
  ┃ ┗ 📄 additional-spring-configuration-metadata.json
  ┣ 📄 application-dev.properties
  ┣ 📄 application-prod.properties
  ┣ 📄 application-test.properties
  ┣ 📄 application.properties
  ┣ 📄 ValidationMessages.properties
  ┣ 📄 banner-dev.txt
  ┗ 📄 import.sql
```

---

## 🛡️ Estrutura da Camada de Segurança

A camada de segurança foi projetada para centralizar autenticação, autorização, validação e proteção dos recursos da API, utilizando uma abordagem modular inspirada em arquiteturas desacopladas e aplicações robustas.

Cada package possui uma responsabilidade bem definida dentro do fluxo de autenticação e segurança da aplicação.

### 🔐 `security.config`

Responsável pelas configurações globais de segurança da aplicação.

### Responsabilidades

- Registro de beans de segurança
- Password Encoder
- Configurações compartilhadas
- Componentes reutilizáveis do Spring Security

### 📄 Classe principal

| Classe                | Responsabilidade                                       |
| --------------------- | ------------------------------------------------------ |
| `SecurityBeansConfig` | Configuração central de beans relacionados à segurança |

### 🎫 `security.oauth2.authorization`

Responsável pela configuração do `Authorization Server` da aplicação.

O **Authorization Server** é encarregado da autenticação dos usuários e emissão dos tokens `JWT`.

### Responsabilidades

- Configuração do OAuth2 Authorization Server
- Geração de JWT
- Assinatura de tokens
- Registro de clients OAuth2
- Configuração do fluxo de autenticação

### 📄 Classe principal

| Classe                      | Responsabilidade                                                        |
| --------------------------- | ----------------------------------------------------------------------- |
| `AuthorizationServerConfig` | Configuração do servidor OAuth2 responsável pela emissão dos tokens JWT |

### 🔐 security.oauth2.resource

Responsável pela configuração do `Resource Server`.

O **Resource Server** protege os endpoints da API e valida os `tokens JWT` recebidos nas requisições.

### Responsabilidades

- Proteção de endpoints REST
- Validação de JWT
- Configuração de autorização
- Controle de acesso por roles
- Configuração de CORS e CSRF

### 📄 Classe principal

| Classe                 | Responsabilidade                                            |
| ---------------------- | ----------------------------------------------------------- |
| `ResourceServerConfig` | Configuração de segurança da API e validação dos tokens JWT |

### 🔐 security.oauth2.grant.password

Implementa o fluxo customizado de autenticação utilizando Password Grant.

Embora versões modernas do OAuth2 desencorajem esse fluxo em cenários públicos, ele foi utilizado neste projeto com fins educacionais e compreensão profunda do processo de autenticação.

### Responsabilidades

- Conversão das credenciais
- Processamento da autenticação
- Criação de tokens autenticados
- Integração com Spring Security

### 📄 Classes principais

| Classe                                  | Responsabilidade                                             |
| --------------------------------------- | ------------------------------------------------------------ |
| `CustomPasswordAuthenticationConverter` | Converte credenciais da requisição em objeto de autenticação |
| `CustomPasswordAuthenticationProvider`  | Processa autenticação do usuário                             |
| `CustomPasswordAuthenticationToken`     | Representa token autenticado durante o fluxo                 |

### 📂 security.userdetails

Responsável por centralizar informações relacionadas ao usuário autenticado dentro do contexto de segurança da aplicação.

### Responsabilidades

- Recuperar usuário autenticado
- Centralizar contexto de autenticação
- Facilitar acesso ao usuário logado

### 📄 Classe principal

| Classe              | Responsabilidade                                              |
| ------------------- | ------------------------------------------------------------- |
| `AuthenticatedUser` | Representação do usuário autenticado no contexto da aplicação |

## 🧾 Camada de Validação

A estrutura de validação foi organizada separando anotações customizadas e validadores específicos por domínio da aplicação.

Essa abordagem melhora:

- coesão;
- reutilização;
- clareza arquitetural;
- manutenção evolutiva.

### 📂 Estrutura

```text
validation
┣ category
┣ product
┣ role
┗ user
```

Cada domínio possui:

| Estrutura | Responsabilidade |
| ------------ | ------------------------------- |
| `annotation` | Define annotations customizadas |
| `validator` | Implementa regras de validação |

### Benefícios da abordagem

- Regras isoladas por domínio
- Fácil manutenção
- Reutilização de validações
- Maior legibilidade
- Melhor organização arquitetural

## 🌐 Camada Web

A camada web concentra os pontos de entrada da aplicação e o tratamento padronizado das respostas HTTP.

### 📂 `web.controller`

Responsável pelos endpoints REST da aplicação.

### 📌 Controllers implementados

| Controller           | Responsabilidade                               |
| -------------------- | ---------------------------------------------- |
| `CategoryController` | Endpoints de categorias                        |
| `ProductController`  | Endpoints de produtos                          |
| `UserController`     | Endpoints relacionados a usuários/autenticação |

### 📂 web.exception

Responsável pelo tratamento global de exceções da aplicação.

### Objetivos

- Padronizar respostas HTTP
- Centralizar tratamento de erros
- Melhorar rastreabilidade
- Facilitar integração frontend/backend

### 📄 Estruturas principais

| Estrutura                    | Responsabilidade                            |
| ---------------------------- | ------------------------------------------- |
| `ControllerExceptionHandler` | Intercepta exceções globalmente             |
| `ErrorType`                  | Enumeração de tipos de erro                 |
| `ProblemDetails`             | Estrutura padronizada da resposta           |
| `FieldMessage`               | Representa erros específicos de validação   |
| `ValidationError`            | Resposta específica para erros de validação |

---

## 🏛️ Estratégia Arquitetural Aplicada

A organização da camada de segurança segue princípios importantes de engenharia de software:

| Princípio                             | Aplicação                                                   |
| ------------------------------------- | ----------------------------------------------------------- |
| SRP (Single Responsibility Principle) | Cada package possui responsabilidade única                  |
| Desacoplamento                        | Separação clara entre autenticação, autorização e validação |
| Modularidade                          | Estrutura preparada para evolução                           |
| Escalabilidade                        | Facilidade para adicionar novos fluxos                      |
| Manutenibilidade                      | Código mais organizado e sustentável                        |

---

## 📊 Integração com Swagger/OpenAPI

A documentação da API foi integrada com autenticação JWT.

### Recursos implementados

- Autorização via Bearer Token (botão **Authorize**)
- Teste de endpoints protegidos
- Documentação automática
- Documentação pública, sem token; as rotas protegidas exigem o token informado no Authorize

---

### 🔗 Acesso

```text
http://localhost:8080/docs-asjcatalog.html
```

O endereço é público e redireciona para `/swagger-ui/index.html`. A especificação OpenAPI em JSON fica em `/docs-asjcatalog`.

---

## 🧪 Testes de Segurança

A aplicação evolui também em termos de testes automatizados de autenticação e autorização.

### 📌 Cenários testados

- Testes de integração com token real, obtido em `/oauth2/token` pelo `TokenUtil` com o usuário ADMIN de exemplo
- Endpoints protegidos chamados com `Authorization: Bearer` em todas as camadas
- Testes de controller com usuário simulado e roles definidas por `@WithMockUser`
- Regras do `@PreAuthorize` avaliadas com a configuração de segurança real (`ResourceServerConfig`)

O inventário completo, como rodar a suíte e o que ela ainda não cobre estão em [Testes Automatizados](docs/guides/TESTING.md).

---

### Ferramentas utilizadas

| Ferramenta           | Finalidade                |
| -------------------- | ------------------------- |
| MockMvc              | Teste de endpoints        |
| Spring Security Test | Simulação de autenticação |
| Mockito              | Mock de serviços          |
| JUnit 5              | Estrutura de testes       |

---

## 🧱 Boas Práticas Aplicadas

- Stateless authentication com JWT
- Externalização de variáveis sensíveis
- Segurança baseada em environment profiles
- Principle of Least Privilege (PoLP)
- Separation of Concerns (SoC)
- Centralização de exception handling
- DTO validation boundary
- Modularização da camada de segurança
- Controle de acesso baseado em roles

---

## 🚀 Evolução Arquitetural do Projeto

Com este capítulo, o projeto ASJCatalog deixa de representar apenas uma API CRUD tradicional e evolui para uma arquitetura backend mais próxima de cenários reais de mercado, incorporando fundamentos essenciais para construção de APIs escaláveis, sistemas seguros e aplicações robustas.

### 📌 Principais evoluções arquiteturais

- Autenticação stateless com JWT
- Autorização baseada em roles (RBAC)
- Proteção centralizada de endpoints
- Validação consistente de dados
- Tratamento padronizado de exceções
- Estrutura modular e desacoplada
- Configuração orientada a ambientes
- Estrutura preparada para ambientes distribuídos e cenários de produção

---

## 🚧 Principais Desafios Encontrados

Durante a implementação deste capítulo, diversos desafios técnicos relacionados à segurança e autenticação moderna foram enfrentados.

### 📌 Principais desafios

- Configuração do Spring Security 6
- Estruturação do OAuth2 Authorization Server
- Integração entre Authorization Server e Resource Server
- Geração e assinatura de JWT
- Implementação do fluxo Password Grant customizado
- Controle de acesso baseado em roles
- Configuração de CORS e CSRF
- Segurança stateless
- Integração do Swagger com autenticação JWT
- Estruturação desacoplada da camada de segurança
- Criação de validações customizadas integradas ao banco de dados

---

## 🧠 Aprendizados Consolidados

- Estruturação de autenticação e autorização com Spring Security 6
- Configuração de OAuth2 Authorization Server e Resource Server
- Implementação de autenticação stateless com JWT
- Aplicação de RBAC com Method Security
- Criação de validações customizadas com Bean Validation
- Padronização de respostas de erro com Exception Handling
- Organização modular da camada de segurança

---

## 🚧 Melhorias Futuras

- Refresh Token
- Revogação de tokens
- Login social (Google/GitHub)
- MFA (autenticação em dois fatores)
- Rate Limiting
- Auditoria de segurança
- Observabilidade com métricas
- Deploy seguro com HTTPS
- Integração com Keycloak

---

## 💼 Competências Demonstradas

Este capítulo consolida competências importantes relacionadas ao desenvolvimento backend moderno com Java e Spring Boot.

### 🔐 Segurança e autenticação

- Spring Security 6
- OAuth2
- JWT Authentication
- Authorization Server
- Resource Server
- Role-Based Access Control (RBAC)
- Method Security
- Stateless Authentication

---

### 🧾 Validação e tratamento de erros

- Bean Validation
- Hibernate Validator
- Validações customizadas
- Exception Handling
- Problem Details Pattern
- Validação integrada ao banco de dados

---

### 🏛️ Arquitetura backend

- Arquitetura em camadas
- DTO Pattern
- Separação de responsabilidades
- Modularização da segurança
- Organização enterprise de packages
- API REST segura

---

### 🧪 Testes e qualidade

- Spring Security Test
- MockMvc
- JUnit 5
- Mockito
- Testes de autorização
- Testes de autenticação

---

### ⚙️ Dev Practices

- Externalização de configurações
- Configuração por profiles
- Segurança baseada em ambiente
- Documentação técnica avançada
- Swagger/OpenAPI
- Estrutura preparada para ambientes distribuídos e produção

---

## 🎓 Conclusão

Este capítulo marca uma evolução significativa na maturidade arquitetural do projeto ASJCatalog.

Mais do que apenas proteger endpoints, a aplicação passa a incorporar conceitos fundamentais utilizados em sistemas corporativos modernos:

- autenticação baseada em JWT;
- autorização com RBAC;
- validação consistente de dados;
- proteção centralizada de recursos;
- arquitetura modular preparada para evolução.

A implementação de OAuth2, JWT e Spring Security consolida competências extremamente relevantes para o desenvolvimento backend moderno com Java e Spring Boot.

> [!IMPORTANT]
> Este capítulo representa um passo importante rumo à construção de APIs escaláveis, sistemas seguros e arquiteturas desacopladas alinhadas às práticas utilizadas no mercado profissional.

---

## 📚 Referências Técnicas

### 🔹 Bean Validation

- https://beanvalidation.org/
- https://docs.jboss.org/hibernate/beanvalidation/spec/2.0/api/overview-summary.html
- https://www.baeldung.com/java-bean-validation-not-null-empty-blank
- https://www.baeldung.com/spring-custom-validation-message-source

---

### 🔹 Segurança e JWT

- https://jwt.io
- https://oauth.net/2/
- https://spring.io/projects/spring-security
- https://docs.spring.io/spring-security/reference/

---

### 🔹 Regex e validações

- https://regexr.com/
- https://regexlib.com/

---

## 🚀 Como Executar

Pré-requisitos: JDK 17 e Git. O Maven vem com o projeto (Maven Wrapper), e o perfil `test` usa o banco H2 em memória, sem PostgreSQL.

**PowerShell**:

```powershell
git clone https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot
git checkout chapter-03-validation-security
cd backend
.\mvnw spring-boot:run '-Dspring-boot.run.arguments=--spring.profiles.active=test'
```

**bash**:

```bash
git clone https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot
git checkout chapter-03-validation-security
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=test
```

Sem o argumento, a aplicação sobe no perfil padrão `dev`, que exige um PostgreSQL com o banco `asjcatalog`.

A API sobe em `http://localhost:8080/api/v1`, e o Swagger fica em `http://localhost:8080/docs-asjcatalog.html`. Para obter um token com o usuário de exemplo (dados apenas de desenvolvimento):

```powershell
curl.exe -s -u myclientid:myclientsecret -d "grant_type=password&username=maria@gmail.com&password=123456" http://localhost:8080/oauth2/token
```

```bash
curl -s -u myclientid:myclientsecret -d "grant_type=password&username=maria@gmail.com&password=123456" http://localhost:8080/oauth2/token
```

Para rodar os testes, dentro de `backend`:

```powershell
.\mvnw verify '-Dspring.profiles.active=test'
```

```bash
./mvnw verify -Dspring.profiles.active=test
```

O passo a passo completo, com o perfil `dev` e o PostgreSQL, está em [Primeiros Passos](docs/guides/GETTING-STARTED.md). O login e as permissões estão em [Autenticação e Autorização](docs/guides/AUTHENTICATION.md), e os testes, inclusive os de integração que rodam à parte, em [Testes Automatizados](docs/guides/TESTING.md).

---

## 📖 Documentação Técnica

| 📘 Documento                                                  | ⚡ Descrição                                                              |
| ------------------------------------------------------------- | ------------------------------------------------------------------------ |
| [🧾 Validação](docs/guides/VALIDATION.md)                     | Regras por DTO, validadores customizados, senha forte e respostas 422    |
| [🔐 Autenticação e Autorização](docs/guides/AUTHENTICATION.md) | Login OAuth2, JWT, cadeias de filtros, roles, 401/403 e CORS             |
| [🚀 Primeiros Passos](docs/guides/GETTING-STARTED.md)         | Como rodar com H2 ou PostgreSQL, obter um token e as primeiras chamadas  |
| [⚙️ Configuração e Perfis](docs/guides/CONFIGURATION.md)      | Perfis, propriedades de segurança, variáveis de ambiente e logs          |
| [🏗️ Arquitetura](docs/guides/ARCHITECTURE.md)                 | Camadas, pacotes e caminho de uma requisição pela segurança e validação  |
| [🧩 Modelo de Domínio](docs/guides/DOMAIN-MODEL.md)           | Entidades `Category`, `Product`, `User` e `Role`                         |
| [🔍 Acesso a Dados](docs/guides/DATA-ACCESS.md)               | Repositórios, consultas, consulta do login, paginação e transações       |
| [🗄️ Migrations](docs/guides/DATABASE-MIGRATIONS.md)           | Flyway (V001 a V105), `import.sql` e `create.sql`                        |
| [🌐 Endpoints da API](docs/guides/API-ENDPOINTS.md)           | Os 21 endpoints, com permissões e exemplos reais                         |
| [⚠️ Tratamento de Erros](docs/guides/ERROR-HANDLING.md)       | `ProblemDetails`, `ValidationError`, status HTTP e o 401 sem corpo       |
| [🧪 Testes Automatizados](docs/guides/TESTING.md)             | Como rodar a suíte, inventário e testes com usuário simulado e token     |
| [📐 Convenções](docs/guides/CONVENTIONS.md)                   | Nomes, validação, segurança, testes, idioma, commits e branches          |
| [🏠 Índice da Documentação](docs/HOME.md)                     | Visão geral dos guias e do que chega nos próximos capítulos              |

---

## 👨‍💻 Autor

**Albert Silva de Jesus**  
Desenvolvedor Backend Java | Spring Boot

---
## 📎 Contato

[![LinkedIn](https://img.shields.io/badge/LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/albert-backend-java-spring-boot/)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com?subject=Contato%20sobre%20o%20projeto%20ASJCatalog)
