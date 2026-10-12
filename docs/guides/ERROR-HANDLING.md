# Tratamento de erros

⬅️ Anterior: [Validação](VALIDATION.md) · [🏠 Índice](../HOME.md) · Próximo: [Internacionalização](INTERNATIONALIZATION.md) ➡️

Este guia explica o formato das respostas de erro da API, qual status HTTP e qual código cada exceção gera, e como as mensagens são traduzidas.

## Sumário

1. [Visão geral](#1-visão-geral)
2. [ProblemDetails](#2-problemdetails)
3. [ValidationError](#3-validationerror)
4. [Exceção, status e código](#4-exceção-status-e-código)
5. [Como as mensagens são traduzidas](#5-como-as-mensagens-são-traduzidas)
6. [Erros que não passam pelo handler](#6-erros-que-não-passam-pelo-handler)
7. [Limitações conhecidas](#7-limitações-conhecidas)

## 1. Visão geral

Quando um controller lança uma exceção, ela não chega crua ao cliente. A classe `web/exception/handler/ControllerExceptionHandler`, anotada com `@RestControllerAdvice`, captura a exceção e devolve uma resposta padronizada. Um **`@RestControllerAdvice`** é uma classe que o Spring consulta sempre que um controller lança uma exceção; cada método `@ExceptionHandler` dela trata um tipo de exceção, e o objeto devolvido vira o corpo JSON da resposta.

Há dois formatos de corpo, ambos no pacote `web/exception/response`:

| Formato | Quando |
| --- | --- |
| `ProblemDetails` | Todos os erros |
| `ValidationError` | Erros de validação (422). É um `ProblemDetails` com a lista `fieldErrors` a mais |

> **Atenção:** `ProblemDetails` é uma **classe do próprio projeto**. Apesar do nome, ela **não segue a RFC 7807** (*Problem Details for HTTP APIs*), que define os campos `type`, `title`, `status`, `detail` e `instance` e o tipo de conteúdo `application/problem+json`. A classe do projeto tem outros campos (seção 2), e o Spring também tem uma classe própria chamada `ProblemDetail`, sem o "s" final, que não é usada aqui.

## 2. ProblemDetails

| Campo | Conteúdo |
| --- | --- |
| `timestamp` | Momento do erro, em UTC (formato ISO-8601) |
| `status` | Status HTTP, repetido no corpo |
| `code` | Código estável do erro (enum `ApiErrorCode`); **não muda com o idioma** |
| `error` | Título do erro, traduzido |
| `message` | Descrição do erro, traduzida |
| `path` | Caminho da requisição |

Resposta real de `GET /api/v1/categories/999999`:

```json
{
  "timestamp": "2026-09-29T21:02:30.319566500Z",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "error": "Recurso não encontrado",
  "message": "Categoria não encontrada",
  "path": "/api/v1/categories/999999"
}
```

A mesma requisição com `Accept-Language: en`:

```json
{
  "timestamp": "2026-09-29T21:02:30.375480900Z",
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "error": "Resource not found",
  "message": "Category not found",
  "path": "/api/v1/categories/999999"
}
```

Use o campo `code` para tomar decisões no cliente, e `error` e `message` para mostrar ao usuário.

## 3. ValidationError

Tem os mesmos campos do `ProblemDetails`, mais `fieldErrors`: uma lista com um item por regra violada, cada um com `fieldName` (o campo) e `message` (a mensagem traduzida). Um mesmo campo pode aparecer várias vezes.

Resposta real de `POST /api/v1/accounts/register` com e-mail inválido e senha `123456`:

```json
{
  "timestamp": "2026-09-29T21:02:44.750117100Z",
  "status": 422,
  "code": "VALIDATION_ERROR",
  "error": "Erro de validação",
  "message": "Um ou mais campos são inválidos",
  "path": "/api/v1/accounts/register",
  "fieldErrors": [
    { "fieldName": "password", "message": "Senha deve conter ao menos uma letra minúscula" },
    { "fieldName": "password", "message": "Senha deve conter ao menos um caractere especial" },
    { "fieldName": "password", "message": "Senha deve possuir ao menos 10 caracteres" },
    { "fieldName": "password", "message": "Senha deve conter ao menos uma letra maiúscula" },
    { "fieldName": "password", "message": "Senha não pode conter sequências numéricas previsíveis" },
    { "fieldName": "email", "message": "Email inválido" },
    { "fieldName": "password", "message": "Senha contém padrões muito comuns e inseguros" }
  ]
}
```

A ordem dos itens de `fieldErrors` não é garantida. As regras de cada campo estão em [VALIDATION.md](VALIDATION.md).

## 4. Exceção, status e código

| Exceção | Origem | Status | `code` (`ApiErrorCode`) |
| --- | --- | --- | --- |
| `ResourceNotFoundException` | Services, quando um id ou token não existe | 404 | `RESOURCE_NOT_FOUND` |
| `NoResourceFoundException` | Spring, quando não há rota nem recurso estático para o caminho | 404 | `RESOURCE_NOT_FOUND` |
| `MethodArgumentNotValidException` | Spring, quando um DTO com `@Valid` é inválido | 422 | `VALIDATION_ERROR` (corpo `ValidationError`) |
| `PasswordUpdateException` | `AccountService`, na troca de senha | 422 | `PASSWORD_UPDATE_ERROR` |
| `InvalidTokenException` | Entidade `Token`: token de conta desativado, expirado ou de outro tipo | 400 | `INVALID_TOKEN` |
| `DatabaseException` | `ProductService.delete`, em violação de integridade | 400 | `DATABASE_ERROR` |
| `DataIntegrityViolationException` | Spring/banco, quando uma restrição do banco é violada | 409 | `CONFLICT` |
| `AccessDeniedException` | Spring Security, quando o `@PreAuthorize` nega acesso | 403 | `ACCESS_DENIED` |
| `DisabledException` | Spring Security (conta desativada) | 403 | `ACCESS_DISABLED` |
| `AuthenticatedUserNotFoundException` | `AuthenticatedUserService`, quando o token não identifica um usuário existente | 401 | `AUTHENTICATION_REQUIRED` |
| Qualquer outra (`Exception`) | Qualquer ponto | 500 | `INTERNAL_SERVER_ERROR` |

As exceções do projeto ficam em `service/exception` e estendem `RuntimeException`. Elas carregam, como mensagem, a **chave** do texto traduzido (por exemplo `error.category.notFound`), e não o texto.

## 5. Como as mensagens são traduzidas

O idioma vem do cabeçalho `Accept-Language` (`pt-BR`, `en` ou `es`; qualquer outro valor, ou a ausência do cabeçalho, resulta em português). Os textos ficam em `backend/src/main/resources/messages_pt_BR.properties`, `messages_en.properties` e `messages_es.properties`.

**Para `ProblemDetails`**, cada handler informa três chaves:

1. a chave do título (por exemplo `error.resource.title`), que vira o campo `error`;
2. a mensagem da exceção (por exemplo `error.category.notFound`), que vira o campo `message`;
3. uma chave reserva (por exemplo `error.resource.message`), usada em `message` quando a mensagem da exceção é nula ou não existe nos arquivos.

**Para `ValidationError`**, o título e a mensagem geral vêm das chaves `error.validation.title` e `error.validation.message`. As mensagens de cada campo vêm das anotações dos DTOs (`message = "{category.name.notBlank}"`), traduzidas pelo Bean Validation com o mesmo idioma da requisição.

Os detalhes da configuração de idiomas estão em [INTERNATIONALIZATION.md](INTERNATIONALIZATION.md).

## 6. Erros que não passam pelo handler

O `ControllerExceptionHandler` só atua em exceções lançadas a partir dos controllers. Dois tipos de erro acontecem antes, e têm outro formato:

**401 do Resource Server.** Quando uma rota protegida pela regra de URL é chamada sem token, ou com token inválido, os filtros do Spring Security recusam a requisição antes de ela chegar ao controller. A resposta real é `401` **sem corpo**, só com o cabeçalho:

```text
WWW-Authenticate: Bearer
```

Com token malformado:

```text
WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Malformed token", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
```

**Erros de `POST /oauth2/token`.** Seguem o formato do OAuth2, com mensagens fixas em inglês. Resposta real para senha errada (`400`):

```json
{"error_description":"Invalid credentials","error":"invalid_grant","error_uri":"https://datatracker.ietf.org/doc/html/rfc6749#section-5.2"}
```

Mais exemplos em [AUTHENTICATION.md](AUTHENTICATION.md#2-login).

## 7. Limitações conhecidas

- **O 401 do Resource Server não tem corpo `ProblemDetails`.** O projeto não configura um `AuthenticationEntryPoint` próprio, então o cliente recebe só o status e o cabeçalho `WWW-Authenticate`, sem `code` nem mensagem traduzida.
- **Os erros de `/oauth2/token` não são traduzidos.** As mensagens (`Invalid credentials`, `Your account has not been activated yet. Please check your email.`, `Account is locked`) são textos fixos em inglês no `CustomPasswordAuthenticationProvider`, e o formato é o do OAuth2.
- **Erros de requisição viram 500.** Não há handler para JSON malformado, parâmetro obrigatório ausente ou tipo errado na URL (como `/categories/abc`): essas exceções do Spring caem no handler genérico e respondem `500 INTERNAL_SERVER_ERROR`, em vez de 400.
- **Funcionalidade não implementada vira 500.** `POST /accounts/deactivate` lança `UnsupportedOperationException`, tratada como erro interno.
- **`DisabledException` sem uso conhecido.** O handler existe, mas não encontrei no código um fluxo que leve essa exceção até um controller: o login de conta inativa é recusado dentro de `/oauth2/token`, com `invalid_grant`.

---

⬅️ Anterior: [Validação](VALIDATION.md) · [🏠 Índice](../HOME.md) · Próximo: [Internacionalização](INTERNATIONALIZATION.md) ➡️
