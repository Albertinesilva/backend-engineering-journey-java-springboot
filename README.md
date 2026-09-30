# 🧩 Capítulo 01 — Operações CRUD com Spring Boot

<p align="center">
<em>Building RESTful backend applications with layered architecture, clean code practices, and scalable CRUD operations using Java and Spring Boot.</em>
</p>

<p align="center">

<img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk&logoColor=white" />

<img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

<img src="https://img.shields.io/badge/API-RESTful-success?style=for-the-badge" />

<img src="https://img.shields.io/badge/Architecture-Layered_Architecture-blue?style=for-the-badge" />

<img src="https://img.shields.io/badge/Database-PostgreSQL%20%7C%20H2-336791?style=for-the-badge&logo=postgresql&logoColor=white" />

<img src="https://img.shields.io/badge/Documentation-Swagger%20%7C%20JavaDoc-85EA2D?style=for-the-badge" />

<a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge" /></a>

<img src="https://img.shields.io/github/last-commit/Albertinesilva/backend-engineering-journey-java-springboot?style=for-the-badge" />

</p>

O projeto **ASJCatalog** foi estruturado seguindo boas práticas de desenvolvimento, adotando **arquitetura em camadas** e separação clara de responsabilidades. O nome ASJCatalog vem das iniciais de Albert Silva de Jesus: o projeto nasceu da base do DSCatalog, do curso DevSuperior, e evoluiu de forma independente. Além das operações básicas de CRUD, foram implementados conceitos importantes como:

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
   - Tratamento de exceções (`ResourceNotFoundException`, `DatabaseException`) com logs detalhados.

2. **Paginação e filtragem**
   - Uso de `Pageable` para controlar páginas, tamanho e ordenação.
   - Consultas case-insensitive e parciais (`findByNameContainingIgnoreCase`) para melhorar experiência do usuário.

3. **Mapeamento de relacionamentos**
   - Recebendo apenas **IDs de categorias** no request e resolvendo vínculos no backend.
   - Atualização parcial de categorias e produtos, sem sobrescrever dados não enviados.

4. **Ambientes de desenvolvimento e testes**

| Aspecto             | Ambiente de Testes (`test`)                         | Ambiente de Desenvolvimento (`dev`)                 |
| ------------------- | --------------------------------------------------- | --------------------------------------------------- |
| Banco de dados      | H2 in-memory (efêmero)                              | PostgreSQL local (persistente)                      |
| Console             | `/h2-console` para inspeção manual                  | Console SQL exibindo queries                        |
| Migrations (Flyway) | Desativado (dados carregados pelo `import.sql`)     | Ativo (`db/migration/schema` + `db/migration/data`) |
| Logs                | DEBUG/TRACE, `logs/test/asjcatalog-test.log`        | DEBUG/TRACE, `logs/dev/asjcatalog-dev.log`          |
| Banner              | Banner personalizado (`banner-dev.txt`)             | Banner personalizado (`banner-dev.txt`)             |
| Objetivo            | Execução rápida, sem depender de um banco externo   | Desenvolvimento realista com dados persistentes     |
| Observações         | Banco efêmero, reset a cada execução                | Controle de schema, rastreabilidade completa        |

> [!IMPORTANT]
> Essa separação permite rodar a aplicação **de forma rápida e isolada**, sem depender de um banco externo, enquanto o desenvolvimento ocorre em ambiente realista com dados persistentes e migrations. Reflete boas práticas de engenharia de software.

5. **Documentação da API e código**
   - **OpenAPI/Swagger** para documentação interativa;
   - **JavaDocs** explicando responsabilidades de controllers e services.

6. **Boas práticas de código e arquitetura**
   - Estrutura clara em **Controller, Service e Repository**;
   - Uso de **logger** para monitoramento e rastreabilidade;
   - Tratamento transacional adequado (`@Transactional`) para consistência de dados.

---

## 📦 Estrutura do Projeto

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
┃ ┣ 📂 `exception`  
┃ ┃ ┣ 📄 `DatabaseException.java`  
┃ ┃ ┗ 📄 `ResourceNotFoundException.java`  
┃ ┣ 📄 `CategoryService.java`  
┃ ┗ 📄 `ProductService.java`  
┣ 📂 `web`  
┃ ┣ 📂 `controller`  
┃ ┃ ┣ 📄 `CategoryController.java`  
┃ ┃ ┗ 📄 `ProductController.java`  
┃ ┗ 📂 `exception`  
┃ ┃ ┣ 📂 `enums`  
┃ ┃ ┃ ┗ 📄 `ErrorType.java`  
┃ ┃ ┣ 📂 `handler`  
┃ ┃ ┃ ┗ 📄 `ControllerExceptionHandler.java`  
┃ ┃ ┗ 📂 `response`  
┃ ┃ ┃ ┗ 📄 `ProblemDetails.java`  
┣ 📄 `AsjcatalogApplication.java`  
┣ 📂 `resources`  
┃ ┣ 📂 `db`  
┃ ┃ ┗ 📂 `migration`  
┃ ┃ ┃ ┣ 📂 `schema`  
┃ ┃ ┃ ┗ 📂 `data`  
┃ ┣ 📄 `application-dev.properties`  
┃ ┣ 📄 `application-prod.properties`  
┃ ┣ 📄 `application-test.properties`  
┃ ┣ 📄 `application.properties`  
┃ ┣ 📄 `banner-dev.txt`  
┃ ┗ 📄 `import.sql`  

