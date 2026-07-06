# 🧩 Capítulo 01 — Operações CRUD com Spring Boot

<p align="center">
<em>Building RESTful backend applications with layered architecture, clean code practices, and scalable CRUD operations using Java and Spring Boot.</em>
</p>

<p align="center">

<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&logoColor=white" />

<img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

<img src="https://img.shields.io/badge/API-RESTful-success?style=for-the-badge" />

<img src="https://img.shields.io/badge/Architecture-Layered_Architecture-blue?style=for-the-badge" />

<img src="https://img.shields.io/badge/Security-JWT%20%7C%20OAuth2-red?style=for-the-badge" />

<img src="https://img.shields.io/badge/Database-PostgreSQL%20%7C%20H2-336791?style=for-the-badge&logo=postgresql&logoColor=white" />

<img src="https://img.shields.io/badge/Tests-JUnit%20%7C%20Mockito-yellow?style=for-the-badge" />

<img src="https://img.shields.io/badge/Container-Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />

<img src="https://img.shields.io/badge/Documentation-Swagger%20%7C%20JavaDoc-85EA2D?style=for-the-badge" />

<img src="https://img.shields.io/github/license/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

<img src="https://img.shields.io/github/last-commit/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

</p>

O projeto **ASJ Catalog** foi estruturado seguindo boas práticas de desenvolvimento, adotando **arquitetura em camadas** e separação clara de responsabilidades. Além das operações básicas de CRUD, foram implementados conceitos importantes como:

- Uso de **DTOs** para comunicação entre camadas, utilizando **records do Java** para estruturas imutáveis de dados;
- Mapeamento com classes dedicadas (**Mapper**);
- Tratamento de **exceções customizadas**;
- Padronização das respostas da API;
- Mecanismo global de tratamento de erros, garantindo robustez e rastreabilidade.

A aplicação contempla a organização em camadas: `controller`, `service` e `repository`, além da camada de DTOs que garante maior controle sobre os dados expostos.

---

## 🎯 Objetivos do Capítulo

1. **Implementação de operações CRUD**
   - Criação, leitura, atualização e exclusão de **produtos** e **categorias** via API REST.
   - Endpoints bem estruturados: `GET`, `POST`, `PATCH`, `DELETE` com status HTTP adequado.
   - Serviços que fazem mapeamento **DTO ↔ entidade** usando **records** e **Mapper**.
   - Validação e tratamento de exceções (`ResourceNotFoundException`, `DatabaseException`) com logs detalhados.

2. **Paginação e filtragem**
   - Uso de `Pageable` para controlar páginas, tamanho e ordenação.
   - Consultas case-insensitive e parciais (`findByNameContainingIgnoreCase`) para melhorar experiência do usuário.

3. **Mapeamento de relacionamentos**
   - Recebendo apenas **IDs de categorias** no request e resolvendo vínculos no backend.
   - Atualização parcial de categorias, sem sobrescrever dados não enviados.

4. **Ambientes de desenvolvimento e testes**

| Aspecto             | Ambiente de Testes (`test`)                       | Ambiente de Desenvolvimento (`dev`)                 |
| ------------------- | ------------------------------------------------- | --------------------------------------------------- |
| Banco de dados      | H2 in-memory (efêmero)                            | PostgreSQL local (persistente)                      |
| Console             | `/h2-console` para inspeção manual                | Console SQL exibindo queries                        |
| Migrations (Flyway) | Desativado                                        | Ativo (`db/migration/schema` + `db/migration/data`) |
| Logs                | DEBUG/TRACE, JSON, `logs/test/dscatalog-test.log` | DEBUG/TRACE, JSON, `logs/dev/dscatalog-dev.log`     |
| Banner              | N/A                                               | Banner personalizado (`banner-dev.txt`)             |
| Objetivo            | Testes isolados, rápidos e reproduzíveis          | Desenvolvimento realista com dados persistentes     |
| Observações         | Banco efêmero, reset a cada execução              | Controle de schema, rastreabilidade completa        |

> [!IMPORTANT]
> Essa separação garante testes **isolados, rápidos e reproduzíveis**, enquanto o desenvolvimento ocorre em ambiente realista com dados persistentes e migrations. Reflete boas práticas de engenharia de software.

5. **Documentação da API e código**
   - **OpenAPI/Swagger** para documentação interativa;
   - **JavaDocs** explicando responsabilidades de controllers e services.

