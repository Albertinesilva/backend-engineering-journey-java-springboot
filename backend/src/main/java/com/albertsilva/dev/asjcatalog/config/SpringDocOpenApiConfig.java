package com.albertsilva.dev.dscatalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringDocOpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components().addSecuritySchemes("security", securityScheme()))
        .info(new Info().title("DSCatalog API")
            .description(
                "RESTful API para gerenciamento de catálogo de produtos, categorias e operações de vendas em um sistema de e-commerce")
            .version("v1")
            .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
            .contact(new Contact().name("Albert Silva de Jesus").email("albertinesilva,17@gmail.com")
                .url("https://github.com/Albertinesilva")));
  }

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
