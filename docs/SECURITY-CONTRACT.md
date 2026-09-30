# SECURITY CONTRACT — ASJCatalog Backend

## 1. Objetivo

Este documento registra o comportamento atual e real do sistema de autenticação e autorização do backend do ASJCatalog. Ele serve como baseline/contrato de compatibilidade para futuras alterações pontuais e controladas.

Este documento descreve o comportamento atual, não necessariamente o comportamento arquiteturalmente ideal.

Uma alteração futura que modifique este contrato deve ser tratada como uma alteração de comportamento e analisada antes de ser implementada.

A finalidade deste documento não é propor melhorias nem corrigir riscos identificados. A finalidade é documentar exatamente o que o código atual faz e o que precisa ser preservado.

## 2. Stack de Segurança

Confirmado pelo código:

- Spring Security
- Spring Authorization Server
- OAuth2
- JWT
- RSA
- BCrypt
- Resource Server

Evidência principal:

- [backend/pom.xml](../backend/pom.xml)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/config/SecurityBeansConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/config/SecurityBeansConfig.java)

## 3. Arquitetura

### 3.1. Componentes principais

- Authorization Server: configuração central do token endpoint e do JWT
- Resource Server: valida o JWT e converte authorities em `GrantedAuthority`
- Custom Password Grant: fluxo customizado para `grant_type=password`
- UserDetails: usuário do sistema, carregado via `UserService`
- `OAuth2AuthorizationService`: persistência em memória das autorizações emitidas

### 3.2. Fluxo atual

```text
Client
  ↓
/oauth2/token
  ↓
Authorization Server
  ↓
CustomPasswordAuthenticationConverter
  ↓
CustomPasswordAuthenticationProvider
  ↓
UserDetailsService
  ↓
BCrypt
  ↓
AuthenticatedUser
  ↓
JWT + Refresh Token
  ↓
Resource Server
  ↓
JwtAuthenticationConverter
  ↓
@PreAuthorize
```

### 3.3. Evidências da arquitetura

- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java)

## 4. Client Authentication Contract

### 4.1. Cliente registrado

Confirmado pelo código em [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java).

O cliente registra:

- `client_id`
- `client_secret`
- scopes: `read`, `write`
- grant types: `password` e `refresh_token`
- `TokenSettings`
- `ClientSettings`

### 4.2. Configuração em application.properties

Confirmado em [backend/src/main/resources/application.properties](../backend/src/main/resources/application.properties):

- `security.client-id=${CLIENT_ID:myclientid}`
- `security.client-secret=${CLIENT_SECRET:myclientsecret}`
- `security.jwt.duration=${JWT_DURATION:86400}`
- `cors.origins=${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}`

### 4.3. Método de autenticação do cliente

Confirmado parcialmente pelo código:

- o cliente possui `client_secret`
- o `client_secret` é codificado com `PasswordEncoder`
- o projeto usa `BCryptPasswordEncoder`
- o comportamento de autenticação do cliente é delegado ao Spring Authorization Server

Observação:

- não há chamada explícita para `clientAuthenticationMethod(...)` no código do projeto.
- portanto, o método específico do cliente não foi definido diretamente neste código.
- o comportamento é tratado pelo Spring Authorization Server como parte do fluxo de OAuth2.

### 4.4. Dependência com o token endpoint

A autenticação do cliente e o token endpoint dependem do `RegisteredClient` configurado e do Spring Authorization Server. O token endpoint customizado usa o cliente autenticado para validar o restante do fluxo de geração do token.

## 5. Login Contract

### 5.1. Endpoint

Endpoint real confirmado:

- `POST /oauth2/token`

### 5.2. Parâmetros do grant customizado

Confirmado pelo código em [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationConverter.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationConverter.java):

- `grant_type` obrigatório
- `username` obrigatório
- `password` obrigatório
- `scope` opcional

### 5.3. Forma de autenticação do cliente

Confirmado por teste em [backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java):

```java
mockMvc.perform(post("/oauth2/token")
    .params(params)
    .with(httpBasic(clientId, clientSecret))
    .accept("application/json;charset=UTF-8"))
```

Isso confirma que, no teste existente, o cliente é autenticado com HTTP Basic autenticando `client_id` e `client_secret`.

Não foi confirmado pelo código que o cliente enviou `client_id` e `client_secret` no corpo da requisição. O teste usa Basic Auth, e isso é evidência de teste.

### 5.4. Requisição esperada

O contrato real confirmado pelo código e pelos testes é:

```http
POST /oauth2/token
Authorization: Basic <base64(clientId:clientSecret)>
Content-Type: application/x-www-form-urlencoded

grant_type=password&username=user@email.com&password=secret
```

`scope` é opcional e tratado como campo de escopo solicitado.

## 6. Custom Password Grant

### 6.1. `CustomPasswordAuthenticationConverter`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationConverter.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationConverter.java)

Responsabilidade:

