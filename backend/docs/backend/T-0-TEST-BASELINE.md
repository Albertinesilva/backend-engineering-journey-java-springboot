# T-0 — Baseline e diagnóstico da camada de testes

> Fase de **diagnóstico**. Nada foi corrigido: nenhum teste, código de produção, `pom.xml` ou recurso foi alterado. O único arquivo criado é este documento.
>
> Rótulos: **[FATO]** confirmado por leitura de código/histórico ou por execução nesta fase; **[INFERÊNCIA]** conclusão razoável a partir de fatos; **[HIPÓTESE]** ainda não comprovada.
> Classificações (A–E) seguem a definição do prompt de T-0 e **não** são prioridade nem ranking.

---

## 1. Objetivo

Estabelecer o baseline real da suíte de testes após B-9, reproduzir os failures conhecidos **antes** de qualquer alteração e classificar cada um (teste desatualizado, contrato a decidir, possível bug de produção, infraestrutura ou incerto).

---

## 2. Estado inicial

| Item | Resultado |
|------|-----------|
| Branch | `chapter-04-domain-orm` |
| `git status --short` (início) | vazio (working tree limpa) [FATO] |
| `git diff --stat` (início) | vazio [FATO] |
| Documentação B-0…B-9 | já versionada em `docs/backend/` (commit `501bfbd`) [FATO] |
| Alterações anteriores em `src/test/**` | nenhuma pendente; histórico em §9 [FATO] |
| JDK / Maven | Java 17.0.1; Maven 3.8.4 (`mvn`); `mvnw` usado no primeiro `mvn test` (ver §5) |

---

## 3. Inventário dos testes

`src/test/java`: **48** arquivos `.java` = **42** com `@Test` + 4 factories + `AbstractIT` + `TokenUtil` (confere com B-0). Não existe `src/test/resources`; o perfil `test` vem de `src/main/resources/application-test.properties` [FATO].

Contagem estática de `@Test` por grupo (a soma confere com o executado: 283 unitários + 93 `*IT` = 376):

| Grupo | Classes (nº de testes) | Total |
|-------|------------------------|------:|
| Contexto | `DscatalogApplicationTests` (1) | 1 |
| Domain | `CategoryTest` (6), `ProductTest` (6), `UserTest` (10) | 22 |
| Repository (`@DataJpaTest`) | `CategoryRepositoryTest` (10), `EmailRepositoryTest` (8), `ProductRepositoryTest` (9), `RoleRepositoryTest` (2), `TokenRepositoryTest` (4), `UserRepositoryTest` (15) | 48 |
| Service (Mockito) | `AccountServiceTest` (13), `CategoryServiceTest` (16), `EmailServiceTest` (6), `ProductServiceTest` (15), `TokenServiceTest` (9), `UserServiceTest` (14) | 73 |
| Controller (`@WebMvcTest`) | `AccountControllerTest` (13), `CategoryControllerTest` (10), `ProductControllerTest` (9), `UserControllerTest` (12) | 44 |
| Exception | `ControllerExceptionHandlerTest` (2) | 2 |
| Security (Mockito) | `AuthenticatedUserServiceTest` (8), `CustomPasswordAuthenticationConverterTest` (9), `CustomPasswordAuthenticationProviderTest` (7) | 24 |
| Validation (Mockito) | `CategoryCreate` (3), `CategoryUpdate` (4), `ProductCreate` (4), `ProductUpdate` (4), `ValidRoles` (5), `PasswordPersonalData` (10), `StrongPassword` (12), `UniqueEmail` (5), `UserUpdate` (11), `ValidEmail` (11) — todos `*ValidatorTest` | 69 |
| **Subtotal `mvn test`** | 34 classes | **283** |
| Integração (`*IT`) | `OAuth2TokenIT` (4), `ResourceServerAuthorizationIT` (7), `CategoryServiceIT` (13), `ProductServiceIT` (11), `UserServiceIT` (15), `CategoryControllerIT` (14), `ProductControllerIT` (14), `UserControllerIT` (15) | **93** |
| Apoio (sem testes) | `CategoryFactory`, `ProductFactory`, `RoleFactory`, `UserFactory`, `AbstractIT`, `TokenUtil` | — |

