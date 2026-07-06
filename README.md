<h1 align="center">🧪 Capítulo 02 — Testes Automatizados no Back-End | Spring Boot</h1>

<p align="center">
<em>Applying professional automated testing strategies with JUnit, Mockito, MockMvc, integration testing, and testable software architecture in Spring Boot applications.</em>
</p>

<p align="center">

<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&logoColor=white" />

<img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

<img src="https://img.shields.io/badge/Testing-JUnit5%20%7C%20Mockito-yellow?style=for-the-badge" />

<img src="https://img.shields.io/badge/Web_Testing-MockMvc-blue?style=for-the-badge" />

<img src="https://img.shields.io/badge/Repository_Testing-DataJpaTest-success?style=for-the-badge" />

<img src="https://img.shields.io/badge/Architecture-SOLID%20%7C%20TDD-black?style=for-the-badge" />

<img src="https://img.shields.io/badge/Test_Strategy-Unit%20%7C%20Integration%20%7C%20Functional-purple?style=for-the-badge" />

<img src="https://img.shields.io/badge/Coverage-Service%20%7C%20Repository%20%7C%20Controller-9cf?style=for-the-badge" />

<img src="https://img.shields.io/badge/Quality-Regression_Safe-brightgreen?style=for-the-badge" />

<img src="https://img.shields.io/badge/Focus-Testability%20%7C%20Maintainability-informational?style=for-the-badge" />

<img src="https://img.shields.io/github/license/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

<img src="https://img.shields.io/github/last-commit/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

</p>

<p align="justify">
<em>
Este capítulo apresenta a construção de uma estratégia profissional de testes automatizados aplicada ao projeto <strong>ASJCatalog</strong>, utilizando <code>Java</code>, <code>Spring Boot 3</code>, <code>JUnit 5</code>, <code>Mockito</code>, <code>MockMvc</code> e princípios sólidos de engenharia de software para garantir qualidade, previsibilidade, segurança evolutiva e manutenção sustentável.
</em>
</p>

<p align="justify">
<em>
Dentro desse contexto, teste de unidade é o processo de verificar o funcionamento de um módulo de software de forma isolada, validando se ele atende às suas especificações. A partir desse fundamento, o capítulo evolui para abordagens mais amplas de validação, contemplando testes de integração, persistência e camada web, formando uma base robusta de qualidade para aplicações back-end profissionais.
</em>
</p>

---

## 📚 Contexto do Projeto

Após a implementação da arquitetura em camadas no Capítulo 01, o projeto evolui para um cenário robusto de validação automatizada, incorporando:

- **Testes unitários** (Service Layer)
- **Testes de persistência** (Repository Layer)
- **Testes da camada web** (Controller Layer)
- **Validação de relacionamentos JPA**
- **Paginação, ordenação e buscas customizadas**
- **Mocking e isolamento de dependências**
- **Factories para fixtures reutilizáveis**
- **Tratamento de exceções**
- **Princípios de TDD**
- **Aplicação de SOLID voltada à testabilidade**

---

## 🎯 Objetivos do Capítulo:

Este capítulo tem como objetivo garantir que cada módulo funcione corretamente de forma independente, contribuindo para a qualidade geral do software.

### 1. Dominar fundamentos de testes automatizados

- Compreender testes unitários, integração e funcionais
- Aplicar isolamento, previsibilidade e independência
- Reduzir regressões
- Produzir documentação viva
- Garantir sustentabilidade arquitetural

---

### 2. Implementar testes robustos com JUnit 5

### 📋 Recursos principais

| Recurso        | Finalidade                     |
| -------------- | ------------------------------ |
| `@Test`        | Define métodos de teste        |
| `@Nested`      | Organiza cenários por contexto |
| `@DisplayName` | Melhora legibilidade           |
| `@BeforeEach`  | Inicializa fixtures            |
| `Assertions`   | Verifica comportamentos        |
| `AssertJ`      | Assertions fluentes            |

---

## 🧩 Fixtures

Fixtures são estruturas reutilizáveis que evitam repetição e aumentam previsibilidade.

**Benefícios:**

- Reuso
- Clareza
- Padronização
- Facilidade de manutenção

---

### 📚 Ciclo de Vida — JUnit 5 vs JUnit 4

