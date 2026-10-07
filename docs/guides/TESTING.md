# Testes

⬅️ Anterior: [Fluxos de conta](ACCOUNT-FLOWS.md) · [🏠 Índice](../HOME.md) · Próximo: [Convenções](CONVENTIONS.md) ➡️

Este guia explica como os testes do ASJCatalog estão organizados, como rodá-los e o que cada grupo cobre.

## Sumário

1. [Tipos de teste](#1-tipos-de-teste)
2. [Surefire e Failsafe](#2-surefire-e-failsafe)
3. [Comandos](#3-comandos)
4. [Perfil test, H2 e import.sql](#4-perfil-test-h2-e-importsql)
5. [Infraestrutura de teste](#5-infraestrutura-de-teste)
6. [O que cada grupo cobre](#6-o-que-cada-grupo-cobre)
7. [Convenções](#7-convenções)
8. [Dependências externas](#8-dependências-externas)
9. [Limitações conhecidas](#9-limitações-conhecidas)

## 1. Tipos de teste

| Tipo | O que testa | Como | Velocidade |
| --- | --- | --- | --- |
| **Unidade** | Uma classe isolada | As dependências são substituídas por **mocks** (objetos falsos, criados com o Mockito, que devolvem o que o teste mandar) | Rápido |
| **Integração** | Várias camadas juntas, com o contexto do Spring e o banco H2 | Sobe a aplicação dentro do teste e faz requisições HTTP simuladas com o **MockMvc** | Mais lento |

Hoje são **301 testes** executados na fase de unidade e **150** na fase de integração (resultado do `./mvnw verify`).

## 2. Surefire e Failsafe

O Maven usa dois plugins, que escolhem as classes pelo **nome**:

| Plugin | Classes | Fase do Maven | Configuração |
| --- | --- | --- | --- |
| **Surefire** | Terminadas em `Test` ou `Tests` | `test` | Padrão do Spring Boot |
| **Failsafe** | Terminadas em `IT` | `integration-test` e `verify` | Declarado no `pom.xml` |

Consequências:

- `./mvnw test` roda só o Surefire;
- `./mvnw verify` roda o Surefire e depois o Failsafe. Se o Surefire falhar, o Maven para e o Failsafe não roda;
- o nome decide o plugin, e não o tipo do teste. Por isso `AsjcatalogApplicationTests` e os testes de repositório (`@DataJpaTest`), que sobem partes do Spring, rodam no Surefire.

## 3. Comandos

Os comandos partem da pasta `backend`. No PowerShell, argumentos `-D` com ponto no nome vão entre aspas simples.

| Objetivo | PowerShell | bash |
| --- | --- | --- |
| Só testes de unidade (Surefire) | `.\mvnw test` | `./mvnw test` |
| Todos os testes | `.\mvnw verify` | `./mvnw verify` |
| Só os testes de integração (`*IT`) | `.\mvnw verify '-Dtest=NenhumTeste' '-Dsurefire.failIfNoSpecifiedTests=false'` | `./mvnw verify -Dtest=NenhumTeste -Dsurefire.failIfNoSpecifiedTests=false` |
| Uma classe de unidade | `.\mvnw test '-Dtest=StrongPasswordValidatorTest'` | `./mvnw test -Dtest=StrongPasswordValidatorTest` |
| Uma classe de integração | `.\mvnw verify '-Dtest=NenhumTeste' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dit.test=CategoryControllerIT'` | `./mvnw verify -Dtest=NenhumTeste -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=CategoryControllerIT` |

Como funciona o comando "só os testes de integração": `-Dtest` filtra o Surefire, e `NenhumTeste` não corresponde a nenhuma classe; `-Dsurefire.failIfNoSpecifiedTests=false` impede que isso seja tratado como erro. O Failsafe usa outro filtro (`-Dit.test`), e por isso roda todos os `*IT`.

Resultados confirmados: só os `*IT`, 150 testes; só `CategoryControllerIT`, 15 testes; só `StrongPasswordValidatorTest`, 12 testes.

Os relatórios ficam em `backend/target/surefire-reports` e `backend/target/failsafe-reports`.

## 4. Perfil test, H2 e import.sql

Todas as classes de teste que sobem o contexto do Spring usam `@ActiveProfiles("test")`. O perfil `test` (detalhado em [CONFIGURATION.md](CONFIGURATION.md#3-comparação-entre-os-perfis)):

- usa o **H2** em memória (`jdbc:h2:mem:testdb`), um banco criado vazio na memória e apagado ao final;
- desliga o Flyway;
- deixa o Hibernate criar as tabelas a partir das entidades e, em seguida, executar o `import.sql`, que insere os mesmos dados de exemplo do perfil `dev` (44 categorias, 163 produtos, 2 usuários e 2 roles). Veja [DATABASE-MIGRATIONS.md](DATABASE-MIGRATIONS.md#5-o-importsql-do-perfil-test).

Por isso os testes de integração contam com dados existentes: por exemplo, a usuária `maria@gmail.com` (ADMIN) e o usuário `albert@gmail.com` (OPERATOR), ambos com a senha `123456`.

**Isolamento.** A maioria das classes de integração tem `@Transactional`. Nesse caso, o Spring abre uma transação para cada teste e a desfaz (*rollback*) ao final, e o que um teste grava não aparece no seguinte.

## 5. Infraestrutura de teste

**`integrations/common/AbstractIT`.** Classe base da maioria dos testes de integração, com `@SpringBootTest` (sobe a aplicação inteira), `@AutoConfigureMockMvc` e `@ActiveProfiles("test")`. Ela oferece às subclasses:

| Membro | Para quê |
| --- | --- |
| `mockMvc` | Fazer requisições HTTP simuladas |
| `objectMapper` e `asJson(objeto)` | Converter objetos em JSON para o corpo da requisição |
| `tokenUtil` | Obter tokens de acesso |
| `bearerToken` e `bearerToken()` | Guardar o token do teste e acrescentar o cabeçalho `Authorization: Bearer ...` numa requisição |

Os testes `integrations/service/*ServiceIT` não estendem `AbstractIT`: usam `@SpringBootTest` direto e chamam os services, sem HTTP.

**`utils/TokenUtil`.** Componente de teste que faz o login de verdade: envia `POST /oauth2/token` pelo MockMvc, com `grant_type=password`, e-mail e senha, autenticando o cliente por HTTP Basic com `security.client-id` e `security.client-secret`. Devolve o `access_token` da resposta. Uso típico: `tokenUtil.obtainAccessToken(mockMvc, "maria@gmail.com", "123456")`.

**`factory/*Factory`.** Métodos estáticos que criam objetos de teste prontos, para não repetir construções em cada teste:

| Fábrica | Exemplos de métodos |
| --- | --- |
| `CategoryFactory` | `createCategory()`, `createCategoryCreateRequest()`, `createCategoryResponse()` |
| `ProductFactory` | `createProduct()`, `createProductCreateRequest()`, `createProductDetailsResponse()` |
| `UserFactory` | `createUser()`, `createUserCreateRequest()`, `createUserDetailsResponse()` |
| `RoleFactory` | `createOperatorRoleResponse()`, `createAdminRoleResponse()` |

**Mocks de e-mail.** Nenhum teste envia e-mail de verdade:

- `service/EmailServiceTest` (unidade) usa mocks de `JavaMailSender`, `SpringTemplateEngine` e `EmailRepository`;
- `integrations/account/AccountFlowIT` troca o `JavaMailSenderImpl` do contexto por um mock com `@MockitoBean`, que devolve uma mensagem vazia em `createMimeMessage()`. Assim, o fluxo de cadastro roda inteiro sem servidor SMTP.

Um **`@MockitoBean`** substitui um bean do contexto do Spring por um mock durante o teste.

## 6. O que cada grupo cobre

**Testes de unidade (Surefire):**

| Pacote | Classes | Tipo | O que cobre |
| --- | --- | --- | --- |
| `domain` | `CategoryTest`, `ProductTest`, `UserTest` | Java puro | Construtores, callbacks `@PrePersist`/`@PreUpdate`, `equals` e regras das entidades |
| `repository` | 6 classes `*RepositoryTest` | `@DataJpaTest` (sobe só a camada JPA, com H2) | Consultas derivadas, JPQL e nativas |
| `service` | 6 classes `*ServiceTest` | Mockito | Regras de negócio de cada service, com repositórios e outros services falsos |
| `validation` | 10 classes `*ValidatorTest` | Mockito | Cada validador customizado, incluindo senha forte, dados pessoais e e-mail |
| `security` | `CustomPasswordAuthenticationConverterTest`, `CustomPasswordAuthenticationProviderTest`, `AuthenticatedUserServiceTest` | Mockito ou Java puro | Leitura dos parâmetros do login, regras do login e leitura do usuário a partir do JWT |
| `web.controller` | 4 classes `*ControllerTest` | `@WebMvcTest` (sobe só a camada web) com os services como `@MockitoBean` | Rotas, status HTTP e JSON dos controllers |
| `web.exception.handler` | `ControllerExceptionHandlerTest` | Java puro | Montagem das respostas de erro |
| `i18n` | `MessagesPropertiesTest` | Java puro | Consistência dos arquivos de mensagens (veja [INTERNATIONALIZATION.md](INTERNATIONALIZATION.md#7-testes)) |
| raiz | `AsjcatalogApplicationTests` | `@SpringBootTest` | O contexto da aplicação sobe |

**Testes de integração (Failsafe):**

| Classe | O que cobre |
| --- | --- |
| `web/controller/CategoryControllerIT`, `ProductControllerIT`, `UserControllerIT` | Endpoints de cada recurso ponta a ponta, com token real e banco H2 |
| `service/CategoryServiceIT`, `ProductServiceIT`, `UserServiceIT` | Services com o banco real (H2), incluindo as consultas nativas |
| `security/ResourceServerAuthorizationIT` | 401 sem token ou com token inválido; ADMIN e OPERATOR em rotas restritas; acesso ao próprio usuário |
| `security/CatalogWriteAuthorizationIT` | Escrita no catálogo: 401 sem token e 403 sem role de escrita |
| `security/CorsIT` | Pré-flight de CORS de origem permitida e não permitida |
| `oauth2/OAuth2TokenIT` | Login, claims do JWT e rotação do refresh token |
| `account/AccountFlowIT` | Cadastro, ativação, login, reenvio, recuperação e redefinição de senha, tokens inválidos e endpoints `/me` |
| `i18n/AcceptLanguageIT` | Respostas em inglês, espanhol e português conforme o `Accept-Language` |

## 7. Convenções

Nomes de método no padrão `<ação>Should<resultado>When<condição>`, `@DisplayName` em inglês e `@Nested` para agrupar testes do mesmo método. Detalhes em [CONVENTIONS.md](CONVENTIONS.md#5-testes).

## 8. Dependências externas

**Servidor SMTP: não é necessário.** O `application.properties` liga `spring.mail.test-connection`, que faz a subida testar a conexão com o servidor de e-mail. O `application-test.properties` desliga esse teste (`spring.mail.test-connection=false`), e nenhum teste envia e-mail de verdade (seção 5). Por isso **os testes não precisam das variáveis `MAIL_USERNAME` e `MAIL_PASSWORD`**: sem elas, `./mvnw verify` passou com 301 e 150 testes, sem erros.

**DNS (internet).** Esta é a única dependência externa dos testes. O validador `@ValidEmail` consulta o registro MX do domínio do e-mail pela internet (veja [VALIDATION.md](VALIDATION.md#6-validação-de-e-mail)). Testes que dependem disso:

- `ValidEmailValidatorTest`, que valida `user@gmail.com` consultando o DNS real;
- testes de integração que enviam e-mails para endpoints com `@ValidEmail`, como o `AccountFlowIT` (cadastro com `joana.flowit@gmail.com`).

Sem acesso ao DNS, esses testes falham, porque o e-mail é considerado inválido.

## 9. Limitações conhecidas

- **Os testes dependem de internet** para a consulta de DNS da validação de e-mail.
- **Flyway não é testado.** O perfil `test` cria as tabelas a partir das entidades, e não pelas migrations. Um erro numa migration só aparece ao subir nos perfis `dev` ou `prod`.
- **Comportamentos sem teste.** Não há teste para `PUT /users/{id}` feito por um OPERATOR no próprio id, para `GET /accounts/me` sem token, para `GET /products?sort=price` nem para o comportamento após reiniciar a aplicação (tokens invalidados). Veja as limitações em [API-ENDPOINTS.md](API-ENDPOINTS.md#9-limitações-conhecidas) e [AUTHENTICATION.md](AUTHENTICATION.md#10-limitações-conhecidas).

---

⬅️ Anterior: [Fluxos de conta](ACCOUNT-FLOWS.md) · [🏠 Índice](../HOME.md) · Próximo: [Convenções](CONVENTIONS.md) ➡️