---

## 4. Padrões encontrados

Levantamento por busca nos arquivos (nº de arquivos que usam):

| Padrão | Arquivos | Onde |
|--------|---------:|------|
| `@DisplayName` | 40 | quase toda a suíte |
| `@Nested` | 32 | controllers, services, validators, ITs |
| `@BeforeEach` | 24 | |
| `Assertions.assert*` (JUnit) | 37 | domain, controllers, ITs, services |
| AssertJ (`assertThat`) | 22 | repositories, services, validators, security |
| `@Mock` / `@InjectMocks` | 18 / 16 | services, validators, security (com `MockitoExtension`) |
| `@WebMvcTest` | 4 | os 4 `*ControllerTest` |
| `@DataJpaTest` | 6 | os 6 `*RepositoryTest` |
| `@SpringBootTest` | 6 | `AbstractIT`, `DscatalogApplicationTests`, `OAuth2TokenIT` e os 3 `*ServiceIT` (estes 4 sem herdar `AbstractIT`) |
| `@MockitoBean` | 4 | controllers `@WebMvcTest` (service, repositories, `JwtDecoder`) |
| `jsonPath` | 8 | controllers + ITs |
| `@WithMockUser` | 3 | controllers `@WebMvcTest` |
| `@ActiveProfiles("test")` / `@TestPropertySource` | 3 / 3 | só em 3 dos 4 `@WebMvcTest` |
| `@Transactional` | 7 | ITs (rollback por teste) |
| `TestEntityManager` | 2 | repositories |
| `@ParameterizedTest`, `@Disabled`, `@Tag` | 0 | nenhum uso |

Observações [FATO]:
- **Camadas**: controllers = `@WebMvcTest` com `@MockitoBean` no service **e nos repositories** (para os validators de DTO), importando `ControllerExceptionHandler` e `ResourceServerConfig`; services/validators/security = Mockito puro; repositories = `@DataJpaTest`; ITs = contexto completo + H2.
- **Perfil**: `application.properties` define `spring.profiles.active=${APP_PROFILE:test}`; os ITs não declaram `@ActiveProfiles`, herdam o padrão `test`. Perfil `test`: H2 em memória (`jdbc:h2:mem:testdb`), `spring.flyway.enabled=false`, dados de `import.sql`.
- **Autenticação nos ITs**: `TokenUtil.obtainAccessToken` faz o grant `password` real em `/oauth2/token` com `maria@gmail.com` / `123456` (usuário do `import.sql`); nos `@WebMvcTest` usa-se `@WithMockUser` + `csrf()`.
- **Factories**: `ProductFactory` e `CategoryFactory` produzem entidades, requests e responses; são compartilhadas entre unitários, `@WebMvcTest` e ITs (ponto de acoplamento: uma mudança em DTO quebra a compilação ou o valor esperado em todos os níveis).
- **Diferenças entre arquivos** (registradas, não padronizadas): `AccountControllerTest`, `ProductControllerTest` e `UserControllerTest` importam `ResourceServerConfig` (segurança real, `@WithMockUser` + `csrf()`), com `@ActiveProfiles("test")` e `@TestPropertySource`; **`CategoryControllerTest` é diferente**: `@WebMvcTest(..., excludeAutoConfiguration = SecurityAutoConfiguration.class)`, sem `ResourceServerConfig` — logo não exercita autorização, ao contrário dos outros três. ITs de serviço (e `OAuth2TokenIT`) usam `@SpringBootTest` direto; os de controller/segurança herdam `AbstractIT`. Convenção de nomes e de `Arrange/Act/Assert` varia de arquivo a arquivo.
- **Ruído**: o perfil `test` liga `DEBUG` de `com.albertsilva.dev.dscatalog`, `DEBUG` de `org.springframework.web` e `TRACE` de `...mvc.method.annotation`; o log do `mvn test` tem ~5,1 mil linhas e o de `*IT` ~10,5 mil.

