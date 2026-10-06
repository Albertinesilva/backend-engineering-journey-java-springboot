package com.albertsilva.dev.asjcatalog.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuração de beans de segurança para a aplicação.
 *
 * <p>
 * Esta classe é responsável por registrar e configurar os beans
 * necessários para operações de segurança na aplicação, especialmente
 * relacionados à codificação e validação de senhas.
 * </p>
 *
 * <p>
 * <b>Responsabilidades:</b>
 * </p>
 * <ul>
 * <li>Fornecer um {@link PasswordEncoder} para codificação segura de
 * senhas</li>
 * <li>Integrar com Spring Security para validação de credenciais</li>
 * </ul>
 *
 * <p>
 * <b>Estratégia de codificação:</b>
 * </p>
 * <p>
 * A classe utiliza {@link BCryptPasswordEncoder}, que implementa o algoritmo
 * bcrypt.
 * Bcrypt é um algoritmo de hash adaptativo que:
 * </p>
 * <ul>
 * <li>Incorpora salt automaticamente</li>
 * <li>Oferece proteção contra ataques de força bruta</li>
 * <li>Pode ser configurado com diferentes "strength" para aumentar a
 * latência</li>
 * </ul>
 *
 * <p>
 * <b>Ciclo de vida:</b>
 * </p>
 * <p>
 * Os beans definidos aqui são registrados no contexto do Spring durante
 * a inicialização da aplicação e podem ser injetados em qualquer componente
 * gerenciado pelo Spring através de {@code @Autowired} ou injeção por
 * constructor.
 * </p>
 *
 * @implNote
 *           Esta configuração é carregada automaticamente pelo Spring Boot
 *           graças à anotação {@code @Configuration}, tornando os beans
 *           disponíveis para injeção em toda a aplicação.
 *
 * @apiNote
 *          Os beans registrados aqui são fundamentais para operações de
 *          segurança,
 *          como autenticação de usuários e validação de senhas em endpoints
 *          protegidos.
 */
@Configuration
public class SecurityBeansConfig {

  /**
   * Cria e registra um bean {@link PasswordEncoder} baseado em bcrypt.
   *
   * <p>
   * Este bean é responsável por codificar senhas no cadastro de usuários
   * e validar senhas fornecidas durante a autenticação.
   * </p>
   *
   * <p>
   * <b>Características do BCryptPasswordEncoder:</b>
   * </p>
   * <ul>
   * <li>Força padrão: 10 (pode ser customizado conforme necessidade)</li>
   * <li>Cada chamada a {@code encode()} produz um hash diferente (salt
   * aleatório)</li>
   * <li>Método {@code matches()} valida se a senha corresponde ao hash
   * armazenado</li>
   * </ul>
   *
   * <p>
   * <b>Exemplo de uso:</b>
   * </p>
   *
   * <pre>
   * // Codificar uma senha no cadastro
   * String encodedPassword = passwordEncoder.encode(rawPassword);
   * userEntity.setPassword(encodedPassword);
   *
   * // Validar uma senha na autenticação
   * boolean isValid = passwordEncoder.matches(rawPassword, encodedPassword);
   * </pre>
   *
   * @return um {@link BCryptPasswordEncoder} pronto para ser utilizado
   *         em operações de codificação e validação de senhas
   *
   * @implNote
   *           O bean é criado sempre que necessário, mas é geralmente
   *           mantido como singleton pelo Spring, reduzindo overhead.
   *
   * @apiNote
   *          Este bean é essencial para implementar autenticação segura
   *          em qualquer aplicação Spring Security.
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
