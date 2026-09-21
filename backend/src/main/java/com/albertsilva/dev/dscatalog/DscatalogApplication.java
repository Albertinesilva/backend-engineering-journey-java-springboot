package com.albertsilva.dev.dscatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação ASJCatalog (API REST em Spring Boot).
 *
 * <p>
 * {@code @SpringBootApplication} combina configuração de classe, varredura de
 * componentes e auto-configuração. A varredura parte do pacote
 * {@code com.albertsilva.dev.dscatalog} e inclui os subpacotes; por isso as
 * classes {@code @Configuration}, {@code @Service}, {@code @RestController} e
 * demais componentes são encontrados sem registro manual.
 * </p>
 *
 * <p>
 * <b>Configuração externa:</b> o perfil ativo é definido em
 * {@code application.properties} por {@code ${APP_PROFILE:test}}, ou seja,
 * sem a variável de ambiente {@code APP_PROFILE} o perfil é {@code test}
 * (H2 em memória). Os detalhes de cada perfil estão em
 * {@code docs/backend/B-8-CONFIG-INFRASTRUCTURE.md}.
 * </p>
 *
 * <p>
 * <b>Recursos que esta classe não habilita:</b> não há {@code @EnableAsync},
 * {@code @EnableScheduling} nem {@code @EnableTransactionManagement} explícitos
 * no projeto; o gerenciamento de transações vem da auto-configuração do
 * Spring Boot, e os métodos {@code @Async} de {@code EmailService} dependem de
 * {@code @EnableAsync}, que não existe (ver a documentação de
 * {@code EmailService}).
 * </p>
 */
@SpringBootApplication
public class DscatalogApplication {
	/**
	 * Inicia o contêiner Spring (e o servidor web embutido).
	 *
	 * @param args argumentos de linha de comando repassados ao Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(DscatalogApplication.class, args);
	}

}