6. **Boas práticas de código e arquitetura**
   - Estrutura clara em **Controller, Service e Repository**;
   - Uso de **logger** para monitoramento e rastreabilidade;
   - Tratamento transacional adequado (`@Transactional`) para consistência de dados.

---

## 📦 Estrutura do Projeto `DSCatalog`

📦 `com.albertsilva.dev.asjcatalog`  
┣ 📂 `config`  
┃ ┗ 📄 `SpringDocOpenApiConfig.java`  
┣ 📂 `dto`  
┃ ┣ 📂 `category`  
┃ ┃ ┣ 📂 `request`  
┃ ┃ ┃ ┣ 📄 `CategoryCreateRequest.java`  
┃ ┃ ┃ ┗ 📄 `CategoryUpdateRequest.java`  
┃ ┃ ┗ 📂 `response`  
┃ ┃ ┃ ┗ 📄 `CategoryResponse.java`  
┃ ┣ 📂 `product`  
┃ ┃ ┣ 📂 `request`  
┃ ┃ ┃ ┣ 📄 `ProductCreateRequest.java`  
┃ ┃ ┃ ┗ 📄 `ProductUpdateRequest.java`  
┃ ┃ ┗ 📂 `response`  
┃ ┃ ┃ ┣ 📄 `ProductDetailsResponse.java`  
┃ ┃ ┃ ┗ 📄 `ProductResponse.java`  
┣ 📂 `entity`  
┃ ┣ 📄 `Category.java`  
┃ ┗ 📄 `Product.java`  
┣ 📂 `mapper`  
┃ ┣ 📂 `category`  
┃ ┃ ┗ 📄 `CategoryMapper.java`  
┃ ┗ 📂 `product`  
┃ ┃ ┗ 📄 `ProductMapper.java`  
┣ 📂 `repository`  
┃ ┣ 📄 `CategoryRepository.java`  
┃ ┗ 📄 `ProductRepository.java`  
┣ 📂 `service`  
┃ ┣ 📂 `exceptions`  
┃ ┃ ┣ 📄 `DatabaseException.java`  
┃ ┃ ┗ 📄 `ResourceNotFoundException.java`  
┃ ┣ 📄 `CategoryService.java`  
┃ ┗ 📄 `ProductService.java`  
┣ 📂 `web`  
┃ ┣ 📂 `controller`  
┃ ┃ ┣ 📄 `CategoryController.java`  
┃ ┃ ┗ 📄 `ProductController.java`  
┃ ┗ 📂 `exceptions`  
┃ ┃ ┣ 📂 `enums`  
┃ ┃ ┃ ┗ 📄 `ErrorType.java`  
┃ ┃ ┣ 📂 `handler`  
┃ ┃ ┃ ┗ 📄 `ControllerExceptionHandler.java`  
┃ ┃ ┗ 📂 `response`  
┃ ┃ ┃ ┗ 📄 `ProblemDetails.java`  
┣ 📄 `DscatalogApplication.java`  
┣ 📂 `resources`  
┃ ┣ 📂 `db`  
┃ ┃ ┣ 📂 `data`  
┃ ┃ ┣ 📂 `migration`  
┃ ┃ ┗ 📂 `schema`  
┃ ┣ 📂 `static`  
┃ ┣ 📂 `templates`  
┃ ┣ 📄 `application-dev.properties`  
┃ ┣ 📄 `application-prod.properties`  
┃ ┣ 📄 `application-test.properties`  
┃ ┣ 📄 `application.properties`  
┃ ┣ 📄 `banner-dev.txt`  
┃ ┗ 📄 `import.sql`  

---

## 🧱 Arquitetura em Camadas

A aplicação **ASJCatalog** segue a arquitetura tradicional **Controller → Service → Repository**, organizada em camadas bem definidas para garantir **manutenção mais fácil, testabilidade e escalabilidade**.

<img src="https://raw.githubusercontent.com/Albertinesilva/devsuperior-java-springboot-bootcamp/chapter-01-crud/docs/assets/imgs/padrao-camadas.png" width="100%">

## Padrão de Camadas

- Consiste em organizar os componentes do sistema em **partes denominadas camadas**.
- Cada camada possui **responsabilidade específica**.
- Componentes de uma camada só podem depender de **componentes da mesma camada** ou da camada **mais abaixo**.

## Descrição das Camadas e Responsabilidades