- verificar se `grant_type` é `password`
- extrair `username`, `password`, `scope`
- capturar parâmetros adicionais
- recuperar o principal do cliente do `SecurityContextHolder`
- criar `CustomPasswordAuthenticationToken`

Comportamento real:

- se `grant_type != "password"`, retorna `null`
- valida `scope`, `username` e `password`
- lança `OAuth2AuthenticationException` com `invalid_request` quando os parâmetros são inválidos

### 6.2. `CustomPasswordAuthenticationToken`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationToken.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationToken.java)

Responsabilidade:

- encapsular username
- encapsular password em texto claro durante o processamento
- encapsular escopos solicitados
- representar o fluxo OAuth2 customizado do tipo `password`

### 6.3. `CustomPasswordAuthenticationProvider`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java)

Fluxo real:

1. valida cliente autenticado
2. carrega usuário via `UserDetailsService`
3. valida senha com `BCryptPasswordEncoder`
4. valida estado do usuário
5. obtém autoridades do usuário
6. filtra scopes permitidos pelo cliente
7. cria `AuthenticatedUser`
8. grava no `SecurityContextHolder`
9. cria `DefaultOAuth2TokenContext`
10. gera `access_token`
11. gera `refresh_token`
12. cria `OAuth2Authorization`
13. salva no `OAuth2AuthorizationService`
14. retorna `OAuth2AccessTokenAuthenticationToken`

## 7. UserDetails Contract

### 7.1. `User`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/User.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/User.java)

Ela implementa `UserDetails` e expõe:

- `getUsername()` → retorna `email`
- `getAuthorities()` → retorna `roles`
- `isEnabled()` → usa campo `active`
- `isAccountNonLocked()` → `true`
- `isAccountNonExpired()` → `true`
- `isCredentialsNonExpired()` → `true`

### 7.2. `UserService`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/service/UserService.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/service/UserService.java)

Implementa `UserDetailsService` e carrega o usuário a partir do email:

- `userRepository.searchUserAndRolesByEmail(username)`
- cria um novo `User`
- monta `roles` a partir das projeções

### 7.3. `Role`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/Role.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/Role.java)

Implementa `GrantedAuthority` e expõe `getAuthority()`.

### 7.4. `AuthenticatedUser`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/userdetails/AuthenticatedUser.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/userdetails/AuthenticatedUser.java)

Esse objeto encapsula:

- `id`
- `username`
- `authorities`

Esse objeto é usado para transportar dados do usuário autenticado durante a geração do JWT.

### 7.5. `AuthenticatedUserService`

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/auth/AuthenticatedUserService.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/auth/AuthenticatedUserService.java)

Responsabilidade real:

- buscar o `Authentication` no `SecurityContextHolder`
- verificar se o principal é um `Jwt`
- extrair `userId` do claim `userId`
- carregar o usuário do banco

## 8. SecurityContext Contract

### 8.1. Fluxo real

```text
SecurityContextHolder
  ↓
OAuth2ClientAuthenticationToken
  ↓
setDetails(AuthenticatedUser)
  ↓
AuthorizationServerConfig.tokenCustomizer()
  ↓
JWT claims
```

### 8.2. Onde isso acontece

Confirmado em [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java):

```java
OAuth2ClientAuthenticationToken oAuth2ClientAuthenticationToken = (OAuth2ClientAuthenticationToken) SecurityContextHolder
    .getContext().getAuthentication();
AuthenticatedUser customPasswordUser = new AuthenticatedUser(userId, username, userDetails.getAuthorities());
oAuth2ClientAuthenticationToken.setDetails(customPasswordUser);

var newcontext = SecurityContextHolder.createEmptyContext();
newcontext.setAuthentication(oAuth2ClientAuthenticationToken);
SecurityContextHolder.setContext(newcontext);
```

### 8.3. Por que isso é sensível

Essa parte cria um acoplamento implícito entre:

- autenticação do cliente
- detalhes do usuário
- geração do JWT

O `tokenCustomizer` lê `context.getPrincipal()` e faz:

```java
OAuth2ClientAuthenticationToken principal = context.getPrincipal();
AuthenticatedUser user = (AuthenticatedUser) principal.getDetails();
```

Esse acoplamento é crítico porque a geração do JWT depende de uma estrutura específica do principal no `SecurityContext`.

### 8.4. Impacto de alteração

Se qualquer uma destas peças mudar:

- `principal`
- `details`
- `SecurityContext`
- `AuthenticatedUser`

o JWT pode deixar de conter `userId` ou `authorities`, e o Resource Server pode deixar de reconhecer corretamente o usuário autenticado.

## 9. JWT Contract

### 9.1. Tipo e assinatura

Confirmado pelo código:

- JWT self-contained
- `OAuth2TokenFormat.SELF_CONTAINED`
- algoritmo `RSA`
- tamanho: 2048 bits
- `NimbusJwtEncoder`
- `OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)`
- cabeçalho do token: `alg=RS256` e `kid` aleatório (confirmado por execução)