---

## 5. Comandos executados

| # | Comando | Resultado |
|---|---------|-----------|
| 1 | `git status --short`, `git diff --stat` | vazios |
| 2 | `./mvnw test` | 283 executados, **5 falhas**, 0 erros, 0 skipped; `BUILD FAILURE`; 01:01 min |
| 3 | `mvn -B verify -Dmaven.test.failure.ignore=true` | mesmos 283 / 5 falhas; `BUILD SUCCESS` (só por causa da flag, apenas na linha de comando); 01:09 min |
| 4 | `mvn -B test -Dtest='*IT' -Dsurefire.failIfNoSpecifiedTests=false` | 93 executados, **5 falhas**, 0 erros, 0 skipped; `BUILD FAILURE`; 01:31 min |
| 5 | `git status --short`, `git diff --stat` (final) | vazios |

Comentários:
- O comando #3 usa `-Dmaven.test.failure.ignore=true` **somente para que o Maven não parasse no surefire**; não altera configuração. Ele existe só para provar o ponto abaixo.
- **`mvn verify` não executa os `*IT`** [FATO]: a lista de plugins executados foi `resources → compiler → resources(test) → compiler(test) → surefire → jar → spring-boot repackage`; não há `maven-failsafe-plugin`. O `pom.xml` não configura Surefire `includes` nem Failsafe, então o Surefire padrão pega `*Test`, `Test*`, `*Tests`, `*TestCase` e ignora `*IT`.
- A forma já estabelecida para rodar os `*IT` (B-0 §13) é a do comando #4; foi usada sem tocar no `pom.xml`.
- Efeitos colaterais: `target/` (ignorado pelo Git; `verify` também regenerou o jar) e `logs/test/` (ignorado). O `git status` final continua vazio.

---

## 6. Baseline unitário (`mvn test`)

| Total | Passed | Failed | Errors | Skipped | Tempo |
|------:|-------:|-------:|-------:|--------:|------:|
| 283 | 278 | **5** | 0 | 0 | 1:01 min |

Failures (mensagem do Maven):

```
ProductTest.productShouldInstantiateCorrectly:29  expected: not <null>
ProductControllerTest.findAllShouldReturnFilteredPage      Status expected:<200> but was:<500>
ProductControllerTest.findAllShouldReturnPage              Status expected:<200> but was:<500>
ProductControllerTest.updateShouldReturnNotFoundWhenIdDoesNotExist        Status expected:<404> but was:<500>
ProductControllerTest.updateShouldReturnUpdatedProductWhenIdExists       Status expected:<200> but was:<500>
```

---

## 7. Baseline de integração (`*IT`)

| Total | Passed | Failed | Errors | Skipped | Tempo |
|------:|-------:|-------:|-------:|--------:|------:|
| 93 | 88 | **5** | 0 | 0 | 1:31 min |

Failures:

```
CategoryControllerIT.insertShouldCreateCategoryAndReturnCreated   No value at JSON path "$.description"
CategoryControllerIT.updateShouldUpdateCategoryWhenIdExists       No value at JSON path "$.description"
ProductControllerIT.insertShouldCreateProductWithValidData        No value at JSON path "$.date"
ProductControllerIT.updateShouldReturnNotFoundWhenIdDoesNotExist  Status expected:<404> but was:<500>
ProductControllerIT.updateShouldReturnProductResponseWhenIdExists Status expected:<200> but was:<500>
```

O contexto Spring completo sobe (H2, OAuth2, JWT); os 88 `*IT` restantes passam, inclusive `OAuth2TokenIT`, `ResourceServerAuthorizationIT`, `UserControllerIT` e os 3 `*ServiceIT`.

