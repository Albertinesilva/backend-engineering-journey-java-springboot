# Testes automatizados

Este guia mostra como rodar os testes do ASJCatalog no capítulo 03, como eles estão organizados e como lidam com a segurança: usuários simulados nos testes de controller e tokens reais nos testes de integração.

## Sumário

1. [Como rodar os testes](#1-como-rodar-os-testes)
2. [Tipos de teste](#2-tipos-de-teste)
3. [Mapa dos pacotes de teste](#3-mapa-dos-pacotes-de-teste)
4. [Inventário das classes](#4-inventário-das-classes)
5. [Testes de controller e a segurança](#5-testes-de-controller-e-a-segurança)
6. [Testes de integração com token real](#6-testes-de-integração-com-token-real)
7. [Factories e dados de exemplo](#7-factories-e-dados-de-exemplo)
8. [O teste do 409 comentado](#8-o-teste-do-409-comentado)
9. [O que os testes de segurança não cobrem](#9-o-que-os-testes-de-segurança-não-cobrem)
10. [Padrões de escrita](#10-padrões-de-escrita)
11. [Limitações conhecidas](#11-limitações-conhecidas)

## 1. Como rodar os testes

Todos os comandos rodam dentro da pasta `backend`. O projeto traz o **Maven Wrapper** (`mvnw` e `mvnw.cmd`), então não é preciso instalar o Maven.

### Suíte padrão, no perfil test

**PowerShell**:

```powershell
cd backend
.\mvnw clean verify '-Dspring.profiles.active=test'
```

**bash**:

```bash
cd backend
./mvnw clean verify -Dspring.profiles.active=test
```

Resultado esperado: `Tests run: 134, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`. No PowerShell, o argumento vai entre aspas simples por causa do ponto no nome.

**Por que informar o perfil.** O perfil padrão deste capítulo é `dev`, que usa PostgreSQL (veja [CONFIGURATION.md](CONFIGURATION.md)). Sem `-Dspring.profiles.active=test`, o teste `AsjcatalogApplicationTests.contextLoads` tenta conectar ao banco `asjcatalog` e falha se ele não existir. Confirmado por execução, com o PostgreSQL rodando e sem o banco: `Tests run: 134, Errors: 1` e `BUILD FAILURE`, com a causa `FATAL: database ... does not exist`. Os outros 133 testes não dependem do banco do perfil: os de repositório usam um banco em memória, e os demais usam mocks.

Quem executa os testes no Maven é o **Surefire**, um plugin que, por padrão, só roda as classes cujo nome começa com `Test` ou termina em `Test`, `Tests` ou `TestCase`. Por isso as classes terminadas em `IT` (testes de integração) **não rodam** no `verify`.

### Testes de integração

```powershell
.\mvnw test '-Dtest=*IT' '-Dspring.profiles.active=test'
```

```bash
./mvnw test '-Dtest=*IT' -Dspring.profiles.active=test
```

Resultado esperado: `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0` e `BUILD SUCCESS`. Os testes que enviam usuários por HTTP precisam de acesso à rede (seção 11).

### Uma classe ou um método

```powershell
.\mvnw test '-Dtest=CategoryServiceTest' '-Dspring.profiles.active=test'
.\mvnw test '-Dtest=CategoryServiceTest$FindByIdOperations#findByIdShouldReturnCategoryWhenIdExists' '-Dspring.profiles.active=test'
```

```bash
./mvnw test -Dtest=CategoryServiceTest -Dspring.profiles.active=test
./mvnw test '-Dtest=CategoryServiceTest$FindByIdOperations#findByIdShouldReturnCategoryWhenIdExists' -Dspring.profiles.active=test
```

Os métodos ficam dentro de classes aninhadas (`@Nested`), por isso o filtro de um método precisa do nome da classe interna, separado por `$`. No bash, as aspas simples impedem que o `$` seja interpretado pelo terminal.

Os relatórios de cada execução ficam em `backend/target/surefire-reports`.

## 2. Tipos de teste

| Tipo | O que sobe | Neste projeto |
| --- | --- | --- |
| **Unitário** | Nada do Spring; as dependências são **mocks** (objetos falsos que simulam a dependência real) | Entidades e services |
| **Fatia** (*slice*) | Só uma camada do Spring: JPA (`@DataJpaTest`) ou MVC (`@WebMvcTest`) | Repositórios e controllers |
| **Integração** | A aplicação inteira, com banco H2, dados de exemplo e segurança real | Classes `*IT` e o teste de contexto |

## 3. Mapa dos pacotes de teste

Os testes ficam em `backend/src/test/java`, espelhando os pacotes do código principal:

```text
com.albertsilva.dev.asjcatalog
├── AsjcatalogApplicationTests        o contexto da aplicação sobe
├── entity                            CategoryTest, ProductTest, UserTest
├── factory                           CategoryFactory, ProductFactory, UserFactory
├── repository                        CategoryRepositoryTest, ProductRepositoryTest, UserRepositoryTest
├── service                           CategoryServiceTest, ProductServiceTest, UserServiceTest
├── web/controller                    CategoryControllerTest, ProductControllerTest, UserControllerTest
├── utils                             TokenUtil (obtém tokens reais)
└── integrations
    ├── common                        AbstractIT (base das ITs de controller)
    ├── service                       CategoryServiceIT, ProductServiceIT, UserServiceIT
    └── web/controller                CategoryControllerIT, ProductControllerIT, UserControllerIT
```

Não existe `src/test/resources`: os testes usam a configuração de `src/main/resources`.

## 4. Inventário das classes

| Classe | Tipo | Testes | O que cobre |
| --- | --- | --- | --- |
| `AsjcatalogApplicationTests` | `@SpringBootTest` | 1 | O contexto da aplicação sobe |
| `CategoryTest`, `ProductTest`, `UserTest` | Unitário | 6, 7, 10 | Construção, callbacks, `equals` e `hashCode`; em `UserTest`, também roles, `getUsername` (o e-mail), `getAuthorities` e `hasRole` |
| `CategoryRepositoryTest`, `ProductRepositoryTest` | `@DataJpaTest` | 10, 9 | Salvar, buscar, busca por nome, ordenação e exclusão |
| `UserRepositoryTest` | `@DataJpaTest` | 15 | Salvar, buscar por id e por e-mail, busca por nome, `existsByEmailIgnoreCase` e `existsByEmailIgnoreCaseAndIdNot`, exclusão |
| `CategoryServiceTest`, `ProductServiceTest` | Mockito | 16, 15 | Criar, buscar, pesquisar, atualizar, ativar, desativar e excluir |
| `UserServiceTest` | Mockito | 14 | Os mesmos casos, para usuários |
| `CategoryControllerTest` | `@WebMvcTest` sem segurança | 10 | POST, GET, PATCH e DELETE, com sucesso e 404 |
| `ProductControllerTest`, `UserControllerTest` | `@WebMvcTest` com segurança e `@WithMockUser` | 9, 12 | As rotas de cada controller, com sucesso e 404 |
| `CategoryServiceIT`, `ProductServiceIT`, `UserServiceIT` | `@SpringBootTest` | 13, 11, 15 | O service com o banco H2 e os dados de exemplo |
| `CategoryControllerIT`, `ProductControllerIT`, `UserControllerIT` | `@SpringBootTest` + token real | 14, 14, 15 | Requisições HTTP autenticadas, passando por todas as camadas |

Total: 134 testes no `verify` e 82 nas classes `*IT`. As ITs têm `@Transactional`: cada teste roda numa transação desfeita (*rollback*) no fim.

## 5. Testes de controller e a segurança

Os três testes de controller tratam a segurança de formas diferentes.

**`CategoryControllerTest`** desliga a segurança: `@WebMvcTest(value = CategoryController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class })`. As rotas são chamadas sem usuário, e o `@PreAuthorize` não é avaliado.

**`ProductControllerTest` e `UserControllerTest`** carregam a configuração de segurança real e simulam um usuário logado:

```java
@WebMvcTest(ProductController.class)
@Import({ ControllerExceptionHandler.class, ResourceServerConfig.class })
@ActiveProfiles("test")
@TestPropertySource(properties = { "spring.h2.console.enabled=false" })
@DisplayName("Tests for ProductController")
class ProductControllerTest {
```

```java
@Test
@DisplayName("Should create product successfully")
@WithMockUser(roles = { "ADMIN", "OPERATOR" })
void createShouldReturnCreatedProduct() throws Exception {

  ProductCreateRequest request = ProductFactory.createProductCreateRequest();

  when(productService.create(any(ProductCreateRequest.class))).thenReturn(productResponse);

  ResultActions resultActions = mockMvc.perform(post(BASE_URL)
      .with(csrf())
      .content(asJson(request))
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON));

  resultActions
      .andExpect(status().isCreated())
      .andExpect(header().exists("Location"))
      .andExpect(jsonPath("$.id").value(productResponse.id()))
      .andExpect(jsonPath("$.name").value(productResponse.name()))
      .andExpect(jsonPath("$.description").value(productResponse.description()))
      .andExpect(jsonPath("$.price").value(productResponse.price()));

  verify(productService).create(any(ProductCreateRequest.class));
}
```

| Recurso | Para que serve |
| --- | --- |
| `@WithMockUser(roles = {...})` | Do **Spring Security Test**: coloca um usuário falso, com as roles indicadas, no contexto de segurança do teste. Não há token nem login |
| `.with(csrf())` | Acrescenta um token CSRF à requisição simulada. A cadeia do Resource Server desliga o CSRF, então ele não é exigido |
| `@Import(ResourceServerConfig.class)` | Traz as cadeias de filtros e o `@EnableMethodSecurity`, para que o `@PreAuthorize` seja avaliado |
| `@TestPropertySource(... h2.console.enabled=false)` | Desliga o console do H2 e, com ele, a cadeia de filtros do console |

`UserControllerTest` também cria mocks de `UserRepository` e `RoleRepository` com `@MockitoBean`, porque os validadores de usuário (`@UniqueEmail`, `@ValidRoles`, `@UserUpdateValid`) recebem esses repositórios e são executados nas requisições com corpo.

## 6. Testes de integração com token real

As ITs de controller estendem `AbstractIT` (`@SpringBootTest` + `@AutoConfigureMockMvc`) e obtêm um token de verdade em `/oauth2/token`, pelo `TokenUtil`:

```java
@BeforeEach
void setUp() throws Exception {
  totalCategoriesCount = categoryRepository.count();

  username = "maria@gmail.com";
  password = "123456";
  bearerToken = tokenUtil.obtainAccessToken(mockMvc, username, password);
}
```

O `TokenUtil` faz o mesmo login descrito em [AUTHENTICATION.md](AUTHENTICATION.md#3-login): `grant_type=password`, client autenticado por Basic Auth (`httpBasic(clientId, clientSecret)`) e devolve o `access_token`. Cada requisição do teste envia o token pelo método `bearerToken()` do `AbstractIT`, que acrescenta o cabeçalho `Authorization: Bearer ...`:

```java
ResultActions resultActions = mockMvc.perform(post(BASE_URL)
    .with(bearerToken())
    .content(jsonRequest)
    .contentType(MediaType.APPLICATION_JSON)
    .accept(MediaType.APPLICATION_JSON));
```

As três ITs de controller usam `maria@gmail.com`, que tem a role ADMIN. Nenhuma IT usa o token de `albert@gmail.com` (só OPERATOR).

## 7. Factories e dados de exemplo

As factories (pacote `factory`) montam objetos de teste e guardam constantes ligadas ao `import.sql`:

| Constante | Valor | Significado |
| --- | --- | --- |
| `CategoryFactory.EXISTING_ID` | `1` | Categoria `Books` |
| `CategoryFactory.DEPENDENT_ID` | `1` | `Books`, que tem produtos vinculados |
| `CategoryFactory.NON_DEPENDENT_ID` | `11` | `Beauty`, sem produtos |
| `CategoryFactory.COUNT_TOTAL_CATEGORIES` | `15` | Total de categorias |
| `ProductFactory.COUNT_TOTAL_PRODUCTS` | `25` | Total de produtos |
| `UserFactory.EXISTING_ID` | `1` | `albert@gmail.com` |
| `UserFactory.COUNT_TOTAL_USERS` | `2` | Total de usuários |
| `UserFactory.ACTIVE_USER_ID` | `1` | Usado no teste de ativação |
| `UserFactory.INACTIVE_USER_ID` | `2` | Usado no teste de desativação. O usuário 2 (maria) está ativo nos dados de exemplo; o nome da constante descreve o resultado esperado, não o estado inicial |
| `UserFactory.EXISTING_EMAIL` | `"maria@gmail.com"` | Declarada, mas não usada |

As requisições de usuário das factories usam a senha `JAVA!@#ResTIc18`, que passa nas regras de senha forte, e e-mails `@gmail.com`, que passam na consulta MX (veja [VALIDATION.md](VALIDATION.md#6-validação-de-e-mail)).

## 8. O teste do 409 comentado

Em `CategoryControllerIT`, o teste `deleteShouldReturnBadRequestWhenCategoryHasAssociatedProducts`, que esperava 409 ao apagar uma categoria com produtos, está **comentado** (`// @Test`). No capítulo 02 ele falhava: o `@Transactional` do teste impede que o `DELETE` chegue ao banco antes do fim da transação, e a resposta era 204 em vez de 409. A API em si responde 409 nesse caso (confirmado por execução; veja [API-ENDPOINTS.md](API-ENDPOINTS.md#5-exemplos)). No capítulo 04, o teste não existe mais.

## 9. O que os testes de segurança não cobrem

- **Nenhum teste verifica 401 ou 403.** Não há asserções `isUnauthorized()` nem `isForbidden()`. As ITs usam sempre um ADMIN com token válido, e os testes de controller usam `@WithMockUser` com a role exigida.
- **Nenhum teste verifica o login com credenciais erradas**, o conteúdo do JWT ou o CORS.
- **Nenhum teste verifica a regra "o próprio usuário"** de `GET` e `PUT /users/{id}`, que não funciona (veja [AUTHENTICATION.md](AUTHENTICATION.md#11-limitações-conhecidas)).

## 10. Padrões de escrita

Os padrões do capítulo 02 continuam: Arrange, Act, Assert; `@DisplayName` em inglês; classes `@Nested` por operação; nomes no formato `<ação>Should<resultado>When<cenário>`. Os testes de `UserControllerTest` usam outra variação, que começa por `should` (`shouldCreateUserAndReturn201WithLocationHeader`).

As ITs de controller passaram a usar `assertEquals` em vez da palavra-chave `assert` do Java, que dependia da opção `-ea` da JVM.

## 11. Limitações conhecidas

- **O `verify` falha no perfil padrão sem PostgreSQL** (seção 1). É preciso informar `-Dspring.profiles.active=test`. A escolha do perfil por variável de ambiente (`APP_PROFILE`) chega no capítulo 04.
- **Os testes de integração não rodam no `verify`.** O `pom.xml` não tem o **Failsafe**, o plugin do Maven que executa as classes `*IT`. O Failsafe entra no capítulo 04.
- **Os testes de usuário por HTTP dependem de DNS.** O `@ValidEmail` consulta o registro MX do domínio, e sem acesso à rede nenhum e-mail é aceito. A validação só roda nas requisições HTTP com corpo: os `POST` e `PUT` de `UserControllerTest` (3 requisições) e de `UserControllerIT` (4). Os testes de service chamam o service diretamente, sem `@Valid`, e não fazem a consulta. Não executei a suíte sem rede para confirmar o efeito.
- **Sem testes de 401 e 403** (seção 9).
- **Um teste comentado** (seção 8).
- **`CategoryControllerTest` não testa a segurança**, porque a desliga.
- **`UserFactory.EXISTING_EMAIL` não é usada** e `INACTIVE_USER_ID` aponta para um usuário ativo (seção 7).