---

## 🧱 Arquitetura em Camadas

A aplicação **ASJCatalog** segue a arquitetura tradicional **Controller → Service → Repository**, organizada em camadas bem definidas para garantir **manutenção mais fácil, testabilidade e escalabilidade**.

<img src="docs/assets/imgs/padrao-camadas.png" width="100%">

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

O projeto **ASJCatalog** foi desenvolvido utilizando um conjunto moderno de tecnologias voltadas para construção de APIs REST robustas, escaláveis e bem estruturadas.

### 📌 Stack Principal

| Categoria    | Tecnologia             | Função                                                                                              |
| ------------ | ---------------------- | --------------------------------------------------------------------------------------------------- |
| Linguagem    | Java 17                | Desenvolvimento backend moderno com recursos atuais da linguagem                                    |
| Framework    | Spring Boot 3.5.13     | Estrutura principal da aplicação e gerenciamento de dependências                                    |
| API REST     | Spring Web             | Criação de endpoints HTTP (RESTful APIs)                                                            |
| Persistência | Spring Data JPA        | Abstração para acesso a dados e integração com ORM                                                  |
| ORM          | Hibernate              | Mapeamento objeto-relacional (Entity ↔ Tabela)                                                      |
| Validação    | Spring Boot Validation | Dependência declarada neste capítulo; as regras de validação são implementadas no capítulo 03       |

---

### 🗄️ Banco de Dados

| Categoria       | Tecnologia  | Função                                                    |
| --------------- | ----------- | --------------------------------------------------------- |
| Banco Principal | PostgreSQL  | Banco relacional utilizado no ambiente de desenvolvimento |
| Banco de Testes | H2 Database | Banco em memória do perfil `test`, rápido e isolado       |
| Console DB      | H2 Console  | Interface web para inspeção de dados no perfil `test`     |

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

| Tecnologia       | Função                                                         |
| ---------------- | -------------------------------------------------------------- |
| Logback (Spring) | Gerenciamento de logs da aplicação, com arquivos por ambiente  |
| SLF4J            | Abstração de logging                                           |

---

> [!IMPORTANT]
> A escolha dessas tecnologias segue padrões amplamente adotados no mercado, garantindo **produtividade, manutenibilidade e escalabilidade**, além de alinhar o projeto com práticas profissionais utilizadas em aplicações corporativas.

---

## 🚀 API REST — Endpoints

A API do **ASJCatalog** expõe endpoints REST seguindo boas práticas de design, utilizando JSON como formato padrão de comunicação.

---

### 📦 Categorias (`/api/v1/categories`)

| Método | Endpoint                      | Descrição                                     |
| ------ | ----------------------------- | --------------------------------------------- |
| POST   | `/categories`                 | Cria uma nova categoria (201)                 |
| GET    | `/categories`                 | Lista categorias (paginado, filtro por nome)  |
| GET    | `/categories/{id}`            | Busca categoria por ID                        |
| PATCH  | `/categories/{id}`            | Atualiza parcialmente uma categoria           |
| PATCH  | `/categories/{id}/activate`   | Ativa uma categoria (204)                     |
| PATCH  | `/categories/{id}/deactivate` | Desativa uma categoria (204)                  |
| DELETE | `/categories/{id}`            | Remove uma categoria (204; 409 se tiver produtos) |

---

## 📌 Endpoints — Categorias

Base URL: `/api/v1/categories`

Esta seção mostra os principais exemplos do recurso **Categoria**. Todos os endpoints, com seus exemplos, estão em [Endpoints da API](docs/guides/API-ENDPOINTS.md).

---

### 📥 Criar Categoria

**POST** `/api/v1/categories`

#### 🔸 Request Body

```json
{
  "name": "Garden",
  "description": "Garden tools"
}
```

> 💡 A categoria é criada sempre ativa. O status muda pelos endpoints `activate` e `deactivate`.

### 🔸 Response (201 Created)

