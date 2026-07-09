<h1 align="center">🏛️ Capítulo 04 — Domain Modeling, ORM, Business Use Cases & Data Access</h1>

<p align="justify">
<em>
This chapter focuses on designing a robust domain model, implementing real business use cases, optimizing database access strategies, and applying advanced JPA/Hibernate techniques to build enterprise-grade backend applications aligned with real-world business requirements.
</em>
</p>

<p align="center">

<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&logoColor=white" />

<img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

<img src="https://img.shields.io/badge/Persistence-Spring_Data_JPA-success?style=for-the-badge" />

<img src="https://img.shields.io/badge/ORM-Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white" />

<img src="https://img.shields.io/badge/Database-PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white" />

<img src="https://img.shields.io/badge/Queries-JPQL%20%7C%20Native_SQL-blue?style=for-the-badge" />

<img src="https://img.shields.io/badge/Pagination-Spring_Data-informational?style=for-the-badge" />

<img src="https://img.shields.io/badge/Performance-N%2B1_Select-red?style=for-the-badge" />

<img src="https://img.shields.io/badge/Architecture-Domain_Driven_Design-purple?style=for-the-badge" />

<img src="https://img.shields.io/badge/Business_Logic-Use_Cases-critical?style=for-the-badge" />

<img src="https://img.shields.io/badge/Account_Management-Sign_Up%20%7C%20Password_Recovery-success?style=for-the-badge" />

<img src="https://img.shields.io/badge/Email-Spring_Mail-yellow?style=for-the-badge" />

<img src="https://img.shields.io/badge/Tokens-Activation%20%7C%20Recovery-orange?style=for-the-badge" />

<img src="https://img.shields.io/badge/Security-RBAC-red?style=for-the-badge" />

<img src="https://img.shields.io/badge/Authentication-OAuth2%20%7C%20JWT-black?style=for-the-badge" />

<img src="https://img.shields.io/badge/Documentation-Swagger%20%7C%20OpenAPI-85EA2D?style=for-the-badge" />

<img src="https://img.shields.io/github/license/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

<img src="https://img.shields.io/github/last-commit/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

</p>

<p align="justify">
<em>
Neste capítulo, o projeto <strong>ASJCatalog</strong> evolui significativamente além dos cenários tradicionais de CRUD, incorporando fluxos de negócio completos encontrados em aplicações corporativas reais.

Foram implementados casos de uso relacionados ao ciclo de vida da conta do usuário, incluindo cadastro, ativação de conta, recuperação de senha, redefinição de credenciais e obtenção do usuário autenticado, utilizando uma arquitetura baseada em regras de negócio explícitas, entidades ricas e serviços especializados.

Além da evolução funcional, a camada de persistência foi aprimorada com consultas otimizadas utilizando <strong>Spring Data JPA</strong>, <strong>JPQL</strong>, consultas nativas, paginação, filtros dinâmicos e estratégias para eliminação do problema <strong>N+1 Select</strong>, garantindo melhor desempenho e escalabilidade.

O capítulo também introduz integração com serviços externos através do envio de e-mails transacionais, gerenciamento de tokens de negócio e aplicação de conceitos inspirados em <strong>Domain-Driven Design (DDD)</strong>, aproximando o projeto dos padrões encontrados em sistemas corporativos modernos.
</em>
</p>

---

## 📚 Contexto do Capítulo

Após a implementação da infraestrutura de autenticação e autorização utilizando Spring Security, OAuth2 e JWT, o projeto ASJCatalog evolui para uma camada mais próxima dos requisitos encontrados em aplicações corporativas reais.
Neste capítulo foram implementados casos de uso completos relacionados ao ciclo de vida da conta do usuário, além da evolução da camada de persistência utilizando JPA/Hibernate, consultas otimizadas e integração com serviços de e-mail.
O foco principal foi construir fluxos de negócio completos, desacoplados e alinhados com boas práticas de arquitetura backend.

---

## 🎯 Objetivos do Capítulo

Este capítulo possui os seguintes objetivos:

- Evoluir a modelagem ORM da aplicação.
- Implementar casos de uso completos relacionados à gestão de contas de usuário.
- Aplicar conceitos de Domain-Driven Design na modelagem de negócio.
- Implementar mecanismos de ativação e recuperação de acesso.
- Resolver problemas de performance relacionados ao carregamento de entidades.
- Utilizar JPQL, consultas nativas e projeções para otimização de consultas.
- Implementar paginação e filtros dinâmicos.
- Integrar a aplicação com serviços de envio de e-mails transacionais.
- Centralizar regras de negócio em serviços e entidades quando apropriado.
- Melhorar a experiência de autenticação e gerenciamento de contas.
- Aplicar estratégias utilizadas em aplicações corporativas para escalabilidade, manutenção e segurança.