### Controller

- Responde interações do usuário (no caso de API REST, as requisições HTTP).
- Recebe os dados do front-end, encaminha para o service e retorna respostas padronizadas.

### Service

- Realiza operações de negócio, cada método deve ter **significado relacionado ao negócio**.
- Pode executar várias operações dentro de uma transação.  
  _Exemplo:_ `registrarPedido` → verificar estoque, salvar pedido, baixar estoque, enviar email.
- Manipula DTOs, valida regras de negócio e interage com o repository.

### Repository

- Executa operações **individuais** de acesso ao banco de dados.
- Responsável pela persistência via **Spring Data JPA**.

### DTOs (Data Transfer Objects)

- Objetos **simples**, usados apenas para transferência de dados.
- Não são gerenciados por ORM / banco de dados.
- Podem conter outros DTOs **aninhados**, mas **nunca devem conter entities**.
- Usos comuns:
  - Projeção de dados
  - Segurança (não expor dados sensíveis)
  - Economia de tráfego
  - Flexibilidade: diferentes representações dos dados
    - Combobox: `{ id: number, nome: string }`
    - Relatório detalhado: `{ id, nome, salario, email, telefones[] }`

### Mapper

- Converte entre **entities** do banco e **DTOs**, mantendo separação de responsabilidades.

### Exception Handler Global

- Captura exceções em toda a aplicação e retorna respostas padronizadas em **JSON**, com mensagens claras e rastreabilidade.

## Por que usar DTOs?

- Separação clara de responsabilidades:
  - **Service e Repository:** foco em transações e monitoramento ORM
  - **Controller:** tráfego simples de dados
- Segurança, economia de tráfego e flexibilidade na API.
- Facilita diferentes representações de dados para front-end e relatórios.

> [!IMPORTANT]  
> Essa separação garante **código limpo, testável e escalável**, permitindo que a aplicação evolua sem impactar outras camadas, além de tornar a leitura do código mais intuitiva para recrutadores e profissionais que avaliam a arquitetura do sistema.

---

## 🛠️ Tecnologias Utilizadas

O projeto **DSCatalog** foi desenvolvido utilizando um conjunto moderno de tecnologias voltadas para construção de APIs REST robustas, escaláveis e bem estruturadas.

### 📌 Stack Principal

| Categoria    | Tecnologia             | Função                                                           |
| ------------ | ---------------------- | ---------------------------------------------------------------- |
| Linguagem    | Java 17                | Desenvolvimento backend moderno com recursos atuais da linguagem |
| Framework    | Spring Boot 3.5.13     | Estrutura principal da aplicação e gerenciamento de dependências |
| API REST     | Spring Web             | Criação de endpoints HTTP (RESTful APIs)                         |
| Persistência | Spring Data JPA        | Abstração para acesso a dados e integração com ORM               |
| ORM          | Hibernate              | Mapeamento objeto-relacional (Entity ↔ Tabela)                   |
| Validação    | Spring Boot Validation | Validação de dados de entrada (Bean Validation)                  |

---

### 🗄️ Banco de Dados

| Categoria       | Tecnologia  | Função                                                    |
| --------------- | ----------- | --------------------------------------------------------- |
| Banco Principal | PostgreSQL  | Banco relacional utilizado no ambiente de desenvolvimento |
| Banco de Testes | H2 Database | Banco em memória para testes rápidos e isolados           |
| Console DB      | H2 Console  | Interface web para inspeção de dados em ambiente de teste |

---

### 🔄 Migração e Versionamento de Banco

| Tecnologia | Função                                                              |
| ---------- | ------------------------------------------------------------------- |
| Flyway     | Controle de versão do banco de dados (migrations de schema e dados) |

---

### 📄 Documentação da API

| Tecnologia                     | Função                                                                          |
| ------------------------------ | ------------------------------------------------------------------------------- |
| SpringDoc OpenAPI (Swagger UI) | Geração automática de documentação interativa da API REST                       |
| JavaDocs                       | Documentação técnica do código, descrevendo responsabilidades, métodos e fluxos |

> [!TIP]
> A API conta com documentação automatizada via **Swagger/OpenAPI**, além de **JavaDocs** bem definidos nos controllers e services, facilitando o entendimento da lógica de negócio e manutenção do código.

---

### 🧪 Testes

