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
## 📑 Sumário

> Navegação do capítulo.

---

| 🧩 Module | ⚡ Description |
| ------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| [📚 Contexto da Implementação](#-contexto-da-implementação) | Contexto da evolução arquitetural e dos novos requisitos de negócio |
| [🎯 Objetivos](#-objetivos) | Metas técnicas e arquiteturais implementadas neste módulo |
| [📂 Organização dos Packages](#-organização-dos-packages) | Estrutura modular da aplicação e responsabilidades das camadas |
| [🧩 Organização da Camada de Domínio](#-organização-da-camada-de-domínio) | Evolução da camada de domínio e organização dos subdomínios |
| [🧠 Conceitos Fundamentais Trabalhados](#-conceitos-fundamentais-trabalhados) | Conceitos de arquitetura, persistência e modelagem aplicados |
| [🛠️ Tecnologias e Frameworks Utilizados](#️-tecnologias-e-frameworks-utilizados) | Stack tecnológica empregada na evolução da aplicação |
| [🗄️ Modelagem ORM](#️-modelagem-orm) | Modelagem das entidades, relacionamentos e estratégias de persistência |
| [🎯 Casos de Uso](#-casos-de-uso) | Fluxos de negócio implementados na camada de serviços |
| [🔍 Consultas ao Banco](#-consultas-ao-banco) | Estratégias de consultas utilizando Spring Data JPA, JPQL e Native SQL |
| [🚀 Otimizações de Persistência](#-otimizações-de-persistência) | Técnicas aplicadas para otimização de desempenho e eliminação do N+1 Select |
| [📧 Integração com E-mail](#-integração-com-e-mail) | Fluxo de envio de e-mails transacionais e gerenciamento de tokens |
| [🧱 Boas Práticas Aplicadas](#-boas-práticas-aplicadas) | Padrões arquiteturais e boas práticas adotadas durante a implementação |
| [📈 Evolução Arquitetural](#-evolução-arquitetural) | Principais evoluções estruturais da aplicação |
| [🎓 Aprendizados](#-aprendizados) | Conhecimentos consolidados ao longo deste capítulo |
| [💼 Competências Técnicas Desenvolvidas](#-competências-técnicas-desenvolvidas) | Competências adquiridas com a implementação |
| [🏁 Conclusão](#-conclusão) | Considerações finais sobre a evolução da arquitetura |
| [📚 Referências Técnicas](#-referências-técnicas) | Documentações oficiais e materiais utilizados |
| [👨‍💻 Autor](#-autor) | Informações sobre o autor da documentação |
| [📎 Contato](#-contato) | Canais de contato e redes profissionais |

---

## 📚 Contexto da Implementação

Após a implementação da infraestrutura de autenticação e autorização baseada em Spring Security, OAuth2 e JWT, o ASJCatalog evolui para incorporar fluxos de negócio mais próximos dos requisitos encontrados em aplicações corporativas reais.
Neste modulo foram implementados casos de uso completos relacionados ao ciclo de vida da conta do usuário, além da evolução da camada de persistência utilizando JPA/Hibernate, consultas otimizadas e integração com serviços de e-mail.
O foco principal foi construir fluxos de negócio completos, desacoplados e alinhados com boas práticas de arquitetura backend.

---

## 🎯 Objetivos

Os principais objetivos deste módulo são:

- Evoluir a modelagem ORM da aplicação.
- Implementar casos de uso completos relacionados à gestão de contas de usuário.
- Aplicar conceitos de Domain-Driven Design na modelagem de negócio.
- Implementar mecanismos de ativação e recuperação de acesso.
- Resolver problemas de performance relacionados ao carregamento de entidades (N + 1 Select).
- Utilizar JPQL, consultas nativas e projeções para otimização de consultas.
- Implementar paginação e filtros dinâmicos.
- Integrar a aplicação com serviços de envio de e-mails transacionais.
- Centralizar regras de negócio em serviços e entidades quando apropriado.
- Melhorar a experiência de autenticação e gerenciamento de contas.
- Aplicar estratégias utilizadas em aplicações corporativas para escalabilidade, manutenção e segurança.

---

## 📂 Organização dos Packages

A evolução do ASJCatalog exigiu uma reorganização estrutural da aplicação para suportar novos requisitos de negócio, mecanismos de segurança, integrações externas e estratégias avançadas de persistência.

A arquitetura foi organizada com base nos princípios de separação de responsabilidades, alta coesão e baixo acoplamento, permitindo que cada módulo possua responsabilidades bem definidas dentro do sistema.

Além das tradicionais camadas de persistência e exposição de APIs REST, foram incorporados componentes especializados para autenticação OAuth2, gerenciamento de tokens de negócio, recuperação de acesso, envio de e-mails transacionais, validações customizadas, projeções para consultas otimizadas e implementação de casos de uso completos.

Essa organização favorece a manutenção, a evolução e a testabilidade da aplicação, aproximando sua arquitetura dos padrões adotados em sistemas corporativos modernos.

---

## 🏗️ Estrutura Arquitetural da Aplicação

A estrutura abaixo apresenta a organização completa dos principais módulos do backend, evidenciando a separação entre domínio, aplicação, infraestrutura, segurança e exposição dos recursos REST.

Cada package possui responsabilidades bem definidas, reduzindo o acoplamento entre os componentes e favorecendo sua evolução independente.

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

Essa mudança vai além de uma alteração de nomenclatura. Ela representa uma mudança de perspectiva: as entidades passaram a representar conceitos centrais do domínio, deixando de ser tratadas apenas como estruturas de persistência.

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

Durante a evolução da aplicação, foram aplicados diversos conceitos fundamentais de arquitetura backend, persistência de dados e modelagem de domínio. Mais do que utilizar os recursos oferecidos pelo ecossistema Spring, a implementação buscou aproximar o projeto das práticas adotadas em aplicações corporativas, com foco em organização arquitetural, separação de responsabilidades, desempenho e manutenção.

A tabela a seguir resume os principais conceitos explorados e a forma como cada um foi aplicado no ASJCatalog.

| 🧩 Conceito                         | 📖 Aplicação no ASJCatalog                                                                                                                                                  | 🎯 Objetivo                                                                   |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **Domain Modeling**                 | Organização do domínio em módulos (`catalog`, `user` e `recovery`) contendo entidades que representam conceitos do negócio.                                                 | Tornar o modelo mais expressivo e alinhado às regras de negócio.              |
| **Domain-Driven Design (DDD)**      | Evolução da antiga camada `entity` para `domain`, aproximando a estrutura da linguagem do domínio e separando responsabilidades por subdomínios.                            | Melhorar organização arquitetural, legibilidade e escalabilidade.             |
| **ORM (Object-Relational Mapping)** | Mapeamento objeto-relacional entre entidades Java e tabelas do PostgreSQL utilizando Jakarta Persistence (JPA) com Hibernate como provedor ORM.                             | Eliminar SQL manual para operações de persistência.                           |
| **Spring Data JPA**                 | Implementação dos repositórios utilizando interfaces derivadas de `JpaRepository` e consultas customizadas.                                                                 | Simplificar operações de acesso aos dados.                                    |
| **Hibernate**                       | Provedor ORM responsável pela implementação da especificação Jakarta Persistence (JPA), gerenciamento do ciclo de vida das entidades e carregamento de relacionamentos.     | Automatizar a persistência orientada a objetos.                               |
| **Relacionamentos JPA**             | Utilização de associações como `@ManyToMany`, `@OneToMany` e `@ManyToOne` entre usuários, papéis, produtos, categorias e tokens.                                            | Representar corretamente as relações existentes no domínio.                   |
| **JPQL**                            | Consultas orientadas às entidades para recuperação de dados utilizando a linguagem de consultas da JPA.                                                                     | Escrever consultas independentes do banco de dados.                           |
| **Native SQL**                      | Consulta nativa utilizada na paginação de produtos com filtros por categorias, reduzindo custo das consultas complexas.                                                     | Obter melhor desempenho em cenários específicos.                              |
| **Projection Pattern**              | Interfaces como `ProductProjection` e `UserDetailsProjection` retornam apenas os atributos necessários das consultas.                                                       | Reduzir transferência de dados e aumentar eficiência.                         |
| **Paginação**                       | Utilização de `Pageable` e `Page` para retorno paginado de produtos e usuários.                                                                                             | Melhorar escalabilidade em consultas com grandes volumes de dados.            |
| **Filtros Dinâmicos**               | Busca por nome e categorias utilizando parâmetros opcionais nas consultas.                                                                                                  | Permitir consultas flexíveis sem duplicação de código.                        |
| **Fetch Join**                      | Estratégia utilizada para carregar categorias juntamente com produtos em uma única consulta.                                                                                | Eliminar consultas adicionais provocadas pelo carregamento lazy.              |
| **Problema N+1 Select**             | Solucionado através da combinação entre consultas nativas, projeções e `JOIN FETCH`.                                                                                        | Reduzir drasticamente o número de consultas executadas pelo Hibernate.        |
| **Service Layer**                   | Serviços especializados (`AccountService`, `UserService`, `ProductService`, `CategoryService`, `TokenService`, `EmailService`) concentram a implementação dos casos de uso. | Centralizar regras de negócio e desacoplar controllers da persistência.       |
| **Repository Pattern**              | Repositórios responsáveis exclusivamente pelo acesso aos dados, abstraindo detalhes da persistência.                                                                        | Separar regras de negócio das operações de banco de dados.                    |
| **DTO Pattern**                     | Utilização de objetos específicos para entrada e saída de dados da API.                                                                                                     | Evitar exposição direta das entidades do domínio.                             |
| **Mapper Pattern**                  | Conversão entre entidades e DTOs através de classes dedicadas de mapeamento.                                                                                                | Reduzir acoplamento entre domínio e camada de apresentação.                   |
| **Transactional Management**        | Métodos anotados com `@Transactional` garantem consistência durante operações de escrita e leitura.                                                                         | Assegurar integridade das transações e controle do contexto de persistência.  |
| **Business Use Cases**              | Implementação completa dos fluxos de cadastro, ativação de conta, recuperação de senha, redefinição de senha e obtenção do usuário autenticado.                             | Aproximar a aplicação de cenários reais encontrados em sistemas corporativos. |
| **Business Tokens**                 | A entidade `Token` encapsula criação, validação, expiração e invalidação de tokens para ativação de conta e recuperação de senha.                                           | Garantir segurança e encapsular regras do domínio diretamente na entidade.    |
| **Factory Methods**                 | Métodos estáticos como `activationToken()` e `passwordRecoveryToken()` criam tokens de negócio com regras padronizadas.                                                     | Padronizar a criação de objetos complexos e evitar duplicação de lógica.      |
| **Transactional Email**             | Integração entre `EmailService`, templates HTML e Spring Mail para envio de e-mails de ativação e recuperação de senha.                                                     | Automatizar comunicações transacionais com usuários.                          |
| **Authentication Context**          | Serviço `AuthenticatedUserService` centraliza a recuperação do usuário autenticado a partir do JWT presente no `SecurityContext`.                                           | Desacoplar a infraestrutura de segurança das regras de negócio.               |
| **Spring Security Integration**     | Implementação de `UserDetailsService`, `GrantedAuthority` e consultas personalizadas para autenticação baseada em OAuth2 e JWT.                                             | Integrar autenticação e autorização ao modelo de domínio da aplicação.        |
| **Exception Handling**              | Exceções específicas, como `InvalidTokenException`, `ResourceNotFoundException` e `AuthenticatedUserNotFoundException`, representam erros de negócio de forma explícita.    | Padronizar o tratamento de erros e melhorar a legibilidade da aplicação.      |

---

## 🛠️ Tecnologias e Frameworks Utilizados

Ao longo desta etapa de evolução do ASJCatalog, a aplicação passou a incorporar recursos normalmente encontrados em sistemas corporativos. Para atender aos novos requisitos de negócio, autenticação, persistência, comunicação por e-mail e documentação da API, foram integradas diversas tecnologias do ecossistema Java e Spring.

Cada ferramenta foi adotada com um propósito específico, contribuindo para aspectos como produtividade, organização arquitetural, segurança, desempenho, manutenibilidade e escalabilidade.

A tabela a seguir resume as principais tecnologias empregadas na evolução da aplicação.

| 🛠️ Tecnologia                              | 📦 Versão    | 📖 Utilização no Projeto                                    | 🎯 Objetivo                                                                                                                       |
| ------------------------------------------ | ------------ | ----------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| **Java**                                   | 17 LTS       | Linguagem principal da aplicação                            | Base da implementação da aplicação, utilizando recursos modernos da linguagem e da plataforma Java.                               |
| **Spring Boot**                            | 3.5.x        | Framework principal do backend                              | Simplificar a configuração, inicialização e execução da aplicação por meio do ecossistema Spring.                                 |
| **Spring Web (Spring MVC)**                | Starter      | Implementação da API REST                                   | Exposição dos endpoints HTTP da aplicação.                                                                                        |
| **Spring Data JPA**                        | Starter      | Camada de persistência                                      | Abstração da camada de persistência por meio de repositórios e consultas orientadas ao domínio.                                   |
| **Hibernate ORM**                          | 6.x          | Implementação da especificação JPA                          | Implementação da especificação JPA, responsável pelo mapeamento objeto-relacional e gerenciamento do ciclo de vida das entidades. |
| **PostgreSQL**                             | Runtime      | Banco de dados principal                                    | Sistema gerenciador de banco de dados relacional utilizado para persistência da aplicação.                                        |
| **H2 Database**                            | Runtime      | Banco em memória                                            | Banco de dados em memória utilizado para testes e cenários de desenvolvimento.                                                    |
| **Flyway**                                 | Starter      | Versionamento do banco de dados                             | Versionamento do schema do banco de dados e gerenciamento evolutivo das migrations.                                               |
| **Spring Validation (Jakarta Validation)** | Starter      | Validação de dados                                          | Validação dos dados de entrada utilizando Bean Validation e validadores customizados.                                             |
| **Spring Security**                        | Starter      | Segurança da aplicação                                      | Implementação de autenticação, autorização e proteção dos recursos REST.                                                          |
| **Spring Authorization Server**            | Atual        | Servidor OAuth2                                             | Emissão e gerenciamento de tokens de acesso seguindo o protocolo OAuth2.                                                          |
| **OAuth2 Resource Server**                 | Starter      | Validação de tokens JWT emitidos pelo Authorization Server. | Proteção dos endpoints utilizando tokens JWT assinados.                                                                           |
| **JWT (JSON Web Token)**                   | OAuth2       | Autenticação stateless                                      | Representação das credenciais de autenticação em formato de token assinado.                                                       |
| **Spring Mail**                            | Starter      | Envio de e-mails                                            | Disparo de e-mails transacionais para ativação de conta e recuperação de senha.                                                   |
| **Thymeleaf**                              | Starter      | Templates HTML                                              | Geração dinâmica dos templates de e-mail enviados aos usuários.                                                                   |
| **SpringDoc OpenAPI**                      | 2.8.x        | Documentação automática                                     | Geração da especificação OpenAPI e interface Swagger UI.                                                                          |
| **Maven**                                  | Build Tool   | Gerenciamento do projeto                                    | Gerenciamento de dependências, plugins e ciclo de build do projeto.                                                               |
| **JUnit 5**                                | Starter Test | Testes automatizados                                        | Base para criação dos testes da aplicação.                                                                                        |
| **Spring Security Test**                   | Starter Test | Testes de segurança                                         | Suporte para testes envolvendo autenticação e autorização.                                                                        |

---

### ⚙️ Recursos da Plataforma Utilizados

Além dos frameworks principais, a aplicação utiliza diversos recursos da plataforma Spring para aumentar a flexibilidade de configuração, facilitar a implantação em diferentes ambientes e melhorar aspectos relacionados à segurança, observabilidade e manutenção, a aplicação incorporou diversos recursos da plataforma Spring para se tornar mais robusta e preparada para diferentes ambientes de execução.

| ⚙️ Recurso                                            | 📖 Aplicação                                                                                                                                                             |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Profiles (`application-dev` e `application-test`)** | Isolamento das configurações específicas para os ambientes de desenvolvimento, testes e produção.                                                                        |
| **Externalização de Configurações**                   | Externalização de configurações sensíveis por meio de propriedades e variáveis de ambiente.                                                                              |
| **Logging Configurável**                              | Configuração dos níveis de log da aplicação e dos principais componentes da infraestrutura.                                                                              |
| **Open Session in View Desabilitado**                 | Desabilitação do padrão Open Session in View para evitar consultas fora da camada de serviço (`spring.jpa.open-in-view=false`) e reduzir acoplamento com a persistência. |
| **Migrations Versionadas**                            | Organização das migrations em módulos distintos para evolução do schema e carga inicial de dados (`schema` e `data`).                                                    |
| **Validação Internacionalizada**                      | Centralização das mensagens de validação no arquivo `ValidationMessages.properties`.                                                                                     |
| **Swagger Customizado**                               | Personalização dos endpoints de documentação OpenAPI e Swagger UI.                                                                                                       |
| **Configuração de CORS**                              | Controle dos domínios autorizados a consumir a API.                                                                                                                      |
| **SMTP Configurável**                                 | Externalização das configurações do servidor SMTP para envio de e-mails transacionais.                                                                                   |
| **Controle de Expiração de Tokens**                   | Parametrização do tempo de expiração dos tokens de ativação de conta e recuperação de senha.                                                                             |

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

> [!NOTE] Essa organização evidencia a separação das responsabilidades entre as camadas da aplicação, na qual o Spring Boot atua como > núcleo da infraestrutura, enquanto os módulos especializados oferecem suporte à construção da API REST, persistência de dados, segurança e comunicação por e-mail.

---

## 🗄️ Modelagem ORM

A camada de persistência do **ASJCatalog** foi implementada utilizando a especificação **Jakarta Persistence (JPA)**, com o **Hibernate** como provedor **ORM** responsável pelo mapeamento entre objetos Java e estruturas relacionais do PostgreSQL.

A modelagem foi estruturada para representar os principais conceitos do domínio da aplicação por meio de entidades, relacionamentos, consultas especializadas e estratégias de otimização de acesso aos dados.

### 📦 Modelo de Domínio

O domínio da aplicação foi dividido em módulos independentes, cada um responsável por um conjunto específico de regras de negócio.

| Entidade     | Responsabilidade                                                   |
| ------------ | ------------------------------------------------------------------ |
| **Product**  | Representa os produtos disponíveis no catálogo.                    |
| **Category** | Organiza os produtos em categorias.                                |
| **User**     | Representa os usuários autenticados da aplicação.                  |
| **Role**     | Define os perfis de acesso utilizados pelo Spring Security (RBAC). |
| **Token**    | Gerencia tokens de ativação de conta e recuperação de senha.       |
| **Email**    | Registra o envio de e-mails transacionais da aplicação.            |

Essa organização promove alta coesão, baixo acoplamento e evolução independente dos módulos que compõem o domínio da aplicação.

---

### 🔄 Relacionamentos entre Entidades

A modelagem utiliza os principais tipos de associações disponibilizados pela JPA para representar os relacionamentos existentes entre as entidades do domínio.

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

| Relacionamento       | Objetivo                                                                                    |
| -------------------- | ------------------------------------------------------------------------------------------- |
| `Product ↔ Category` | Permite que um produto pertença a várias categorias e uma categoria possua vários produtos. |
| `User ↔ Role`        | Implementa o controle de acesso baseado em papéis (RBAC).                                   |
| `User → Token`       | Gerencia tokens utilizados em ativação de conta e recuperação de senha.                     |
| `User → Email`       | Mantém o histórico dos e-mails transacionais enviados ao usuário.                           |

---

### 🔍 Consultas ao Banco de Dados

A camada de persistência combina diferentes estratégias de consulta conforme os requisitos de desempenho, flexibilidade e complexidade de cada operação.

| Estratégia        | Aplicação                                                                                                                                                                   |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Query Methods** | Consultas derivadas automaticamente pelo mecanismo de geração de consultas do Spring Data JPA., como `findByNameContainingIgnoreCase()`.                                    |
| **JPQL**          | Consultas orientadas às entidades, como o carregamento de produtos utilizando `JOIN FETCH`.                                                                                 |
| **Native SQL**    | Consultas SQL nativas utilizadas em cenários que demandam maior controle sobre desempenho ou recursos específicos do banco de dados (`relacionamentos muitos-para-muitos`). |
| **Repositories**  | Centralizam toda a comunicação com o banco de dados através do padrão Repository.                                                                                           |

A combinação dessas estratégias permite utilizar consultas derivadas para operações simples e recorrer a JPQL ou SQL nativo quando requisitos de desempenho ou flexibilidade tornam essa abordagem mais adequada.

---

### 🚀 Estratégias de Otimização

Além da modelagem `ORM`, foram adotadas estratégias de otimização para reduzir o custo das consultas ao `banco de dados`, minimizar a quantidade de operações executadas pelo `Hibernate` e melhorar o desempenho da camada de persistência.

| Técnica                        | Como foi aplicada                                                                                                              | Benefício                                                                   |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------- |
| **Projection**                 | `ProductProjection` retorna apenas os campos necessários durante a paginação.                                                  | Reduz transferência de dados e consumo de memória.                          |
| **Native Query**               | Primeira consulta recupera apenas os IDs dos produtos filtrados.                                                               | Paginação eficiente em relacionamentos muitos-para-muitos.                  |
| **Fetch Join**                 | Segunda consulta utiliza `JOIN FETCH` para carregar produtos e categorias em uma única operação.                               | Evita consultas adicionais decorrentes do carregamento lazy das categorias. |
| **Reordenação dos Resultados** | `IdentifiableUtils.reorderByReference()` preserva a ordem retornada pela consulta paginada.                                    | Mantém consistência entre paginação e carregamento das entidades.           |
| **Eliminação do N+1 Select**   | Combinação entre Projection, Native SQL e Fetch Join para carregar produtos e categorias em apenas duas consultas controladas. | Elimina dezenas ou centenas de consultas extras em cenários paginados.      |

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
Mappeamento para → DTO
    │
    ▼
Resposta da API
```

Essa estratégia elimina o problema clássico de **N+1 Select**, mantendo a paginação eficiente e garantindo o carregamento completo das categorias associadas aos produtos com um número reduzido de consultas ao banco de dados.

---

## 🎯 Casos de Uso

Além das operações CRUD tradicionais, o ASJCatalog implementa casos de uso que representam fluxos completos de negócio encontrados em aplicações corporativas. Cada caso de uso é encapsulado na camada de serviços (Service Layer), responsável por aplicar validações, regras de negócio, controle transacional, persistência e integrações externas, enquanto os controllers permanecem responsáveis exclusivamente pela exposição dos endpoints REST.

| Caso de Uso                        | Descrição                                                                                                                                                            |
| ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 👤 **Gerenciamento de Usuários**   | Cadastro, consulta, atualização, ativação, desativação e remoção de usuários, com validações de negócio, criptografia de senhas e gerenciamento de perfis de acesso. |
| 🛍️ **Gerenciamento de Produtos**   | CRUD completo de produtos, associação com categorias, paginação, filtros, atualização parcial e controle de status (ativo/inativo).                                  |
| 🗂️ **Gerenciamento de Categorias** | Cadastro, consulta, atualização e remoção de categorias utilizadas na organização do catálogo de produtos.                                                           |
| 🔐 **Registro de Conta**           | Criação de novas contas com atribuição automática de permissões, geração de token de ativação e envio de e-mail de confirmação.                                      |
| ✉️ **Ativação de Conta**           | Validação do token de ativação, habilitação da conta e invalidação dos tokens utilizados.                                                                            |
| 🔄 **Reenvio de Ativação**         | Geração de um novo token de ativação para usuários que ainda não confirmaram o cadastro.                                                                             |
| 🔑 **Recuperação de Senha**        | Solicitação de redefinição de senha mediante geração de token temporário e envio de e-mail transacional.                                                             |
| 🔒 **Redefinição de Senha**        | Validação do token de recuperação, atualização segura da senha e invalidação dos tokens utilizados.                                                                  |
| 👤 **Usuário Autenticado**         | Recuperação dos dados do usuário autenticado diretamente a partir do contexto de segurança do Spring Security.                                                       |
| 🔑 **Autenticação**                | Integração com OAuth2 Authorization Server e JWT para autenticação e autorização baseada em papéis (RBAC).                                                           |

### Fluxo Geral dos Casos de Uso

```java
Cliente
    │
    ▼
Controller (REST API)
    │
    ▼
Service Layer
    │
    ├── Validações
    ├── Regras de negócio
    ├── Controle transacional
    ├── Integração com outros serviços
    ▼
Repositories
    │
    ▼
Banco de Dados

           │
           ├────────► TokenService
           │
           ├────────► EmailService
           │
           └────────► AuthenticatedUserService
```

### Organização por Responsabilidade

| Serviço                    | Responsabilidade Principal                                                                            |
| -------------------------- | ----------------------------------------------------------------------------------------------------- |
| `ProductService`           | Gerencia produtos, categorias associadas, paginação, filtros e otimizações de consulta.               |
| `CategoryService`          | Centraliza as operações relacionadas às categorias do catálogo.                                       |
| `UserService`              | Gerencia usuários, perfis de acesso, autenticação, CRUD e regras administrativas.                     |
| `AccountService`           | Implementa o ciclo completo de vida da conta: registro, ativação, recuperação e redefinição de senha. |
| `TokenService`             | Cria, valida, expira e invalida tokens de ativação e recuperação de senha.                            |
| `EmailService`             | Gera e envia e-mails transacionais utilizando templates HTML.                                         |
| `AuthenticatedUserService` | Recupera o usuário autenticado a partir do `SecurityContext`.                                         |

> 💡 Observação: Os casos de uso foram implementados seguindo uma arquitetura em camadas (Controller → Service → Repository), onde os > controllers recebem as requisições HTTP e delegam o processamento aos services. Essa abordagem centraliza regras de negócio, validações, transações e integrações (e-mail, tokens e autenticação), reduz o acoplamento entre as camadas e torna a aplicação mais organizada, testável e alinhada às práticas de desenvolvimento de sistemas corporativos.

---

## 🔍 Consultas ao Banco

A camada de persistência do **ASJCatalog** foi implementada com **Spring Data JPA**, combinando consultas derivadas, **JPQL**, **Native SQL** e **Projections** para atender diferentes cenários de negócio com equilíbrio entre simplicidade, flexibilidade e desempenho.

Enquanto as consultas derivadas simplificam operações comuns, consultas personalizadas foram empregadas para implementar filtros avançados, paginação e otimizações específicas relacionadas ao relacionamento muitos-para-muitos entre produtos e categorias.

### Estratégias utilizadas

| Estratégia            | Aplicação no ASJCatalog                                                                    | Benefício                                                           |
| --------------------- | ------------------------------------------------------------------------------------------ | ------------------------------------------------------------------- |
| **Query Methods**     | Métodos derivados como `findByNameContainingIgnoreCase()` e `existsByNameIgnoreCase()`     | Reduz código repetitivo por meio das convenções do Spring Data JPA. |
| **JPQL**              | Consulta com `JOIN FETCH` para carregar produtos juntamente com suas categorias.           | Evita carregamentos adicionais e melhora o desempenho.              |
| **Native SQL**        | Consulta personalizada para busca paginada com filtros por nome e categorias.              | Permite consultas mais eficientes em cenários complexos.            |
| **Projection**        | Interface `ProductProjection` retorna apenas os campos necessários para paginação inicial. | Reduz transferência de dados e consumo de memória.                  |
| **Paginação**         | Utilização de `Page`, `Pageable` e `PageImpl`.                                             | Permite consultas escaláveis para grandes volumes de dados.         |
| **Filtros Dinâmicos** | Busca por nome e múltiplas categorias utilizando parâmetros opcionais.                     | Oferece maior flexibilidade sem duplicação de consultas.            |

> 💡 **Observação:** A combinação entre consultas derivadas, JPQL e Native SQL permitiu utilizar a estratégia mais adequada para cada cenário, equilibrando simplicidade de desenvolvimento, legibilidade e desempenho.

---

## 🚀 Otimizações de Persistência

Além da modelagem do domínio, esta etapa do projeto dedicou atenção especial ao desempenho da camada de persistência e à eficiência das consultas executadas pelo Hibernate.

O principal desafio encontrado foi o carregamento do relacionamento **muitos-para-muitos** entre produtos e categorias, que poderia gerar o conhecido problema **N+1 Select**.

### Estratégias adotadas

| Otimização                     | Como foi aplicada                                                                                                        | Benefício                                                                     |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------- |
| **Projection Pattern**         | A consulta inicial recupera apenas os atributos necessários por meio da interface `ProductProjection`.                   | Reduz o volume de dados recuperados do banco.                                 |
| **Native SQL**                 | Consulta otimizada para paginação e filtragem antes do carregamento das entidades completas.                             | Melhora a eficiência em consultas complexas.                                  |
| **Fetch Join**                 | Utilização de `JOIN FETCH` para carregar produtos e categorias em uma única consulta.                                    | Evita consultas adicionais provocadas pelo carregamento lazy.                 |
| **Reordenação dos Resultados** | Utilização do `IdentifiableUtils.reorderByReference()` para preservar a ordem original da paginação após o `JOIN FETCH`. | Mantém consistência dos resultados apresentados ao usuário.                   |
| **Eliminação do N+1 Select**   | Combinação entre Projection, Native SQL e Fetch Join durante o fluxo de busca paginada.                                  | Reduz significativamente a quantidade de consultas executadas pelo Hibernate. |

### Fluxo da otimização

```text
Native SQL
      │
      ▼
ProductProjection
      │
      ▼
Lista de IDs
      │
      ▼
JPQL + JOIN FETCH
      │
      ▼
Produtos + Categorias carregados
      │
      ▼
Reordenação (IdentifiableUtils)
      │
      ▼
DTO de resposta
```

> 🚀 Essa estratégia mantém a paginação eficiente sem abrir mão do carregamento completo das categorias, solucionando um dos problemas clássicos de performance em aplicações que utilizam JPA/Hibernate.

---

## 📧 Integração com E-mail

O ASJCatalog implementa integração com serviços de e-mail para suportar os fluxos de ativação de conta e recuperação de senha, aproximando a aplicação das práticas adotadas em sistemas corporativos.

A implementação utiliza **Spring Mail**, **JavaMailSender** e **Thymeleaf**, permitindo gerar mensagens HTML personalizadas e enviá-las de forma assíncrona.

### Fluxo de envio

```text
Controller
      │
      ▼
AccountService
      │
      ▼
TokenService
      │
      ▼
EmailService
      │
      ▼
Template Thymeleaf
      │
      ▼
Servidor SMTP
      │
      ▼
Usuário
```

### Recursos implementados

| Recurso                             | Aplicação                                                                           |
| ----------------------------------- | ----------------------------------------------------------------------------------- |
| **Spring Mail**                     | Envio de e-mails transacionais.                                                     |
| **JavaMailSender**                  | Comunicação com servidor SMTP.                                                      |
| **Thymeleaf**                       | Geração de templates HTML personalizados.                                           |
| **@Async**                          | Envio assíncrono dos e-mails sem bloquear a requisição HTTP.                        |
| **TokenService**                    | Geração, validação e invalidação de tokens de ativação e recuperação de senha.      |
| **Factory Methods**                 | Criação padronizada de tokens através de métodos estáticos da entidade `Token`.     |
| **Persistência de Logs**            | Registro dos e-mails enviados na entidade `Email` para auditoria e rastreabilidade. |
| **Externalização de Configurações** | URLs, SMTP e tempo de expiração configurados via propriedades da aplicação.         |

> 💡 A entidade `Token` encapsula parte importante das regras de negócio, sendo responsável por validar expiração, tipo do token, estado de utilização e criação padronizada através de Factory Methods.

---

## 🧱 Boas Práticas Aplicadas

Durante o desenvolvimento deste capítulo foram adotadas diversas práticas utilizadas em aplicações corporativas construídas com Spring Boot.

| Boa prática                             | Aplicação no projeto                                                        |
| --------------------------------------- | --------------------------------------------------------------------------- |
| **Arquitetura em Camadas**              | Separação entre Controller, Service, Repository e Domain.                   |
| **Separação por Domínio**               | Organização dos módulos `catalog`, `user` e `recovery`.                     |
| **DTO Pattern**                         | Evita exposição direta das entidades.                                       |
| **Mapper Pattern**                      | Conversão centralizada entre entidades e DTOs.                              |
| **Repository Pattern**                  | Isolamento da camada de persistência.                                       |
| **Service Layer**                       | Centralização das regras de negócio.                                        |
| **Bean Validation**                     | Validações declarativas através de anotações customizadas.                  |
| **Tratamento Global de Exceções**       | Padronização das respostas de erro utilizando `ControllerExceptionHandler`. |
| **Transações**                          | Utilização de `@Transactional` para garantir consistência dos dados.        |
| **Externalização de Configurações**     | Uso de arquivos `.properties` e variáveis de ambiente.                      |
| **Versionamento do Banco de Dados**     | Versionamento do schema utilizando Flyway.                                  |
| **Documentação da API**                 | Integração com OpenAPI/Swagger.                                             |
| **Princípio da Responsabilidade Única** | Cada classe possui uma responsabilidade bem definida.                       |

---

## 📈 Evolução Arquitetural

A evolução arquitetural do ASJCatalog reflete a transição de uma aplicação inicialmente focada na persistência de entidades para uma arquitetura orientada ao domínio, aproximando sua organização dos princípios e padrões adotados em sistemas corporativos. Essa evolução reorganizou a estrutura da aplicação em torno dos conceitos centrais do negócio, substituindo uma abordagem baseada apenas em entidades persistentes por um modelo mais expressivo, modular e alinhado às boas práticas de arquitetura de software.

Entre as principais evoluções destacam-se:

- Migração da antiga camada `entity` para `domain`.
- Organização do domínio em módulos (`catalog`, `user` e `recovery`).
- Introdução de serviços especializados para autenticação, tokens e envio de e-mails.
- Implementação de casos de uso completos relacionados ao ciclo de vida das contas.
- Separação mais clara entre infraestrutura, domínio e exposição da API.
- Evolução da camada de persistência com consultas otimizadas e estratégias de desempenho.
- Fortalecimento da arquitetura baseada em responsabilidades bem definidas.

> [!IMPORTANT]
> Essa evolução tornou a aplicação mais organizada, extensível e preparada para receber novas funcionalidades sem comprometer sua estrutura.

---

## 🎓 Aprendizados

Esta etapa consolidou conhecimentos relacionados à modelagem de domínio, persistência de dados, otimização de consultas e implementação de arquiteturas backend utilizando `Spring Boot`, `Spring Data JPA` e `Hibernate`.

Os principais aprendizados incluem:

- Modelagem de domínios mais expressivos.
- Utilização avançada do Spring Data JPA.
- Construção de consultas otimizadas.
- Resolução do problema N+1 Select.
- Implementação de relacionamentos complexos com Hibernate.
- Aplicação de boas práticas de arquitetura em camadas.
- Desenvolvimento de casos de uso completos.
- Integração com serviços externos utilizando SMTP.
- Gerenciamento seguro de tokens de negócio.
- Organização de aplicações inspiradas em Domain-Driven Design.

---

## 💼 Competências Técnicas Desenvolvidas

Ao concluir este capítulo foram desenvolvidas competências relacionadas à construção de aplicações backend corporativas.

| Competência                      | Nível de aplicação |
| -------------------------------- | ------------------ |
| Modelagem ORM com JPA/Hibernate  | ✔️                 |
| Spring Data JPA                  | ✔️                 |
| JPQL e Native SQL                | ✔️                 |
| Paginação e filtros dinâmicos    | ✔️                 |
| Relacionamentos complexos        | ✔️                 |
| Otimização de consultas          | ✔️                 |
| Fetch Join e Projection          | ✔️                 |
| Resolução de N+1 Select          | ✔️                 |
| Domain-Driven Design (conceitos) | ✔️                 |
| Service Layer Pattern            | ✔️                 |
| Repository Pattern               | ✔️                 |
| DTO e Mapper Pattern             | ✔️                 |
| Bean Validation                  | ✔️                 |
| Spring Mail + Thymeleaf          | ✔️                 |
| OAuth2 + JWT                     | ✔️                 |
| Flyway                           | ✔️                 |
| OpenAPI/Swagger                  | ✔️                 |

---

## 🏁 Conclusão

Este capítulo marcou a transição do ASJCatalog para um backend mais próximo dos padrões adotados em aplicações corporativas. Além da evolução da modelagem de domínio e da camada de persistência, foram implementados fluxos completos de negócio, mecanismos de recuperação de acesso, integração com serviços de e-mail e estratégias avançadas de otimização de consultas.

A adoção de práticas arquiteturais como separação em camadas, modelagem orientada ao domínio, consultas otimizadas, tratamento centralizado de exceções, versionamento do banco de dados e documentação da API contribuiu para tornar a aplicação mais organizada, robusta, escalável e de fácil manutenção.

Com essa base consolidada, o projeto passa a oferecer uma arquitetura modular e extensível, preparada para incorporar novos requisitos funcionais sem comprometer aspectos como organização, desempenho, manutenibilidade e escalabilidade, estabelecendo uma base sólida para a evolução contínua da aplicação.

---

## 📚 Referências Técnicas

### 🔹 Spring Boot

- https://docs.spring.io/spring-boot/documentation.html
- https://spring.io/projects/spring-boot

---

### 🔹 Spring Data JPA e Persistência

- https://spring.io/projects/spring-data-jpa
- https://docs.spring.io/spring-data/jpa/reference/
- https://jakarta.ee/specifications/persistence/
- https://hibernate.org/orm/documentation/

---

### 🔹 Hibernate ORM

- https://hibernate.org/orm/
- https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html

---

### 🔹 PostgreSQL

- https://www.postgresql.org/docs/

---

### 🔹 Flyway

- https://flywaydb.org/documentation/
- https://documentation.red-gate.com/flyway

---

### 🔹 Spring Mail e Thymeleaf

- https://docs.spring.io/spring-framework/reference/integration/email.html
- https://www.thymeleaf.org/documentation.html

---

### 🔹 OpenAPI e Swagger

- https://swagger.io/specification/
- https://springdoc.org/

---

### 🔹 Domain-Driven Design (DDD)

- https://domainlanguage.com/ddd/
- Eric Evans — _Domain-Driven Design: Tackling Complexity in the Heart of Software_

---

### 🔹 Arquitetura e Padrões

- Martin Fowler — _Patterns of Enterprise Application Architecture_
- https://martinfowler.com/eaaCatalog/

---

## 👨‍💻 Autor

**Albert Silva de Jesus**  
Desenvolvedor Backend Java | Spring Boot

---

## 📎 Contato

[![LinkedIn](https://img.shields.io/badge/LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/albert-backend-java-spring-boot/)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com)