| JUnit 5       | JUnit 4        | Objetivo                 |
| ------------- | -------------- | ------------------------ |
| `@BeforeAll`  | `@BeforeClass` | Antes de todos os testes |
| `@AfterAll`   | `@AfterClass`  | Após todos os testes     |
| `@BeforeEach` | `@Before`      | Antes de cada teste      |
| `@AfterEach`  | `@After`       | Após cada teste          |

---

### 🏗️ Organização AAA

| Etapa   | Objetivo    |
| ------- | ----------- |
| Arrange | Preparação  |
| Act     | Execução    |
| Assert  | Verificação |

---

### 3. Aplicar estratégias reais no ecossistema Spring Boot

### ⚙️ Principais anotações

| Annotation                                | Tipo           | Finalidade                                 |
| ----------------------------------------- | -------------- | ------------------------------------------ |
| `@SpringBootTest`                         | Integração     | Carrega contexto completo                  |
| `@SpringBootTest + @AutoConfigureMockMvc` | Integração Web | Testa aplicação completa sem servidor real |
| `@WebMvcTest`                             | Web Layer      | Carrega apenas controllers                 |
| `@ExtendWith(SpringExtension.class)`      | Unitário       | Recursos Spring sem contexto completo      |
| `@ExtendWith(MockitoExtension.class)`     | Unitário       | Mockito puro                               |
| `@DataJpaTest`                            | Repository     | Carrega camada JPA com rollback            |

---

## 🌐 MockMvc

Permite validar:

- Endpoints REST
- Status HTTP
- JSON
- Headers
- Serialização
- Exception Handling

**Benefícios:**

- Rápido
- Isolado
- Sem servidor real
- Alta confiabilidade

---

### 4. Simular dependências com Mockito

### 🎭 Recursos utilizados

| Recurso                      | Objetivo                |
| ---------------------------- | ----------------------- |
| `@Mock`                      | Mock sem contexto       |
| `Mockito.mock()`             | Mock manual             |
| `@InjectMocks`               | Injeta mocks            |
| `@MockBean` / `@MockitoBean` | Mock no contexto Spring |
| `when().thenReturn()`        | Simula retorno          |
| `doThrow()`                  | Simula exceções         |
| `doNothing()`                | Simula métodos void     |
| `verify()`                   | Verifica interações     |
| `ArgumentMatchers`           | Flexibiliza argumentos  |

---

### 🆚 `@Mock` vs `@MockBean`

| Recurso                      | Quando usar                | Características       |
| ---------------------------- | -------------------------- | --------------------- |
| `@Mock`                      | Testes unitários puros     | Mais rápido           |
| `@MockBean` / `@MockitoBean` | Testes com contexto Spring | Substitui beans reais |

---

### 📌 Estratégia por camada

| Camada     | Estratégia                  |
| ---------- | --------------------------- |
| Service    | `@Mock` + `@InjectMocks`    |
| Controller | `@WebMvcTest` + `@MockBean` |
| Repository | `@DataJpaTest`              |
| Integração | `@SpringBootTest`           |

---

### 5. Aplicar TDD e SOLID

- Desenvolvimento orientado por testes
- Código desacoplado
- Arquitetura evolutiva
- Refatoração segura
- Design profissional

---

### 🧠 Fundamentos de Testes Automatizados

### 📌 Tipos de testes

| Tipo       | Objetivo                              | Escopo          | Dependências |
| ---------- | ------------------------------------- | --------------- | ------------ |
| Unitário   | Validar comportamento isolado         | Métodos/classes | Não          |
| Integração | Validar comunicação entre componentes | Banco/contexto  | Sim          |
| Funcional  | Validar fluxo completo                | Sistema         | Sim          |

---

## 🧪 Testes Unitários no Projeto

### Aplicados em:

- `CategoryServiceTest`
- `ProductServiceTest`

### Cenários cobertos:

- Insert
- Update
- Delete
- FindById
- FindAllPaged
- SearchByName
- Tratamento de exceções

### Benefícios:

- Velocidade
- Segurança
- Isolamento
- Refatoração confiável

---

## 🔗 Testes de Repository

### Validam:

- Persistência real
- Geração de IDs
- Paginação
- Ordenação
- Queries customizadas
- Integridade relacional
- ManyToMany

### Aplicação:

```java
@DataJpaTest
public class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Test
    public void testInsert() {

        // Arrange
        Product product = new Product(null, "Test Product", "Description", 10.0, "image.jpg");

        // Act
        Product savedProduct = repository.save(product);

        // Assert
        Assertions.assertNotNull(savedProduct.getId());
        Assertions.assertEquals("Test Product", savedProduct.getName());
    }
}
```