---

## 📂 Organização dos Packages

A evolução do ASJCatalog exigiu uma reorganização estrutural da aplicação para suportar novos requisitos de negócio, mecanismos de segurança, integrações externas e estratégias avançadas de persistência.

A arquitetura foi organizada seguindo princípios de separação de responsabilidades, alta coesão e baixo acoplamento, permitindo que cada módulo possua uma responsabilidade clara dentro do sistema.

Além das tradicionais camadas de persistência e exposição de APIs REST, foram incorporados componentes especializados para autenticação OAuth2, gerenciamento de tokens de negócio, recuperação de acesso, envio de e-mails transacionais, validações customizadas, projeções para consultas otimizadas e implementação de casos de uso completos.

Essa organização facilita a manutenção, evolução e testabilidade da aplicação, aproximando o projeto de arquiteturas encontradas em sistemas corporativos modernos.

---

## 🏗️ Estrutura Arquitetural da Aplicação

A estrutura abaixo apresenta a organização completa dos principais módulos do backend, evidenciando a separação entre domínio, aplicação, infraestrutura, segurança e exposição dos recursos REST.

Cada package possui uma responsabilidade específica dentro da arquitetura, reduzindo dependências indevidas e favorecendo a evolução independente dos componentes.

```text
📦 com.albertsilva.dev.asjcatalog
┃
┣ 📄 DscatalogApplication.java
┃
┣ 📂 config
┃ ┗ 📄 SpringDocOpenApiConfig.java
┃
┣ 📂 domain
┃ ┣ 📄 Identifiable.java
┃ ┃
┃ ┣ 📂 catalog
┃ ┃ ┣ 📄 Category.java
┃ ┃ ┗ 📄 Product.java
┃ ┃
┃ ┣ 📂 recovery
┃ ┃ ┣ 📄 Email.java
┃ ┃ ┣ 📄 Token.java
┃ ┃ ┃
┃ ┃ ┗ 📂 enums
┃ ┃   ┣ 📄 EmailStatus.java
┃ ┃   ┗ 📄 TokenType.java
┃ ┃
┃ ┗ 📂 user
┃   ┣ 📄 Role.java
┃   ┗ 📄 User.java
┃
┣ 📂 dto
┃ ┣ 📂 category
┃ ┃ ┣ 📂 request
┃ ┃ ┃ ┣ 📄 CategoryCreateRequest.java
┃ ┃ ┃ ┗ 📄 CategoryUpdateRequest.java
┃ ┃ ┃
┃ ┃ ┗ 📂 response
┃ ┃   ┣ 📄 CategoryDetailsResponse.java
┃ ┃   ┗ 📄 CategoryResponse.java
┃ ┃
┃ ┣ 📂 email
┃ ┃ ┗ 📂 request
┃ ┃   ┗ 📄 EmailRegisterRequest.java
┃ ┃
┃ ┣ 📂 product
┃ ┃ ┣ 📂 request
┃ ┃ ┃ ┣ 📄 ProductCreateRequest.java
┃ ┃ ┃ ┗ 📄 ProductUpdateRequest.java
┃ ┃ ┃
┃ ┃ ┗ 📂 response
┃ ┃   ┣ 📄 ProductDetailsResponse.java
┃ ┃   ┗ 📄 ProductResponse.java
┃ ┃
┃ ┣ 📂 role
┃ ┃ ┗ 📂 response
┃ ┃   ┗ 📄 RoleResponse.java
┃ ┃
┃ ┗ 📂 user
┃   ┣ 📂 request
┃   ┃ ┣ 📄 PasswordResetRequest.java
┃   ┃ ┣ 📄 UserCreateRequest.java
┃   ┃ ┣ 📄 UserEmailRequest.java
┃   ┃ ┣ 📄 UserRegisterRequest.java
┃   ┃ ┗ 📄 UserUpdateRequest.java
┃   ┃
┃   ┗ 📂 response
┃     ┣ 📄 UserDetailsResponse.java
┃     ┗ 📄 UserResponse.java
┃
┣ 📂 mapper
┃ ┣ 📂 category
┃ ┃ ┗ 📄 CategoryMapper.java
┃ ┣ 📂 product
┃ ┃ ┗ 📄 ProductMapper.java
┃ ┗ 📂 user
┃   ┗ 📄 UserMapper.java
┃
┣ 📂 projection
┃ ┣ 📄 ProductProjection.java
┃ ┗ 📄 UserDetailsProjection.java
┃
┣ 📂 repository
┃ ┣ 📄 CategoryRepository.java
┃ ┣ 📄 EmailRepository.java
┃ ┣ 📄 ProductRepository.java
┃ ┣ 📄 RoleRepository.java
┃ ┣ 📄 TokenRepository.java
┃ ┗ 📄 UserRepository.java
┃
┣ 📂 security
┃ ┣ 📂 auth
┃ ┃ ┗ 📄 AuthenticatedUserService.java
┃ ┃
┃ ┣ 📂 config
┃ ┃ ┗ 📄 SecurityBeansConfig.java
┃ ┃
┃ ┣ 📂 oauth2
┃ ┃ ┣ 📂 authorization
┃ ┃ ┃ ┗ 📂 config
┃ ┃ ┃   ┗ 📄 AuthorizationServerConfig.java
┃ ┃ ┃
┃ ┃ ┣ 📂 grant
┃ ┃ ┃ ┗ 📂 password
┃ ┃ ┃   ┣ 📄 CustomPasswordAuthenticationConverter.java
┃ ┃ ┃   ┣ 📄 CustomPasswordAuthenticationProvider.java
┃ ┃ ┃   ┗ 📄 CustomPasswordAuthenticationToken.java
┃ ┃ ┃
┃ ┃ ┗ 📂 resource
┃ ┃   ┗ 📂 config
┃ ┃     ┗ 📄 ResourceServerConfig.java
┃ ┃
┃ ┗ 📂 userdetails
┃   ┗ 📄 AuthenticatedUser.java
┃
┣ 📂 service
┃ ┣ 📄 AccountService.java
┃ ┣ 📄 CategoryService.java
┃ ┣ 📄 EmailService.java
┃ ┣ 📄 ProductService.java
┃ ┣ 📄 TokenService.java
┃ ┣ 📄 UserService.java
┃ ┃
┃ ┗ 📂 exception
┃   ┣ 📄 AuthenticatedUserNotFoundException.java
┃   ┣ 📄 DatabaseException.java
┃   ┣ 📄 InvalidTokenException.java
┃   ┗ 📄 ResourceNotFoundException.java
┃
┣ 📂 util
┃ ┗ 📄 IdentifiableUtils.java
┃
┣ 📂 validation
┃ ┣ 📂 category
┃ ┃ ┣ 📂 annotation
┃ ┃ ┃ ┣ 📄 CategoryCreateValid.java
┃ ┃ ┃ ┗ 📄 CategoryUpdateValid.java
┃ ┃ ┃
┃ ┃ ┗ 📂 validator
┃ ┃   ┣ 📄 CategoryCreateValidator.java
┃ ┃   ┗ 📄 CategoryUpdateValidator.java
┃ ┃
┃ ┣ 📂 product
┃ ┃ ┣ 📂 annotation
┃ ┃ ┃ ┣ 📄 ProductCreateValid.java
┃ ┃ ┃ ┗ 📄 ProductUpdateValid.java
┃ ┃ ┃
┃ ┃ ┗ 📂 validator
┃ ┃   ┣ 📄 ProductCreateValidator.java
┃ ┃   ┗ 📄 ProductUpdateValidator.java
┃ ┃
┃ ┣ 📂 role
┃ ┃ ┣ 📂 annotation
┃ ┃ ┃ ┗ 📄 ValidRoles.java
┃ ┃ ┃
┃ ┃ ┗ 📂 validator
┃ ┃   ┗ 📄 ValidRolesValidator.java
┃ ┃
┃ ┗ 📂 user
┃   ┣ 📂 annotation
┃   ┃ ┣ 📄 StrongPassword.java
┃   ┃ ┣ 📄 UniqueEmail.java
┃   ┃ ┣ 📄 UserCreateValid.java
┃   ┃ ┣ 📄 UserUpdateValid.java
┃   ┃ ┗ 📄 ValidEmail.java
┃   ┃
┃   ┗ 📂 validator
┃     ┣ 📄 StrongPasswordValidator.java
┃     ┣ 📄 UniqueEmailValidator.java
┃     ┣ 📄 UserCreateValidator.java
┃     ┣ 📄 UserUpdateValidator.java
┃     ┗ 📄 ValidEmailValidator.java
┃
┣ 📂 web
┃ ┣ 📂 controller
┃ ┃ ┣ 📄 AccountController.java
┃ ┃ ┣ 📄 CategoryController.java
┃ ┃ ┣ 📄 ProductController.java
┃ ┃ ┗ 📄 UserController.java
┃ ┃
┃ ┗ 📂 exception
┃   ┣ 📂 enums
┃   ┃ ┗ 📄 ErrorType.java
┃   ┃
┃   ┣ 📂 handler
┃   ┃ ┗ 📄 ControllerExceptionHandler.java
┃   ┃
┃   ┗ 📂 response
┃     ┣ 📄 FieldMessage.java
┃     ┣ 📄 ProblemDetails.java
┃     ┗ 📄 ValidationError.java
┃
┗ 📂 resources
  ┣ 📂 db
  ┃ ┗ 📂 migration
  ┃   ┣ 📂 data
  ┃   ┃ ┣ 📄 V100__insert_categories.sql
  ┃   ┃ ┣ 📄 V101__insert_products.sql
  ┃   ┃ ┣ 📄 V102__insert_product_category.sql
  ┃   ┃ ┣ 📄 V103__insert_user.sql
  ┃   ┃ ┣ 📄 V104__insert_role.sql
  ┃   ┃ ┗ 📄 V105__insert_user_role.sql
  ┃   ┃
  ┃   ┗ 📂 schema
  ┃     ┣ 📄 V001__create_table_category.sql
  ┃     ┣ 📄 V002__create_table_product.sql
  ┃     ┣ 📄 V003__create_table_product_category.sql
  ┃     ┣ 📄 V004__alter_table_product_category.sql
  ┃     ┣ 📄 V005__create_table_role.sql
  ┃     ┣ 📄 V006__create_table_user.sql
  ┃     ┣ 📄 V007__create_table_user_role.sql
  ┃     ┣ 📄 V008__alter_table_user_role.sql
  ┃     ┣ 📄 V009__create_table_token.sql
  ┃     ┣ 📄 V010__alter_table_token.sql
  ┃     ┗ 📄 V011__create_table_email.sql
  ┃
  ┣ 📂 META-INF
  ┃ ┗ 📄 additional-spring-configuration-metadata.json
  ┃
  ┣ 📂 static
  ┃ ┗ 📂 image
  ┃
  ┣ 📂 templates
  ┃ ┣ 📄 activate_user_by_email_template.html
  ┃ ┣ 📄 reactivate_user_by_email_template.html
  ┃ ┗ 📄 reset_password_email_template.html
  ┃
  ┣ 📄 application.properties
  ┣ 📄 application-dev.properties
  ┣ 📄 application-test.properties
  ┣ 📄 application-prod.properties
  ┣ 📄 ValidationMessages.properties
  ┣ 📄 banner-dev.txt
  ┗ 📄 import.sql
```

