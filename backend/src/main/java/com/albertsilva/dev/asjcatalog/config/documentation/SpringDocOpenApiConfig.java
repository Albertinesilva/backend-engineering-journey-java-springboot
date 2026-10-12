package com.albertsilva.dev.asjcatalog.config.documentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados do OpenAPI/Swagger (springdoc) da API: título, descrição, versão,
 * licença, contato e o esquema de segurança {@code security} (HTTP Bearer/JWT),
 * referenciado por {@code @SecurityRequirement(name = "security")} nos
 * controllers.
 *
 * <p>
 * <b>Caminhos:</b> definidos por perfil em {@code application-*.properties}
 * ({@code /docs-asjcatalog} nos perfis {@code dev} e {@code test} (desativado
 * em {@code prod})); a segurança só libera os caminhos {@code /docs-asjcatalog*} e
 * {@code /swagger-ui/**}. Os controllers escaneados são os de
 * {@code web.controller}. No perfil {@code prod}, o springdoc é desativado por
 * {@code springdoc.api-docs.enabled=false} e
 * {@code springdoc.swagger-ui.enabled=false} em
 * {@code application-prod.properties}.
 * </p>
 */
@Configuration
public class SpringDocOpenApiConfig {

  /**
   * @return definição OpenAPI com informações da API e o esquema de segurança
   *         {@code security}
   */
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components().addSecuritySchemes("security", securityScheme()))
        .info(new Info().title("ASJCatalog API")
            .description(
                "RESTful API para gerenciamento de catálogo de produtos, categorias e usuários, com suporte a autenticação e autorização via JWT.")
            .version("v1")
            .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
            .contact(new Contact().name("Albert Silva de Jesus").email("albertinesilva.17@gmail.com")
                .url("https://github.com/Albertinesilva")));
  }

  /**
   * @return esquema HTTP {@code bearer} com formato {@code JWT}, chamado
   *         {@code security}
   */
  private SecurityScheme securityScheme() {
    return new SecurityScheme()
        .description("Insira um bearer token valido para prosseguir, exemplo: Bearer {token}")
        .type(SecurityScheme.Type.HTTP)
        .in(SecurityScheme.In.HEADER)
        .scheme("bearer")
        .bearerFormat("JWT")
        .name("security");

  }

}