---

## Destaques

### CategoryRepository

- Busca por nome
- Ordenação
- Segurança relacional

### ProductRepository

- Persistência com categorias
- Exclusão segura
- Preservação relacional

---

## 🌐 Testes Web (Controllers)

### Ferramentas:

- `@WebMvcTest`
- `MockMvc`
- `ObjectMapper`
- `ControllerExceptionHandler`

### Cobertura:

- POST
- GET
- PATCH
- DELETE

### Validações:

- Status HTTP
- JSON
- Headers
- Serialização
- Exceções globais

---

## 📈 Benefícios Estratégicos

| Benefício                   | Impacto             |
| --------------------------- | ------------------- |
| Segurança contra regressões | Evolução segura     |
| Documentação viva           | Clareza             |
| Isolamento arquitetural     | Menor acoplamento   |
| Robustez multicamadas       | Qualidade sistêmica |
| Escalabilidade              | Sustentabilidade    |

> [!IMPORTANT]
> Testes automatizados representam investimento estrutural em qualidade contínua, confiabilidade e maturidade profissional.

---

## 🔄 TDD — Test Driven Development

### 📖 Conceito

Segundo Kent Beck:

| Etapa    | Descrição                  |
| -------- | -------------------------- |
| Red      | Criar teste falhando       |
| Green    | Implementar solução mínima |
| Refactor | Melhorar design            |

---

## 🚀 Benefícios

- Requisitos claros
- Menor regressão
- Melhor design
- Maior cobertura
- Código desacoplado

---

## 🏛️ SOLID Aplicado à Testabilidade

### 📌 Visão Geral

| Princípio | Benefício           |
| --------- | ------------------- |
| SRP       | Componentes menores |
| OCP       | Extensão segura     |
| LSP       | Previsibilidade     |
| ISP       | Interfaces enxutas  |
| DIP       | Mocking facilitado  |

---

## 🔽 DIP — Exemplo prático

### ❌ Ruim:

```java
ProductService depende de ProductRepositoryImpl
```

### ✅ Correto:

```java
ProductService depende de ProductRepository (interface)
```

### Resultado:

```java
@Mock
private ProductRepository repository;
```

### Benefícios:

- Menor acoplamento
- Isolamento
- Facilidade de testes
- Melhor manutenção

---

### 🧩 SRP — Exemplo

| Classe           | Responsabilidade     |
| ---------------- | -------------------- |
| `ProductService` | Regras de negócio    |
| `ProductMapper`  | Conversão DTO/Entity |

> [!TIP]
> SOLID melhora diretamente velocidade, confiabilidade e manutenção dos testes.

---

## 🧱 Boas Práticas Aplicadas

### 📌 Nomenclatura Profissional de Testes

### Estrutura:

```java
<ação>Should<resultado>When<cenário>
```

| Elemento      | Função                 |
| ------------- | ---------------------- |
| `<ação>`      | Método testado         |
| `Should`      | Comportamento esperado |
| `<resultado>` | Resultado esperado     |
| `When`        | Condição               |
| `<cenário>`   | Contexto específico    |

### ✅ Exemplos

| Nome                                                             | Significado                  |
| ---------------------------------------------------------------- | ---------------------------- |
| `findByIdShouldReturnProductWhenIdExists`                        | Retorna produto se ID existe |
| `deleteShouldThrowDatabaseExceptionWhenIntegrityViolationOccurs` | Lança exceção em violação    |
| `updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist`   | Falha para ID inexistente    |

---

## 🎯 Benefícios dos Testes Automatizados

- Melhora a qualidade do software: identifica e corrige falhas precocemente.
- Facilita o desenvolvimento: permite evoluir o código com segurança.
- Reduz custos: falhas corrigidas cedo custam menos.
- Aumenta a confiabilidade: garante funcionamento esperado.
- Melhora a documentação do código: testes explicam comportamento e propósito.

### 🎯 Benefícios da Nomenclatura Profissional

| Benefício          | Impacto         |
| ------------------ | --------------- |
| Clareza            | Fácil leitura   |
| Documentação viva  | Autoexplicativo |
| Padronização       | Consistência    |
| Diagnóstico rápido | Erros claros    |

### ❌ Nomes ruins

| Exemplo      | Problema     |
| ------------ | ------------ |
| `test1`      | Genérico     |
| `shouldWork` | Ambíguo      |
| `testDelete` | Sem contexto |