---

## 8. Failures reproduzidos

**Os 10 failures conhecidos foram reproduzidos, com os mesmos nomes e mensagens do B-0/B-9** [FATO] (5 em `mvn test` + 5 em `*IT`). A execução atual é a autoridade; não houve mudança de número.

Ocorrências: `ProductTest` (1), `ProductControllerTest` (4), `CategoryControllerIT` (2), `ProductControllerIT` (3). Nenhuma outra classe falha.

---

## 9. Análise individual dos failures

Legenda das fontes de histórico: `6027fc2` (2026-06-19) *"simplify product model and synchronize API contracts"*; `e82cee2` (2026-06-02) introduz `findAllPaged` no controller; `f8ceb98` (2026-06-02) reduz `CategoryResponse` para `(id, name)` e cria `CategoryDetailsResponse`; `19687f6` (2026-07-14) i18n.

### F-1 — `ProductTest.productShouldInstantiateCorrectly`

```
Teste:      ProductTest
Método:     productShouldInstantiateCorrectly (linha 29)
Arquivo:    src/test/.../domain/catalog/ProductTest.java
Tipo:       unitário de entidade (sem Spring)
Mensagem:   expected: not <null>   (assertNotNull(product.getCreatedAt()))
Produção:   Product.createdAt/updatedAt, @PrePersist prePersist() privado; ProductFactory.createProduct()
Contrato esperado pelo teste: um Product recém-construído já tem createdAt e updatedAt.
Comportamento observado: os dois campos são null.
Evidência:
  - O construtor de Product (5 e 6 args) não atribui createdAt/updatedAt; só o callback JPA prePersist (privado) os define.
  - O Javadoc da entidade diz literalmente que ficam null "em instâncias ainda não persistidas".
  - Em 6027fc2 o campo `date` (setado no construtor) virou createdAt/updatedAt (callback), e o MESMO commit trocou no
    teste assertNotNull(getDate()) por assertNotNull(getCreatedAt()) + assertNotNull(getUpdatedAt()). O construtor deixou de
    receber a data no mesmo commit; portanto o assert novo nunca pôde passar sem persistência [INFERÊNCIA a partir do diff].
  - CategoryTest passa porque Category tem prePersist() PÚBLICO e o teste o chama (categoryPrePersistShouldSetCreatedAt).
  - Nenhum teste persiste um Product e verifica createdAt/updatedAt (nem ProductRepositoryTest).
Classificação: A — TESTE DESATUALIZADO (a asserção contradiz o contrato documentado da entidade)
Ressalva: a forma de resolver (chamar o callback, persistir, ou tornar prePersist público como em Category) envolve uma
          escolha de design de teste/visibilidade → registrada em §12 (QC-3). Nenhuma evidência de bug de produção.
```

### F-2 e F-3 — `ProductControllerTest.findAllShouldReturnPage` / `findAllShouldReturnFilteredPage`

