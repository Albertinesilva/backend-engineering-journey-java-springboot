# Endpoints da API

Este guia lista todos os endpoints da API do ASJCatalog, quem pode acessar cada um, o que recebem e o que devolvem.

## Sumário

1. [Convenções](#1-convenções)
2. [Categorias](#2-categorias)
3. [Produtos](#3-produtos)
4. [Usuários](#4-usuários)
5. [Conta](#5-conta)
6. [Token de acesso](#6-token-de-acesso)
7. [Paginação e ordenação](#7-paginação-e-ordenação)
8. [Exemplos](#8-exemplos)
9. [Limitações conhecidas](#9-limitações-conhecidas)

## 1. Convenções

- Todas as rotas da API começam com `/api/v1`. Com a aplicação rodando localmente: `http://localhost:8080/api/v1`.
- Corpo de requisição e de resposta em JSON (`Content-Type: application/json`).
- Rotas protegidas exigem o cabeçalho `Authorization: Bearer <access_token>`. Como obter o token: [AUTHENTICATION.md](AUTHENTICATION.md).
- O cabeçalho `Accept-Language` (`pt-BR`, `en` ou `es`) escolhe o idioma das mensagens de erro. Veja [INTERNATIONALIZATION.md](INTERNATIONALIZATION.md).
- O formato das respostas de erro está em [ERROR-HANDLING.md](ERROR-HANDLING.md).
- A lista também pode ser consultada no Swagger, em `/docs-asjcatalog.html` (perfis `dev` e `test`).

**Legenda da coluna "Quem acessa":**

| Valor | Significado |
| --- | --- |
| Público | Não precisa de token |
| Autenticado | Qualquer usuário com token válido |
| OPERATOR ou ADMIN | Token de um usuário com `ROLE_OPERATOR` ou `ROLE_ADMIN` |
| ADMIN | Token de um usuário com `ROLE_ADMIN` |

**Status de erro comuns:** `401` sem token (ou token inválido) em rota protegida; `403` token válido sem a role exigida; `404` recurso não encontrado; `422` dados inválidos; `500` erro inesperado.

## 2. Categorias

Base: `/api/v1/categories`.

| Método | Rota | Quem acessa | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| GET | `/` | Público | — (query: `name`, `page`, `size`, `sort`) | 200, página de `CategoryResponse` | — |
| GET | `/{id}` | Público | — | 200, `CategoryDetailsResponse` | 404 |
| POST | `/` | OPERATOR ou ADMIN | `CategoryCreateRequest` | 201, `CategoryResponse` e cabeçalho `Location` | 401, 403, 422 |
| PATCH | `/{id}` | OPERATOR ou ADMIN | `CategoryUpdateRequest` | 200, `CategoryResponse` | 401, 403, 404, 422 |
| PATCH | `/{id}/activate` | OPERATOR ou ADMIN | — | 204 | 401, 403, 404 |
| PATCH | `/{id}/deactivate` | OPERATOR ou ADMIN | — | 204 | 401, 403, 404 |
| DELETE | `/{id}` | OPERATOR ou ADMIN | — | 204 | 401, 403, 404 |

Formatos: `CategoryResponse` = `id`, `name`; `CategoryDetailsResponse` = `id`, `name`, `description`, `active`. `CategoryCreateRequest` e `CategoryUpdateRequest` = `name`, `description`.

## 3. Produtos

Base: `/api/v1/products`.

| Método | Rota | Quem acessa | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| GET | `/` | Público | — (query: `name`, `categoryIds`, `page`, `size`, `sort`) | 200, página de `ProductResponse` | 500 com `sort` diferente de `id` ou `name` (seção 9) |
| GET | `/{id}` | Público | — | 200, `ProductDetailsResponse` | 404 |
| POST | `/` | OPERATOR ou ADMIN | `ProductCreateRequest` | 201, `ProductResponse` e cabeçalho `Location` | 401, 403, 422 |
| PUT | `/{id}` | OPERATOR ou ADMIN | `ProductUpdateRequest` | 200, `ProductResponse` | 401, 403, 404, 422 |
| PATCH | `/{id}/activate` | OPERATOR ou ADMIN | — | 204 | 401, 403, 404 |
| PATCH | `/{id}/deactivate` | OPERATOR ou ADMIN | — | 204 | 401, 403, 404 |
| DELETE | `/{id}` | OPERATOR ou ADMIN | — | 204 | 401, 403, 404 |

Formatos:

- `ProductResponse` = `id`, `name`, `description`, `price`, `imgUrl`, `categories` (lista de `CategoryResponse`);
- `ProductDetailsResponse` = os mesmos campos, mais `createdAt`, `updatedAt` e `active`, com `categories` como lista de `CategoryDetailsResponse`;
- `ProductCreateRequest` = `name`, `description`, `price`, `imgUrl`, `date`, `categoryIds`;
- `ProductUpdateRequest` = `name`, `description`, `price`, `imgUrl`, `categoryIds`.

Filtros de `GET /`: `name` busca por trecho do nome; `categoryIds` recebe ids separados por vírgula (`categoryIds=1,3`), e o padrão `0` significa todas as categorias.

## 4. Usuários

Base: `/api/v1/users`. Gestão administrativa de usuários.

| Método | Rota | Quem acessa | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| GET | `/` | ADMIN | — (query: `firstName`, `page`, `size`, `sort`) | 200, página de `UserResponse` | 401, 403 |
| GET | `/{id}` | ADMIN, ou OPERATOR consultando o próprio id | — | 200, `UserDetailsResponse` | 401, 403, 404 |
| POST | `/` | ADMIN | `UserCreateRequest` | 201, `UserResponse` e cabeçalho `Location` | 401, 403, 422 |
| PUT | `/{id}` | ADMIN (a regra para OPERATOR não funciona; seção 9) | `UserUpdateRequest` | 200, `UserResponse` | 401, 403, 404, 422 |
| PATCH | `/{id}/activate` | ADMIN | — | 204 | 401, 403, 404 |
| PATCH | `/{id}/deactivate` | ADMIN | — | 204 | 401, 403, 404 |
| DELETE | `/{id}` | ADMIN | — | 204 | 401, 403, 404 |

Formatos:

- `UserResponse` = `id`, `firstName`, `lastName`, `email`, `roles` (lista de `{id, authority}`);
- `UserDetailsResponse` = os mesmos campos, mais `active`;
- `UserCreateRequest` = `firstName`, `lastName`, `email`, `password`, `roleIds`;
- `UserUpdateRequest` = `firstName`, `lastName`, `email`, `password` (opcional) e `roleIds` (opcional).

## 5. Conta

Base: `/api/v1/accounts`. Operações da própria conta. Os fluxos estão detalhados em [ACCOUNT-FLOWS.md](ACCOUNT-FLOWS.md).

| Método | Rota | Quem acessa | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| POST | `/register` | Público | `UserRegisterRequest` (`firstName`, `lastName`, `email`, `password`) | 201, `UserResponse` | 422 |
| GET | `/activate?token=...` | Público | — | 204 | 400 (token desativado, expirado ou de outro tipo), 404 (token inexistente), 500 (sem o parâmetro `token`) |
| POST | `/resend-activation` | Público | `UserEmailRequest` (`email`) | 204, também para e-mail não cadastrado ou conta já ativa | 422 |
| POST | `/password-recovery` | Público | `UserEmailRequest` (`email`) | 204, também para e-mail não cadastrado | 422 |
| POST | `/reset-password` | Público | `PasswordResetRequest` (`token`, `password`) | 204 | 400, 404, 422 |
| GET | `/me` | Autenticado | — | 200, `UserResponse` | 403 sem token (seção 9) |
| PUT | `/me` | Autenticado | `AuthenticatedUserUpdateRequest` (`firstName`, `lastName`, `email`) | 200, `UserResponse` | 401, 422 |
| PATCH | `/me/password` | Autenticado | `PasswordUpdateRequest` (`currentPassword`, `newPassword`, `confirmPassword`) | 204 | 401, 422 |
| POST | `/deactivate` | OPERATOR ou ADMIN | — | Não implementado (seção 9) | 403 sem token; 500 com token |

`/resend-activation` e `/password-recovery` respondem 204 mesmo quando o e-mail não existe. Assim, quem chama a API não consegue descobrir quais e-mails estão cadastrados.

## 6. Token de acesso

| Método | Rota | Quem acessa | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- | --- |
| POST | `/oauth2/token` | Cliente OAuth2 (HTTP Basic com `client_id` e `client_secret`) | Formulário (`application/x-www-form-urlencoded`): `grant_type=password`, `username`, `password`; ou `grant_type=refresh_token`, `refresh_token` | 200, `access_token`, `refresh_token`, `token_type`, `expires_in` | 400 (`invalid_grant`), 401 (`invalid_client`) |

Esta rota não fica em `/api/v1`, e os erros seguem o formato do OAuth2, e não o `ProblemDetails`. Detalhes em [AUTHENTICATION.md](AUTHENTICATION.md).

## 7. Paginação e ordenação

As listagens (`GET /categories`, `GET /products` e `GET /users`) aceitam:

| Parâmetro | Significado | Padrão |
| --- | --- | --- |
| `page` | Número da página, começando em 0 | `0` |
| `size` | Itens por página | `20` |
| `sort` | Campo e direção, como `name,asc` ou `name,desc` | Sem ordenação |

Resposta real de `GET /api/v1/categories?size=2&sort=name,asc`:

```json
{
  "content": [
    { "id": 30, "name": "Audio" },
    { "id": 12, "name": "Automotive" }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 2,
    "sort": { "empty": false, "sorted": true, "unsorted": false },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 44,
  "totalPages": 22,
  "last": false,
  "size": 2,
  "number": 0,
  "sort": { "empty": false, "sorted": true, "unsorted": false },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

Como a paginação funciona por dentro: [DATA-ACCESS.md](DATA-ACCESS.md#6-paginação-e-ordenação).

## 8. Exemplos

Os exemplos usam PowerShell. Nos que não enviam corpo JSON, basta trocar `curl.exe` por `curl` no bash. `$token` é o `access_token` obtido como em [GETTING-STARTED.md](GETTING-STARTED.md#8-primeira-requisição-autenticada).

**Listar produtos de uma categoria (público):**

```powershell
curl.exe -s "http://localhost:8080/api/v1/products?categoryIds=1&name=soundbar&size=5"
```

**Buscar um usuário (ADMIN, ou OPERATOR no próprio id).** Resposta real com o token do usuário `albert@gmail.com`:

```powershell
curl.exe -s http://localhost:8080/api/v1/users/1 -H "Authorization: Bearer $token"
```

```json
{"id":1,"firstName":"Albert","lastName":"Silva","email":"albert@gmail.com","roles":[{"id":1,"authority":"ROLE_OPERATOR"}],"active":true}
```

**Criar uma categoria (OPERATOR ou ADMIN).** No Windows PowerShell, passar JSON com aspas e espaços direto para o `curl.exe` quebra o corpo em pedaços. Use o `Invoke-RestMethod`:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/categories -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' -Body '{"name":"Jardinagem","description":"Ferramentas e plantas"}'
```

Ou grave o JSON num arquivo e peça ao `curl.exe` para lê-lo com `@`:

```powershell
'{"name":"Jardinagem","description":"Ferramentas e plantas"}' | Out-File -Encoding ascii categoria.json
curl.exe -s -X POST http://localhost:8080/api/v1/categories -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d '@categoria.json'
```

**bash**:

```bash
curl -s -X POST http://localhost:8080/api/v1/categories -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"name":"Jardinagem","description":"Ferramentas e plantas"}'
```

Em caso de sucesso, a resposta é 201 com `{"id": ..., "name": "Jardinagem"}` e o cabeçalho `Location` apontando para `/api/v1/categories/{id}`.

**Categoria inexistente (404).** Resposta real:

```powershell
curl.exe -s http://localhost:8080/api/v1/categories/999999
```

```json
{"timestamp":"2026-09-29T21:02:30.319566500Z","status":404,"code":"RESOURCE_NOT_FOUND","error":"Recurso não encontrado","message":"Categoria não encontrada","path":"/api/v1/categories/999999"}
```

## 9. Limitações conhecidas

- **`GET /products?sort=price` responde 500.** A primeira consulta da listagem de produtos é SQL nativo e seleciona só `id` e `name` numa subconsulta chamada `tb_result`. O Spring acrescenta `order by tb_result.price`, e o PostgreSQL recusa com `ERROR: column tb_result.price does not exist`. Só `sort=id` e `sort=name` funcionam. Detalhes em [DATA-ACCESS.md](DATA-ACCESS.md#8-limitações-conhecidas).
- **`GET /accounts/me` sem token responde 403, e não 401.** A regra de URL libera `GET /api/v1/accounts/**` para acesso público (por causa de `/activate`). A proteção de `/me` vem só do `@PreAuthorize("isAuthenticated()")`, que nega o acesso anônimo com `AccessDeniedException`, tratada como 403. O mesmo acontece em `POST /accounts/deactivate` sem token.
- **`POST /accounts/deactivate` não está implementado.** O método do service só lança `UnsupportedOperationException("Unimplemented method 'deactivateAccount'")`, e a API responde 500 a qualquer usuário autenticado.
- **`PUT /users/{id}` não funciona para OPERATOR, nem no próprio id.** A regra `#id == authentication.principal.id` compara o id da URL com o claim `jti` do JWT, e não com o id do usuário. Veja [AUTHENTICATION.md](AUTHENTICATION.md#10-limitações-conhecidas).
- **Parâmetros inválidos respondem 500.** Um id não numérico (`/categories/abc`), a falta do parâmetro `token` em `/activate` e um JSON malformado caem no tratamento genérico de exceções e devolvem 500 em vez de 400.
- **Rota inexistente sem token responde 401.** Como toda rota fora das públicas exige autenticação, um endereço errado sem token devolve 401, e não 404.
