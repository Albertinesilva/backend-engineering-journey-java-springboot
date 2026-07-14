<h1 align="center">🌍 Internacionalização (Internationalization - i18n)</h1>

<p align="justify">
<em>
This documentation explores the internationalization (i18n) infrastructure of ASJCatalog, covering the implementation of multilingual API responses, centralized message management, locale resolution, standardized error handling, and Spring's native internationalization features to build scalable and globally ready REST APIs.
</em>
</p>

<p align="center">

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Framework](https://img.shields.io/badge/Spring_Framework-6.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Internationalization](https://img.shields.io/badge/Internationalization-i18n-blue?style=for-the-badge)
![MessageSource](https://img.shields.io/badge/MessageSource-Spring-success?style=for-the-badge)
![LocaleResolver](https://img.shields.io/badge/AcceptHeaderLocaleResolver-orange?style=for-the-badge)
![Bean Validation](https://img.shields.io/badge/Bean_Validation-Jakarta-red?style=for-the-badge)
![REST API](https://img.shields.io/badge/REST_API-ProblemDetails-005571?style=for-the-badge)
![Languages](https://img.shields.io/badge/Languages-PT--BR%20%7C%20EN%20%7C%20ES-purple?style=for-the-badge)

</p>

<p align="center">

![Stateless](https://img.shields.io/badge/Stateless-API-blueviolet?style=flat-square)
![UTF-8](https://img.shields.io/badge/UTF--8-Unicode-informational?style=flat-square)
![ApiErrorCode](https://img.shields.io/badge/ApiErrorCode-Stable-success?style=flat-square)
![ProblemDetails](https://img.shields.io/badge/ProblemDetails-RFC7807-blue?style=flat-square)
![Accept-Language](https://img.shields.io/badge/Accept--Language-RFC9110-orange?style=flat-square)

</p>

---
## 📑 Sumário

> Navegação da documentação de Internacionalização (i18n).

---

| 🧩 Módulo                                                                   | ⚡ Descrição                                                  |
| --------------------------------------------------------------------------- | ------------------------------------------------------------- |
| [📖 Visão Geral](#-visão-geral)                                             | Introdução à arquitetura de internacionalização               |
| [🎯 Objetivos](#-objetivos)                                                 | Metas e princípios adotados na implementação do i18n          |
| [🏗 Arquitetura](#-arquitetura)                                             | Fluxo de resolução de mensagens com componentes do Spring     |
| [⚙️ Configuração](#️-configuração)                                          | Configuração do MessageSource e do LocaleResolver             |
| [📂 Organização dos Arquivos](#-organização-dos-arquivos)                   | Estrutura dos arquivos de mensagens por idioma                |
| [🧩 Estrutura das Mensagens](#-estrutura-das-mensagens)                     | Organização das chaves de tradução por domínio                |
| [🌐 Recursos Internacionalizados](#-recursos-internacionalizados)           | Validações, exceções e respostas localizadas da API           |
| [📦 ProblemDetails](#-problemdetails)                                       | Padronização das respostas de erro multilíngues               |
| [🔄 Fluxo da Internacionalização](#-fluxo-da-internacionalização)           | Ciclo de resolução de mensagens durante a requisição          |
| [🚀 Benefícios](#-benefícios)                                               | Vantagens da arquitetura adotada                              |
| [📈 Evolução da Implementação](#-evolução-da-implementação)                 | Evolução da validação para uma infraestrutura completa de i18n |
| [🎯 Boas Práticas Aplicadas](#-boas-práticas-aplicadas)                     | Boas práticas de internacionalização com Spring Framework     |
| [🎓 Aprendizados](#-aprendizados)                                           | Conceitos consolidados durante a implementação                |
| [💼 Competências Desenvolvidas](#-competências-desenvolvidas)               | Competências técnicas demonstradas                            |
| [🏁 Conclusão](#-conclusão)                                                 | Considerações finais sobre a implementação                    |
| [📚 Referências](#-referências)                                             | Documentações oficiais e materiais de apoio                   |
| [👨‍💻 Autor](#-autor)                                                      | Informações sobre o autor                                     |
| [📎 Contato](#-contato)                                                     | Canais de contato profissional                                |

---
## 📖 Visão Geral

O ASJCatalog implementa um mecanismo completo de internacionalização (Internationalization — **i18n**) baseado na infraestrutura nativa do Spring Framework, permitindo que todas as mensagens retornadas pela API sejam apresentadas no idioma solicitado pelo cliente.

A solução foi projetada para desacoplar completamente os textos da lógica da aplicação, centralizando todas as mensagens em arquivos de propriedades organizados por idioma.

Atualmente são suportados os seguintes idiomas:

| Idioma                | Locale  |
| --------------------- | ------- |
| 🇧🇷 Português (Brasil) | `pt-BR` |
| 🇺🇸 Inglês             | `en`    |
| 🇪🇸 Espanhol           | `es`    |

A seleção do idioma ocorre automaticamente através do cabeçalho HTTP **Accept-Language**, sem necessidade de parâmetros adicionais na URL ou armazenamento de preferência em sessão.

---

## 🎯 Objetivos

A internacionalização foi implementada com os seguintes objetivos:

- desacoplar completamente as mensagens da lógica de negócio;
- permitir múltiplos idiomas utilizando a mesma base de código;
- fornecer mensagens consistentes para toda a API;
- facilitar manutenção e tradução das mensagens;
- melhorar a experiência de integração para consumidores internacionais da API;
- centralizar todas as mensagens em um único mecanismo de resolução.

---

## 🏗 Arquitetura

A infraestrutura de internacionalização utiliza os componentes nativos do Spring Framework.

```java
Cliente HTTP
      │
Accept-Language
      │
      ▼
AcceptHeaderLocaleResolver
      │
      ▼
Locale
      │
      ▼
MessageSource
      │
      ▼
messages_pt_BR.properties
messages_en.properties
messages_es.properties
      │
      ▼
Mensagem traduzida
```

Toda a resolução das mensagens é realizada durante o processamento da requisição.

---

## ⚙️ Configuração

A configuração da internacionalização encontra-se centralizada na classe:

```java
MessageSourceConfig
```

Ela possui duas responsabilidades principais:

- configurar o `MessageSource`;
- definir o resolvedor de Locale baseado no cabeçalho HTTP.

### MessageSource

Foi utilizado o componente:

```java
ReloadableResourceBundleMessageSource
```

Configuração aplicada:

| Configuração                   | Finalidade                                         |
| ------------------------------ | -------------------------------------------------- |
| `basename=messages`            | Localização dos arquivos de mensagens              |
| `UTF-8`                        | Suporte completo a caracteres especiais            |
| `fallbackToSystemLocale=false` | Impede utilização do Locale do sistema operacional |
| `defaultLocale=pt-BR`          | Português como idioma padrão                       |

---

### LocaleResolver

O projeto utiliza:

```java
AcceptHeaderLocaleResolver
```

A escolha do idioma ocorre automaticamente através do cabeçalho:

```http
Accept-Language
```

Exemplos:

```http
Accept-Language: pt-BR
```

```http
Accept-Language: en
```

```http
Accept-Language: es
```

Caso nenhum idioma seja informado, a aplicação utiliza:

```text
pt-BR
```

como idioma padrão.

Essa estratégia torna a API completamente stateless, sem necessidade de armazenar idioma em sessão ou cookies.

---

## 📂 Organização dos Arquivos

As mensagens encontram-se organizadas em arquivos independentes por idioma.

```java
src/main/resources

messages_pt_BR.properties
messages_en.properties
messages_es.properties
```

Cada arquivo contém exatamente as mesmas chaves, alterando apenas os textos traduzidos.

Essa organização facilita:

- inclusão de novos idiomas;
- manutenção;
- revisão das traduções;
- consistência entre todos os idiomas suportados.

---

## 🧩 Estrutura das Mensagens

As mensagens foram organizadas por domínio funcional.

```java
API
 ├── ProblemDetails
 ├── Erros Globais

Categoria
 ├── Bean Validation
 ├── Validações customizadas

Produto
 ├── Bean Validation
 ├── Validações customizadas

Usuário
 ├── Dados pessoais
 ├── Senha

Role

Tokens

Autenticação
```

Essa divisão melhora significativamente a organização dos arquivos de tradução.

---

## 🌐 Recursos Internacionalizados

A internacionalização não se limita apenas às validações da aplicação.

Ela está presente em toda a API.

### Bean Validation

Todas as anotações de validação utilizam chaves de mensagens.

Exemplos:

```java
category.name.notBlank

product.price.positive

user.email.invalid
```

---

### Validadores Customizados

Os validadores implementados pela aplicação também utilizam o `MessageSource`.

Isso permite que regras específicas do domínio retornem mensagens traduzidas da mesma forma que as validações nativas do Bean Validation.

Exemplos:

- categoria já existente;
- produto duplicado;
- e-mail já cadastrado;
- categorias inexistentes;
- roles inválidas.

---

### Exceções de Negócio

As exceções da camada de serviço não retornam textos fixos.

Em vez disso, retornam códigos de mensagem.

Exemplo:

```java
error.product.notFound
```

Durante o tratamento da exceção, esse código é convertido para o idioma solicitado pelo cliente.

---

### ControllerExceptionHandler

Toda a tradução das respostas de erro é centralizada no:

```java
ControllerExceptionHandler
```

A classe recebe automaticamente o `Locale` resolvido pelo Spring.

```text
Locale locale
```

Cada resposta utiliza o `MessageSource` para resolver:

- título;
- descrição;
- mensagens específicas.

Essa abordagem garante consistência em todas as respostas da API.

---

### 🏷 ApiErrorCode

Além das mensagens traduzidas, o projeto utiliza um código estável de erro através do enum:

```java
ApiErrorCode
```

Cada resposta contém duas informações distintas:

| Campo     | Finalidade                                    |
| --------- | --------------------------------------------- |
| `code`    | Código estável para integração entre sistemas |
| `error`   | Título traduzido                              |
| `message` | Descrição traduzida                           |

Exemplo:

```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "error": "Recurso não encontrado",
  "message": "Produto não encontrado"
}
```

Em inglês:

```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "error": "Resource not found",
  "message": "Product not found"
}
```

O campo `code` nunca muda de idioma, permitindo que aplicações clientes utilizem esse identificador para regras de negócio, enquanto os usuários recebem mensagens no idioma adequado.

---

## 📦 ProblemDetails

Todas as respostas de erro seguem um formato padronizado.

Estrutura:

```text
timestamp
status
code
error
message
path
```

Nos casos de erro de validação também é retornada a lista:

```text
fieldErrors
```

com cada campo contendo sua mensagem já traduzida.

---

## 🔄 Fluxo da Internacionalização

```java
Cliente

    │

Accept-Language

    │

    ▼

AcceptHeaderLocaleResolver

    │

    ▼

Locale

    │

    ▼

ControllerExceptionHandler

    │

    ▼

MessageSource

    │

    ▼

messages_pt_BR.properties
messages_en.properties
messages_es.properties

    │

    ▼

ProblemDetails

    │

    ▼

Resposta JSON traduzida
```

---

## 🚀 Benefícios

A arquitetura adotada oferece diversos benefícios:

- completa separação entre lógica e textos;
- facilidade para inclusão de novos idiomas;
- mensagens consistentes em toda a aplicação;
- compatibilidade total com Bean Validation;
- integração nativa com o Spring Framework;
- API preparada para clientes internacionais;
- manutenção simplificada;
- ausência de textos fixos no código-fonte.

---

## 📈 Evolução da Implementação

Inicialmente a aplicação utilizava um único arquivo:

```
ValidationMessages.properties
```

Durante a evolução do projeto, essa abordagem foi substituída por uma estrutura completa de internacionalização baseada em múltiplos arquivos de mensagens:

```
messages_pt_BR.properties

messages_en.properties

messages_es.properties
```

Essa mudança permitiu internacionalizar não apenas as mensagens de validação, mas também:

- erros globais;
- exceções de negócio;
- respostas do `ControllerExceptionHandler`;
- mensagens associadas ao `ApiErrorCode`;
- validações customizadas;
- mensagens do domínio.

Com isso, toda a API passou a responder integralmente no idioma solicitado pelo consumidor.

---

## 🎯 Boas Práticas Aplicadas

Durante a implementação foram adotadas diversas práticas recomendadas pelo ecossistema Spring:

| Boa prática                | Aplicação                               |
| -------------------------- | --------------------------------------- |
| Uso do `MessageSource`     | Centralização das mensagens             |
| AcceptHeaderLocaleResolver | Resolução automática do idioma          |
| UTF-8                      | Compatibilidade internacional           |
| Locale padrão              | Português (Brasil)                      |
| Separação por arquivos     | Um arquivo para cada idioma             |
| Mensagens por chave        | Nenhum texto fixo na aplicação          |
| ApiErrorCode               | Código estável independente da tradução |
| ProblemDetails             | Respostas padronizadas                  |
| Bean Validation            | Integração nativa                       |
| ControllerExceptionHandler | Tradução centralizada                   |

---

## 🎓 Aprendizados

A implementação da internacionalização consolidou conhecimentos relacionados a:

- Internationalization (i18n);
- MessageSource;
- Locale;
- AcceptHeaderLocaleResolver;
- Bean Validation internacionalizado;
- tratamento global de exceções;
- organização de arquivos de mensagens;
- APIs multilíngues;
- integração entre Spring MVC e Spring Validation.

---

## 💼 Competências Desenvolvidas

| Competência                   | Aplicação |
| ----------------------------- | --------- |
| Internationalization (i18n)   | ✔️        |
| Spring MessageSource          | ✔️        |
| AcceptHeaderLocaleResolver    | ✔️        |
| Bean Validation               | ✔️        |
| Validações Customizadas       | ✔️        |
| Tratamento Global de Exceções | ✔️        |
| ProblemDetails                | ✔️        |
| ApiErrorCode                  | ✔️        |
| APIs Multilíngues             | ✔️        |
| Organização de Recursos       | ✔️        |

---

## 🏁 Conclusão

A implementação da internacionalização transformou o ASJCatalog em uma API preparada para consumidores de diferentes idiomas, utilizando exclusivamente recursos nativos do ecossistema Spring. A adoção do `MessageSource`, do `AcceptHeaderLocaleResolver` e da centralização das mensagens em arquivos de propriedades permitiu desacoplar completamente os textos da lógica da aplicação, tornando o sistema mais organizado, escalável e de fácil manutenção.

Além das mensagens de validação, a internacionalização passou a abranger exceções de negócio, respostas padronizadas da API, códigos de erro, validações customizadas e mensagens do domínio, garantindo consistência em toda a comunicação com os clientes. Essa abordagem aproxima o projeto das boas práticas utilizadas em aplicações corporativas, facilitando a evolução para novos idiomas e ampliando sua capacidade de integração em ambientes internacionais.

---

## 📚 Referências

- Spring Framework – Internationalization
  https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html

- Spring MessageSource
  https://docs.spring.io/spring-framework/reference/core/beans/context.html

- Spring Validation
  https://docs.spring.io/spring-framework/reference/core/validation/

- Jakarta Bean Validation
  https://jakarta.ee/specifications/bean-validation/

- RFC 9110 – Accept-Language Header
  https://www.rfc-editor.org/rfc/rfc9110.html

- Unicode UTF-8
  https://unicode.org/

---

## 👨‍💻 Autor

**Albert Silva de Jesus**  
Desenvolvedor Backend Java | Spring Boot

---

## 📎 Contato

[![LinkedIn](https://img.shields.io/badge/LinkedIn-%230077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/albert-backend-java-spring-boot/)
[![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)](mailto:albertinesilva.17@gmail.com)
