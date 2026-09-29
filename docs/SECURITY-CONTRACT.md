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

Se qualquer um destas peças mudar:

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

Não foi configurado explicitamente no código. Não foi possível determinar com segurança a partir do código disponível.

## 10. Authorities Contract

As authorities confirmadas no sistema são:

- `ROLE_ADMIN`
- `ROLE_OPERATOR`
- `ROLE_USER`

### 10.1. Origem

- [backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/Role.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/Role.java)
- [backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/User.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/domain/user/User.java)

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

## 11. OAuth2 Scopes Contract

### 11.1. Scopes atuais

Scopos confirmados no cliente registrado:

- `read`
- `write`

Evidência: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)

### 11.2. Diferença actual entre scopes e authorities

A diferença real no projeto é:

- OAuth2 scopes: `read`, `write`
- Spring Security authorities: `ROLE_ADMIN`, `ROLE_OPERATOR`, `ROLE_USER`

Esse contraste existe no código e não foi corrigido. O projeto usa a lista de authorities para produzir o claim `authorities` e o Resource Server usa esse claim para criar `GrantedAuthority`.

Não foi implementado um mapeamento semântico formal entre os scopes OAuth2 e as roles do usuário.

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

- `GET /api/v1/categories/**`
- `GET /api/v1/products/**`
- `POST /api/v1/accounts/**`
- `/docs-asjcatalog/**`
- `/swagger-ui/**`

### 12.4. Endpoints protegidos

Todos os demais endpoints requerem autenticação conforme `anyRequest().authenticated()`.

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

Não foi encontrado código customizado para invalidação ou rotação de refresh token.

Status: Não confirmado pelo código disponível.

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

## 15. Token Response Contract

### 15.1. Confirmado pelo código

A aplicação gera e retorna um `OAuth2AccessTokenAuthenticationToken` contendo:

- `accessToken`
- `refreshToken`

O token e o refresh token são processados pelo Spring Authorization Server para serialização da resposta HTTP.

### 15.2. Confirmado por teste

O teste existente [backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java) confirma que a resposta da requisição ao `/oauth2/token` é tratada como JSON e contém `access_token`.

### 15.3. Gap de cobertura

Não foi encontrado teste explícito validando a estrutura completa do JSON final do endpoint `/oauth2/token`, incluindo todos os campos e suas presenças. Portanto, a resposta final precisa ser tratada com atenção.

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

## 19. Test Contract

### 19.1. Testes relacionados à segurança encontrados

| Arquivo                                                                                                                                                                                                                   | Comportamento validado               | Contrato protegido                        |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | ----------------------------------------- |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/utils/TokenUtil.java)                                                                   | gera token com `grant_type=password` | `/oauth2/token` e autenticação do cliente |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/CategoryControllerIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/CategoryControllerIT.java) | acesso com token JWT                 | Resource Server                           |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/ProductControllerIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/ProductControllerIT.java)   | acesso com token JWT                 | Resource Server                           |
| [backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/UserControllerIT.java](../backend/src/test/java/com/albertsilva/dev/asjcatalog/integrations/web/controller/UserControllerIT.java)         | acesso autenticado e autorização     | `@PreAuthorize`                           |

### 19.2. Gaps de cobertura

Não foram encontrados testes explícitos para:

- `grant_type=refresh_token`
- validação completa do JSON final de `/oauth2/token`
- `invalid_client`
- `unauthorized_client`
- `unsupported_grant_type`
- `server_error`
- invalidação e rotação de refresh token
- claims exatos no JWT serializado

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

2. Uso de `InMemoryOAuth2AuthorizationService`.
   - Local: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)
   - Risco: reinício da aplicação e múltiplas instâncias quebram o contrato de autorização.

3. Inconsistência semântica entre OAuth2 scopes e Spring Security authorities.
   - Local: [backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java](../backend/src/main/java/com/albertsilva/dev/asjcatalog/security/oauth2/authorization/config/AuthorizationServerConfig.java)
   - Risco: a autorização depende de um mapeamento implícito.

### ALTO

1. Não há handler customizado específico para erros OAuth2.
2. O refresh token é gerado e persistido, mas o uso real depende do Spring Authorization Server.
3. O cliente e o usuário estão acoplados em um pipeline customizado que não é abstrato.

### MÉDIO

1. O `username` no JWT e no login é baseado no email.
2. Não há `issuer` e `audience` explicitamente configurados.
3. CORS usa duas camadas de configuração.

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