```json
{
  "id": 16,
  "name": "Garden",
  "description": "Garden tools",
  "active": true
}
```

O cabeçalho `Location` aponta para a nova categoria (`/api/v1/categories/16`).

---

### 📄 Listar Categorias (Paginado)

**GET** `/api/v1/categories`

A paginação usa o padrão nativo do **Spring Data**, por meio do `Pageable`.

#### 🔸 Query Params

| Parâmetro | Tipo   | Default       | Descrição                                   |
| --------- | ------ | ------------- | ------------------------------------------- |
| page      | int    | 0             | Número da página                            |
| size      | int    | 20            | Quantidade de registros                     |
| sort      | string | sem ordenação | Campo e direção (ex: `name,asc`)            |
| name      | string | sem filtro    | Trecho do nome (case insensitive e parcial) |

#### 🔸 Exemplo

```http
GET /api/v1/categories?page=0&size=2&sort=name,asc
```

### 🔸 Response (200 OK)

```json
{
  "content": [
    { "id": 12, "name": "Automotive", "description": "Car parts and automotive accessories", "active": true },
    { "id": 11, "name": "Beauty", "description": "Beauty and cosmetics products", "active": true }
  ],
  "totalElements": 15,
  "totalPages": 8,
  "size": 2,
  "number": 0
}
```

> 🛡️ Segurança (em desenvolvimento)  
> Este endpoint será protegido com autenticação e controle de acesso (ROLE ADMIN) em versões futuras da API.

---

### ✏️ Demais operações

- **Buscar por ID** (`GET /api/v1/categories/{id}`): 200 com a categoria, ou 404.
- **Atualizar parcialmente** (`PATCH /api/v1/categories/{id}`): só os campos enviados (`name`, `description`) são alterados; campos nulos são ignorados.
- **Ativar e desativar** (`PATCH /api/v1/categories/{id}/activate` e `/deactivate`): 204.
- **Remover** (`DELETE /api/v1/categories/{id}`): 204, 404 ou 409 se a categoria tiver produtos.

### ⚠️ Padrão de Erro

- Todos os erros seguem um padrão unificado:

```json
{
  "timestamp": "2026-09-30T01:27:43.886780700Z",
  "status": 404,
  "error": "Resource not found",
  "message": "Entity not found id: 9999",
  "path": "/api/v1/categories/9999"
}
```

```json
{
  "timestamp": "2026-09-30T01:44:34.523364700Z",
  "status": 409,
  "error": "Conflict",
  "message": "Cannot delete resource because it has related entities",
  "path": "/api/v1/categories/1"
}
```

> [!IMPORTANT]
> A API segue boas práticas REST, utilizando corretamente os métodos HTTP (POST, GET, PATCH, DELETE), códigos de status e padronização de respostas, garantindo previsibilidade e facilidade de integração.

---

### 📦 Produtos (`/api/v1/products`)

| Método | Endpoint                    | Descrição                                       |
| ------ | --------------------------- | ----------------------------------------------- |
| POST   | `/products`                 | Cria um novo produto (201)                      |
| GET    | `/products`                 | Lista produtos (paginado, filtro por nome)      |
| GET    | `/products/{id}`            | Busca produto por ID (detalhado, com categorias) |
| PATCH  | `/products/{id}`            | Atualiza parcialmente um produto                |
| PATCH  | `/products/{id}/activate`   | Ativa um produto (204)                          |
| PATCH  | `/products/{id}/deactivate` | Desativa um produto (204)                       |
| DELETE | `/products/{id}`            | Remove um produto (204)                         |

---

## 📌 Endpoints — Produtos

Base URL: `/api/v1/products`

Esta seção mostra os principais exemplos do recurso **Produto**. Todos os endpoints, com seus exemplos, estão em [Endpoints da API](docs/guides/API-ENDPOINTS.md).

---

### 📥 Criar Produto

**POST** `/api/v1/products`

#### 🔸 Request Body

```json
{
  "name": "Garden Hose",
  "description": "15m hose",
  "price": 79.9,
  "imgUrl": "https://example.com/hose.png",
  "date": "2020-07-20T10:00:00Z",
  "categoryIds": [2]
}
```

> 💡 As categorias devem ser enviadas apenas como IDs (categoryIds)
> 💡 O backend é responsável por resolver o relacionamento com categorias

### 🔸 Response (201 Created)

```json
{
  "id": 26,
  "name": "Garden Hose",
  "description": "15m hose",
  "price": 79.9,
  "imgUrl": "https://example.com/hose.png",
  "date": "2020-07-20T10:00:00Z",
  "categories": []
}
```

> 💡 As categorias são gravadas, mas a resposta de criação, atualização e listagem traz `categories` vazio. Elas aparecem na busca por ID.