```
Teste:      ProductControllerTest (@Nested FindAllTests)
Métodos:    findAllShouldReturnPage; findAllShouldReturnFilteredPage
Tipo:       @WebMvcTest com @MockitoBean ProductService
Mensagem:   Status expected:<200> but was:<500>
Stack:      NullPointerException: Cannot invoke "Page.getTotalElements()" because "response" is null
            (ProductController.findAll) → ControllerExceptionHandler "Unexpected error" → 500
Produção:   ProductController.findAll → productService.findAllPaged(name, categoryIds, pageable)
Contrato esperado pelo teste: GET /products chama ProductService.search(name, pageable).
   Mocks: when(productService.search(any(), any(Pageable.class))) e when(productService.search(eq("pc"), any(Pageable.class)));
   verify(productService).search(...).
Comportamento observado: o controller chama findAllPaged(name, "0", pageable), método NÃO stubado; Mockito devolve null;
   o controller lê response.getTotalElements() (dentro do logger.debug) → NPE → 500.
Evidência:
  - `findAllPaged` no controller desde e82cee2 (2026-06-02; antes era `search`, diff do commit). O teste continua mockando
    `search`; `git log -S"productService.search" -- src/test` mostra 5af123a (2026-05-13) como o último commit a alterar essa
    ocorrência, ou seja, o teste não foi ajustado desde a troca no controller.
  - ProductService.search continua existindo e é testado (ProductServiceTest, ProductServiceIT), mas o Javadoc do service e do
    controller registram que NENHUM código de src/main o chama.
  - Não há decisão de contrato envolvida: o endpoint é o mesmo (GET /api/v1/products); mudou o método interno do service.
Classificação: A — TESTE DESATUALIZADO
Observação de produção (não altera a classificação): o NPE só ocorre se o service devolver null, o que não acontece em
   produção; o log de debug avalia response.getTotalElements() antes do teste de nulidade.
Estado desde: 2026-06-02 (o teste falha desde então) [INFERÊNCIA].
```

### F-4 e F-5 — `ProductControllerTest.updateShouldReturnUpdatedProductWhenIdExists` / `updateShouldReturnNotFoundWhenIdDoesNotExist`

```
Teste:      ProductControllerTest (@Nested UpdateTests, @DisplayName "PATCH /products/{id}")
Tipo:       @WebMvcTest
Mensagem:   Status expected:<200>|<404> but was:<500>
Stack:      HttpRequestMethodNotSupportedException: Request method 'PATCH' is not supported
            → ControllerExceptionHandler.Exception (genérico) → 500
Produção:   ProductController.update mapeado com @PutMapping("/{id}")
Contrato esperado pelo teste: PATCH /products/{id}
Comportamento observado: PUT existe; PATCH em /{id} não tem mapeamento → 405 do Spring → convertido em 500 pelo handler genérico.
Evidência:
  - 6027fc2 (2026-06-19) trocou @PatchMapping por @PutMapping no ProductController (import de PutMapping adicionado) e
    ajustou o ProductUpdateRequest, mapper e factory no mesmo commit, mas NÃO tocou ProductControllerTest/ProductControllerIT.
  - ProductControllerTest foi tocado depois (19687f6, 2026-07-14), mas só para trocar a mensagem de ResourceNotFoundException
    por `"error.product.notFound"`; o verbo PATCH permaneceu (fato: o teste foi editado depois do PUT e continuou falhando).
  - Todas as fontes de contrato no repositório dizem PUT: @PutMapping, Javadoc do ProductController e do ProductUpdateRequest
    ("PUT /api/v1/products/{id}"), OpenAPI (gerado por springdoc a partir das anotações; não há arquivo openapi estático).
    Só os testes dizem PATCH. Não há frontend no repositório e nenhum doc (README, docs/) cita o verbo do produto.
  - Assimetria de projeto: Category usa PATCH (testes de Category passam), Product e User usam PUT.
Classificação: A — TESTE DESATUALIZADO (evidência histórica e documental convergente; uma decisão do autor foi
               explícita no commit "synchronize API contracts")
Ressalva: o histórico não decide a intenção. Confirmar que PUT é o contrato desejado (e a assimetria Category × Product)
          é decisão de contrato → QC-1. NENHUMA correção foi feita.
Observação separada (candidata a C, não classificada aqui): um verbo sem mapeamento devolve 500 em vez de 405 porque o
          handler genérico captura HttpRequestMethodNotSupportedException (comportamento de produção, ver §11 D-5).
```

### F-6 e F-7 — `CategoryControllerIT.insertShouldCreateCategoryAndReturnCreated` / `updateShouldUpdateCategoryWhenIdExists`