| Tecnologia               | Função                           |
| ------------------------ | -------------------------------- |
| Spring Boot Starter Test | Testes unitários e de integração |

---

### ⚙️ Ferramentas de Desenvolvimento

| Ferramenta           | Função                                                   |
| -------------------- | -------------------------------------------------------- |
| Spring Boot DevTools | Hot reload e aumento de produtividade no desenvolvimento |
| IntelliJ IDEA        | IDE principal para desenvolvimento backend               |
| VS Code              | Editor auxiliar                                          |
| Postman              | Teste de endpoints e simulação de requisições HTTP       |
| pgAdmin              | Administração e gerenciamento do banco PostgreSQL        |

---

### 📦 Build e Gerenciamento

| Tecnologia     | Função                                           |
| -------------- | ------------------------------------------------ |
| Maven          | Gerenciamento de dependências e build do projeto |
| Maven Compiler | Compilação com suporte ao Java 17                |
| Maven Javadoc  | Geração de documentação técnica do código        |

---

### 📊 Observabilidade e Logs

| Tecnologia       | Função                                        |
| ---------------- | --------------------------------------------- |
| Logback (Spring) | Gerenciamento de logs da aplicação            |
| SLF4J            | Abstração de logging                          |
| JSON Logging     | Logs estruturados para melhor rastreabilidade |

---

> [!IMPORTANT]
> A escolha dessas tecnologias segue padrões amplamente adotados no mercado, garantindo **produtividade, manutenibilidade e escalabilidade**, além de alinhar o projeto com práticas profissionais utilizadas em aplicações corporativas.

---

## 🚀 API REST — Endpoints

A API do **DSCatalog** expõe endpoints REST seguindo boas práticas de design, utilizando JSON como formato padrão de comunicação.

---

### 📦 Categorias (`/api/v1/categories`)

| Método | Endpoint             | Descrição                           |
| ------ | -------------------- | ----------------------------------- |
| POST   | `/categories`        | Cria uma nova categoria             |
| GET    | `/categories`        | Lista categorias (paginado)         |
| GET    | `/categories/{id}`   | Busca categoria por ID              |
| GET    | `/categories/search` | Busca categorias por nome           |
| PATCH  | `/categories/{id}`   | Atualiza parcialmente uma categoria |
| DELETE | `/categories/{id}`   | Remove uma categoria                |

---

## 📌 Endpoints — Categorias

Base URL: `/api/v1/categories`

Esta seção documenta todos os endpoints relacionados ao recurso **Categoria**, incluindo exemplos de requisição e resposta.

---

### 📥 Criar Categoria

**POST** `/api/v1/categories`

Cria uma nova categoria no sistema.

#### 🔸 Request Body

```json
{
  "name": "Eletrônicos",
  "description": "Produtos eletrônicos em geral",
  "active": true
}
```

> 💡 O campo active é opcional. Caso não seja informado, será definido como false.

### 🔸 Response (201 Created)

```json
{
  "id": 1,
  "name": "Eletrônicos",
  "description": "Produtos eletrônicos em geral",
  "active": true
}
```

### 🔸 Headers

```
Location: /api/v1/categories/1
```

---

### 📄 Listar Categorias (Paginado)

**GET** `/api/v1/categories`

Retorna uma lista paginada de categorias.

> ⚠️ **Observação**  
> Este padrão de parametrização de paginação foi definido explicitamente na API com o objetivo de padronizar a comunicação com o front-end. Apesar disso, o **Spring Data** já fornece suporte nativo à paginação e ordenação por meio do `Pageable`, tornando essa configuração manual opcional. A abordagem adotada aqui prioriza clareza no contrato da API e previsibilidade para o consumo no front-end.

#### 🔸 Query Params

| Parâmetro    | Tipo   | Default | Descrição               |
| ------------ | ------ | ------- | ----------------------- |
| page         | int    | 0       | Número da página        |
| linesPerPage | int    | 12      | Quantidade de registros |
| orderBy      | string | name    | Campo de ordenação      |
| direction    | string | ASC     | Direção (ASC ou DESC)   |

#### 🔸 Exemplo

```http
GET /api/v1/categories?page=0&linesPerPage=10&orderBy=name&direction=ASC
```

### 🔸 Response (200 OK)

