# Endpoints da API

Este guia lista os 14 endpoints da API do ASJCatalog no capítulo 01, o que recebem e o que devolvem.

## Sumário

1. [Convenções](#1-convenções)
2. [Categorias](#2-categorias)
3. [Produtos](#3-produtos)
4. [Paginação e ordenação](#4-paginação-e-ordenação)
5. [Exemplos](#5-exemplos)
6. [Limitações conhecidas](#6-limitações-conhecidas)

## 1. Convenções

- Todas as rotas começam com `/api/v1`. Com a aplicação rodando localmente: `http://localhost:8080/api/v1`.
- Corpo de requisição e de resposta em JSON (`Content-Type: application/json`).
- **Todas as rotas são públicas**: este capítulo não tem autenticação.
- O formato das respostas de erro está em [ERROR-HANDLING.md](ERROR-HANDLING.md).
- A lista também pode ser consultada no Swagger, em `/docs-asjcatalog.html` (perfis `dev` e `test`).

**Status de erro comuns:** `404` recurso não encontrado; `409` violação de integridade no banco; `500` erro inesperado.

## 2. Categorias

Base: `/api/v1/categories`.

| Método | Rota | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- |
| GET | `/` | — (query: `name`, `page`, `size`, `sort`) | 200, página de `CategoryResponse` | — |
| GET | `/{id}` | — | 200, `CategoryResponse` | 404 |
| POST | `/` | `CategoryCreateRequest` | 201, `CategoryResponse` e cabeçalho `Location` | — |
| PATCH | `/{id}` | `CategoryUpdateRequest` | 200, `CategoryResponse` | 404 |
| PATCH | `/{id}/activate` | — | 204 | 404 |
| PATCH | `/{id}/deactivate` | — | 204 | 404 |
| DELETE | `/{id}` | — | 204 | 404; 409 se a categoria tiver produtos |

Formatos:

- `CategoryCreateRequest` e `CategoryUpdateRequest` = `name`, `description`;
- `CategoryResponse` = `id`, `name`, `description`, `active`.

O filtro `name` busca por trecho do nome, sem diferenciar maiúsculas. O `PATCH` é parcial: só os campos enviados (não nulos) são alterados.

## 3. Produtos

Base: `/api/v1/products`.

| Método | Rota | Corpo | Sucesso | Erros |
| --- | --- | --- | --- | --- |
| GET | `/` | — (query: `name`, `page`, `size`, `sort`) | 200, página de `ProductResponse` | — |
| GET | `/{id}` | — | 200, `ProductDetailsResponse` | 404 |
| POST | `/` | `ProductCreateRequest` | 201, `ProductResponse` e cabeçalho `Location` | 404 se algum `categoryIds` não existir |
| PATCH | `/{id}` | `ProductUpdateRequest` | 200, `ProductResponse` | 404 (produto ou categoria inexistente) |
| PATCH | `/{id}/activate` | — | 204 | 404 |
| PATCH | `/{id}/deactivate` | — | 204 | 404 |
| DELETE | `/{id}` | — | 204 | 404 |

Formatos:

- `ProductCreateRequest` e `ProductUpdateRequest` = `name`, `description`, `price`, `imgUrl`, `date`, `categoryIds`;
- `ProductResponse` e `ProductDetailsResponse` = `id`, `name`, `description`, `price`, `imgUrl`, `date`, `categories` (lista de `CategoryResponse`).

As categorias são enviadas só pelos ids, em `categoryIds`. No `PATCH`, os campos nulos são ignorados, e `categoryIds`, quando enviado, **substitui** as categorias do produto. Detalhes em [DATA-ACCESS.md](DATA-ACCESS.md#4-relacionamento-com-categorias).

Apagar um produto que tem categorias funciona (204): os vínculos em `tb_product_category` são removidos junto.

## 4. Paginação e ordenação

As listagens aceitam:

| Parâmetro | Significado | Padrão |
| --- | --- | --- |
| `page` | Número da página, começando em 0 | `0` |
| `size` | Itens por página | `20` |
| `sort` | Campo e direção, como `name,asc` ou `name,desc` | Sem ordenação |
| `name` | Trecho do nome, sem diferenciar maiúsculas | Sem filtro |

Resposta real de `GET /api/v1/categories?size=2&sort=name,asc` (no perfil `test`, o JSON vem indentado):

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
  "pageable" : {
    "pageNumber" : 0,
    "pageSize" : 2,
    "sort" : { "empty" : false, "sorted" : true, "unsorted" : false },
    "offset" : 0,
    "paged" : true,
    "unpaged" : false
  },
  "last" : false,
  "totalElements" : 15,
  "totalPages" : 8,
  "size" : 2,
  "number" : 0,
  "sort" : { "empty" : false, "sorted" : true, "unsorted" : false },
  "first" : true,
  "numberOfElements" : 2,
  "empty" : false
}
```

## 5. Exemplos

Respostas reais, obtidas no perfil `test`. No PowerShell, passar JSON com aspas e espaços direto para o `curl.exe` quebra o corpo em pedaços; por isso os exemplos de envio usam o `Invoke-RestMethod`.

**Criar uma categoria:**

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/categories -ContentType 'application/json' -Body '{"name":"Garden","description":"Garden tools"}'
```

```bash
curl -s -X POST http://localhost:8080/api/v1/categories -H 'Content-Type: application/json' -d '{"name":"Garden","description":"Garden tools"}'
```

Resposta: `201`, cabeçalho `Location` com o endereço da nova categoria (montado a partir da URL da requisição, por exemplo `http://localhost:8080/api/v1/categories/16`) e corpo:

```json
{ "id" : 16, "name" : "Garden", "description" : "Garden tools", "active" : true }
```

**Atualizar só a descrição (`PATCH` parcial):**

```bash
curl -s -X PATCH http://localhost:8080/api/v1/categories/16 -H 'Content-Type: application/json' -d '{"description":"Garden tools and plants"}'
```

```json
{ "id" : 16, "name" : "Garden", "description" : "Garden tools and plants", "active" : true }
```

**Desativar uma categoria:**

```powershell
curl.exe -s -i -X PATCH http://localhost:8080/api/v1/categories/16/deactivate
```

Resposta: `204`, sem corpo. Depois disso, `GET /api/v1/categories/16` mostra `"active" : false`.

**Criar um produto:**

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/products -ContentType 'application/json' -Body '{"name":"Garden Hose","description":"15m hose","price":79.9,"imgUrl":"https://example.com/hose.png","date":"2020-07-20T10:00:00Z","categoryIds":[2]}'
```

```bash
curl -s -X POST http://localhost:8080/api/v1/products -H 'Content-Type: application/json' -d '{"name":"Garden Hose","description":"15m hose","price":79.9,"imgUrl":"https://example.com/hose.png","date":"2020-07-20T10:00:00Z","categoryIds":[2]}'
```

Resposta (`201`), com `categories` vazio (veja a seção 6):

```json
{ "id" : 26, "name" : "Garden Hose", "description" : "15m hose", "price" : 79.9, "imgUrl" : "https://example.com/hose.png", "date" : "2020-07-20T10:00:00Z", "categories" : [ ] }
```

**Buscar o produto pelo id** (`GET /api/v1/products/26`), que mostra as categorias:

```json
{ "id" : 26, "name" : "Garden Hose", "description" : "15m hose", "price" : 79.9, "imgUrl" : "https://example.com/hose.png", "date" : "2020-07-20T10:00:00Z", "categories" : [ { "id" : 2, "name" : "Electronics", "description" : "Electronic devices and gadgets", "active" : true } ] }
```

**Categoria inexistente** (`GET /api/v1/categories/9999`):

```json
{ "timestamp" : "2026-09-30T01:27:43.886780700Z", "status" : 404, "error" : "Resource not found", "message" : "Entity not found id: 9999", "path" : "/api/v1/categories/9999" }
```

**Apagar uma categoria com produtos** (`DELETE /api/v1/categories/1`):

```json
{ "timestamp" : "2026-09-30T01:44:34.523364700Z", "status" : 409, "error" : "Conflict", "message" : "Cannot delete resource because it has related entities", "path" : "/api/v1/categories/1" }
```

## 6. Limitações conhecidas

- **Não há validação dos dados de entrada.** Os DTOs não têm anotações de validação, e o `ProductController` nem usa `@Valid`. Confirmado por execução: `POST /api/v1/categories` com `{}` cria uma categoria com `name` nulo (201), e `POST /api/v1/products` sem nome e com preço `-5` também é aceito (201). A validação (Bean Validation, com resposta 422) chega no capítulo 03.
- **JSON malformado e id não numérico respondem 500.** Um corpo JSON inválido ou uma URL como `/api/v1/products/abc` caem no tratamento genérico de exceções e devolvem `500 Internal server error`, em vez de 400. Esse comportamento continua no capítulo 04.
- **`/swagger-ui.html` responde 500.** O Swagger fica em `/docs-asjcatalog.html`, que redireciona para `/swagger-ui/index.html`.
- **As respostas de listagem, criação e atualização de produtos trazem `categories` vazio.** Só `GET /api/v1/products/{id}` mostra as categorias. Veja [DATA-ACCESS.md](DATA-ACCESS.md#6-limitações-conhecidas).
- **Sem autenticação.** Qualquer cliente pode criar, alterar e apagar dados. Autenticação e autorização chegam no capítulo 03.
- **Campo `active` não é aceito na entrada.** Os DTOs de criação e atualização não têm `active`: tudo é criado ativo, e o status só muda por `activate` e `deactivate`.