```
Teste:      CategoryControllerIT (@Nested CreateOperations / UpdateOperations)
Tipo:       integração (H2 + OAuth2 real)
Mensagem:   java.lang.AssertionError: No value at JSON path "$.description"
Produção:   CategoryController.create/update → CategoryResponse(Long id, String name)
Contrato esperado pelo teste: a resposta de POST e PATCH /categories inclui `description` (igual à do request).
Comportamento observado: a resposta contém apenas `id` e `name`; status 201/200 e demais asserts passam antes do jsonPath.
Evidência:
  - CategoryResponse era (id, name, description, active) e virou (id, name) em f8ceb98 (2026-06-02), no mesmo commit em que
    nasceu CategoryDetailsResponse (id, name, description, active) — separação resumo × detalhe, o mesmo padrão de
    ProductResponse × ProductDetailsResponse.
  - Em f8ceb98 o autor ATUALIZOU CategoryControllerTest e CategoryServiceTest para o novo record (asserts só de id e name) e
    a CategoryFactory.createCategoryResponse() é (id, name); CategoryControllerIT (última alteração 2026-05-19) ficou de fora.
  - O Javadoc de CategoryResponse diz explicitamente que NÃO expõe descrição nem active.
  - PATCH e o restante do IT de Category (incluindo 404 e activate/deactivate) passam: o verbo está alinhado.
  - Causa estrutural provável: os `*IT` não rodam em `mvn test`, então essa deriva não aparece no build padrão [INFERÊNCIA].
Classificação: A — TESTE DESATUALIZADO (o teste unitário do mesmo controller e a factory já preservam o contrato novo)
Ressalva: se o produto quiser que create/update devolvam a descrição, o correto seria mudar produção — decisão registrada
          como QC-2 (campos de resposta). Não decidido.
```

### F-8 — `ProductControllerIT.insertShouldCreateProductWithValidData`

```
Teste:      ProductControllerIT (@Nested CreateOperations)
Tipo:       integração
Mensagem:   No value at JSON path "$.date"
Produção:   ProductController.create → ProductResponse(id, name, description, price, imgUrl, categories)
Contrato esperado pelo teste: a resposta do POST devolve `date` igual ao `date` enviado no request
   (.value(request.date().toString())).
Comportamento observado: ProductResponse não tem `date`; o request ainda tem `date` (@PastOrPresent, validado) mas o mapper
   o descarta (Javadoc do controller: "O campo date é validado, mas descartado"); createdAt/updatedAt nascem do @PrePersist.
Evidência:
  - `date` saiu de Product/ProductResponse em 6027fc2 (2026-06-19); o IT foi escrito antes (última alteração 2026-05-19).
  - ProductCreateRequest ainda declara e valida `date` (ProductFactory.createProductCreateRequest ainda o preenche).
  - O teste afirma um round-trip de campo que a aplicação não persiste mais.
Classificação: B — CONTRATO A DECIDIR
Justificativa: a expectativa do teste está obsoleta, mas o contrato atual é ambíguo — o request aceita e valida um campo
   que é ignorado. Só se sabe o que consertar (teste, request ou resposta) decidindo o destino de `date` (QC-2, QC-4).
```

### F-9 e F-10 — `ProductControllerIT.updateShouldReturnProductResponseWhenIdExists` / `updateShouldReturnNotFoundWhenIdDoesNotExist`

```
Tipo:       integração; Mensagem: Status expected:<200>|<404> but was:<500>
Stack:      HttpRequestMethodNotSupportedException: Request method 'PATCH' is not supported (logs de /products/1 e /products/1000)
Causa, evidência e histórico: idênticos a F-4/F-5 (PATCH × @PutMapping); ProductControllerIT não é tocado desde 2026-05-19.
Classificação: A — TESTE DESATUALIZADO (mesma ressalva QC-1)
```

Os `activate`/`deactivate` de Product e Category (também `PATCH`) **passam** nos ITs, o que confirma que o verbo `PATCH` para `/{id}/activate|deactivate` existe e está alinhado.

---

## 10. Classificação