---

### 🔍 Buscar Produto por ID

**GET** `/api/v1/products/{id}`

Retorna os detalhes do produto, incluindo suas categorias.

### 🔸 Response (200 OK)

```json
{
  "id": 26,
  "name": "Garden Hose",
  "description": "15m hose",
  "price": 79.9,
  "imgUrl": "https://example.com/hose.png",
  "date": "2020-07-20T10:00:00Z",
  "categories": [
    { "id": 2, "name": "Electronics", "description": "Electronic devices and gadgets", "active": true }
  ]
}
```

---

### ✏️ Demais operações

- **Listar** (`GET /api/v1/products`): mesmos parâmetros de paginação e filtro das categorias (`page`, `size`, `sort`, `name`).
- **Atualizar parcialmente** (`PATCH /api/v1/products/{id}`): campos nulos são ignorados; se `categoryIds` for informado, as categorias são substituídas.
- **Ativar e desativar** (`PATCH /api/v1/products/{id}/activate` e `/deactivate`): 204.
- **Remover** (`DELETE /api/v1/products/{id}`): 204, ou 404 se o produto não existir.

> [!IMPORTANT]
> A API segue boas práticas REST, utilizando corretamente os métodos HTTP (POST, GET, PATCH, DELETE), códigos de status e padronização de respostas, garantindo previsibilidade e facilidade de integração.

---

### 🚀 Como Executar

**Pré-requisitos:** JDK 17 e Git; PostgreSQL só para o perfil `dev`.

Com o perfil `test`, a aplicação sobe com o banco H2 em memória, sem instalar nada além do JDK:

**PowerShell**:

```powershell
git clone https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot; git checkout chapter-01-crud
cd backend
.\mvnw spring-boot:run '-Dspring-boot.run.arguments=--spring.profiles.active=test'
```

**bash**:

```bash
git clone https://github.com/Albertinesilva/backend-engineering-journey-java-springboot.git
cd backend-engineering-journey-java-springboot && git checkout chapter-01-crud
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=test
```

O perfil `dev` (padrão) usa PostgreSQL: é preciso criar o banco `asjcatalog` e definir as variáveis `POSTGRES_DATASOURCE_USER` e `POSTGRES_DATASOURCE_PASSWORD`.

> [!TIP]
> O passo a passo completo, com o perfil `dev`, o console do H2 e as primeiras chamadas, está em [Primeiros Passos](docs/guides/GETTING-STARTED.md).

---

### 📖 Documentação Técnica

| 📘 Documento                                              | ⚡ Descrição                                                        |
| --------------------------------------------------------- | ------------------------------------------------------------------ |
| [🚀 Primeiros Passos](docs/guides/GETTING-STARTED.md)     | Como rodar localmente, com H2 ou PostgreSQL, e as primeiras chamadas |
| [⚙️ Configuração e Perfis](docs/guides/CONFIGURATION.md)  | Perfis `dev`, `test` e `prod` e variáveis de ambiente              |
| [🏗️ Arquitetura](docs/guides/ARCHITECTURE.md)             | Camadas, pacotes e caminho de uma requisição                       |
| [🧩 Modelo de Domínio](docs/guides/DOMAIN-MODEL.md)       | Entidades `Category` e `Product` e o relacionamento entre elas     |
| [🔍 Acesso a Dados](docs/guides/DATA-ACCESS.md)           | Repositórios, consultas, paginação e transações                    |
| [🗄️ Migrations](docs/guides/DATABASE-MIGRATIONS.md)       | Flyway, `import.sql` e `create.sql`                                |
| [🌐 Endpoints da API](docs/guides/API-ENDPOINTS.md)       | Os 14 endpoints, com exemplos reais                                |
| [⚠️ Tratamento de Erros](docs/guides/ERROR-HANDLING.md)   | `ProblemDetails`, `ErrorType` e status HTTP                        |
| [📐 Convenções](docs/guides/CONVENTIONS.md)               | Nomes, idioma, JavaDoc, commits, branches e migrations             |
| [🏠 Índice da Documentação](docs/HOME.md)                 | Visão geral dos guias e do que chega nos próximos capítulos        |

---

## 📊 Documentação Interativa (Swagger)

A API disponibiliza documentação interativa utilizando **Swagger UI**, permitindo visualizar e testar os endpoints diretamente pelo navegador, sem necessidade de ferramentas externas.

### 🔗 Acesso

Após iniciar a aplicação (perfis `dev` ou `test`), acesse:

- http://localhost:8080/docs-asjcatalog.html
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
> Atualmente não implementado, mas planejado para o capítulo 03 do curso.

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
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com?subject=Contato%20sobre%20o%20projeto%20ASJCatalog)