```json
{
  "content": [
    {
      "id": 1,
      "name": "Eletrônicos",
      "description": "Produtos eletrônicos",
      "active": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

> 🛡️ Segurança (em desenvolvimento)  
> Este endpoint será protegido com autenticação e controle de acesso (ROLE ADMIN) em versões futuras da API.

---

### 🔍 Buscar Categoria por ID

**GET** `/api/v1/categories/{id}`

Retorna os dados de uma categoria específica.

### 🔸 Response (200 OK)

```json
{
  "id": 1,
  "name": "Eletrônicos",
  "description": "Produtos eletrônicos",
  "active": true
}
```

### 🔸 Erros possíveis

```json
{
  "timestamp": "2026-04-09T18:42:25.491392800Z",
  "status": 404,
  "error": "Resource not found",
  "message": "Entity not found id: 100",
  "path": "/api/v1/categories/100"
}
```

---

### 🔎 Buscar Categorias por Nome

**GET** `/api/v1/categories/search`

Busca categorias por nome (case insensitive e parcial).

### 🔸 Query Params

| Parâmetro | Tipo   | Descrição      |
| --------- | ------ | -------------- |
| name      | string | Termo de busca |

### 🔸 Exemplo

```http
GET /api/v1/categories/search?name=eletrônicos
```

### 🔸 Response (200 OK)

```json
{
  "content": [
    {
      "id": 1,
      "name": "Eletrônicos",
      "description": "Produtos eletrônicos",
      "active": true
    }
  ]
}
```

---

### ✏️ Atualizar Categoria (Parcial)

**PATCH** `/api/v1/categories/{id}`

Atualiza parcialmente os dados de uma categoria.

### 🔸 Request Body

```json
{
  "name": "Eletrônicos Atualizado",
  "active": false
}
```

> 💡 Apenas campos enviados são atualizados
> 💡 Campos null são ignorados

### 🔸 Response (200 OK)

```json
{
  "id": 1,
  "name": "Eletrônicos Atualizado",
  "description": "Produtos eletrônicos",
  "active": false
}
```

---

### ❌ Remover Categoria

**DELETE** `/api/v1/categories/{id}`

Remove uma categoria do sistema.

### 🔸 Response

- 204 No Content

### 🔸 Erros possíveis

- 404 Not Found
- 409 Conflict (violação de integridade)

### ⚠️ Padrão de Erro

- Todos os erros seguem um padrão unificado:

```json
{
  "timestamp": "2026-04-09T18:50:14.708743400Z",
  "status": 404,
  "error": "Resource not found",
  "message": "Entity not found id: 100",
  "path": "/api/v1/categories/100"
}
```

```json
{
  "timestamp": "2026-04-09T18:50:44.722862600Z",
  "status": 409,
  "error": "Database error",
  "message": "Cannot delete resource because it has related entities",
  "path": "/api/v1/categories/1"
}
```

> [!IMPORTANT]
> A API segue boas práticas REST, utilizando corretamente os métodos HTTP (POST, GET, PATCH, DELETE), códigos de status e padronização de respostas, garantindo previsibilidade e facilidade de integração.

---

### 📦 Produtos (`/api/v1/products`)

| Método | Endpoint         | Descrição                        |
| ------ | ---------------- | -------------------------------- |
| POST   | `/products`      | Cria um novo produto             |
| GET    | `/products`      | Lista produtos (paginado)        |
| GET    | `/products/{id}` | Busca produto por ID (detalhado) |
| PATCH  | `/products/{id}` | Atualiza parcialmente um produto |
| DELETE | `/products/{id}` | Remove um produto                |

---

## 📌 Endpoints — Produtos

Base URL: `/api/v1/products`

Esta seção documenta todos os endpoints relacionados ao recurso **Produto**, incluindo exemplos de requisição e resposta.

---

### 📥 Criar Produto

**POST** `/api/v1/products`

Cria um novo produto no sistema.

#### 🔸 Request Body

```json
{
  "name": "Notebook Gamer",
  "description": "Notebook de alta performance",
  "price": 4500.0,
  "imgUrl": "https://image.com/notebook.png",
  "date": "2025-01-01T10:00:00Z",
  "active": true,
  "categoryIds": [1, 2]
}
```

> 💡 As categorias devem ser enviadas apenas como IDs (categoryIds)
> 💡 O backend é responsável por resolver o relacionamento com categorias

### 🔸 Response (201 Created)

```json
{
  "id": 1,
  "name": "Notebook Gamer",
  "description": "Notebook de alta performance",
  "price": 4500.0,
  "imgUrl": "https://image.com/notebook.png",
  "date": "2025-01-01T10:00:00Z",
  "categories": []
}
```

### 🔸 Headers

```http
Location: /api/v1/products/1
```

---

### 📄 Listar Produtos (Paginado)

**GET** `/api/v1/products`

Retorna uma lista paginada de produtos.

> ⚠️ **Observação**  
> Neste endpoint foi adotado o padrão nativo de paginação do **Spring Data**, utilizando os parâmetros `page`, `size` e `sort`. Diferentemente do endpoint de categorias, essa abordagem demonstra a forma padrão recomendada pelo framework, evidenciando como a paginação pode ser implementada de maneira mais direta com o uso de `Pageable`.

### 🔸 Query Params

| Parâmetro | Tipo   | Descrição                |
| --------- | ------ | ------------------------ |
| page      | int    | Número da página         |
| size      | int    | Quantidade de registros  |
| sort      | string | Ordenação (ex: name,asc) |

### 🔸 Exemplo

```http
GET /api/v1/products?page=0&size=10&sort=name,asc
```

### 🔸 Response (200 OK)

```json
{
  "content": [
    {
      "id": 1,
      "name": "Notebook Gamer",
      "description": "Notebook de alta performance",
      "price": 4500.0,
      "imgUrl": "https://image.com/notebook.png",
      "date": "2025-01-01T10:00:00Z",
      "categories": []
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

> 🛡️ Segurança (em desenvolvimento)  
> Este endpoint será protegido com autenticação e controle de acesso (ROLE ADMIN) em versões futuras da API.

---

### 🔍 Buscar Produto por ID

**GET** `/api/v1/products/{id}`

Retorna os detalhes completos de um produto, incluindo suas categorias.

### 🔸 Response (200 OK)

```json
{
  "id": 1,
  "name": "Notebook Gamer",
  "description": "Notebook de alta performance",
  "price": 4500.0,
  "imgUrl": "https://image.com/notebook.png",
  "date": "2025-01-01T10:00:00Z",
  "categories": [
    {
      "id": 1,
      "name": "Eletrônicos",
      "description": "Produtos eletrônicos",
      "active": true
    }
  ]
}
```

### 🔸 Erros possíveis

```json
{
  "timestamp": "2026-04-09T18:42:25.491392800Z",
  "status": 404,
  "error": "Resource not found",
  "message": "Entity not found id: 100",
  "path": "/api/v1/products/100"
}
```

---

### ✏️ Atualizar Produto (Parcial)

**PATCH** `/api/v1/products/{id}`

Atualiza parcialmente os dados de um produto.

### 🔸 Request Body

```json
{
  "name": "Notebook Atualizado",
  "price": 4200.0,
  "categoryIds": [2, 3]
}
```

> 💡 Apenas campos enviados são atualizados
> 💡 Campos null são ignorados
> 💡 Se categoryIds for informado, as categorias serão substituídas

🔸 Response (200 OK)

```json
{
  "id": 1,
  "name": "Notebook Atualizado",
  "description": "Notebook de alta performance",
  "price": 4200.0,
  "imgUrl": "https://image.com/notebook.png",
  "date": "2025-01-01T10:00:00Z",
  "categories": []
}
```

---

### ❌ Remover Produto

**DELETE** `/api/v1/products/{id}`

Remove um produto do sistema.

### 🔸 Response

- 204 No Content

### 🔸 Erros possíveis

- 404 Not Found
- 409 Conflict (violação de integridade)

⚠️ Padrão de Erro

Todos os erros seguem um padrão unificado:

```json
{
  "timestamp": "2026-04-09T18:50:14.708743400Z",
  "status": 404,
  "error": "Resource not found",
  "message": "Entity not found id: 100",
  "path": "/api/v1/products/100"
}
```

```json
{
  "timestamp": "2026-04-09T18:50:44.722862600Z",
  "status": 409,
  "error": "Database error",
  "message": "Cannot delete resource because it has related entities",
  "path": "/api/v1/products/1"
}
```

> [!IMPORTANT]
> A API segue boas práticas REST, utilizando corretamente os métodos HTTP (POST, GET, PATCH, DELETE), códigos de status e padronização de respostas, garantindo previsibilidade e facilidade de integração.

---

### ▶️ Como Executar o Projeto

### 🔧 Pré-requisitos

- Java 17+
- Maven 3.9+
- PostgreSQL (para ambiente `dev`)

---

### 🚀 Executando em ambiente de desenvolvimento

```bash
# Clonar o repositório
git clone https://github.com/seu-usuario/seu-repo.git

# Entrar na pasta do projeto
cd dscatalog

# Executar a aplicação
mvn spring-boot:run
```

### ⚙️ Configuração do banco (PostgreSQL)

Edite o arquivo: `src/main/resources/application-dev.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/dscatalog
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
```

---

### 🧪 Executando em ambiente de teste (H2)

A aplicação utiliza banco em memória automaticamente:

```properties
spring.datasource.url=jdbc:h2:mem:dscatalog
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

Console disponível em: `http://localhost:8080/h2-console`

---

## 📊 Documentação Interativa (Swagger)

A API disponibiliza documentação interativa utilizando **Swagger UI**, permitindo visualizar e testar os endpoints diretamente pelo navegador, sem necessidade de ferramentas externas.

### 🔗 Acesso

Após iniciar a aplicação, acesse:

- http://localhost:8080/swagger-ui.html
- http://localhost:8080/swagger-ui/index.html

> [!TIP]
> Utilize o Swagger para explorar os endpoints, validar requisições e entender rapidamente os contratos da API.

---

### 🔄 Fluxo de Requisição

Exemplo de fluxo ao criar um produto:

1. Cliente envia requisição HTTP (POST `/products`)
2. `ProductController` recebe os dados (DTO)
3. `ProductService` aplica regras de negócio
4. `ProductMapper` converte DTO → Entity
5. `ProductRepository` persiste no banco
6. Resposta é convertida para DTO e retornada

> [!IMPORTANT]
> Esse fluxo garante separação de responsabilidades e baixo acoplamento.

---

### 🧠 Decisões de Arquitetura

Algumas decisões importantes tomadas no projeto:

- Uso de **DTOs com records** → imutabilidade e clareza
- Separação de **Mapper** → evita acoplamento entre camadas
- Uso de **PATCH** → atualização parcial eficiente
- Relacionamento resolvido no backend → evita inconsistência no client
- Tratamento global de exceções → padronização e rastreabilidade

> [!NOTE]
> Essas decisões seguem boas práticas utilizadas em sistemas corporativos.

---

### 🔐 Segurança (Roadmap)

A API está preparada para evolução com segurança baseada em:

- Spring Security
- Autenticação via JWT
- Controle de acesso por roles (ROLE ADMIN)

> [!NOTE]
> Atualmente não implementado, mas planejado para o proximo capítulo do curso.

---

### 🚧 Melhorias Futuras

- Implementação de autenticação com JWT
- Upload de imagens para produtos
- Cache com Redis
- Deploy em cloud (AWS / Railway / Render)
- CI/CD com GitHub Actions
- Testes de integração mais robustos

> [!NOTE]
> O projeto foi estruturado pensando em evolução contínua.

---

### 🎓 Conclusão e Aprendizados

Este projeto foi fundamental para consolidar conceitos essenciais no desenvolvimento de APIs REST com Java e Spring Boot.

Durante a implementação, foram aplicados na prática:

- Estruturação de aplicações em **arquitetura em camadas**
- Uso de **DTOs e Mappers** para desacoplamento
- Implementação de **operações CRUD completas**
- Uso de **paginação, ordenação e filtros**
- Tratamento de **exceções padronizado**
- Organização de código voltada para **manutenção e escalabilidade**

Além disso, o projeto reforçou a importância de:

- Separação clara de responsabilidades
- Padronização de respostas da API
- Uso correto de métodos HTTP e status codes
- Escrita de código limpo e bem documentado

Mais do que apenas um CRUD, este projeto representa a construção de uma base sólida para desenvolvimento de aplicações backend profissionais, seguindo boas práticas amplamente utilizadas no mercado.

> 🚀 Este é um passo importante na evolução como desenvolvedor backend Java, preparando o caminho para projetos mais complexos e ambientes de produção.

---

## 👨‍💻 Autor

**Albert Silva de Jesus**  
Desenvolvedor Backend Java | Spring Boot

---

### 📎 Contato

[![LinkedIn](https://img.shields.io/badge/LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/albert-backend-java-spring-boot/)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com?subject=Contato%20sobre%20o%20projeto%20CAD-MOTOTAXISTA)