### Principais Responsabilidades

| Package      | Responsabilidade                                     |
| ------------ | ---------------------------------------------------- |
| `config`     | Configurações globais da aplicação                   |
| `domain`     | Objetos centrais do domínio e regras de negócio      |
| `dto`        | Contratos de entrada e saída da API                  |
| `mapper`     | Conversão entre entidades e DTOs                     |
| `projection` | Projeções utilizadas para consultas otimizadas       |
| `repository` | Acesso e persistência de dados                       |
| `security`   | Autenticação, autorização e infraestrutura OAuth2    |
| `service`    | Implementação dos casos de uso da aplicação          |
| `validation` | Validações customizadas e regras de consistência     |
| `web`        | Exposição dos endpoints REST                         |
| `resources`  | Configurações, templates e scripts de banco de dados |

<p>A adoção dessa estrutura permite que novas funcionalidades sejam adicionadas sem impactar diretamente os demais módulos, favorecendo escalabilidade e manutenibilidade a longo prazo.</p>

---
## 🧩 Organização da Camada de Domínio

Uma das principais evoluções arquiteturais deste capítulo foi a transformação da antiga camada baseada apenas em entidades persistentes para uma camada efetivamente orientada ao domínio.

Nas primeiras versões do projeto, as classes eram organizadas em um package denominado `entity`, refletindo principalmente sua função de mapeamento para o banco de dados. Com o crescimento da aplicação e o surgimento de novos requisitos de negócio, essa abordagem passou a limitar a expressividade do modelo.

