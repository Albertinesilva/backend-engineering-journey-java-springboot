<h1 align="center">📦 ASJCatalog — Backend Engineering Journey | Java & Spring Boot</h1>

<p align="center">
<em>An open source product catalog API built chapter by chapter: layered architecture, automated tests, OAuth2 security, domain modeling and data access with Java and Spring Boot.</em>
</p>

<p align="center">

<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&logoColor=white" />

<img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

<img src="https://img.shields.io/badge/Security-OAuth2%20%7C%20JWT-red?style=for-the-badge" />

<img src="https://img.shields.io/badge/Database-PostgreSQL%20%7C%20H2-336791?style=for-the-badge&logo=postgresql&logoColor=white" />

<img src="https://img.shields.io/badge/Migrations-Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white" />

<img src="https://img.shields.io/badge/Tests-JUnit5%20%7C%20Mockito-yellow?style=for-the-badge" />

<img src="https://img.shields.io/badge/Documentation-Swagger%20%7C%20OpenAPI-85EA2D?style=for-the-badge" />

<a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" /></a>

<img src="https://img.shields.io/github/last-commit/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

</p>

<p align="justify">
<em>
O <strong>ASJCatalog</strong> é a API de um catálogo de produtos construída em Java e Spring Boot. O nome vem das iniciais de <strong>Albert Silva de Jesus</strong>: o projeto nasceu da base do <strong>DSCatalog</strong>, do curso DevSuperior, e evoluiu de forma independente até se tornar praticamente outro projeto.

É um projeto <strong>open source e sem fins comerciais</strong>: um laboratório de aprendizado, uma fonte de consulta para o futuro e um material para ajudar desenvolvedores júnior. A evolução é contada em capítulos, um por branch, cada um publicado no LinkedIn.
</em>
</p>

<p align="center">
<a href="https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/HOME.md"><strong>📖 Documentação técnica</strong></a> · <a href="https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/GETTING-STARTED.md">🚀 Primeiros Passos</a> · <a href="https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/issues">🗂️ Issues</a> · <a href="https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/milestone/1">🎯 Pronto para produção</a> · <a href="CONTRIBUTING.md">🤝 Contribuir</a>
</p>

---

## ✨ Destaques técnicos

- **Listagem de produtos sem N+1**: paginação numa consulta nativa e carregamento das categorias numa segunda consulta com `JOIN FETCH`. → [DATA-ACCESS](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/DATA-ACCESS.md#5-o-problema-n1-e-a-listagem-de-produtos)
- **OAuth2 com JWT**: Authorization Server e Resource Server na mesma aplicação, com access token JWT, refresh token com rotação e controle de acesso por roles. → [AUTHENTICATION](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/AUTHENTICATION.md)
- **Fluxos de conta com e-mail**: cadastro, ativação, recuperação e redefinição de senha, com e-mails HTML gerados pelo Thymeleaf. → [ACCOUNT-FLOWS](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/ACCOUNT-FLOWS.md)
- **Internacionalização**: mensagens da API em português do Brasil, inglês e espanhol, escolhidas pelo cabeçalho `Accept-Language`. → [INTERNATIONALIZATION](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/INTERNATIONALIZATION.md)
- **Suíte de testes em duas fases**: testes de unidade no Surefire e testes de integração (`*IT`) no Failsafe, todos executados com `./mvnw verify`. → [TESTING](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/TESTING.md)
- **Decisões técnicas registradas**: contexto, motivo, alternativas e consequências das principais escolhas do projeto. → [DESIGN-DECISIONS](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/guides/DESIGN-DECISIONS.md)

---

## 📚 Capítulos

Cada capítulo fica em uma branch própria, com um README que apresenta o que foi construído naquela etapa.

| Nº | Tema | Branch | Resumo |
| --- | --- | --- | --- |
| 01 | Operações CRUD com Spring Boot | [`chapter-01-crud`](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/tree/chapter-01-crud) | CRUD de produtos e categorias em arquitetura em camadas, com DTOs (records), mappers, paginação e tratamento global de exceções. |
| 02 | Testes automatizados no back-end | [`chapter-02-tests`](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/tree/chapter-02-tests) | Testes de unidade, de repositório e da camada web com JUnit 5, Mockito e MockMvc, usando factories e foco em testabilidade. |
| 03 | Validação e segurança (OAuth2 \| JWT) | [`chapter-03-validation-security`](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/tree/chapter-03-validation-security) | Spring Security 6 com Authorization Server e Resource Server, JWT, controle de acesso por roles e Bean Validation. |
| 04 | Domínio, ORM, casos de uso e acesso a dados | [`chapter-04-domain-orm`](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/tree/chapter-04-domain-orm) | Ciclo de vida completo da conta com e-mails transacionais, JPQL e SQL nativo, solução do N+1 e domínio em módulos inspirados em DDD. |

---

## 🌿 Onde está o código

| Branch | O que contém |
| --- | --- |
| [`develop`](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/tree/develop) | O projeto completo e atualizado (capítulo 04), com a documentação técnica em `docs/`. É nela que o trabalho acontece e para ela que vão os Pull Requests. |
| `main` | Esta página de entrada. Recebe o código do projeto apenas no deploy final; a pasta `backend/` daqui é, por enquanto, só o esqueleto inicial. |
| `chapter-*` | O registro de cada capítulo, como foi publicado. |

---

## 📖 Documentação técnica

Os guias do backend (primeiros passos, configuração, arquitetura, domínio, acesso a dados, endpoints, autenticação, testes, convenções e decisões técnicas), o contrato de segurança e o histórico de auditorias estão reunidos no [Índice da Documentação](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/blob/develop/docs/HOME.md), na branch `develop`.

---

## 🖥️ Frontend

O frontend do ASJCatalog é independente e fica em outro repositório: [asjcatalog-frontend](https://github.com/Albertinesilva/asjcatalog-frontend).

---

## 🧭 Próximos passos

- Ainda faltam **dois capítulos** para encerrar a evolução do projeto. Eles serão publicados em novas branches.
- As melhorias pendentes estão registradas como [issues](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/issues). As necessárias para colocar a API em produção estão no milestone [Pronto para produção](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/milestone/1).

---

## 🤝 Como contribuir

Qualquer pessoa pode contribuir, inclusive quem está começando. Escolha uma issue (as marcadas com [`good first issue`](https://github.com/Albertinesilva/backend-engineering-journey-java-springboot/labels/good%20first%20issue) são um bom ponto de partida), crie sua branch a partir da `develop` e abra o Pull Request **para a `develop`**.

O passo a passo completo está no [CONTRIBUTING.md](CONTRIBUTING.md). Ao participar, siga o [Código de Conduta](CODE_OF_CONDUCT.md).

---

## 📄 Licença

Distribuído sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).

---

## 👨‍💻 Autor

**Albert Silva de Jesus**  
Desenvolvedor Backend Java | Spring Boot

---

## 📎 Contato

[![LinkedIn](https://img.shields.io/badge/LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/albert-backend-java-spring-boot/)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com)