**Chave RSA em memória.** O par de chaves é gerado por `generateRsa()` a cada subida da aplicação (`KeyPairGenerator`, 2048 bits, `keyID` com UUID aleatório) e não é persistido. Consequência: depois de um reinício, todos os access tokens emitidos antes deixam de ser aceitos, porque a assinatura não confere com a chave nova.

### 9.2. TTL

Confirmado em [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java):

```java
.accessTokenTimeToLive(Duration.ofSeconds(jwtDurationSeconds))
```

Propriedade:

- [backend/src/main/resources/application.properties](../backend/src/main/resources/application.properties)
- `security.jwt.duration=${JWT_DURATION:86400}`

### 9.3. Claims customizados

O código adiciona estes claims no access token:

- `authorities`
- `userId`
- `username`

### 9.4. Classe responsável

- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)

### 9.5. Tabela de claims

| Claim         | Origem                               | Classe responsável                            | Consumidor                       | Impacto se removido ou renomeado                         |
| ------------- | ------------------------------------ | --------------------------------------------- | -------------------------------- | -------------------------------------------------------- |
| `authorities` | `AuthenticatedUser.getAuthorities()` | `AuthorizationServerConfig.tokenCustomizer()` | `JwtGrantedAuthoritiesConverter` | quebra autorização de `@PreAuthorize` no Resource Server |
| `userId`      | `AuthenticatedUser.getId()`          | `AuthorizationServerConfig.tokenCustomizer()` | `AuthenticatedUserService`       | quebra resolução do usuário autenticado no backend       |
| `username`    | `AuthenticatedUser.getUsername()`    | `AuthorizationServerConfig.tokenCustomizer()` | qualquer consumidor do JWT       | perde informação de identidade do usuário                |

### 9.6. `issuer`, `audience`, `subject`

Não são configurados explicitamente no código. Valores observados por execução, num token emitido em `http://localhost:8086`:

- `sub`: o `client_id` (`myclientid`), e não o usuário;
- `aud`: o `client_id` (`myclientid`);
- `iss`: a URL base da requisição (`http://localhost:8086`).

O usuário é identificado apenas pelos claims `userId` e `username`.

## 10. Authorities Contract

As authorities confirmadas no sistema são:

- `ROLE_ADMIN`
- `ROLE_OPERATOR`

Não existe `ROLE_USER`: as únicas roles são as inseridas pela migration `V104__insert_role.sql` (pasta `db/migration/reference`, aplicada nos perfis `dev` e `prod`) e pelo `import.sql` do perfil `test`.

### 10.1. Origem

- [backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/Role.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/Role.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/User.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/User.java)
- [backend/src/main/resources/db/migration/reference/V104__insert_role.sql](../backend/src/main/resources/db/migration/reference/V104__insert_role.sql)

Novas contas criadas pelo cadastro público (`POST /api/v1/accounts/register`) recebem sempre `ROLE_OPERATOR`: `AccountService.register` busca essa role com `roleRepository.findByAuthority("ROLE_OPERATOR")` e lança `IllegalStateException` se ela não existir no banco.

### 10.2. `GrantedAuthority`

A classe `Role` implementa `GrantedAuthority` e expõe `getAuthority()`.

### 10.3. Claim `authorities`

O claim `authorities` no JWT é montado a partir de `user.getAuthorities()` e injeta a lista de authorities no token.

### 10.4. `JwtGrantedAuthoritiesConverter`

Em [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java), a conversão faz:

```java
JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
grantedAuthoritiesConverter.setAuthorityPrefix("");
```

### 10.5. `@PreAuthorize`

Conforme os controllers, a autorização depende de `hasRole('ADMIN')`, `hasRole('OPERATOR')`, `isAuthenticated()` e combinações semelhantes.

Exemplos:

- [backend/src/main/java/com/albertsilva/dev/asjcatalog/web/controller/CategoryController.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/web/controller/CategoryController.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/web/controller/ProductController.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/web/controller/ProductController.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/web/controller/UserController.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/web/controller/UserController.java)

### 10.6. Regra do `PUT /api/v1/users/{id}` para OPERATOR

Confirmado pelo código e por execução.

`UserController.update` usa:

```java
@PreAuthorize("hasRole('ADMIN') OR (hasRole('OPERATOR') AND #id == authentication.principal.id)")
```

Numa requisição autenticada por JWT, `authentication.principal` é o objeto `Jwt`, e `principal.id` resolve para `Jwt.getId()`, isto é, o claim `jti` (texto aleatório), e não o claim `userId`. A comparação nunca é verdadeira: um OPERATOR recebe `403` mesmo ao alterar o próprio id. Execução: `PUT /api/v1/users/1` com o token de `albert@gmail.com` (id 1, só `ROLE_OPERATOR`) respondeu `403 ACCESS_DENIED`.

`UserController.findById` usa outra regra, que funciona:

```java
@PreAuthorize("hasRole('ADMIN') OR (hasRole('OPERATOR') AND @authenticatedUserService.isCurrentUser(#id))")
```

`isCurrentUser` lê o claim `userId`. Execução: `GET /api/v1/users/1` com o mesmo token respondeu `200`.