Para representar de forma mais adequada os conceitos centrais do sistema, o package foi evoluído para `domain`, refletindo uma visão mais próxima das práticas adotadas em arquiteturas orientadas ao domínio.

Essa mudança vai além de uma simples alteração de nomenclatura. Ela representa uma mudança de perspectiva: os objetos passaram a ser vistos como elementos do negócio e não apenas como registros persistidos no banco de dados.

### Estrutura Atual do Domínio

```text
📂 domain
┃
┣ 📄 Identifiable.java
┃
┣ 📂 catalog
┃ ┣ 📄 Category.java
┃ ┗ 📄 Product.java
┃
┣ 📂 user
┃ ┣ 📄 User.java
┃ ┗ 📄 Role.java
┃
┗ 📂 recovery
  ┣ 📄 Token.java
  ┣ 📄 Email.java
  ┗ 📂 enums
    ┣ 📄 TokenType.java
    ┗ 📄 EmailStatus.java
```

### Módulo Catalog
Responsável pelos conceitos relacionados ao catálogo de produtos da aplicação.

```package
catalog
├── Product
└── Category
```
Contém as entidades responsáveis pela representação dos produtos comercializados e suas respectivas categorias, além dos relacionamentos necessários para consultas e regras de negócio do catálogo.

### Módulo User

