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
7. [Padrão de qualidade dos testes](#7-padrão-de-qualidade-dos-testes)
8. [Dependências externas](#8-dependências-externas)
9. [Cobertura com JaCoCo](#9-cobertura-com-jacoco)
10. [Limitações conhecidas](#10-limitações-conhecidas)

## 1. Tipos de teste

| Tipo | O que testa | Como | Velocidade |
| --- | --- | --- | --- |
| **Unidade** | Uma classe isolada | As dependências são substituídas por **mocks** (objetos falsos, criados com o Mockito, que devolvem o que o teste mandar) | Rápido |
| **Integração** | Várias camadas juntas, com o contexto do Spring e o banco H2 | Sobe a aplicação dentro do teste e faz requisições HTTP simuladas com o **MockMvc** | Mais lento |

Hoje são **471 testes** executados na fase de unidade e **165** na fase de integração (resultado do `./mvnw verify`). A cobertura que eles alcançam é medida pelo JaCoCo ([seção 9](#9-cobertura-com-jacoco)).

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
| Teste opcional contra o DNS real (precisa de internet) | `.\mvnw test '-Dtest=JndiMxRecordLookupDnsTest' '-Dsurefire.excludedGroups=none'` | `./mvnw test -Dtest=JndiMxRecordLookupDnsTest -Dsurefire.excludedGroups=none` |
| Todos os testes com o DNS simulado fora do ar | `.\mvnw clean verify '-Djava.naming.provider.url=dns://127.0.0.1:9'` | `./mvnw clean verify -Djava.naming.provider.url=dns://127.0.0.1:9` |

Como funciona o comando "só os testes de integração": `-Dtest` filtra o Surefire, e `NenhumTeste` não corresponde a nenhuma classe; `-Dsurefire.failIfNoSpecifiedTests=false` impede que isso seja tratado como erro. O Failsafe usa outro filtro (`-Dit.test`), e por isso roda todos os `*IT`.

Resultados confirmados: só os `*IT`, 165 testes; só `CategoryControllerIT`, 15 testes; só `StrongPasswordValidatorTest`, 12 testes.

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
| `TokenFactory` | `createActivationToken(user)`, `createPasswordRecoveryToken(user)` |
| `UserDetailsFactory` | `createOperatorProjections()`, `createOperatorAndAdminProjections()` |

As fábricas também guardam as constantes dos testes. Algumas são valores fixos do seed (`import.sql`), como `UserFactory.ADMIN_USER_ID`, `OPERATOR_EMAIL` e `SEED_PASSWORD`, e os totais `COUNT_TOTAL_*`; o JavaDoc delas cita a #21, que trata desse acoplamento.

**Mocks de e-mail.** Nenhum teste envia e-mail de verdade:

- `service/EmailServiceTest` (unidade) usa mocks de `JavaMailSender`, `SpringTemplateEngine` e `EmailRepository`;
- `integrations/account/AccountFlowIT` troca o `JavaMailSenderImpl` do contexto por um mock com `@MockitoBean`, que devolve uma mensagem vazia em `createMimeMessage()`. Assim, o fluxo de cadastro roda inteiro sem servidor SMTP.

Um **`@MockitoBean`** substitui um bean do contexto do Spring por um mock durante o teste.

**Consulta de MX substituída.** O `@ValidEmail` não consulta o DNS diretamente: ele recebe um `MxRecordLookup` (pacote `validation.user.dns`), cuja implementação de produção é o `JndiMxRecordLookup`. Nos testes:

- `ValidEmailValidatorTest` usa um mock de `MxRecordLookup`;
- `JndiMxRecordLookupTest` testa a consulta JNDI com um `DirContext` falso (domínio com MX, sem MX, MX vazio e falha de consulta), sem rede;
- `utils/MxRecordLookupTestConfig` (`@TestConfiguration`) registra um `MxRecordLookup` `@Primary` que considera que todo domínio tem MX. Ele é importado pelo `AbstractIT` (todos os testes de integração com MockMvc) e pelo `UserControllerTest` e pelo `AccountControllerTest`. Nesses dois `@WebMvcTest`, o import é obrigatório: o `@WebMvcTest` não carrega `@Component` comuns, e sem ele o validador ficaria sem a dependência.

O formato do e-mail continua validado pela expressão regular real; só a consulta de MX é substituída.

## 6. O que cada grupo cobre

**Testes de unidade (Surefire):**

| Pacote | Classes | Tipo | O que cobre |
| --- | --- | --- | --- |
| `domain` | `CategoryTest`, `ProductTest`, `UserTest`, `TokenTest`, `EmailTest`, `RoleTest` e `EntityIdentityContractTest` | Java puro | Construtores, callbacks `@PrePersist`/`@PreUpdate` e regras das entidades; o contrato de `equals`/`hashCode` das seis entidades fica num único teste parametrizado |
| `mapper` | `CategoryMapperTest`, `ProductMapperTest`, `UserMapperTest` | Java puro | Conversões entre entidades e DTOs, inclusive as atualizações parciais |
| `repository` | 6 classes `*RepositoryTest` | `@DataJpaTest` (sobe só a camada JPA, com H2) | Consultas derivadas, JPQL e nativas |
| `service` | 6 classes `*ServiceTest` | Mockito | Regras de negócio de cada service, com repositórios e outros services falsos |
| `validation` | 11 classes `*ValidatorTest`, `JndiMxRecordLookupTest` e `JndiMxRecordLookupDnsTest` (opcional, [seção 8](#8-dependências-externas)) | Mockito | Cada validador customizado, incluindo senha forte, dados pessoais e e-mail, e a consulta de MX |
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

## 7. Padrão de qualidade dos testes

Regras que todo teste novo ou alterado segue. Cada regra traz um exemplo de "evitar" e "preferir" tirado do próprio projeto. O `UserServiceTest` é a referência de estrutura, nomes e dados.

### 7.1 Nomes e `@DisplayName`

- Nome do método: `<método>Should<resultado>When<condição>`. O `<método>` é o método testado (no teste HTTP, o método do controller, como `create` ou `findById`). O `When<condição>` só fica de fora quando o comportamento não depende de condição, como em `allMessageFilesShouldContainSameKeys`.
- `@DisplayName` em inglês, dizendo **o mesmo** que o nome:
  - teste de unidade: `"<método> should <resultado> when <condição>"`;
  - teste HTTP: `"<VERBO> <rota> should <resultado> when <condição>"`.
- `@Nested`: `"<Método> Operations"` nos testes de unidade e `"<VERBO> <rota>"` nos testes HTTP. Classe: `"Tests for <Classe>"`.
- Se o teste espera `UsernameNotFoundException`, o nome não pode dizer `ResourceNotFoundException`: nome e `@DisplayName` descrevem exatamente o que é conferido.

```java
// Evitar: não diz qual método é testado
void shouldReturnCategoryWhenIdExists()

// Preferir
void findByIdShouldReturnCategoryWhenIdExists()
```

```java
// Evitar: igual em três operações; no relatório não se sabe qual falhou
@DisplayName("Should return 404 when id does not exist")

// Preferir
@DisplayName("DELETE /categories/{id} should return 404 when id does not exist")
```

### 7.2 Estrutura

- Comentários `// Arrange`, `// Act` e `// Assert` separam preparação, execução e conferência. Quando a execução e a conferência são a mesma linha (exceção), use `// Act & Assert`.
- Um `@Nested` por método ou endpoint.
- **Um comportamento por teste.** Dois cenários (por exemplo, "usuário inativo" e "usuário já ativo") são dois testes. Nos testes de integração de fluxo, como o `AccountFlowIT`, vários passos no mesmo teste são aceitos, porque o fluxo é o comportamento testado.
- Classes e métodos de teste sem `public`: o JUnit 5 não exige o modificador.

```java
// Evitar
public class CategoryTest {

// Preferir
class CategoryTest {
```

### 7.3 Asserções

**Biblioteca.** Os testes usam o **AssertJ** (`assertThat`), que já vem no `spring-boot-starter-test`. A regra vale para os testes novos e para cada classe que for retrabalhada; as classes que ainda usam as `Assertions` do JUnit migram quando forem alteradas, sem uma migração em massa. **Dentro de uma mesma classe, não misture os dois estilos.** Nos testes HTTP, a conferência da resposta continua com o `andExpect` do MockMvc.

| JUnit | AssertJ |
| --- | --- |
| `assertEquals(expected, result)` | `assertThat(result).isEqualTo(expected)` |
| `assertTrue(user.isActive())` | `assertThat(user.isActive()).isTrue()` |
| `assertThrows(...)` + `assertEquals(..., e.getMessage())` | `assertThatThrownBy(() -> ...).isInstanceOf(...).hasMessage(...)` |

**Compare o valor esperado por inteiro.** Conferir só que algo existe não prova que está certo.

```java
// Evitar
assertNotNull(category.getCreatedAt());
resultActions.andExpect(header().exists("Location"));

// Preferir
assertThat(result).isEqualTo(expectedResponse);
resultActions.andExpect(header().string("Location", "http://localhost/api/v1/users/1"));
```

**Exceções: confira o tipo e a mensagem completa.** As exceções de negócio carregam a chave da mensagem; compare a chave inteira. Mensagens montadas com valores são comparadas pelo texto completo, montado com as mesmas constantes.

```java
// Evitar: qualquer ResourceNotFoundException passaria
assertThrows(ResourceNotFoundException.class, () -> service.findById(NON_EXISTING_ID));

// Preferir
assertThatThrownBy(() -> service.findById(NON_EXISTING_ID))
    .isInstanceOf(ResourceNotFoundException.class)
    .hasMessage("error.user.notFound");

assertThatThrownBy(() -> service.loadUserByUsername(NON_EXISTING_EMAIL))
    .isInstanceOf(UsernameNotFoundException.class)
    .hasMessage("User not found with email: " + NON_EXISTING_EMAIL);
```

**`verify` e `never`.** O `verify` do Mockito confere que um método do mock foi chamado. Use-o com o argumento esperado sempre que ele for conhecido; `any()` só quando o argumento não importa ou é criado dentro do código testado (nesse caso, capture-o com `ArgumentCaptor` e confira os campos). Nos caminhos de erro, `never()` confirma que não houve efeito colateral, como uma gravação.

```java
// Evitar
verify(userService).create(any(UserCreateRequest.class));

// Preferir
verify(userService).create(request);
verify(repository, never()).save(any());
```

### 7.4 Dados de teste

- Dados vêm das fábricas do pacote `factory` (`UserFactory`, `RoleFactory`, `CategoryFactory`, `ProductFactory`, `UserDetailsFactory`) e das constantes delas, como `EXISTING_ID`, `NON_EXISTING_EMAIL` e `RoleFactory.OPERATOR_ROLE_ID`.
- **Sem valores mágicos**: ids, e-mails, nomes, senhas e roles escritos à mão no teste.
- Se faltar uma constante, acrescente-a na fábrica correspondente, com um nome que diga o que ela representa. Não crie constantes que nenhum teste usa.

```java
// Evitar
Role role = new Role(1L, "ROLE_OPERATOR");
when(roleRepository.existsById(1L)).thenReturn(true);

// Preferir
Role role = RoleFactory.createOperatorRole();
when(roleRepository.existsById(RoleFactory.OPERATOR_ROLE_ID)).thenReturn(true);
```

### 7.5 Mocks, spies e testes parametrizados

- **Mock** (objeto falso criado pelo Mockito) só para **colaboradores**: repositórios, outros services, o encoder de senha. DTOs são `record`s simples e vêm das fábricas, não de mocks.
- **Stub** é o comportamento programado num mock (`when(...).thenReturn(...)`). Com o `MockitoExtension`, um stub que o teste não usa faz o teste falhar (`UnnecessaryStubbingException`), o que ajuda a manter o teste enxuto. Use `lenient()` só quando o stub do `@BeforeEach` serve a todos os testes da classe.
- **Spy** é um objeto real "espionado": os métodos funcionam de verdade, mas podem ser conferidos com `verify`. Use só quando o resultado final é o mesmo em dois caminhos e é preciso provar qual deles foi seguido, com um comentário explicando o motivo (exemplo: `activateShouldNotChangeUserWhenUserIsAlreadyActive`, no `UserServiceTest`).
- **`@ParameterizedTest`** roda o mesmo teste com vários valores de entrada. Use quando o comportamento é o mesmo para todos eles (por exemplo, senhas fracas recusadas pelo mesmo motivo), com `name = "[{0}]"`. O Surefire está configurado (`usePhrasedTestCaseMethodName`, no `pom.xml`) para gravar no relatório XML o `@DisplayName` de cada teste seguido do nome do caso, por exemplo `equals should return false when other is null[Token]`. No console, um caso que falha aparece pelo índice (`[5]`); para o nome aparecer também ali, use uma descrição na asserção (`.as(...)`), como no `EntityIdentityContractTest`. Valores que levam a comportamentos diferentes continuam em testes separados.

```java
// Evitar
CategoryUpdateRequest request = Mockito.mock(CategoryUpdateRequest.class);

// Preferir
CategoryUpdateRequest request = CategoryFactory.createCategoryUpdateRequest();
```

```java
// Preferir (um teste por valor no relatório)
@ParameterizedTest(name = "[{0}]")
@ValueSource(strings = { "user.example.com", "user@@example.com", "user@" })
@DisplayName("isValid should reject email when format is invalid")
void isValidShouldRejectEmailWhenFormatIsInvalid(String email) {
```

### 7.6 Determinismo

Um teste precisa dar o mesmo resultado em qualquer máquina, em qualquer ordem e a qualquer hora.

- **Sem rede.** A consulta de DNS do `@ValidEmail` é substituída nos testes (seção 5).
- **Sem depender da ordem.** Não use `@Order`. Os testes de integração que gravam no banco são `@Transactional`: o que cada teste grava é desfeito ao final.
- **Relógio.** O código de produção usa `Instant.now()` sem um `Clock` injetável. Por isso, confira datas por intervalo, e não com um único `isAfter(Instant.now())`, que não prova a duração.
- **Sem `Thread.sleep`.** A única exceção é `shouldRefreshOnlyUpdatedAtWhenPersistedProductIsModified`, no `ProductRepositoryTest`: sem um `Clock` nem um `setUpdatedAt` na entidade, a pausa é o único jeito de garantir instantes diferentes sem mudar o código de produção.

```java
// Evitar: não prova que a validade é de 24 horas
assertThat(result.getExpireDate()).isAfter(Instant.now());

// Preferir
Instant before = Instant.now();
Token result = service.createActivationToken(user);
Instant after = Instant.now();

assertThat(result.getExpireDate())
    .isBetween(before.plus(24, ChronoUnit.HOURS), after.plus(24, ChronoUnit.HOURS));
```

### 7.7 Comportamento com issue aberta

- Não escreva teste que congele um comportamento errado, nem teste que dependa de um bug já estar corrigido.
- Se o comportamento correto ainda não foi decidido (por exemplo, usuário criado sem roles, na #14), o caso fica sem teste. Cite a issue no commit.
- Um teste que já existe e está ligado a uma issue aberta não é reforçado. Ele muda junto com a correção da issue.

**Cenários pendentes.** Todo cenário que fica sem teste por causa de uma issue aberta é registrado no JavaDoc da classe `@Nested` da operação, numa seção "Cenários pendentes", com uma linha por cenário: o que não é testado, o motivo e o número da issue. Se a operação não tiver `@Nested`, o registro vai no JavaDoc da classe de teste. Assim, quem lê o teste sabe que a lacuna é intencional e onde ela será resolvida.

```java
/**
 * Testes de {@code ProductService.create}.
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>{@code categoryIds} nulo ou vazio: o produto seria criado sem categoria
 * e não apareceria na listagem; hoje o {@code @NotEmpty} do DTO bloqueia o
 * caso na API (#4).</li>
 * </ul>
 */
@Nested
@DisplayName("Create Operations")
class CreateOperations {
```

**Dois casos diferentes.** Um teste pode se relacionar com uma issue aberta de duas formas, e cada uma tem um tratamento:

| Caso | Exemplo | O que fazer |
| --- | --- | --- |
| O teste **apenas passa** por um caminho com issue aberta, sem afirmar o valor que a issue diz estar errado | o `delete` com violação de integridade, que força um `catch` que talvez nunca dispare (#25) | manter o teste, sem reforço, com um comentário acima do método |
| Uma **asserção afirma um valor** que a issue diz estar errado | o link de recuperação com `/reset-password` (#46); `status = PENDING` no registro de e-mail (#26) | remover a asserção (o resto do teste continua) e registrar o cenário em "Cenários pendentes" |

Manter uma asserção que afirma o valor errado congelaria o defeito: o teste passaria a defender o comportamento que a issue quer corrigir.

**Teste ligado a uma issue.** Quando um teste existente passa por um comportamento com issue aberta e não é reforçado, um comentário curto acima do método cita a issue e o motivo.

```java
// #25, item 4: o catch de DataIntegrityViolationException talvez nunca dispare,
// porque a exclusão só vai ao banco no commit. Mantido sem reforço até a decisão da issue.
@Test
@DisplayName("delete should throw DatabaseException when delete violates integrity")
void deleteShouldThrowDatabaseExceptionWhenDeleteViolatesIntegrity() {
```

### 7.8 Checklist de revisão

Use esta lista ao escrever ou revisar um teste (inclusive em Pull Requests):

- [ ] O nome segue `<método>Should<resultado>When<condição>`, e o `@DisplayName` diz o mesmo.
- [ ] Há um `@Nested` por método ou endpoint, e um comportamento por teste.
- [ ] Os comentários `// Arrange`, `// Act` e `// Assert` estão presentes.
- [ ] As asserções comparam o valor esperado por inteiro; exceções conferem tipo e mensagem completa.
- [ ] `verify` usa o argumento esperado, e os caminhos de erro conferem `never()` nos efeitos colaterais.
- [ ] Os dados vêm das fábricas e constantes, sem valores mágicos.
- [ ] Mocks só para colaboradores; `spy` e `lenient()` só com justificativa.
- [ ] O teste não depende de rede, de `sleep` nem da ordem de execução.
- [ ] A classe usa um único estilo de asserção (AssertJ nos testes novos e retrabalhados).
- [ ] Nenhum teste congela comportamento ligado a uma issue aberta.
- [ ] Cenários sem teste por issue aberta estão na seção "Cenários pendentes" do JavaDoc, e testes ligados a issue têm o comentário citando-a.
- [ ] Nenhuma asserção afirma um valor que uma issue aberta diz estar errado.
- [ ] Classe e métodos sem `public`.

## 8. Dependências externas

**Servidor SMTP: não é necessário.** O `application.properties` liga `spring.mail.test-connection`, que faz a subida testar a conexão com o servidor de e-mail. O `application-test.properties` desliga esse teste (`spring.mail.test-connection=false`), e nenhum teste envia e-mail de verdade (seção 5). Por isso **os testes não precisam das variáveis `MAIL_USERNAME` e `MAIL_PASSWORD`**: sem elas, `./mvnw verify` passa com 471 e 165 testes, sem erros.

**DNS (internet): não é necessário.** Em produção, o `@ValidEmail` consulta o registro MX do domínio do e-mail pela internet (veja [VALIDATION.md](VALIDATION.md#6-validação-de-e-mail)). Nos testes, essa consulta é substituída (seção 5), e `./mvnw verify` passa sem acesso à rede.

Para conferir, rode a suíte com o DNS simulado fora do ar (comando na seção 3). A propriedade `java.naming.provider.url` aponta o JNDI para uma porta do próprio computador em que nada responde, e qualquer consulta real de DNS falha. Antes da correção da #20, 19 testes falhavam assim (8 de unidade e 11 de integração); hoje, nenhum falha.

**Teste opcional contra o DNS real.** O `JndiMxRecordLookupDnsTest` consulta o DNS de verdade (`gmail.com` tem MX; `asjcatalog.invalid` não existe) e confere que a consulta JNDI de produção continua funcionando. Ele tem a tag `dns`, que fica fora do build padrão: o `pom.xml` passa a propriedade `surefire.excludedGroups` (valor `dns`) ao `excludedGroups` do Surefire. Para rodá-lo, troque o valor por uma tag que nenhum teste usa, como `none` (comando na seção 3). Sem internet, o teste de `gmail.com` falha.

## 9. Cobertura com JaCoCo

**Cobertura de testes** é a medida de quanto do código de produção foi executado pelos testes. O projeto mede com o **JaCoCo** (Java Code Coverage), configurado como plugin no `backend/pom.xml` (`jacoco-maven-plugin` 0.8.15). Cobertura alta mostra que o código **rodou** durante os testes, e não que todos os cenários foram conferidos.

### 9.1 Os três relatórios

O `./mvnw verify` gera três relatórios em HTML, em `backend/target/site/`:

| Relatório | Pasta | O que mede | Gerado na fase |
| --- | --- | --- | --- |
| Unidade | `jacoco/` | Só os testes do Surefire (`*Test` e `*Tests`) | `test` |
| Integração | `jacoco-it/` | Só os testes do Failsafe (`*IT`) | `post-integration-test` |
| **Consolidado** | `jacoco-merged/` | Unidade e integração somadas. **É o principal** e o usado pelo limite mínimo (9.5) | `verify` |

Para abrir o consolidado depois do build (a partir da pasta `backend`):

| PowerShell | bash (Linux) | bash (macOS) |
| --- | --- | --- |
| `Start-Process .\target\site\jacoco-merged\index.html` | `xdg-open target/site/jacoco-merged/index.html` | `open target/site/jacoco-merged/index.html` |

Cada pasta também traz um `jacoco.csv` (os números por classe) e um `jacoco.xml`, úteis para conferir os totais por script. A página **Sessions** lista as execuções que alimentaram o relatório; os nomes são fixos (`asjcatalog-unit` e `asjcatalog-it`, propriedade `sessionId` no `pom.xml`) para que o nome da máquina não apareça num relatório publicado.

**Relatório publicado.** O relatório consolidado do fim do capítulo 05 está em [asjcatalog-jacoco.netlify.app](https://asjcatalog-jacoco.netlify.app/). É um retrato fixo, gerado a partir do commit `48ba709`: não é atualizado a cada push.

### 9.2 Unidade, integração e consolidado

Os dois tipos de teste cobrem partes diferentes do código, e por isso os três relatórios dão números diferentes:

| Relatório | Linhas | Branches |
| --- | --- | --- |
| Unidade | 1530/1600 (95,6%) | 419/454 (92,3%) |
| Integração | 1220/1600 (76,3%) | 198/454 (43,6%) |
| Consolidado | 1539/1600 (96,2%) | 421/454 (92,7%) |

Os testes de integração passam por muitas linhas (sobem a aplicação inteira), mas por poucos branches: em geral, seguem o caminho de sucesso e alguns erros. Os testes de unidade cobrem os desvios com mais detalhe. Um exemplo do início do capítulo: os mappers eram mockados em todos os `*ServiceTest`, e por isso tinham de 2% a 5% de linhas cobertas no relatório de unidade e de 81% a 90% no consolidado, porque só os ITs os executavam. Olhar apenas um dos relatórios esconde parte da cobertura real.

### 9.3 Como ler o relatório

O relatório começa pela lista de pacotes; cada pacote abre a lista de classes, cada classe abre a lista de métodos, e o nome do arquivo `.java` abre o código colorido. As colunas são:

| Coluna | O que conta |
| --- | --- |
| **Instructions** | Instruções de **bytecode** (o código que o compilador Java gera a partir do `.java`). É a medida mais fina e não depende da formatação do código |
| **Branches** | Cada resultado possível de uma decisão: os dois lados de um `if`, cada condição de um `&&` ou `\|\|`, cada `case` de um `switch` |
| **Cxty** | **Complexidade ciclomática**: o número mínimo de caminhos diferentes por um método. Um método sem decisões tem 1; cada decisão soma 1 |
| **Lines** | Linhas do `.java` com ao menos uma instrução |
| **Methods** e **Classes** | Métodos e classes com ao menos uma instrução executada |

Cada coluna "Missed" mostra o que **não** foi executado. No Total do consolidado: 261 de 6.391 instruções (95%), 33 de 454 branches (92%) e 61 de 1.600 linhas (96%) sem cobertura. As porcentagens diferem porque cada coluna conta uma unidade diferente: uma linha com um `if` pode estar "coberta" (executou) e ainda ter um branch sem teste.

No código colorido:

| Marca | Significado |
| --- | --- |
| Linha verde | Todas as instruções da linha foram executadas |
| Linha amarela | Parte das instruções ou dos branches foi executada |
| Linha vermelha | Nenhuma instrução da linha foi executada |
| Diamante verde | Todos os branches da decisão foram executados |
| Diamante amarelo | Parte dos branches foi executada (passe o mouse para ver quantos faltam) |
| Diamante vermelho | Nenhum branch da decisão foi executado |

### 9.4 Exclusões

Algumas classes ficam fora do cálculo porque não têm lógica para testar (`<excludes>` na configuração do plugin):

| Exclusão | Motivo |
| --- | --- |
| `AsjcatalogApplication` | Só o `main` com `SpringApplication.run` |
| `dto/**` | `record`s sem lógica |
| `projection/**` | Interfaces sem métodos `default` |
| `config/documentation/**` e `config/i18n/**` | Configurações que só criam beans, sem desvios |
| `ProblemDetails` e `FieldMessage` | Respostas de erro sem lógica. O `ValidationError` continua no cálculo por causa do `addError` |

As interfaces dos repositórios não aparecem no relatório: o código delas é gerado pelo Spring Data em tempo de execução, e o JaCoCo só mede as classes compiladas do projeto.

### 9.5 Limite mínimo (`check`)

A execução `check-merged` do JaCoCo, na fase `verify`, **falha o build** se a cobertura do consolidado, somada no projeto inteiro (elemento `BUNDLE`), ficar abaixo de:

| Contador | Limite | Medido no fim do capítulo 05 | Propriedade do `pom.xml` |
| --- | --- | --- | --- |
| Linhas | 0,93 | 0,9619 | `jacoco.minimum.line.coverage` |
| Branches | 0,89 | 0,9273 | `jacoco.minimum.branch.coverage` |

**Regra dos 3 pontos de folga.** Cada limite é o valor medido menos 0,03, arredondado para baixo na segunda casa. A folga deixa espaço para código novo ainda sem teste completo, mas impede uma queda grande sem que ninguém perceba. O limite é por projeto, e não por classe ou pacote: vários pacotes têm de 2 a 20 linhas, e um limite por pacote faria o build falhar pela mudança de um único teste.

Os limites podem ser trocados numa execução, sem alterar o `pom.xml` (útil para conferir o `check`):

| PowerShell | bash |
| --- | --- |
| `.\mvnw verify '-Djacoco.minimum.line.coverage=0.97'` | `./mvnw verify -Djacoco.minimum.line.coverage=0.97` |

### 9.6 Quando o `check` falha

A mensagem diz qual contador ficou abaixo do limite:

```text
[WARNING] Rule violated for bundle asjcatalog: lines covered ratio is 0.96, but expected minimum is 0.97
[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.15:check (check-merged) on project asjcatalog: Coverage checks have not been met.
```

O que fazer:

1. Abra o relatório consolidado e ordene pela coluna "Missed" do contador citado, para achar o pacote e a classe que perderam cobertura.
2. Abra o código colorido e veja as linhas vermelhas e os diamantes amarelos ou vermelhos do código novo ou alterado.
3. Escreva os testes que faltam, no padrão da seção 7. Se o cenário depende de uma issue aberta, registre-o em "Cenários pendentes" (7.7) e não escreva um teste que congele o comportamento errado.
4. **Não baixe o limite para o build passar.** Mudar os limites é uma decisão do projeto, feita num commit próprio (`build(jacoco): ...`), com a nova medição.

Se um teste falha, o build para antes do `check`; o JaCoCo só é avaliado quando todos os testes passam.

## 10. Limitações conhecidas

- **Cobertura não garante cenários completos.** No início do capítulo 05, com 88,7% de linhas no consolidado, `POST /products` aceitava produto sem preço (#44): o código do cadastro estava coberto, mas nenhum teste enviava o preço nulo. A cobertura mostra o que não foi executado; o que foi executado ainda pode estar sem a conferência certa. Desde o PR #50 (migration V106), a coluna `price` é `NOT NULL`, e o banco recusa o produto sem preço; depois do capítulo, a correção da #44 acrescentou a validação no DTO, e a API passou a responder 422 com erro no campo `price`.
- **Código sem uso e cenários pendentes.** Parte do que aparece sem cobertura é código sem uso (registrado na #25, como `Token.isValid()` e `ProductMapper.toResponsePage`) ou cenário que depende de uma issue aberta (seção 7.7). Esses trechos ficam sem teste de propósito, e por isso a meta não é 100%.
- **Relatório publicado não é atualizado.** O relatório do Netlify é um retrato do commit `48ba709`; a publicação automática a cada build fica para o capítulo de Docker e CI/CD.
- **Flyway não é testado.** O perfil `test` cria as tabelas a partir das entidades, e não pelas migrations. Um erro numa migration só aparece ao subir nos perfis `dev` ou `prod`.
- **Comportamentos sem teste.** Não há teste para `PUT /users/{id}` feito por um OPERATOR no próprio id, para `GET /accounts/me` sem token, para `GET /products?sort=price` nem para o comportamento após reiniciar a aplicação (tokens invalidados). Veja as limitações em [API-ENDPOINTS.md](API-ENDPOINTS.md#9-limitações-conhecidas) e [AUTHENTICATION.md](AUTHENTICATION.md#10-limitações-conhecidas).

---

⬅️ Anterior: [Fluxos de conta](ACCOUNT-FLOWS.md) · [🏠 Índice](../HOME.md) · Próximo: [Convenções](CONVENTIONS.md) ➡️
