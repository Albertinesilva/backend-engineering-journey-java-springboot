# Tratamento de erros

Este guia explica o formato das respostas de erro da API no capítulo 03, qual status HTTP cada situação gera e por que o 401 é diferente dos demais.

## Sumário

1. [Visão geral](#1-visão-geral)
2. [ProblemDetails e ValidationError](#2-problemdetails-e-validationerror)
3. [Exceção e status](#3-exceção-e-status)
4. [O 401 fica fora do handler](#4-o-401-fica-fora-do-handler)
5. [ErrorType](#5-errortype)
6. [Limitações conhecidas](#6-limitações-conhecidas)

## 1. Visão geral

Quando um controller lança uma exceção, ela não chega crua ao cliente. A classe `web/exception/handler/ControllerExceptionHandler`, anotada com `@RestControllerAdvice`, captura a exceção e devolve uma resposta padronizada.

Um **`@RestControllerAdvice`** é uma classe que o Spring consulta sempre que um controller lança uma exceção. Cada método `@ExceptionHandler` dela trata um tipo de exceção, e o objeto devolvido vira o corpo JSON da resposta.

As exceções de negócio ficam em `service/exception` e estendem `RuntimeException`:

| Exceção | Quando é lançada |
| --- | --- |
| `ResourceNotFoundException` | Id de categoria, produto ou usuário inexistente (`Entity not found id: <id>`), categoria inexistente em `categoryIds` ou role inexistente em `roleIds` |
| `DatabaseException` | No `ProductService.delete`, quando a remoção viola a integridade do banco |

Neste capítulo, o handler ganhou três tratamentos: erros de validação (422), acesso negado (403) e rota inexistente (404).

## 2. ProblemDetails e ValidationError

Quase todas as respostas de erro usam a classe `web/exception/response/ProblemDetails`, com cinco campos:

| Campo | Conteúdo |
| --- | --- |
| `timestamp` | Momento do erro, em UTC (formato ISO-8601) |
| `status` | Status HTTP, repetido no corpo |
| `error` | Título do erro |
| `message` | Descrição do erro |
| `path` | Caminho da requisição |

Os erros de validação usam `ValidationError`, que estende `ProblemDetails` e acrescenta `fieldErrors`: uma lista de `FieldMessage`, cada um com `fieldName` e `message`.

Resposta real de um `POST /api/v1/categories` com nome inválido e descrição curta:

```json
{
  "timestamp" : "2026-10-06T01:53:06.401075100Z",
  "status" : 422,
  "error" : "Validation Error",
  "message" : "One or more fields are invalid",
  "path" : "/api/v1/categories",
  "fieldErrors" : [ {
    "fieldName" : "name",
    "message" : "O nome da categoria possui caracteres inválidos"
  }, {
    "fieldName" : "description",
    "message" : "A descrição deve ter entre 3 e 255 caracteres"
  } ]
}
```

As regras de validação e a origem das mensagens estão em [VALIDATION.md](VALIDATION.md).

## 3. Exceção e status

| Exceção | Status | `error` | `message` |
| --- | --- | --- | --- |
| `MethodArgumentNotValidException` (validação do `@Valid`) | 422 | `Validation Error` | `One or more fields are invalid`, com `fieldErrors` |
| `AccessDeniedException` (`@PreAuthorize` negado) | 403 | `Access denied` | `Você não possui permissão para acessar este recurso` |
| `ResourceNotFoundException` | 404 | `Resource not found` | A mensagem da exceção, por exemplo `Entity not found id: 9999` |
| `NoResourceFoundException` (rota inexistente) | 404 | `Resource not found` | `Static resource not found` |
| `DatabaseException` | 400 | `Database error` | A mensagem da exceção |
| `DataIntegrityViolationException` | 409 | `Conflict` | Sempre `Cannot delete resource because it has related entities` |
| Qualquer outra (`Exception`) | 500 | `Internal server error` | Sempre `Unexpected error occurred` |

Respostas reais:

**403**, `GET /api/v1/users` com o token de `albert@gmail.com` (só OPERATOR):

```json
{
  "timestamp" : "2026-10-06T02:09:48.928708100Z",
  "status" : 403,
  "error" : "Access denied",
  "message" : "Você não possui permissão para acessar este recurso",
  "path" : "/api/v1/users"
}
```

**404 de rota inexistente**, `GET /api/v1/nada` com token:

```json
{
  "timestamp" : "2026-10-06T02:09:50.640960700Z",
  "status" : 404,
  "error" : "Resource not found",
  "message" : "Static resource not found",
  "path" : "/api/v1/nada"
}
```

**409**, `DELETE /api/v1/categories/1` (categoria com produtos):

```json
{
  "timestamp" : "2026-10-06T02:09:50.750502500Z",
  "status" : 409,
  "error" : "Conflict",
  "message" : "Cannot delete resource because it has related entities",
  "path" : "/api/v1/categories/1"
}
```

A `DataIntegrityViolationException` vem do Spring quando o banco recusa uma operação por uma restrição. O `CategoryService.delete` não a trata: ela surge quando a transação é confirmada, já fora do método, e chega ao handler.

## 4. O 401 fica fora do handler

Um **filtro** de segurança roda antes do controller. Quando a rota exige autenticação e o token falta ou é inválido, o próprio Spring Security responde 401, e a requisição nunca chega ao controller nem ao `ControllerExceptionHandler`. Por isso o 401 **não tem corpo**: só o cabeçalho `WWW-Authenticate`.

Respostas reais de `GET /api/v1/users`:

- sem token → `401`, `WWW-Authenticate: Bearer` e `Content-Length: 0`;
- com token malformado → `401`, `WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Malformed token", ...`.

Já o 403 passa pelo handler, porque é lançado pelo `@PreAuthorize`, que roda no controller. A diferença entre os dois está em [AUTHENTICATION.md](AUTHENTICATION.md#8-401-e-403).

Os erros do endpoint de login (`/oauth2/token`) também têm formato próprio, definido pelo OAuth2, como `{"error":"invalid_client"}`. Veja [AUTHENTICATION.md](AUTHENTICATION.md#3-login).

## 5. ErrorType

A enum `web/exception/enums/ErrorType` guarda os títulos usados pelos tratamentos que vieram do capítulo 01. Os valores estão exatamente como no código, inclusive a constante `CONFLIT`, sem a letra "C" antes do "T" (a grafia em inglês seria `CONFLICT`):

| Constante | Texto |
| --- | --- |
| `RESOURCE_NOT_FOUND` | `Resource not found` |
| `DATABASE_ERROR` | `Database error` |
| `CONFLIT` | `Conflict` |
| `INTERNAL_SERVER_ERROR` | `Internal server error` |
| `UNEXPECTED_ERROR_OCCURRED` | `Unexpected error occurred` |

Os três tratamentos novos deste capítulo (422, 403 e o 404 de rota inexistente) escrevem os títulos e as mensagens diretamente no código, sem usar o `ErrorType`.

## 6. Limitações conhecidas

- **401 sem corpo.** O 401 não segue o formato `ProblemDetails` (seção 4). Continua no capítulo 04.
- **Idiomas misturados.** Os títulos (`error`) são em inglês; a mensagem do 403 e as mensagens dos campos de validação são em português; as demais mensagens são em inglês. Mensagens em português, inglês e espanhol, escolhidas pelo cabeçalho `Accept-Language`, chegam no capítulo 04.
- **Sem código estável de erro.** O `ProblemDetails` não tem um campo de código para o cliente tomar decisões; só textos. O campo `code` (enum `ApiErrorCode`, que substitui o `ErrorType`) chega no capítulo 04.
- **Erros de requisição viram 500.** JSON malformado e id não numérico na URL caem no handler genérico e respondem `500`, em vez de 400 (confirmado por execução). Esse comportamento continua no capítulo 04.
- **Criar usuário sem senha responde 500.** Veja [VALIDATION.md](VALIDATION.md#9-limitações-conhecidas).
- **Mensagem fixa no 409.** A resposta de violação de integridade sempre diz `Cannot delete resource because it has related entities`, qualquer que seja a operação que causou o erro.
- **`DatabaseException` não observada com o banco real.** Ela é lançada no `ProductService.delete`, mas apagar um produto com categorias funciona (204). O único teste que a cobre simula o erro do banco com um mock.