Centraliza os conceitos relacionados à identidade e autorização dos usuários.

```text
user
├── User
└── Role
```

Esse módulo é responsável por representar usuários autenticados, perfis de acesso e relacionamentos utilizados pelos mecanismos de autorização implementados com Spring Security.

### Módulo Recovery

Introduzido para suportar fluxos de negócio relacionados ao gerenciamento de acesso e comunicação com usuários.

```text
recovery
├── Token
├── Email
├── TokenType
└── EmailStatus
```

Esse módulo concentra os conceitos utilizados nos processos de:

- Ativação de conta
- Reenvio de ativação
- Recuperação de senha
- Redefinição de senha
- Controle de envio de e-mails transacionais

A criação desse módulo permitiu encapsular responsabilidades específicas de recuperação de acesso sem sobrecarregar as entidades relacionadas aos usuários.

---
## 🧠 Conceitos Fundamentais Trabalhados

Durante o desenvolvimento deste capítulo, foram aplicados diversos conceitos fundamentais de arquitetura backend, persistência de dados e modelagem de domínio. Mais do que utilizar recursos do Spring Boot, a implementação buscou aproximar o projeto das práticas adotadas em aplicações corporativas, com foco em organização arquitetural, separação de responsabilidades, desempenho e manutenção.

A tabela a seguir resume os principais conceitos explorados e a forma como cada um foi aplicado no ASJCatalog.