## 11. OAuth2 Scopes Contract

### 11.1. Scopes atuais

Scopes confirmados no cliente registrado:

- `read`
- `write`

Evidência: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)

### 11.2. Diferença atual entre scopes e authorities

A diferença real no projeto é:

- OAuth2 scopes: `read`, `write`
- Spring Security authorities: `ROLE_ADMIN`, `ROLE_OPERATOR`

Esse contraste existe no código e não foi corrigido. O projeto usa a lista de authorities para produzir o claim `authorities` e o Resource Server usa esse claim para criar `GrantedAuthority`.

Não foi implementado um mapeamento semântico formal entre os scopes OAuth2 e as roles do usuário.

### 11.3. Scopes autorizados sempre vazios

Confirmado pelo código em `CustomPasswordAuthenticationProvider`: os scopes autorizados são as authorities do usuário filtradas pelos scopes do cliente (`read`, `write`). Como nenhuma `ROLE_*` coincide com esses scopes, o conjunto é sempre vazio, e a resposta de `/oauth2/token` não traz o campo `scope`. Confirmado por execução.

## 12. Resource Server Contract

### 12.1. Configuração principal

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java)

### 12.2. Elementos confirmados

- `oauth2ResourceServer().jwt()`
- `JwtAuthenticationConverter`
- `JwtGrantedAuthoritiesConverter`
- `authoritiesClaimName("authorities")`
- `authorityPrefix("")`
- endpoints públicos e protegidos
- CORS
- CSRF desabilitado

### 12.3. Endpoints públicos confirmados

Regras de URL em `ResourceServerConfig` (constantes `PUBLIC_GET_ENDPOINTS`, `PUBLIC_POST_ENDPOINTS` e `DOCUMENTATION_OPENAPI`):

- `GET /api/v1/categories/**`
- `GET /api/v1/products/**`
- `GET /api/v1/accounts/**`
- `POST /api/v1/accounts/**`
- documentação, em qualquer método: `/docs-asjcatalog`, `/docs-asjcatalog/**`, `/docs-asjcatalog.html` e `/swagger-ui/**`

### 12.4. Endpoints protegidos

Todos os demais endpoints requerem autenticação conforme `anyRequest().authenticated()`. Sem token, a resposta é `401` sem corpo, com o cabeçalho `WWW-Authenticate: Bearer` (confirmado por execução).

**Rotas de conta protegidas só por `@PreAuthorize`.** Como `GET /api/v1/accounts/**` e `POST /api/v1/accounts/**` são públicos pela regra de URL, estes endpoints ficam protegidos apenas pela anotação:

| Endpoint | `@PreAuthorize` | Sem token (confirmado por execução) |
| --- | --- | --- |
| `GET /api/v1/accounts/me` | `isAuthenticated()` | `403 ACCESS_DENIED`, e não `401` |
| `POST /api/v1/accounts/deactivate` | `hasAnyRole('ADMIN', 'OPERATOR')` | `403 ACCESS_DENIED` |

A requisição anônima chega ao método, o `@PreAuthorize` lança `AccessDeniedException`, e o `ControllerExceptionHandler` a converte em `403`. Já `PUT /api/v1/accounts/me` e `PATCH /api/v1/accounts/me/password` não estão nas regras públicas (os métodos `PUT` e `PATCH` não são liberados) e respondem `401` sem token.

### 12.5. Fluxo real

```text
JWT
  ↓
JwtDecoder
  ↓
JwtAuthenticationConverter
  ↓
GrantedAuthority
  ↓
SecurityContext
  ↓
@PreAuthorize
  ↓
Controller
```

## 13. SecurityFilterChain Contract

### 13.1. Cadeias confirmadas

| Order | Matcher                         | Responsabilidade                       | Endpoint principal |
| ----- | ------------------------------- | -------------------------------------- | ------------------ |
| 1     | `PathRequest.toH2Console()`     | permitir console H2 em desenvolvimento | H2 console         |
| 2     | `/oauth2/**`, `/.well-known/**` | Authorization Server                   | `/oauth2/token`    |
| 3     | todas as requisições restantes  | Resource Server                        | `/api/**`          |

### 13.2. Descrição real

- `/oauth2/token` → Authorization Server
- `/oauth2/**` → Authorization Server
- `/.well-known/**` → Authorization Server
- `/api/**` → Resource Server
- Swagger → configurado como público conforme `requestMatchers(DOCUMENTATION_OPENAPI)`
- H2 → configurado em desenvolvimento, condicionado pela propriedade `spring.h2.console.enabled=true`

### 13.3. Ordem demonstrada

Confirmado por `@Order` e pela configuração da ordem dos beans em:

- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java)

### 13.4. Perfil `prod`

Confirmado em [backend/src/main/resources/application-prod.properties](../backend/src/main/resources/application-prod.properties):

