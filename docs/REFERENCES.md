# Referências técnicas

Documentações oficiais e livros usados no ASJCatalog, organizados por assunto. Cada grupo indica o guia que mostra como o assunto foi aplicado no projeto. Quando a documentação é publicada por versão, o link aponta para a versão usada no projeto.

## Spring Boot

Guias relacionados: [GETTING-STARTED](guides/GETTING-STARTED.md) e [CONFIGURATION](guides/CONFIGURATION.md).

- [Spring Boot 3.5 — documentação de referência](https://docs.spring.io/spring-boot/3.5/index.html)
- [Spring Boot — página do projeto](https://spring.io/projects/spring-boot/)

## Arquitetura e padrões

Guia relacionado: [ARCHITECTURE](guides/ARCHITECTURE.md).

- [Catálogo de padrões de *Patterns of Enterprise Application Architecture*](https://martinfowler.com/eaaCatalog/)
- Martin Fowler — _Patterns of Enterprise Application Architecture_

## Domain-Driven Design (DDD)

Guias relacionados: [DOMAIN-MODEL](guides/DOMAIN-MODEL.md) e [DESIGN-DECISIONS](guides/DESIGN-DECISIONS.md).

- [Domain Language — Domain-Driven Design](https://www.domainlanguage.com/ddd/)
- Eric Evans — _Domain-Driven Design: Tackling Complexity in the Heart of Software_

## Hibernate ORM

Guias relacionados: [DOMAIN-MODEL](guides/DOMAIN-MODEL.md) e [DATA-ACCESS](guides/DATA-ACCESS.md).

- [Hibernate ORM — página do projeto](https://hibernate.org/orm/)
- [Hibernate ORM 6.6 — User Guide](https://docs.hibernate.org/orm/6.6/userguide/html_single/)

## Spring Data JPA e Jakarta Persistence

Guia relacionado: [DATA-ACCESS](guides/DATA-ACCESS.md).

- [Spring Data JPA — página do projeto](https://spring.io/projects/spring-data-jpa/)
- [Spring Data JPA 3.5 — documentação de referência](https://docs.spring.io/spring-data/jpa/reference/3.5/index.html)
- [Jakarta Persistence 3.1 — especificação](https://jakarta.ee/specifications/persistence/3.1/)

## PostgreSQL

Guia relacionado: [GETTING-STARTED](guides/GETTING-STARTED.md).

- [PostgreSQL — documentação](https://www.postgresql.org/docs/)

## Flyway

Guia relacionado: [DATABASE-MIGRATIONS](guides/DATABASE-MIGRATIONS.md).

- [Flyway — documentação](https://documentation.red-gate.com/flyway)

## OpenAPI e Swagger

Guia relacionado: [API-ENDPOINTS](guides/API-ENDPOINTS.md).

- [OpenAPI Specification — OpenAPI Initiative](https://spec.openapis.org/oas/latest.html)
- [springdoc-openapi — documentação](https://springdoc.org/)

## Validação

Guia relacionado: [VALIDATION](guides/VALIDATION.md).

- [Jakarta Validation 3.0 — especificação](https://jakarta.ee/specifications/bean-validation/3.0/)
- [Hibernate Validator 8.0 — guia de referência](https://docs.hibernate.org/validator/8.0/reference/en-US/html_single/)

## Tratamento de erros

Guia relacionado: [ERROR-HANDLING](guides/ERROR-HANDLING.md).

- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457.html)

A RFC 9457 substitui a RFC 7807. O ASJCatalog **não segue** esse padrão: a classe `ProblemDetails` é do próprio projeto, apesar do nome parecido, e o formato dela está descrito no guia.

## Internacionalização

Guia relacionado: [INTERNATIONALIZATION](guides/INTERNATIONALIZATION.md).

- [Spring Framework 6.2 — internacionalização com `MessageSource`](https://docs.spring.io/spring-framework/reference/6.2/core/beans/context-introduction.html)
- [Spring Framework 6.2 — `LocaleResolver` no Spring MVC](https://docs.spring.io/spring-framework/reference/6.2/web/webmvc/mvc-servlet/localeresolver.html)

## Spring Security e OAuth2 Resource Server

Guias relacionados: [AUTHENTICATION](guides/AUTHENTICATION.md) e [SECURITY-CONTRACT](SECURITY-CONTRACT.md).

- [Spring Security — página do projeto](https://spring.io/projects/spring-security/)
- [Spring Security 6.5 — documentação de referência](https://docs.spring.io/spring-security/reference/6.5/index.html)
- [Spring Security 6.5 — OAuth 2.0 Resource Server com JWT](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html)

## Spring Authorization Server

Guias relacionados: [AUTHENTICATION](guides/AUTHENTICATION.md) e [SECURITY-CONTRACT](SECURITY-CONTRACT.md).

- [Spring Authorization Server — documentação de referência](https://docs.spring.io/spring-authorization-server/reference/)

## Spring Mail e Thymeleaf

Guia relacionado: [ACCOUNT-FLOWS](guides/ACCOUNT-FLOWS.md).

- [Spring Framework 6.2 — envio de e-mail](https://docs.spring.io/spring-framework/reference/6.2/integration/email.html)
- [Thymeleaf — documentação](https://www.thymeleaf.org/documentation.html)

## Testes: JUnit 5 e Mockito

Guia relacionado: [TESTING](guides/TESTING.md).

- [JUnit 5.12 — User Guide](https://docs.junit.org/5.12.2/user-guide/)
- [Mockito — site oficial](https://site.mockito.org/)