| 🧩 Conceito                         | 📖 Aplicação no DSCatalog                                                                                                                                                   | 🎯 Objetivo                                                                    |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| **Domain Modeling**                 | Organização do domínio em módulos (`catalog`, `user` e `recovery`) contendo entidades que representam conceitos do negócio.                                                 | Tornar o modelo mais expressivo e alinhado às regras de negócio.               |
| **Domain-Driven Design (DDD)**      | Evolução da antiga camada `entity` para `domain`, aproximando a estrutura da linguagem do domínio e separando responsabilidades por subdomínios.                            | Melhorar organização arquitetural, legibilidade e escalabilidade.              |
| **ORM (Object-Relational Mapping)** | Mapeamento entre objetos Java e tabelas do PostgreSQL utilizando JPA e Hibernate.                                                                                           | Eliminar SQL manual para operações de persistência.                            |
| **Spring Data JPA**                 | Implementação dos repositórios utilizando interfaces derivadas de `JpaRepository` e consultas customizadas.                                                                 | Simplificar operações de acesso aos dados.                                     |
| **Hibernate**                       | Responsável pela implementação da JPA, gerenciamento do ciclo de vida das entidades e carregamento de relacionamentos.                                                      | Automatizar a persistência orientada a objetos.                                |
| **Relacionamentos JPA**             | Utilização de associações como `@ManyToMany`, `@OneToMany` e `@ManyToOne` entre usuários, papéis, produtos, categorias e tokens.                                            | Representar corretamente as relações existentes no domínio.                    |
| **JPQL**                            | Consultas orientadas às entidades para recuperação de dados utilizando a linguagem de consultas da JPA.                                                                     | Escrever consultas independentes do banco de dados.                            |
| **Native SQL**                      | Consulta nativa utilizada na paginação de produtos com filtros por categorias, reduzindo custo das consultas complexas.                                                     | Obter melhor desempenho em cenários específicos.                               |
| **Projection Pattern**              | Interfaces como `ProductProjection` e `UserDetailsProjection` retornam apenas os atributos necessários das consultas.                                                       | Reduzir transferência de dados e aumentar eficiência.                          |
| **Paginação**                       | Utilização de `Pageable` e `Page` para retorno paginado de produtos e usuários.                                                                                             | Melhorar escalabilidade em consultas com grandes volumes de dados.             |
| **Filtros Dinâmicos**               | Busca por nome e categorias utilizando parâmetros opcionais nas consultas.                                                                                                  | Permitir consultas flexíveis sem duplicação de código.                         |
| **Fetch Join**                      | Estratégia utilizada para carregar categorias juntamente com produtos em uma única consulta.                                                                                | Eliminar consultas adicionais provocadas pelo carregamento lazy.               |
| **Problema N+1 Select**             | Solucionado através da combinação entre consultas nativas, projeções e `JOIN FETCH`.                                                                                        | Reduzir drasticamente o número de consultas executadas pelo Hibernate.         |
| **Service Layer**                   | Serviços especializados (`AccountService`, `UserService`, `ProductService`, `CategoryService`, `TokenService`, `EmailService`) concentram a implementação dos casos de uso. | Centralizar regras de negócio e desacoplar controllers da persistência.        |
| **Repository Pattern**              | Repositórios responsáveis exclusivamente pelo acesso aos dados, abstraindo detalhes da persistência.                                                                        | Separar regras de negócio das operações de banco de dados.                     |
| **DTO Pattern**                     | Utilização de objetos específicos para entrada e saída de dados da API.                                                                                                     | Evitar exposição direta das entidades do domínio.                              |
| **Mapper Pattern**                  | Conversão entre entidades e DTOs através de classes dedicadas de mapeamento.                                                                                                | Reduzir acoplamento entre domínio e camada de apresentação.                    |
| **Transactional Management**        | Métodos anotados com `@Transactional` garantem consistência durante operações de escrita e leitura.                                                                         | Assegurar integridade das transações e controle do contexto de persistência.   |
| **Business Use Cases**              | Implementação completa dos fluxos de cadastro, ativação de conta, recuperação de senha, redefinição de senha e obtenção do usuário autenticado.                             | Aproximar a aplicação de cenários reais encontrados em sistemas corporativos.  |
| **Business Tokens**                 | A entidade `Token` encapsula criação, validação, expiração e invalidação de tokens para ativação de conta e recuperação de senha.                                           | Garantir segurança e encapsular regras do domínio diretamente na entidade.     |
| **Factory Methods**                 | Métodos estáticos como `activationToken()` e `passwordRecoveryToken()` criam tokens de negócio com regras padronizadas.                                                     | Padronizar a criação de objetos complexos e evitar duplicação de lógica.       |
| **Transactional Email**             | Integração entre `EmailService`, templates HTML e Spring Mail para envio de e-mails de ativação e recuperação de senha.                                                     | Automatizar comunicações transacionais com usuários.                           |
| **Authentication Context**          | Serviço `AuthenticatedUserService` centraliza a recuperação do usuário autenticado a partir do JWT presente no `SecurityContext`.                                           | Desacoplar a infraestrutura de segurança das regras de negócio.                |
| **Spring Security Integration**     | Implementação de `UserDetailsService`, `GrantedAuthority` e consultas personalizadas para autenticação baseada em OAuth2 e JWT.                                             | Integrar autenticação e autorização ao modelo de domínio da aplicação.         |
| **Exception Handling**              | Exceções específicas, como `InvalidTokenException`, `ResourceNotFoundException` e `AuthenticatedUserNotFoundException`, representam erros de negócio de forma explícita.    | Padronizar o tratamento de erros e melhorar a legibilidade da aplicação.       |

---

## 🛠️ Tecnologias e Frameworks Utilizados

Ao longo deste capítulo, o ASJCatalog evoluiu para uma aplicação backend mais próxima dos padrões encontrados em sistemas corporativos. Para suportar os novos requisitos de negócio, autenticação, persistência, comunicação por e-mail e documentação da API, foram integradas diversas tecnologias do ecossistema Java e Spring.

Cada ferramenta foi adotada com um propósito específico, contribuindo para aspectos como produtividade, organização arquitetural, segurança, desempenho, manutenibilidade e escalabilidade.

A tabela abaixo apresenta as principais tecnologias utilizadas durante a implementação deste capítulo.