| Item | Comportamento em `prod` |
| --- | --- |
| `security.client-id` | `${CLIENT_ID}`, sem valor padrão (obrigatório) |
| `security.client-secret` | `${CLIENT_SECRET}`, sem valor padrão (obrigatório) |
| `cors.origins` | `${CORS_ORIGINS}`, sem valor padrão (obrigatório) |
| Swagger/OpenAPI | Desligado: `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` |
| Console do H2 | Desligado: `spring.h2.console.enabled=false`. Com isso, a cadeia de `@Order(1)` não é criada (`@ConditionalOnProperty`) |

Os valores padrão `myclientid` e `myclientsecret` (em `application.properties`) valem apenas nos perfis `dev` e `test`.

Sem uma dessas variáveis, a aplicação não sobe: as propriedades são lidas com `@Value`, e o Spring interrompe a inicialização com `Could not resolve placeholder` (comportamento confirmado por execução com uma variável inexistente em `cors.origins`).

## 14. Refresh Token Contract

### 14.1. Etapas do refresh token

Separação exigida:

- geração
- persistência
- retorno
- validação
- uso
- invalidação

### 14.2. Geração

Confirmado pelo código em [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java):

- `OAuth2RefreshTokenGenerator`
- `refreshTokenContext`
- `tokenGenerator.generate(refreshTokenContext)`

Status: Confirmado pelo código.

### 14.3. Persistência

Confirmado pelo código:

- `authorizationBuilder.refreshToken(refreshToken);`
- `this.authorizationService.save(authorization);`

Implementação atual:

- `InMemoryOAuth2AuthorizationService`

Status: Confirmado pelo código.

### 14.4. Retorno

Confirmado em parte pelo código: o retorno do provider é `OAuth2AccessTokenAuthenticationToken(registeredClient, clientPrincipal, accessToken, refreshToken)`. Isso mostra que o refresh token é parte do objeto devolvido ao Spring durante a geração do token.

Status: Confirmado pelo código.

### 14.5. Validação e uso

Há suporte padrão do Spring Authorization Server para refresh token, mas não há implementação customizada específica no projeto.

Status: Comportamento delegado ao Spring Authorization Server.

### 14.6. Invalidação e rotação

Não há código customizado de invalidação. A rotação vem da configuração `reuseRefreshTokens(false)` em `TokenSettings`: a cada uso, o Spring Authorization Server emite um novo par (access token e refresh token), e o refresh token anterior deixa de valer.

Confirmado por teste em [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/oauth2/OAuth2TokenIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/oauth2/OAuth2TokenIT.java):

- `refreshTokenGrantShouldIssueNewAccessTokenAndRotateRefreshToken`: o refresh devolve access e refresh tokens diferentes dos originais e mantém os claims `username`, `userId` e `authorities`;
- `refreshTokenGrantShouldRejectReuseOfTheOldRefreshTokenAfterRotation`: reutilizar o refresh token antigo responde `400` com `invalid_grant`;
- `refreshTokenGrantShouldRejectInvalidRefreshToken`: refresh token inexistente responde `400` com `invalid_grant`.

Também confirmado por execução.

Status: Confirmado por teste.

### 14.7. TTL

Confirmado pelo código:

```java
.refreshTokenTimeToLive(Duration.ofDays(30))
.reuseRefreshTokens(false)
```

### 14.8. Observações importantes

- o refresh token é salvo em memória
- após reinício da aplicação, o estado em memória é perdido
- com múltiplas instâncias, o estado não é compartilhado
- o token renovado reaproveita o principal guardado na autorização no momento do login (`AuthenticatedUser` nos `details`), então as `authorities` do novo access token são as do login, e não as atuais do banco

## 15. Token Response Contract

### 15.1. Confirmado pelo código

A aplicação gera e retorna um `OAuth2AccessTokenAuthenticationToken` contendo:

- `accessToken`
- `refreshToken`

O token e o refresh token são processados pelo Spring Authorization Server para serialização da resposta HTTP.

### 15.2. Confirmado por teste

O teste existente [backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java) confirma que a resposta da requisição ao `/oauth2/token` é tratada como JSON e contém `access_token`.

O `OAuth2TokenIT` (`passwordGrantShouldIssueAccessAndRefreshTokenWithUserClaims`) confirma que a resposta traz `access_token` e `refresh_token` e que o JWT contém os claims `username`, `userId` e `authorities`.

Confirmado por execução: a resposta tem os campos `access_token`, `refresh_token`, `token_type` (`Bearer`) e `expires_in` (`86400` com o valor padrão). Não há campo `scope` (veja 11.3).

### 15.3. Gap de cobertura

Não foi encontrado teste que valide a estrutura completa do JSON final do endpoint `/oauth2/token` (presença de `token_type`, `expires_in` e ausência de `scope`).

O contrato JSON final não foi implementado manualmente no backend. A serialização é produzida pelo Spring Authorization Server.

## 16. Error Contract

### 16.1. Erros confirmados

