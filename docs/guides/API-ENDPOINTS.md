# Endpoints da API

Este guia lista os 21 endpoints da API do ASJCatalog no capítulo 03: quem pode chamar cada um, o que recebem e o que devolvem.

## Sumário

1. [Convenções](#1-convenções)
2. [Categorias](#2-categorias)
3. [Produtos](#3-produtos)
4. [Paginação e ordenação](#4-paginação-e-ordenação)
5. [Exemplos](#5-exemplos)
6. [Usuários](#6-usuários)
7. [Limitações conhecidas](#7-limitações-conhecidas)

## 1. Convenções

- Todas as rotas começam com `/api/v1`. Com a aplicação rodando localmente: `http://localhost:8080/api/v1`.
- Corpo de requisição e de resposta em JSON (`Content-Type: application/json`).
- **As listagens e buscas por id de categorias e produtos são públicas.** As demais rotas exigem o cabeçalho `Authorization: Bearer <token>`. Como obter o token: [AUTHENTICATION.md](AUTHENTICATION.md#3-login).
- Os corpos de entrada são validados; as regras estão em [VALIDATION.md](VALIDATION.md#2-regras-por-dto).
- O formato das respostas de erro está em [ERROR-HANDLING.md](ERROR-HANDLING.md).
- A lista também pode ser consultada no Swagger, em `/docs-asjcatalog.html` (perfis `dev` e `test`).

**Status comuns:** `401` sem token ou token inválido; `403` sem a role exigida; `404` recurso não encontrado; `409` violação de integridade no banco; `422` dados inválidos; `500` erro inesperado.

## 2. Categorias

Base: `/api/v1/categories`.

| Método | Rota | Acesso | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| GET | `/` | Público | — (query: `name`, `page`, `size`, `sort`) | 200, página de `CategoryResponse` | — |
| GET | `/{id}` | Público | — | 200, `CategoryResponse` | 404 |
| POST | `/` | ADMIN ou OPERATOR | `CategoryCreateRequest` | 201, `CategoryResponse` e cabeçalho `Location` | 401, 403, 422 |
| PATCH | `/{id}` | ADMIN ou OPERATOR | `CategoryUpdateRequest` | 200, `CategoryResponse` | 401, 403, 404, 422 |
| PATCH | `/{id}/activate` | ADMIN ou OPERATOR | — | 204 | 401, 403, 404 |
| PATCH | `/{id}/deactivate` | ADMIN ou OPERATOR | — | 204 | 401, 403, 404 |
| DELETE | `/{id}` | ADMIN ou OPERATOR | — | 204 | 401, 403, 404; 409 se a categoria tiver produtos |

Formatos:

- `CategoryCreateRequest` e `CategoryUpdateRequest` = `name`, `description`;
- `CategoryResponse` = `id`, `name`, `description`, `active`.

O filtro `name` busca por trecho do nome, sem diferenciar maiúsculas. Embora a rota seja um `PATCH`, o `name` é obrigatório (veja Limitações conhecidas).

## 3. Produtos

Base: `/api/v1/products`.

| Método | Rota | Acesso | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| GET | `/` | Público | — (query: `name`, `page`, `size`, `sort`) | 200, página de `ProductResponse` | — |
| GET | `/{id}` | Público | — | 200, `ProductDetailsResponse` | 404 |
| POST | `/` | ADMIN ou OPERATOR | `ProductCreateRequest` | 201, `ProductResponse` e cabeçalho `Location` | 401, 403, 422 |
| PATCH | `/{id}` | ADMIN ou OPERATOR | `ProductUpdateRequest` | 200, `ProductResponse` | 401, 403, 404, 422 |
| PATCH | `/{id}/activate` | ADMIN ou OPERATOR | — | 204 | 401, 403, 404 |
| PATCH | `/{id}/deactivate` | ADMIN ou OPERATOR | — | 204 | 401, 403, 404 |
| DELETE | `/{id}` | ADMIN ou OPERATOR | — | 204 | 401, 403, 404 |

Formatos:

- `ProductCreateRequest` e `ProductUpdateRequest` = `name`, `description`, `price`, `imgUrl`, `date`, `categoryIds`;
- `ProductResponse` = `id`, `name`, `description`, `price`, `imgUrl`, `date`, `categories`;
- `ProductDetailsResponse` = os mesmos campos e `active`.

As categorias são enviadas só pelos ids, em `categoryIds`, que é obrigatório. Na atualização, as categorias são **substituídas**. Apagar um produto que tem categorias funciona (204): os vínculos são removidos junto.

## 4. Paginação e ordenação

As listagens aceitam:

| Parâmetro | Significado | Padrão |
| --- | --- | --- |
| `page` | Número da página, começando em 0 | `0` |
| `size` | Itens por página | `20` |
| `sort` | Campo e direção, como `name,asc` ou `name,desc` | Sem ordenação |
| `name` (categorias e produtos) ou `firstName` (usuários) | Trecho do nome, sem diferenciar maiúsculas | Sem filtro |

Resposta real de `GET /api/v1/categories?size=2&sort=name,asc` (no perfil `test`, o JSON vem indentado), com o trecho da paginação resumido:

```json
{
  "content" : [ {
    "id" : 12,
    "name" : "Automotive",
    "description" : "Car parts and automotive accessories",
    "active" : true
  }, {
    "id" : 11,
    "name" : "Beauty",
    "description" : "Beauty and cosmetics products",
    "active" : true
  } ],
  "pageable" : { "pageNumber" : 0, "pageSize" : 2, ... },
  "totalPages" : 8,
  "totalElements" : 15,
  "size" : 2,
  "number" : 0,
  ...
}
```

## 5. Exemplos

Respostas reais, obtidas no perfil `test`. Nos exemplos com token, `$token` (PowerShell) e `$TOKEN` (bash) vêm do login descrito em [AUTHENTICATION.md](AUTHENTICATION.md#3-login).

**Criar uma categoria** (token de `albert@gmail.com`, OPERATOR):

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/categories -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' -Body '{"name":"Gaming","description":"Gaming products"}'
```

```bash
curl -s -X POST http://localhost:8080/api/v1/categories -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"name":"Gaming","description":"Gaming products"}'
```

Resposta: `201`, com a categoria criada (`id` 16 nos dados de exemplo).

**Atualizar uma categoria** (`PATCH /api/v1/categories/2`, com `{"name":"Electronics","description":"Devices"}`):

```json
{"id":2,"name":"Electronics","description":"Devices","active":true}
```

**Categoria inexistente** (`GET /api/v1/categories/9999`):

```json
{ "timestamp" : "2026-10-06T01:19:17.152625200Z", "status" : 404, "error" : "Resource not found", "message" : "Entity not found id: 9999", "path" : "/api/v1/categories/9999" }
```

**Apagar uma categoria com produtos** (`DELETE /api/v1/categories/1`, token de ADMIN):

```json
{ "timestamp" : "2026-10-06T02:09:50.750502500Z", "status" : 409, "error" : "Conflict", "message" : "Cannot delete resource because it has related entities", "path" : "/api/v1/categories/1" }
```

**Dados inválidos:** veja os exemplos de 422 em [VALIDATION.md](VALIDATION.md#8-exemplos-reais).

## 6. Usuários

Base: `/api/v1/users`. Todas as rotas exigem token.

| Método | Rota | Acesso | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| POST | `/` | ADMIN | `UserCreateRequest` | 201, `UserResponse` e cabeçalho `Location` | 401, 403, 422 |
| GET | `/` | ADMIN | — (query: `firstName`, `page`, `size`, `sort`) | 200, página de `UserResponse` | 401, 403 |
| GET | `/{id}` | ADMIN (veja Limitações conhecidas) | — | 200, `UserDetailsResponse` | 401, 403, 404 |
| PUT | `/{id}` | ADMIN (idem) | `UserUpdateRequest` | 200, `UserResponse` | 401, 403, 404, 422 |
| PATCH | `/{id}/activate` | ADMIN | — | 204 | 401, 403, 404 |
| PATCH | `/{id}/deactivate` | ADMIN | — | 204 | 401, 403, 404 |
| DELETE | `/{id}` | ADMIN | — | 204 | 401, 403, 404 |

Formatos:

- `UserCreateRequest` e `UserUpdateRequest` = `firstName`, `lastName`, `email`, `password`, `roleIds`;
- `UserResponse` = `id`, `firstName`, `lastName`, `email`, `roles` (lista com os nomes das roles);
- `UserDetailsResponse` = `id`, `firstName`, `lastName`, `email`, `roles` (lista de objetos com `id` e `authority`), `active`.

A senha nunca aparece nas respostas. No `PUT`, a senha é opcional: se não for enviada, a atual é mantida (confirmado por execução). As roles informadas **substituem** as atuais.

**Criar um usuário** (token de `maria@gmail.com`, ADMIN), com `{"firstName":"Pedro","lastName":"Santos","email":"pedro@gmail.com","password":"JAVA!@#ResTIc18","roleIds":[1]}`. Resposta `201`, cabeçalho `Location: http://localhost:8080/api/v1/users/3` (a porta é a da aplicação) e corpo:

```json
{
  "id" : 3,
  "firstName" : "Pedro",
  "lastName" : "Santos",
  "email" : "pedro@gmail.com",
  "roles" : [ "ROLE_OPERATOR" ]
}
```

**Detalhes de um usuário** (`GET /api/v1/users/2`, ADMIN):

```json
{
  "id" : 2,
  "firstName" : "Maria",
  "lastName" : "Green",
  "email" : "maria@gmail.com",
  "roles" : [ { "id" : 1, "authority" : "ROLE_OPERATOR" }, { "id" : 2, "authority" : "ROLE_ADMIN" } ],
  "active" : true
}
```

## 7. Limitações conhecidas

- **O `PATCH` de categoria e de produto não é parcial.** Os DTOs de atualização exigem os mesmos campos obrigatórios da criação. Veja [VALIDATION.md](VALIDATION.md#9-limitações-conhecidas). Continua no capítulo 04.
- **O OPERATOR não acessa o próprio usuário.** As regras de `GET` e `PUT /users/{id}` citam o OPERATOR no próprio usuário, mas a comparação nunca é verdadeira; ele recebe 403. Veja [AUTHENTICATION.md](AUTHENTICATION.md#11-limitações-conhecidas). No capítulo 04, o `GET` é corrigido e o `PUT` continua igual.
- **Criar usuário sem senha responde 500.** Veja [VALIDATION.md](VALIDATION.md#9-limitações-conhecidas). Corrigido no capítulo 04.
- **JSON malformado e id não numérico respondem 500.** Um corpo JSON inválido ou uma URL como `/api/v1/products/abc` caem no tratamento genérico de exceções (confirmado por execução). Esse comportamento continua no capítulo 04.
- **Rota inexistente responde 401 sem token.** Uma URL que não existe exige autenticação, porque cai na regra "qualquer outra rota". Com token, responde 404.
- **`/swagger-ui.html` responde 401.** O Swagger fica em `/docs-asjcatalog.html`, que redireciona para `/swagger-ui/index.html`. Continua no capítulo 04.
- **Descrições do Swagger que não correspondem ao código.** As listagens e buscas por id de categorias e produtos dizem "Exige Bearer Token. Acesso restrito a ..." , mas são públicas. Os `POST` documentam uma resposta 409 ("Categoria já existente", "Produto já existente", "Usuário já existente"), mas o nome ou e-mail repetido responde 422. Continua no capítulo 04 para as rotas públicas.
- **As respostas de listagem, criação e atualização de produtos trazem `categories` vazio.** Só `GET /api/v1/products/{id}` mostra as categorias. Veja [DATA-ACCESS.md](DATA-ACCESS.md#7-limitações-conhecidas).
- **Aviso na serialização das páginas.** Veja [DATA-ACCESS.md](DATA-ACCESS.md#7-limitações-conhecidas).