| 🛠️ Tecnologia                             | 📦 Versão    | 📖 Utilização no Projeto           | 🎯 Objetivo                                                                                               |
| ------------------------------------------ | ------------ | ---------------------------------- | --------------------------------------------------------------------------------------------------------- |
| **Java**                                   | 17 LTS       | Linguagem principal da aplicação   | Base da implementação utilizando recursos modernos da linguagem.                                          |
| **Spring Boot**                            | 3.5.x        | Framework principal do backend     | Simplificar configuração, inicialização e desenvolvimento da aplicação.                                   |
| **Spring Web (Spring MVC)**                | Starter      | Implementação da API REST          | Exposição dos endpoints HTTP da aplicação.                                                                |
| **Spring Data JPA**                        | Starter      | Camada de persistência             | Abstração do acesso ao banco de dados através de repositórios.                                            |
| **Hibernate ORM**                          | 6.x          | Implementação da especificação JPA | Gerenciamento do ciclo de vida das entidades e mapeamento objeto-relacional.                              |
| **PostgreSQL**                             | Runtime      | Banco de dados principal           | Persistência dos dados em ambiente de desenvolvimento e produção.                                         |
| **H2 Database**                            | Runtime      | Banco em memória                   | Execução de testes rápidos sem dependência de infraestrutura externa.                                     |
| **Flyway**                                 | Starter      | Versionamento do banco de dados    | Controle evolutivo do schema e dos dados iniciais através de migrations.                                  |
| **Spring Validation (Jakarta Validation)** | Starter      | Validação de dados                 | Garantir consistência dos dados recebidos pela API utilizando Bean Validation e validadores customizados. |
| **Spring Security**                        | Starter      | Segurança da aplicação             | Implementação de autenticação, autorização e proteção dos recursos REST.                                  |
| **Spring Authorization Server**            | Atual        | Servidor OAuth2                    | Emissão e gerenciamento de tokens de acesso seguindo o protocolo OAuth2.                                  |
| **OAuth2 Resource Server**                 | Starter      | Validação de JWT                   | Proteção dos endpoints utilizando tokens JWT assinados.                                                   |
| **JWT (JSON Web Token)**                   | OAuth2       | Autenticação stateless             | Transporte seguro das credenciais entre cliente e servidor.                                               |
| **Spring Mail**                            | Starter      | Envio de e-mails                   | Disparo de e-mails transacionais para ativação de conta e recuperação de senha.                           |
| **Thymeleaf**                              | Starter      | Templates HTML                     | Geração dinâmica dos templates de e-mail enviados aos usuários.                                           |
| **SpringDoc OpenAPI**                      | 2.8.x        | Documentação automática            | Geração da especificação OpenAPI e interface Swagger UI.                                                  |
| **Maven**                                  | Build Tool   | Gerenciamento do projeto           | Gerenciamento de dependências, plugins e ciclo de build da aplicação.                                     |
| **JUnit 5**                                | Starter Test | Testes automatizados               | Base para criação dos testes da aplicação.                                                                |
| **Spring Security Test**                   | Starter Test | Testes de segurança                | Suporte para testes envolvendo autenticação e autorização.                                                |

---
### ⚙️ Recursos da Plataforma Utilizados

Além dos frameworks principais, este capítulo incorporou diversos recursos da plataforma Spring para tornar a aplicação mais robusta e preparada para diferentes ambientes de execução.

| ⚙️ Recurso                                            | 📖 Aplicação                                                                                             |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| **Profiles (`application-dev` e `application-test`)** | Separação de configurações por ambiente.                                                                 |
| **Externalização de Configurações**                   | Utilização de variáveis de ambiente para credenciais, URLs e configurações sensíveis.                    |
| **Logging Configurável**                              | Configuração de níveis de log para aplicação, Spring, Hibernate e Flyway, incluindo rotação de arquivos. |
| **Open Session in View Desabilitado**                 | Evita consultas inesperadas fora da camada de serviço (`spring.jpa.open-in-view=false`).                 |
| **Migrations Versionadas**                            | Organização dos scripts Flyway em módulos distintos (`schema` e `data`).                                 |
| **Validação Internacionalizada**                      | Centralização das mensagens de validação no arquivo `ValidationMessages.properties`.                     |
| **Swagger Customizado**                               | Documentação disponível em endpoints personalizados para OpenAPI e Swagger UI.                           |
| **Configuração de CORS**                              | Controle dos domínios autorizados a consumir a API.                                                      |
| **SMTP Configurável**                                 | Configuração parametrizada do serviço de envio de e-mails.                                               |
| **Controle de Expiração de Tokens**                   | Configuração externa para tempo de validade dos tokens de ativação e recuperação de senha.               |

### 🏗️ Organização Tecnológica

```java
                  Backend Stack

                    Java 17
                       │
               Spring Boot 3.5
                       │
 ┌───────────────┬───────────────┬────────────────┐
 │               │               │                │
Spring Web   Spring Data JPA  Spring Security  Spring Mail
 │               │               │                │
 │          Hibernate ORM     OAuth2 + JWT    Thymeleaf
 │               │               │                │
 └───────────────┴───────┬───────┴────────────────┘
                         │
                     PostgreSQL
                         │
                      Flyway
```

---

## 🗄️ Modelagem ORM

A camada de persistência do **ASJCatalog** foi construída utilizando **JPA (Jakarta Persistence API)** com implementação provida pelo **Hibernate**, permitindo mapear objetos Java para tabelas relacionais do PostgreSQL.

A modelagem foi organizada para representar o domínio da aplicação de forma expressiva, utilizando entidades, relacionamentos, consultas especializadas e estratégias de otimização de acesso aos dados.

---

### 📦 Modelo de Domínio