| Erro                     | Origem    | Classe                                                | Condição                           | Comportamento                 |
| ------------------------ | --------- | ----------------------------------------------------- | ---------------------------------- | ----------------------------- |
| `invalid_request`        | converter | `CustomPasswordAuthenticationConverter`               | parâmetros inválidos ou duplicados | erro OAuth2 no token endpoint |
| `invalid_client`         | provider  | `CustomPasswordAuthenticationProvider`                | cliente não autenticado            | erro OAuth2                   |
| `invalid_grant`          | provider  | `CustomPasswordAuthenticationProvider`                | senha errada ou conta inválida     | erro OAuth2                   |
| `unauthorized_client`    | Spring    | Comportamento delegado ao Spring Authorization Server | cliente sem permissão para o grant | padrão do Spring              |
| `unsupported_grant_type` | Spring    | Comportamento delegado ao Spring Authorization Server | grant não suportado                | padrão do Spring              |
| `server_error`           | provider  | `CustomPasswordAuthenticationProvider`                | falha ao gerar token               | erro do servidor              |

### 16.2. Customização vs padrão

- customizado: `invalid_request`, `invalid_client`, `invalid_grant`, `server_error`
- delegação ao Spring: `unauthorized_client`, `unsupported_grant_type`

### 16.3. Mensagens fixas

As descrições dos erros de `/oauth2/token` são textos fixos em inglês no `CustomPasswordAuthenticationProvider`, e não passam pelo `MessageSource` (não são traduzidas pelo `Accept-Language`):

| Condição | `error` | `error_description` |
| --- | --- | --- |
| E-mail não cadastrado ou senha errada | `invalid_grant` | `Invalid credentials` |
| Conta inativa (`isEnabled() == false`) | `invalid_grant` | `Your account has not been activated yet. Please check your email.` |
| Conta bloqueada | `invalid_grant` | `Account is locked` |
| Conta expirada | `invalid_grant` | `Account expired` |

Todas usam `error_uri` = `https://datatracker.ietf.org/doc/html/rfc6749#section-5.2`.

Respostas confirmadas por execução:

- senha errada: `400` `{"error_description":"Invalid credentials","error":"invalid_grant","error_uri":"https://datatracker.ietf.org/doc/html/rfc6749#section-5.2"}`;
- e-mail com maiúscula (`Maria@gmail.com`): a mesma resposta, porque a consulta de login compara o e-mail com `=`;
- `client_secret` errado: `401` `{"error":"invalid_client"}`.

### 16.4. 401 do Resource Server

Não há `AuthenticationEntryPoint` customizado. Requisições sem token ou com token inválido em rotas protegidas recebem `401` sem corpo, só com o cabeçalho `WWW-Authenticate` (confirmado por execução). Exemplo com token malformado:

```text
WWW-Authenticate: Bearer error="invalid_token", error_description="An error occurred while attempting to decode the Jwt: Malformed token", error_uri="https://tools.ietf.org/html/rfc6750#section-3.1"
```

## 17. CORS Contract

### 17.1. Configuração real

Arquivo: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/resource/config/ResourceServerConfig.java)

Configuração confirmada:

- origins: `cors.origins`
- métodos: `POST`, `GET`, `PUT`, `DELETE`, `PATCH`
- headers: `Authorization`, `Content-Type`
- credentials: `true`
- filtro CORS com `Ordered.HIGHEST_PRECEDENCE`

Também há `http.cors(...)` no `SecurityFilterChain` e `FilterRegistrationBean<CorsFilter>`.

### 17.2. Impacto

O CORS do backend interfere com acessos de frontend e também com pré-flight de requisições ao `/oauth2/token` em contextos web.

## 18. Persistence Contract

### 18.1. `InMemoryOAuth2AuthorizationService`

Confirmado em [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java):

```java
@Bean
public OAuth2AuthorizationService authorizationService() {
  return new InMemoryOAuth2AuthorizationService();
}
```

### 18.2. O que é armazenado

- access token
- refresh token
- escopos autorizados
- principal
- grant type
- metadados de token

### 18.3. Impactos conhecidos

- reinício da aplicação apaga o estado
- múltiplas instâncias não compartilham autorização
- refresh token pode deixar de funcionar após reinício da aplicação
- a persistência do token depende do ciclo de vida da JVM
- a chave RSA de assinatura também é gerada em memória a cada subida (veja 9.1): após reinício, access tokens antigos são recusados pelo Resource Server

## 19. Test Contract

### 19.1. Testes relacionados à segurança encontrados