### ✅ Nomes profissionais

| Ruim         | Bom                                            |
| ------------ | ---------------------------------------------- |
| `testDelete` | `deleteShouldRemoveProductWhenIdExists`        |
| `testFind`   | `findByIdShouldReturnCategoryWhenIdExists`     |
| `testUpdate` | `updateShouldThrowExceptionWhenIdDoesNotExist` |

---

## 📌 Estrutura modular com @Nested

Benefícios:

- Separação por contexto
- Organização
- Legibilidade
- Manutenção

---

## 📌 Factories e Fixtures

Utilizadas:

- ProductFactory
- CategoryFactory
- Benefícios:
- Reuso
- Consistência
- Redução de duplicação
- Testes previsíveis

---

## 📂 Estrutura do Projeto de Testes

```text
test/java/com/albertsilva/dev/dscatalog
┣ entities
┣ factory
┣ integrations
┣ repositories
┣ services
┣ web
┗ DscatalogApplicationTests.java
```

---

## 📊 Pirâmide de Testes

```
Funcionais
Integração
Unitários
```

---

### 🏛️ Princípios Arquiteturais Aplicados

| Princípio                      | Nome Completo                                                         | Conceito                                                                               | Aplicação no Projeto                                                                                          | Benefício                                              |
| ------------------------------ | --------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| **SRP**                        | Single Responsibility Principle (Princípio da Responsabilidade Única) | Cada classe deve ter apenas uma única responsabilidade                                 | `ProductService` cuida das regras de negócio, enquanto `ProductMapper` realiza apenas conversões DTO ↔ Entity | Código mais organizado, modular e fácil de testar      |
| **DIP**                        | Dependency Inversion Principle (Princípio da Inversão de Dependência) | Componentes devem depender de abstrações (interfaces), não de implementações concretas | `ProductService` depende de `ProductRepository` (interface), permitindo uso de mocks em testes                | Menor acoplamento, maior flexibilidade e testabilidade |
| **Desacoplamento**             | Separação entre componentes                                           | Reduz dependências rígidas entre camadas                                               | Services, repositories e controllers possuem responsabilidades bem definidas                                  | Facilita manutenção, evolução e refatoração            |
| **Refatoração Segura**         | Evolução protegida por testes                                         | Melhorias estruturais sem quebrar funcionalidades                                      | Testes automatizados garantem segurança durante mudanças                                                      | Sustentabilidade de longo prazo                        |
| **Sustentabilidade Evolutiva** | Capacidade de expansão contínua                                       | Arquitetura preparada para novas features                                              | Base robusta para crescimento do sistema                                                                      | Escalabilidade profissional                            |

---

## 🧠 Conclusão — Evolução Profissional

Ao concluir este capítulo, o projeto DSCatalog consolida competências fundamentais para desenvolvimento backend profissional:

### 🚀 Competências adquiridas

| Categoria         | Competências Desenvolvidas                                                                              | Impacto Profissional                                                      |
| ----------------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| **Técnicas**      | `JUnit 5`, `Mockito`, `MockMvc`, `@DataJpaTest`, `@SpringBootTest`, `TDD`, `SOLID`, testes multicamadas | Domínio completo de estratégias modernas de validação backend             |
| **Arquiteturais** | `DIP`, `SRP`, desacoplamento, refatoração segura, sustentabilidade evolutiva                            | Construção de sistemas escaláveis, testáveis e preparados para manutenção |
| **Profissionais** | Qualidade enterprise, segurança contra regressão, confiabilidade sistêmica, base para CI/CD             | Preparação sólida para projetos corporativos e mercado profissional       |

---

## 📌 Resultado:

O projeto deixa de ser apenas uma API CRUD e passa a representar:

- Sistema validado profissionalmente
- Arquitetura sustentável
- Base confiável para crescimento
- Demonstração prática de maturidade em engenharia de software

> [!SUCCESS]
> Este capítulo consolida uma mentalidade de engenharia profissional: desenvolver software confiável, testável, sustentável e preparado para evolução contínua.

## 👨‍💻 Autor

**Albert Silva de Jesus**  
Desenvolvedor Backend Java | Spring Boot

---

### 📎 Contato

[![LinkedIn](https://img.shields.io/badge/LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/albert-backend-java-spring-boot/)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com?subject=Contato%20sobre%20o%20projeto%20CAD-MOTOTAXISTA)