O domínio da aplicação foi dividido em módulos independentes, cada um responsável por um conjunto específico de regras de negócio.

| Entidade | Responsabilidade |
|----------|------------------|
| **Product** | Representa os produtos disponíveis no catálogo. |
| **Category** | Organiza os produtos em categorias. |
| **User** | Representa os usuários autenticados da aplicação. |
| **Role** | Define os perfis de acesso utilizados pelo Spring Security (RBAC). |
| **Token** | Gerencia tokens de ativação de conta e recuperação de senha. |
| **Email** | Registra o envio de e-mails transacionais da aplicação. |

Essa organização favorece alta coesão, separação de responsabilidades e evolução independente de cada módulo do domínio.

---

### 🔄 Relacionamentos entre Entidades

A modelagem utiliza os principais tipos de relacionamentos disponibilizados pela JPA.

```text
                   Many-to-Many
+-----------+ <----------------------> +------------+
| Category  |                          |  Product   |
+-----------+                          +------------+

                   Many-to-Many
+---------+ <------------------------> +---------+
|  Role   |                            |  User   |
+---------+                            +---------+

                   One-to-Many
+---------+ -------------------------> +---------+
|  User   |                            | Token   |
+---------+ <------------------------- +---------+
                    Many-to-One

                   One-to-Many
+---------+ -------------------------> +---------+
|  User   |                            | Email   |
+---------+ <------------------------- +---------+
                    Many-to-One
```

#### Principais associações

| Relacionamento | Objetivo |
|---------------|----------|
| `Product ↔ Category` | Permite que um produto pertença a várias categorias e uma categoria possua vários produtos. |
| `User ↔ Role` | Implementa o controle de acesso baseado em papéis (RBAC). |
| `User → Token` | Gerencia tokens utilizados em ativação de conta e recuperação de senha. |
| `User → Email` | Mantém o histórico de e-mails enviados ao usuário. |

---

### 🔍 Consultas ao Banco de Dados

A camada de persistência utiliza diferentes estratégias de consulta de acordo com a complexidade da operação.

| Estratégia | Aplicação |
|------------|-----------|
| **Query Methods** | Consultas derivadas automaticamente pelo Spring Data JPA, como `findByNameContainingIgnoreCase()`. |
| **JPQL** | Consultas orientadas às entidades, como o carregamento de produtos utilizando `JOIN FETCH`. |
| **Native SQL** | Consulta otimizada para paginação e filtragem por categorias em relacionamentos muitos-para-muitos. |
| **Repositories** | Centralizam toda a comunicação com o banco de dados através do padrão Repository. |

Essa combinação permite utilizar consultas simples quando possível e consultas especializadas quando maior desempenho é necessário.

---

### 🚀 Estratégias de Otimização

Além da modelagem ORM, foram aplicadas técnicas para reduzir o custo das consultas e melhorar o desempenho da aplicação.

| Técnica | Como foi aplicada | Benefício |
|----------|-------------------|-----------|
| **Projection** | `ProductProjection` retorna apenas os campos necessários durante a paginação. | Reduz transferência de dados e consumo de memória. |
| **Native Query** | Primeira consulta recupera apenas os IDs dos produtos filtrados. | Paginação eficiente em relacionamentos muitos-para-muitos. |
| **Fetch Join** | Segunda consulta utiliza `JOIN FETCH` para carregar produtos e categorias em uma única operação. | Evita consultas adicionais para carregamento das categorias. |
| **Reordenação dos Resultados** | `IdentifiableUtils.reorderByReference()` preserva a ordem retornada pela consulta paginada. | Mantém consistência entre paginação e carregamento das entidades. |
| **Eliminação do N+1 Select** | Combinação entre Projection, Native SQL e Fetch Join para carregar produtos e categorias em apenas duas consultas controladas. | Elimina dezenas ou centenas de consultas extras em cenários paginados. |

#### Fluxo da otimização

```text
Cliente
    │
    ▼
Native SQL + Projection
(busca apenas IDs paginados)
    │
    ▼
Lista de IDs
    │
    ▼
JPQL + JOIN FETCH
(carrega produtos + categorias)
    │
    ▼
Reordenação da lista
    │
    ▼
Mapper → DTO
    │
    ▼
Resposta da API
```

Essa estratégia elimina o problema clássico de **N+1 Select**, mantendo a paginação eficiente e garantindo o carregamento completo das categorias associadas aos produtos com um número reduzido de consultas ao banco de dados.

---
## 👨‍💻 Autor

**Albert Silva de Jesus**  
Desenvolvedor Backend Java | Spring Boot

---
## 📎 Contato

[![LinkedIn](https://img.shields.io/badge/LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/albert-backend-java-spring-boot/)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com)
