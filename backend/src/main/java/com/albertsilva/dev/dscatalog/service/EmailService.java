package com.albertsilva.dev.dscatalog.service;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.albertsilva.dev.dscatalog.domain.recovery.Email;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.dto.email.request.EmailRegisterRequest;
import com.albertsilva.dev.dscatalog.repository.EmailRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Serviço que monta e envia os e-mails transacionais do sistema (ativação de
 * conta e recuperação de senha) e grava um registro de cada envio em
 * {@code tb_email}. É chamado apenas por {@code AccountService}, por meio dos
 * métodos {@code ...Async}.
 *
 * <p>
 * <b>Dependências externas:</b> {@code JavaMailSender} (SMTP configurado por
 * {@code spring.mail.*}), {@code SpringTemplateEngine} (templates Thymeleaf
 * {@code activate_user_by_email_template} e
 * {@code reset_password_email_template}) e {@code EmailRepository}. Os links
 * enviados usam a propriedade {@code frontend.url}.
 * </p>
 *
 * <p>
 * <b>Métodos {@code ...Async}:</b> anotados com {@code @Async} e retornando
 * {@link CompletableFuture}. O projeto não possui {@code @EnableAsync} em
 * nenhuma classe; portanto, provavelmente a anotação não tem efeito e o envio
 * ocorre na thread e na transação de quem chama (o comportamento em execução
 * não foi verificado). Os wrappers capturam qualquer {@link Exception}, a
 * registram em log e a devolvem no futuro; como {@code AccountService} descarta
 * esse futuro, falhas de envio são apenas logadas.
 * </p>
 *
 * <p>
 * <b>Transações:</b> nenhum método declara {@code @Transactional}. O
 * {@code save} do registro usa a transação de quem chama, se existir; caso
 * contrário, o próprio repositório abre uma para a gravação.
 * </p>
 *
 * <p>
 * <b>Registro em {@code tb_email}:</b> só é gravado depois de o envio ter
 * sucesso. O status fica sempre {@code PENDING} (nenhum código o altera), o
 * remetente registrado é fixo ({@code asjcatalog@gmail.com}, diferente do
 * {@code From} da mensagem) e o conteúdo registrado é apenas um rótulo fixo
 * ("Confirmação de Cadastro" ou "Redefinição de Senha"), não o corpo HTML.
 * </p>
 */
@Service
public class EmailService {

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  @Value("${frontend.url}")
  private String frontendUrl;

  /** Valor de {@code backend.url}. Injetado, mas não utilizado por nenhum método desta classe. */
  @Value("${backend.url}")
  private String backendUrl;

  private final JavaMailSender emailSender;
  private final SpringTemplateEngine templateEngine;
  private final EmailRepository emailRepository;

  /**
   * Construtor para injeção de dependências.
   *
   * @param emailSender     o serviço de envio de e-mails
   * @param templateEngine  o mecanismo de template para geração de conteúdo de
   *                        e-mail
   * @param emailRepository o repositório para persistência de registros de e-mail
   */
  public EmailService(JavaMailSender emailSender, SpringTemplateEngine templateEngine,
      EmailRepository emailRepository) {
    this.emailSender = emailSender;
    this.templateEngine = templateEngine;
    this.emailRepository = emailRepository;
  }

  /**
   * Envia o e-mail de ativação e devolve o resultado em um
   * {@link CompletableFuture}.
   *
   * <p>
   * Chama {@link #sendActivationEmail(String, String, String)}. Qualquer
   * {@link Exception} (falha de template, de montagem da mensagem, de SMTP ou
   * de gravação do registro) é registrada em log e devolvida como futuro
   * falho; em caso de sucesso, devolve um futuro concluído. Anotado com
   * {@code @Async}, mas sem {@code @EnableAsync} no projeto a anotação
   * provavelmente não tem efeito (ver documentação da classe).
   * </p>
   *
   * @param name            nome do destinatário (usado na saudação)
   * @param email           endereço de e-mail do destinatário
   * @param activationToken valor do token de ativação, embutido no link
   * @return futuro concluído em caso de sucesso, ou falho com a exceção ocorrida
   */
  @Async
  public CompletableFuture<Void> sendActivationEmailAsync(String name, String email, String activationToken) {

    try {
      sendActivationEmail(name, email, activationToken);

      return CompletableFuture.completedFuture(null);

    } catch (Exception ex) {

      logger.error("Erro ao enviar email de ativação para {}", email, ex);

      return CompletableFuture.failedFuture(ex);
    }
  }

