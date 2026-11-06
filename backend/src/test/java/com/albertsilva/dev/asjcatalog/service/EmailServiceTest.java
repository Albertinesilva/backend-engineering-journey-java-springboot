package com.albertsilva.dev.asjcatalog.service;

import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.ACTIVATION_TOKEN;
import static com.albertsilva.dev.asjcatalog.factory.TokenFactory.PASSWORD_RECOVERY_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.albertsilva.dev.asjcatalog.domain.recovery.Email;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.repository.EmailRepository;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@DisplayName("Tests for EmailService")
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  private static final String FRONTEND_URL = "https://frontend.example.com";
  private static final String HTML_BODY = "<html><body>email body</body></html>";
  private static final String FROM_ADDRESS = "nao-responder@asjcatalog.com.br";
  private static final String ACTIVATION_TEMPLATE = "activate_user_by_email_template";
  private static final String ACTIVATION_SUBJECT = "Confirmação de Cadastro";
  private static final String ACTIVATION_TEXT = "Estamos felizes em tê-lo(a) conosco. Para começar a usar o ASJ Catalog, "
      + "confirme seu cadastro clicando no link abaixo.";
  private static final String PASSWORD_RECOVERY_TEMPLATE = "reset_password_email_template";
  private static final String PASSWORD_RECOVERY_SUBJECT = "Redefinição de Senha";
  private static final String PASSWORD_RECOVERY_TEXT = "Recebemos uma solicitação para redefinir a senha da sua conta. "
      + "Para criar uma nova senha, clique no botão abaixo. Se você não fez essa solicitação, ignore este e-mail.";

  @InjectMocks
  private EmailService service;

  @Mock
  private JavaMailSender emailSender;

  @Mock
  private SpringTemplateEngine templateEngine;

  @Mock
  private EmailRepository emailRepository;

  private User user;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
    user = UserFactory.createUser();
  }

  private static MimeMessage mimeMessage() {
    return new MimeMessage(Session.getInstance(new Properties()));
  }

  /** Captura o contexto entregue ao Thymeleaf para o template informado. */
  private Context renderedContext(String templateName) {
    ArgumentCaptor<Context> captor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(eq(templateName), captor.capture());
    return captor.getValue();
  }

  /** Captura o registro de e-mail gravado no repositório. */
  private Email savedEmail() {
    ArgumentCaptor<Email> captor = ArgumentCaptor.forClass(Email.class);
    verify(emailRepository).save(captor.capture());
    return captor.getValue();
  }

  /**
   * Testes de {@code EmailService.sendActivationEmail}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code sender}, {@code content} e {@code status} do registro de e-mail:
   * hoje são o remetente {@code asjcatalog@gmail.com} (diferente do real), o
   * assunto e {@code PENDING}, que a issue diz estarem errados (#26).</li>
   * </ul>
   */
  @Nested
  @DisplayName("SendActivationEmail Operations")
  class SendActivationEmailOperations {

    @Test
    @DisplayName("sendActivationEmail should render, send and register the activation email")
    void sendActivationEmailShouldRenderSendAndRegisterEmail() throws MessagingException {

      // Arrange
      MimeMessage message = mimeMessage();

      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      // Act
      service.sendActivationEmail(user.getFirstName(), user.getEmail(), ACTIVATION_TOKEN);

      // Assert
      assertThat(message.getRecipients(Message.RecipientType.TO)[0]).hasToString(user.getEmail());
      assertThat(message.getSubject()).isEqualTo(ACTIVATION_SUBJECT);
      assertThat(message.getFrom()[0]).hasToString(FROM_ADDRESS);
      verify(emailSender).send(message);

      Context context = renderedContext(ACTIVATION_TEMPLATE);
      assertThat(context.getVariable("nome")).isEqualTo(user.getFirstName());
      assertThat(context.getVariable("titulo")).isEqualTo("Bem-vindo ao ASJ Catalog, " + user.getFirstName() + "!");
      assertThat(context.getVariable("texto")).isEqualTo(ACTIVATION_TEXT);
      assertThat(context.getVariable("linkConfirmacao"))
          .isEqualTo(FRONTEND_URL + "/activate-account?token=" + ACTIVATION_TOKEN);

      assertThat(savedEmail().getRecipient()).isEqualTo(user.getEmail());
    }
  }

  /**
   * Testes de {@code EmailService.sendActivationEmailAsync}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Registro de e-mail quando o envio falha: hoje nada é gravado, e a issue
   * pede um registro com status {@code ERROR} (#26).</li>
   * <li>Execução realmente assíncrona: o método tem {@code @Async}, mas o projeto
   * não tem {@code @EnableAsync}, então roda na thread de quem chama (#26).</li>
   * </ul>
   */
  @Nested
  @DisplayName("SendActivationEmailAsync Operations")
  class SendActivationEmailAsyncOperations {

    @Test
    @DisplayName("sendActivationEmailAsync should complete normally when email is sent")
    void sendActivationEmailAsyncShouldCompleteNormallyWhenEmailIsSent() {

      // Arrange
      MimeMessage message = mimeMessage();

      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      // Act
      CompletableFuture<Void> result = service.sendActivationEmailAsync(user.getFirstName(), user.getEmail(),
          ACTIVATION_TOKEN);

      // Assert
      assertThat(result).isCompletedWithValue(null);
      verify(emailSender).send(message);
    }

    @Test
    @DisplayName("sendActivationEmailAsync should complete exceptionally when sending fails")
    void sendActivationEmailAsyncShouldCompleteExceptionallyWhenSendingFails() {

      // Arrange
      IllegalStateException failure = new IllegalStateException("SMTP unavailable");

      when(emailSender.createMimeMessage()).thenThrow(failure);

      // Act
      CompletableFuture<Void> result = service.sendActivationEmailAsync(user.getFirstName(), user.getEmail(),
          ACTIVATION_TOKEN);

      // Assert
      assertThat(result).isCompletedExceptionally();
      assertThatThrownBy(result::join).hasCause(failure);
    }
  }

  /**
   * Testes de {@code EmailService.sendPasswordRecoveryEmail}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Link de redefinição: hoje usa o caminho {@code /reset-password}, e deve
   * passar a {@code /auth/reset-password} (#46).</li>
   * <li>{@code sender}, {@code content} e {@code status} do registro de e-mail:
   * hoje são o remetente {@code asjcatalog@gmail.com} (diferente do real), o
   * assunto e {@code PENDING}, que a issue diz estarem errados (#26).</li>
   * </ul>
   */
  @Nested
  @DisplayName("SendPasswordRecoveryEmail Operations")
  class SendPasswordRecoveryEmailOperations {

    @Test
    @DisplayName("sendPasswordRecoveryEmail should render, send and register the recovery email")
    void sendPasswordRecoveryEmailShouldRenderSendAndRegisterEmail() throws MessagingException {

      // Arrange
      MimeMessage message = mimeMessage();

      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      // Act
      service.sendPasswordRecoveryEmail(user, PASSWORD_RECOVERY_TOKEN);

      // Assert
      assertThat(message.getRecipients(Message.RecipientType.TO)[0]).hasToString(user.getEmail());
      assertThat(message.getSubject()).isEqualTo(PASSWORD_RECOVERY_SUBJECT);
      assertThat(message.getFrom()[0]).hasToString(FROM_ADDRESS);
      verify(emailSender).send(message);

      Context context = renderedContext(PASSWORD_RECOVERY_TEMPLATE);
      assertThat(context.getVariable("nome")).isEqualTo(user.getFirstName());
      assertThat(context.getVariable("token")).isEqualTo(PASSWORD_RECOVERY_TOKEN);
      assertThat(context.getVariable("titulo")).isEqualTo(PASSWORD_RECOVERY_SUBJECT);
      assertThat(context.getVariable("texto")).isEqualTo(PASSWORD_RECOVERY_TEXT);

      assertThat(savedEmail().getRecipient()).isEqualTo(user.getEmail());
    }
  }

  /**
   * Testes de {@code EmailService.sendPasswordRecoveryEmailAsync}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>Registro de e-mail quando o envio falha: hoje nada é gravado, e a issue
   * pede um registro com status {@code ERROR} (#26).</li>
   * <li>Execução realmente assíncrona: o método tem {@code @Async}, mas o projeto
   * não tem {@code @EnableAsync}, então roda na thread de quem chama (#26).</li>
   * </ul>
   */
  @Nested
  @DisplayName("SendPasswordRecoveryEmailAsync Operations")
  class SendPasswordRecoveryEmailAsyncOperations {

    @Test
    @DisplayName("sendPasswordRecoveryEmailAsync should complete normally when email is sent")
    void sendPasswordRecoveryEmailAsyncShouldCompleteNormallyWhenEmailIsSent() {

      // Arrange
      MimeMessage message = mimeMessage();

      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      // Act
      CompletableFuture<Void> result = service.sendPasswordRecoveryEmailAsync(user, PASSWORD_RECOVERY_TOKEN);

      // Assert
      assertThat(result).isCompletedWithValue(null);
      verify(emailSender).send(message);
    }

    @Test
    @DisplayName("sendPasswordRecoveryEmailAsync should complete exceptionally when rendering fails")
    void sendPasswordRecoveryEmailAsyncShouldCompleteExceptionallyWhenRenderingFails() {

      // Arrange
      IllegalStateException failure = new IllegalStateException("Template unavailable");

      when(emailSender.createMimeMessage()).thenReturn(mimeMessage());
      when(templateEngine.process(anyString(), any(Context.class))).thenThrow(failure);

      // Act
      CompletableFuture<Void> result = service.sendPasswordRecoveryEmailAsync(user, PASSWORD_RECOVERY_TOKEN);

      // Assert
      assertThat(result).isCompletedExceptionally();
      assertThatThrownBy(result::join).hasCause(failure);
      verify(emailSender, never()).send(any(MimeMessage.class));
    }
  }
}
