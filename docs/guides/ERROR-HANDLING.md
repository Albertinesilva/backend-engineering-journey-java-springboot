# Tratamento de erros

Este guia explica o formato das respostas de erro da API no capítulo 01 e qual status HTTP cada exceção gera.

## Sumário

1. [Visão geral](#1-visão-geral)
2. [ProblemDetails](#2-problemdetails)
3. [ErrorType](#3-errortype)
4. [Exceção e status](#4-exceção-e-status)
5. [Limitações conhecidas](#5-limitações-conhecidas)

## 1. Visão geral

Quando um controller lança uma exceção, ela não chega crua ao cliente. A classe `web/exception/handler/ControllerExceptionHandler`, anotada com `@RestControllerAdvice`, captura a exceção e devolve uma resposta padronizada.

Um **`@RestControllerAdvice`** é uma classe que o Spring consulta sempre que um controller lança uma exceção. Cada método `@ExceptionHandler` dela trata um tipo de exceção, e o objeto devolvido vira o corpo JSON da resposta.

As exceções de negócio ficam em `service/exception` e estendem `RuntimeException`:

| Exceção | Quando é lançada |
| --- | --- |
| `ResourceNotFoundException` | Id de categoria ou produto inexistente (`Entity not found id: <id>`), ou categoria inexistente em `categoryIds` (`One or more categories not found`) |
| `DatabaseException` | No `ProductService.delete`, quando a remoção viola a integridade do banco |

## 2. ProblemDetails

Todas as respostas de erro usam a classe `web/exception/response/ProblemDetails`, do próprio projeto, com cinco campos:

| Campo | Conteúdo |
| --- | --- |
| `timestamp` | Momento do erro, em UTC (formato ISO-8601) |
| `status` | Status HTTP, repetido no corpo |
| `error` | Título do erro, vindo do `ErrorType` |
| `message` | Descrição do erro |
| `path` | Caminho da requisição |

Resposta real de `GET /api/v1/categories/9999`:

```json
{
  "timestamp" : "2026-09-30T01:27:43.886780700Z",
  "status" : 404,
  "error" : "Resource not found",
  "message" : "Entity not found id: 9999",
  "path" : "/api/v1/categories/9999"
}
```

## 3. ErrorType

A enum `web/exception/enums/ErrorType` guarda os textos usados no campo `error` (e, no erro genérico, também em `message`). Os valores estão exatamente como no código, inclusive a constante `CONFLIT`, que no código está sem a letra "C" antes do "T" (a grafia em inglês seria `CONFLICT`):

| Constante | Texto |
| --- | --- |
| `RESOURCE_NOT_FOUND` | `Resource not found` |
| `DATABASE_ERROR` | `Database error` |
| `CONFLIT` | `Conflict` |
| `INTERNAL_SERVER_ERROR` | `Internal server error` |
| `UNEXPECTED_ERROR_OCCURRED` | `Unexpected error occurred` |

## 4. Exceção e status

| Exceção | Status | `error` | `message` |
| --- | --- | --- | --- |
| `ResourceNotFoundException` | 404 | `Resource not found` | A mensagem da exceção, por exemplo `Entity not found id: 9999` |
| `DatabaseException` | 400 | `Database error` | A mensagem da exceção |
| `DataIntegrityViolationException` | 409 | `Conflict` | Sempre `Cannot delete resource because it has related entities` |
| Qualquer outra (`Exception`) | 500 | `Internal server error` | Sempre `Unexpected error occurred` |

A `DataIntegrityViolationException` vem do Spring quando o banco recusa uma operação por uma restrição, por exemplo ao apagar uma categoria que ainda tem produtos vinculados. Resposta real de `DELETE /api/v1/categories/1`:

```json
{
  "timestamp" : "2026-09-30T01:44:34.523364700Z",
  "status" : 409,
  "error" : "Conflict",
  "message" : "Cannot delete resource because it has related entities",
  "path" : "/api/v1/categories/1"
}
```

## 5. Limitações conhecidas

- **Mensagens fixas em inglês.** Os títulos (`ErrorType`) e as mensagens das exceções são textos fixos em inglês no código, e não mudam com o idioma do cliente. Mensagens em português, inglês e espanhol, escolhidas pelo cabeçalho `Accept-Language`, chegam no capítulo 04.
- **Sem código estável de erro.** O `ProblemDetails` não tem um campo de código para o cliente tomar decisões; só textos. O campo `code` (enum `ApiErrorCode`, que substitui o `ErrorType`) chega no capítulo 04.
- **Sem tratamento de erros de validação.** Não há handler para `MethodArgumentNotValidException`, nem validação nos DTOs. O tratamento de validação, com resposta 422, chega no capítulo 03.
- **Erros de requisição viram 500.** JSON malformado e id não numérico na URL caem no handler genérico e respondem `500`, em vez de 400. Esse comportamento continua no capítulo 04.
- **Mensagem fixa no 409.** A resposta de violação de integridade sempre diz `Cannot delete resource because it has related entities`, qualquer que seja a operação que causou o erro.
- **`DatabaseException` não observada.** Ela é lançada no `ProductService.delete`, mas, ao apagar um produto com categorias, a remoção funcionou (204), e não encontrei um caso que a dispare.