| # | Teste | Classificação | Dependência de decisão |
|---|-------|---------------|-------------------------|
| F-1 | `ProductTest.productShouldInstantiateCorrectly` | **A** | resolução: QC-3 |
| F-2 | `ProductControllerTest.findAllShouldReturnPage` | **A** | nenhuma |
| F-3 | `ProductControllerTest.findAllShouldReturnFilteredPage` | **A** | nenhuma |
| F-4 | `ProductControllerTest.updateShouldReturnUpdatedProductWhenIdExists` | **A** | confirmar QC-1 |
| F-5 | `ProductControllerTest.updateShouldReturnNotFoundWhenIdDoesNotExist` | **A** | confirmar QC-1 |
| F-6 | `CategoryControllerIT.insertShouldCreateCategoryAndReturnCreated` | **A** | confirmar QC-2 |
| F-7 | `CategoryControllerIT.updateShouldUpdateCategoryWhenIdExists` | **A** | confirmar QC-2 |
| F-8 | `ProductControllerIT.insertShouldCreateProductWithValidData` | **B** | QC-2, QC-4 |
| F-9 | `ProductControllerIT.updateShouldReturnProductResponseWhenIdExists` | **A** | confirmar QC-1 |
| F-10 | `ProductControllerIT.updateShouldReturnNotFoundWhenIdDoesNotExist` | **A** | confirmar QC-1 |

Resumo: **A** = 9 · **B** = 1 · **C** (bug de produção comprovado por teste alinhado) = 0 · **D** (infraestrutura) = 0 · **E** (incerto) = 0.

Isso difere da leitura conservadora do B-9 (que deixava 7 dos 10 como "contrato divergente que depende de decisão"): com o histórico e o fato de que os testes de Category unitários já foram sincronizados em `f8ceb98`, cada caso ganhou evidência de que o **teste** está atrás. O que continua dependendo de decisão é a *confirmação* do contrato, não a existência da divergência. As ressalvas acima permanecem valendo; a classificação A não autoriza correção automática.

Nenhum failure tem causa de **infraestrutura**: o contexto sobe, H2, OAuth2, JWT e Flyway (desligado no perfil `test`) funcionam; nenhum dos 10 envolve SMTP.

---

## 11. Divergências teste × produção

| ID | Divergência | Onde | Relação com B-9 |
|----|-------------|------|-----------------|
| D-1 | Verbo `PATCH` (testes) × `PUT` (produção) em `/products/{id}` | `ProductControllerTest`, `ProductControllerIT` × `ProductController` | K-01 |
| D-2 | Mock `search` × chamada `findAllPaged` | `ProductControllerTest` × `ProductController` | K-11 |
| D-3 | `$.description` esperado × `CategoryResponse(id,name)` | `CategoryControllerIT` × `CategoryResponse` | decisão #16 |
| D-4 | `$.date` esperado × `ProductResponse` sem `date` | `ProductControllerIT` × `ProductResponse` | decisão #16 / #19 |
| D-5 | Verbo sem mapeamento devolve **500** em vez de **405** | `ControllerExceptionHandler` (handler `Exception`) — os testes só o expõem | B-9 (handler genérico) |
| D-6 | `createdAt`/`updatedAt` esperados em instância nova × preenchidos só por `@PrePersist` privado | `ProductTest` × `Product` (`Category` tem `prePersist` público) | T-05 |

Divergências sem teste correspondente (achados desta análise):
- **G-1** — `ProductService.findAllPaged`, que atende `GET /products`, **não é chamado por nenhum teste** (nem unitário, nem IT). `search`, que **não** é usado em produção, tem 3 testes unitários e 4 de IT. O caminho real da listagem (SQL nativo + `JOIN FETCH` + reordenação) está sem cobertura [FATO, busca por `findAllPaged(`/`searchProducts` em `src/test`].
- **G-2** — nenhum teste persiste um `Product` e verifica `createdAt`/`updatedAt` (o callback JPA não é exercitado).