| Arquivo                                                                                                                                                                                                                   | Comportamento validado               | Contrato protegido                        |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | ----------------------------------------- |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java)                                                                   | gera token com `grant_type=password` | `/oauth2/token` e autenticação do cliente |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/CategoryControllerIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/CategoryControllerIT.java) | acesso com token JWT                 | Resource Server                           |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/ProductControllerIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/ProductControllerIT.java)   | acesso com token JWT                 | Resource Server                           |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/UserControllerIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/UserControllerIT.java)         | acesso autenticado e autorização     | `@PreAuthorize`                           |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/oauth2/OAuth2TokenIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/oauth2/OAuth2TokenIT.java) | `grant_type=password` emite access e refresh token com os claims `username`, `userId` e `authorities`; `grant_type=refresh_token` rotaciona o par; reuso do refresh antigo e refresh inexistente respondem `invalid_grant` | `/oauth2/token`, claims JWT, rotação do refresh token |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/security/ResourceServerAuthorizationIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/security/ResourceServerAuthorizationIT.java) | `401` sem token e com token inválido; ADMIN acessa rota só de ADMIN; OPERATOR recebe `403` nela; OPERATOR acessa o próprio `GET /users/{id}` e recebe `403` no de outro usuário; listagem pública de categorias sem token | Resource Server, `@PreAuthorize`, `isCurrentUser` |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/security/CatalogWriteAuthorizationIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/security/CatalogWriteAuthorizationIT.java) | criar, atualizar, remover, ativar e desativar categorias e produtos: `401` sem token e `403` sem role de escrita | Regras de escrita do catálogo |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/security/CorsIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/security/CorsIT.java) | pré-flight de origem permitida é aceito; de origem não permitida é recusado | CORS |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/account/AccountFlowIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/account/AccountFlowIT.java) | cadastro, ativação e login; login recusado antes da ativação; tokens de conta inexistentes, desativados, expirados ou de outro tipo; `GET`/`PUT /accounts/me` e `PATCH /accounts/me/password` com token | Fluxos de conta e `AuthenticatedUserService` |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationConverterTest.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationConverterTest.java) | criação do token do grant; scopes e parâmetros adicionais preservados; `null` para outro `grant_type`; `invalid_request` com `username` ou `password` ausentes e com `username`, `password` ou `scope` duplicados | `CustomPasswordAuthenticationConverter` (teste de unidade) |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProviderTest.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProviderTest.java) | autenticação válida; recusa de usuário inexistente, senha errada, conta inativa e cliente não autenticado; tipos de autenticação suportados | `CustomPasswordAuthenticationProvider` (teste de unidade) |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/security/auth/AuthenticatedUserServiceTest.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/security/auth/AuthenticatedUserServiceTest.java) | usuário carregado pelo claim `userId`; exceções para autenticação nula, principal que não é `Jwt`, claim ausente ou inválido e usuário inexistente; `isCurrentUser` verdadeiro e falso | `AuthenticatedUserService` (teste de unidade) |

### 19.2. Gaps de cobertura

Não foram encontrados testes explícitos para:

- validação completa do JSON final de `/oauth2/token` (`token_type`, `expires_in`, ausência de `scope`)
- `unauthorized_client`
- `unsupported_grant_type`
- `server_error`
- `PUT /api/v1/users/{id}` por um OPERATOR no próprio id (o problema descrito em 10.6 não é detectado pelos testes)
- `GET /api/v1/accounts/me` e `POST /api/v1/accounts/deactivate` sem token (resposta `403`, descrita em 12.4)
- comportamento após reinício (chave RSA e autorizações em memória)
- `authorities` do token renovado após mudança de roles no banco

## 20. Compatibility Contract

### NÃO QUEBRAR SEM ANÁLISE

Os itens abaixo são parte do contrato atual e devem ser preservados:

- `grant_type=password`
- autenticação do cliente
- `CustomPasswordAuthenticationConverter`
- `CustomPasswordAuthenticationProvider`
- `AuthenticatedUser`
- `SecurityContextHolder`
- `tokenCustomizer`
- claims JWT: `authorities`, `userId`, `username`
- `JwtAuthenticationConverter`
- `@PreAuthorize`
- `RegisteredClient`
- `TokenSettings`
- refresh token
- `OAuth2AuthorizationService`
- `SecurityFilterChain`
- CORS
- endpoints públicos

## 21. Change Impact Matrix

| Alteração                    | Componentes afetados                                              | Risco   | Testes necessários | Alteração coordenada? |
| ---------------------------- | ----------------------------------------------------------------- | ------- | ------------------ | --------------------- |
| `grant_type`                 | converter, provider, client registration                          | crítico | sim                | sim                   |
| autenticação do cliente      | `RegisteredClient`, Spring Authorization Server                   | crítico | sim                | sim                   |
| `RegisteredClient`           | client auth, grant types, scopes                                  | crítico | sim                | sim                   |
| claims JWT                   | `AuthorizationServerConfig`, `AuthenticatedUser`, Resource Server | crítico | sim                | sim                   |
| `authorities`                | JWT, Resource Server, `@PreAuthorize`                             | crítico | sim                | sim                   |
| `roles`                      | `User`, `Role`, JWT, controllers                                  | crítico | sim                | sim                   |
| `scopes`                     | `RegisteredClient`, provider, token context                       | alto    | sim                | sim                   |
| `AuthenticatedUser`          | JWT generation, `SecurityContext`                                 | crítico | sim                | sim                   |
| `SecurityContext`            | token customizer, JWT claims                                      | crítico | sim                | sim                   |
| `tokenCustomizer`            | JWT claims, userId, authorities                                   | crítico | sim                | sim                   |
| `JwtAuthenticationConverter` | Resource Server authorization                                     | crítico | sim                | sim                   |
| `TokenSettings`              | access/refresh TTL                                                | alto    | sim                | sim                   |
| refresh token                | generator, persistence, Authorization Server                      | alto    | sim                | sim                   |
| `OAuth2AuthorizationService` | authorization persistence                                         | crítico | sim                | sim                   |
| `SecurityFilterChain`        | routing de login e resource API                                   | crítico | sim                | sim                   |
| endpoints públicos           | segurança e acesso                                                | alto    | sim                | sim                   |
| CORS                         | browser + frontend                                                | médio   | sim                | sim                   |