  /**
   * Envia o e-mail de recuperação de senha e devolve o resultado em um
   * {@link CompletableFuture}. Segue o mesmo contrato de
   * {@link #sendActivationEmailAsync(String, String, String)}: qualquer
   * {@link Exception} é registrada em log e devolvida como futuro falho.
   *
   * <p>
   * O {@code User} é usado apenas para ler e-mail e primeiro nome.
   * </p>
   *
   * @param user  usuário que solicitou a recuperação de senha
   * @param token valor do token de recuperação, embutido no link
   * @return futuro concluído em caso de sucesso, ou falho com a exceção ocorrida
   */
  @Async
  public CompletableFuture<Void> sendPasswordRecoveryEmailAsync(User user, String token) {

    try {
      sendPasswordRecoveryEmail(user, token);

      return CompletableFuture.completedFuture(null);

    } catch (Exception ex) {

      logger.error("Erro ao enviar email de recuperação de senha para {}", user.getEmail(), ex);

      return CompletableFuture.failedFuture(ex);
    }

  }

  /**
   * Monta e envia, de forma síncrona, o e-mail de ativação.
   *
   * <p>
   * Renderiza o template {@code activate_user_by_email_template} com o nome, o
   * título, o texto e o link {@code frontend.url + "/activate-account?token=" +
   * token}; monta a mensagem HTML (assunto "Confirmação de Cadastro", remetente
   * {@code nao-responder@asjcatalog.com.br}, logo inline); envia por
   * {@code JavaMailSender}; e, depois do envio, grava um registro em
   * {@code tb_email} (ver documentação da classe). Se algo falhar antes da
   * gravação, nenhum registro é criado.
   * </p>
   *
   * @param name            nome do destinatário
   * @param email           endereço de e-mail do destinatário
   * @param activationToken valor do token de ativação
   * @throws MessagingException se a criação ou o preenchimento da mensagem
   *                            falhar (falhas de envio do
   *                            {@code JavaMailSender} são exceções não
   *                            verificadas)
   */
  public void sendActivationEmail(String name, String email, String activationToken) throws MessagingException {
    MimeMessage message = emailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

    Context context = new Context();
    context.setVariable("nome", name);
    context.setVariable("titulo", "Bem-vindo ao ASJ Catalog, " + name + "!");
    context.setVariable("texto",
        "Estamos felizes em tê-lo(a) conosco. Para começar a usar o ASJ Catalog, confirme seu cadastro clicando no link abaixo.");
    context.setVariable("linkConfirmacao", frontendUrl + "/activate-account?token=" + activationToken);

    String htmlBody = templateEngine.process("activate_user_by_email_template", context);
    helper.setTo(email);
    helper.setText(htmlBody, true);
    helper.setSubject("Confirmação de Cadastro");
    helper.setFrom("nao-responder@asjcatalog.com.br");
    helper.addInline("logo", new ClassPathResource("/static/image/logo-ASJ-Catalog-favicon.ico"));

    emailSender.send(message);
    logger.info("Email de ativação enviado para {}", email);

    EmailRegisterRequest registerMail = new EmailRegisterRequest("asjcatalog@gmail.com", email,
        "Confirmação de Cadastro");
    registerEmailLog(registerMail);
  }

  /**
   * Monta e envia, de forma síncrona, o e-mail de recuperação de senha.
   *
   * <p>
   * Renderiza o template {@code reset_password_email_template} com o nome, o
   * token, o título e o link {@code frontend.url + "/reset-password?token=" +
   * token}; monta a mensagem HTML (assunto "Redefinição de Senha", mesmo
   * remetente e logo inline); envia; e grava o registro em {@code tb_email}. A
   * variável {@code texto}, referenciada pelo template, não é definida por este
   * método.
   * </p>
   *
   * @param user  usuário que solicitou a recuperação (e-mail e primeiro nome)
   * @param token valor do token de recuperação
   * @throws MessagingException se a criação ou o preenchimento da mensagem
   *                            falhar
   */
  public void sendPasswordRecoveryEmail(User user, String token) throws MessagingException {

    MimeMessage message = emailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

    helper.setTo(user.getEmail());
    helper.setSubject("Redefinição de Senha");

    Context context = new Context();
    context.setVariable("nome", user.getFirstName());
    context.setVariable("token", token);
    context.setVariable("titulo", "Redefinição de Senha");
    context.setVariable("linkRedefinicaoSenha", frontendUrl + "/reset-password?token=" + token);

    String htmlBody = templateEngine.process("reset_password_email_template", context);
    helper.setText(htmlBody, true);
    helper.setFrom("nao-responder@asjcatalog.com.br");
    helper.addInline("logo", new ClassPathResource("/static/image/logo-ASJ-Catalog-favicon.ico"));

    emailSender.send(message);

    EmailRegisterRequest registerMail = new EmailRegisterRequest("asjcatalog@gmail.com", user.getEmail(),
        "Redefinição de Senha");
    registerEmailLog(registerMail);
  }

  /**
   * Grava em {@code tb_email} o registro de um e-mail já enviado.
   *
   * <p>
   * Cria a entidade {@code Email} a partir do DTO (status inicial
   * {@code PENDING}, data de criação atual) e a salva. Não atualiza o status
   * depois.
   * </p>
   *
   * @param dataRegisterMail remetente, destinatário e conteúdo a registrar
   */
  private void registerEmailLog(EmailRegisterRequest dataRegisterMail) {
    Email email = new Email(dataRegisterMail);
    emailRepository.save(email);
  }

}