---

## 12. Questões de contrato (decisão do autor; **não** decididas aqui)

- **QC-1 — Verbo de atualização de Product.** `PUT` (atual, com `name` e `categoryIds` obrigatórios) é o desejado? E a assimetria com Category (`PATCH`)? Evidência favorece `PUT`; decisão pendente.
- **QC-2 — Campos de resposta.** `POST`/`PATCH /categories` devem devolver `description` (hoje só `id`,`name`)? `POST /products` deve devolver algo além de `ProductResponse` (`createdAt`, `active`)? Hoje: resumo em criação/atualização/listagem, detalhes só no `GET /{id}`.
- **QC-3 — `Product.prePersist` privado × `Category.prePersist` público.** Padronizar a visibilidade e como os testes devem exercitar o callback (chamar direto × persistir).
- **QC-4 — `ProductCreateRequest.date`.** Aceitar-e-ignorar um campo (`@PastOrPresent`) é intencional? Manter, remover do request ou expor `createdAt`?
- **QC-5 — 405 × 500.** Um verbo inexistente deve responder 405? (comportamento de produção independente dos 10 failures; classificaria como C se o contrato desejado for 405).

---

## 13. Hipóteses ainda não comprovadas

- **H-1** — Os `*IT` derivam porque não rodam em `mvn test` (o `CategoryControllerIT` ficou fora do ajuste de `f8ceb98`); explica os 5 `*IT`, mas os 5 unitários também estão fora de sincronia e o build padrão está vermelho, o que sugere que o resultado do `mvn test` **não** vinha sendo observado [INFERÊNCIA/HIPÓTESE].
- **H-2** — `ProductTest.F-1` nunca passou desde `6027fc2` (inferido do diff; não executei o commit antigo).
- **H-3** — `ProductControllerTest` de `findAll` falha desde `e82cee2`/2026-06-02 (mesma inferência).
- **H-4** — O erro de `PATCH`→500 em produção real (fora do MockMvc) é idêntico ao dos testes; não foi verificado com servidor HTTP.
- **H-5** — Estabilidade: os `*IT` compartilham um H2 em memória com contexto em cache e rollback por `@Transactional`; não foi verificado se a ordem de execução altera algum resultado (nesta execução todos os 88 restantes passaram).

---

## 14. Pontos para a próxima fase

1. Rever este documento e responder QC-1…QC-5.
2. Só então definir T-1 (primeiro conjunto deliberado de correções). Ordem sugerida pelos dados, sem decidir: F-2/F-3 (independente de contrato) → F-4/F-5/F-9/F-10 conforme QC-1 → F-6/F-7/F-8 conforme QC-2/QC-4 → F-1 conforme QC-3.
3. Decidir se os `*IT` devem entrar no build padrão (Failsafe ou `includes`) — hoje `mvn test` e `mvn verify` são vermelhos em 5 e cegos para outros 5.
4. Só depois desta fase, abrir T-2 para os gaps: G-1 (`findAllPaged`), G-2 (auditoria de `Product`), e os itens deliberadamente **fora** de T-0: validators/OAuth2/`AccountService`/`EmailService`/CORS/templates/`@Async`, e o restante do backlog do B-9 §17.
5. Reexecutar este baseline (283 + 93) ao fim de cada correção para medir o efeito.

---

## 15. O que não foi alterado

- Nenhum arquivo de `src/main/**`, `src/test/**`, `pom.xml` ou `src/main/resources/**`.
- Nenhum teste corrigido, atualizado, removido ou criado; nenhum mock, assertion ou verbo trocado.
- Nenhum commit, nenhum `git add`, sem reset/stash.
- Único arquivo criado: `docs/backend/T-0-TEST-BASELINE.md`.
- Arquivos gerados e ignorados pelo Git: `target/` (compilação, surefire-reports, jar de `verify`) e `logs/test/`.