## 22. Known Risks

### CRÍTICO

1. Acoplamento da geração do JWT ao `SecurityContextHolder` e ao `AuthenticatedUser`.
   - Local: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/grant/password/CustomPasswordAuthenticationProvider.java)
   - Risco: qualquer alteração no principal/details pode quebrar o token.

2. Uso de `InMemoryOAuth2AuthorizationService` e de chave RSA gerada em memória.
   - Local: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)
   - Risco: reinício da aplicação e múltiplas instâncias quebram o contrato de autorização; após reinício, todos os access e refresh tokens emitidos deixam de valer.

3. Inconsistência semântica entre OAuth2 scopes e Spring Security authorities.
   - Local: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)
   - Risco: a autorização depende de um mapeamento implícito.

### ALTO

1. Não há handler customizado específico para erros OAuth2; as mensagens de `/oauth2/token` são fixas em inglês (16.3).
2. O refresh token é gerado e persistido, mas o uso real depende do Spring Authorization Server.
3. O cliente e o usuário estão acoplados em um pipeline customizado que não é abstrato.
4. A regra de `PUT /api/v1/users/{id}` para OPERATOR usa `authentication.principal.id`, que no JWT é o claim `jti`, e nunca autoriza (10.6).

### MÉDIO

1. O `username` no JWT e no login é baseado no email.
2. Não há `issuer` e `audience` explicitamente configurados; `sub` e `aud` recebem o `client_id`, e não o usuário (9.6).
3. CORS usa duas camadas de configuração.
4. `GET /api/v1/accounts/me` e `POST /api/v1/accounts/deactivate` dependem só do `@PreAuthorize` e respondem `403` sem token, e não `401` (12.4).

### BAIXO

1. O projeto usa `authorityPrefix("")`, o que pode ser menos explícito em cenários externos.

## 23. Regras para futuras alterações

1. Identificar qual parte do contrato será alterada.
2. Identificar todos os consumidores dessa parte do contrato.
3. Identificar testes existentes que validam esse comportamento.
4. Identificar gaps de cobertura antes de editar qualquer coisa.
5. Fazer a menor alteração possível.
6. Não refatorar componentes não relacionados.
7. Executar os testes relevantes.
8. Comparar comportamento antes/depois.
9. Validar os claims JWT.
10. Validar autorização do Resource Server.
11. Validar login.
12. Validar refresh token quando afetado.

## 24. Regra Principal

Este documento descreve o comportamento atual, não necessariamente o comportamento arquiteturalmente ideal.

Não corrigir automaticamente um risco identificado. Uma correção pode alterar o contrato existente e deve ser tratada como uma mudança deliberada, isolada e testada.

## CHECKPOINT PARA FUTURAS ALTERAÇÕES

### 1. O que pode ser alterado isoladamente

- ajustes de documentação
- ajustes de logs/observabilidade
- mudanças em mensagens de erro sem alterar o tipo de erro
- ajustes de CORS, desde que o contrato de frontend e endpoints seja revisado

### 2. O que exige testes

- `SecurityFilterChain`
- `RegisteredClient`
- `CustomPasswordAuthenticationConverter`
- `CustomPasswordAuthenticationProvider`
- `TokenSettings`
- `JwtAuthenticationConverter`
- `@PreAuthorize`
- refresh token

### 3. O que exige alteração coordenada

- `authorities` e `scopes`
- JWT claims
- `AuthenticatedUser`
- `SecurityContext`
- `OAuth2AuthorizationService`
- `refresh_token`
- `RegisteredClient`

### 4. O que não deve ser alterado sem revisar toda a cadeia de autenticação

- `grant_type=password`
- `SecurityContextHolder`
- `AuthenticatedUser`
- `tokenCustomizer`
- `authorities` do JWT
- `RegisteredClient`
- `TokenSettings`
- `OAuth2AuthorizationService`
- `ResourceServerConfig`
- `@PreAuthorize`

## 25. Resumo final

Este contrato documenta o comportamento atual do backend do ASJCatalog. Ele foi construído somente com leitura do código real, das configurações e dos testes existentes. Nenhuma alteração foi aplicada ao backend, e nenhuma correção foi feita nos riscos identificados.

O documento serve como base para futuras alterações pontuais e para análise de compatibilidade antes de qualquer mudança em segurança, autenticação, autorização, JWT, refresh token, CORS e regras de acesso.
