package com.albertsilva.dev.dscatalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.albertsilva.dev.dscatalog.domain.recovery.Email;
import com.albertsilva.dev.dscatalog.domain.recovery.enums.EmailStatus;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.repository.EmailRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;

@DisplayName("Tests for EmailService")
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  private static final String FRONTEND_URL = "https://frontend.example.com";
  private static final String HTML_BODY = "<html><body>email body</body></html>";

  @InjectMocks
  private EmailService service;

  @Mock
  private JavaMailSender emailSender;

  @Mock
  private SpringTemplateEngine templateEngine;

  @Mock
  private EmailRepository emailRepository;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "frontendUrl", FRONTEND_URL);
  }

  @Nested
  @DisplayName("activation emails")
  class ActivationEmailTests {

    @Test
    @DisplayName("sendActivationEmail should render, send and register the activation email")
    void sendActivationEmailShouldRenderSendAndRegisterEmail() throws Exception {
      MimeMessage message = mimeMessage();
      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      service.sendActivationEmail("Maria", "maria@example.com", "activation-token");

      assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString())
          .isEqualTo("maria@example.com");
      assertThat(message.getSubject()).isEqualTo("Confirmação de Cadastro");
      assertThat(message.getFrom()[0].toString()).isEqualTo("nao-responder@asjcatalog.com.br");
      verifyTemplate("activate_user_by_email_template", "Maria", "Bem-vindo ao ASJ Catalog, Maria!",
          "https://frontend.example.com/activate-account?token=activation-token", null);
      verifyEmailLog("maria@example.com", "Confirmação de Cadastro");
      verify(emailSender).send(message);
    }

    @Test
    @DisplayName("sendActivationEmailAsync should complete successfully")
    void sendActivationEmailAsyncShouldCompleteSuccessfully() throws Exception {
      MimeMessage message = mimeMessage();
      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      CompletableFuture<Void> result = service.sendActivationEmailAsync("Maria", "maria@example.com",
          "activation-token");

      assertThat(result).isCompletedWithValue(null);
      verify(emailSender).send(message);
    }

    @Test
    @DisplayName("sendActivationEmailAsync should complete exceptionally when sending fails")
    void sendActivationEmailAsyncShouldCompleteExceptionallyWhenSendingFails() throws Exception {
      when(emailSender.createMimeMessage()).thenThrow(new IllegalStateException("SMTP unavailable"));

      CompletableFuture<Void> result = service.sendActivationEmailAsync("Maria", "maria@example.com",
          "activation-token");

      assertThat(result).isCompletedExceptionally();
      assertThatThrownBy(result::join).hasCauseInstanceOf(IllegalStateException.class);
      verify(emailRepository, never()).save(any(Email.class));
    }
  }

  @Nested
  @DisplayName("password recovery emails")
  class PasswordRecoveryEmailTests {

    @Test
    @DisplayName("sendPasswordRecoveryEmail should render, send and register the recovery email")
    void sendPasswordRecoveryEmailShouldRenderSendAndRegisterEmail() throws Exception {
      User user = UserFactory.createUser();
      user.setEmail("maria@example.com");
      user.setFirstName("Maria");
      MimeMessage message = mimeMessage();
      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      service.sendPasswordRecoveryEmail(user, "recovery-token");

      assertThat(message.getRecipients(jakarta.mail.Message.RecipientType.TO)[0].toString())
          .isEqualTo("maria@example.com");
      assertThat(message.getSubject()).isEqualTo("Redefinição de Senha");
      assertThat(message.getFrom()[0].toString()).isEqualTo("nao-responder@asjcatalog.com.br");
      verifyTemplate("reset_password_email_template", "Maria", "Redefinição de Senha",
          "https://frontend.example.com/reset-password?token=recovery-token", "recovery-token");
      verifyEmailLog("maria@example.com", "Redefinição de Senha");
      verify(emailSender).send(message);
    }

    @Test
    @DisplayName("sendPasswordRecoveryEmailAsync should complete successfully")
    void sendPasswordRecoveryEmailAsyncShouldCompleteSuccessfully() throws Exception {
      User user = UserFactory.createUser();
      user.setEmail("maria@example.com");
      MimeMessage message = mimeMessage();
      when(emailSender.createMimeMessage()).thenReturn(message);
      when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_BODY);

      CompletableFuture<Void> result = service.sendPasswordRecoveryEmailAsync(user, "recovery-token");

      assertThat(result).isCompletedWithValue(null);
      verify(emailSender).send(message);
    }

    @Test
    @DisplayName("sendPasswordRecoveryEmailAsync should complete exceptionally when rendering fails")
    void sendPasswordRecoveryEmailAsyncShouldCompleteExceptionallyWhenRenderingFails() {
      User user = UserFactory.createUser();
      when(emailSender.createMimeMessage()).thenReturn(mimeMessage());
      when(templateEngine.process(anyString(), any(Context.class)))
          .thenThrow(new IllegalStateException("Template unavailable"));

      CompletableFuture<Void> result = service.sendPasswordRecoveryEmailAsync(user, "recovery-token");

      assertThat(result).isCompletedExceptionally();
      assertThatThrownBy(result::join).hasCauseInstanceOf(IllegalStateException.class);
      verify(emailSender, never()).send(any(MimeMessage.class));
      verify(emailRepository, never()).save(any(Email.class));
    }
  }

  private MimeMessage mimeMessage() {
    return new MimeMessage(Session.getInstance(new Properties()));
  }

  private void verifyTemplate(String templateName, String name, String title, String link, String token) {
    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(templateEngine).process(org.mockito.Mockito.eq(templateName), contextCaptor.capture());

    Context context = contextCaptor.getValue();
    assertThat(context.getVariable("nome")).isEqualTo(name);
    assertThat(context.getVariable("titulo")).isEqualTo(title);
    if (token == null) {
      assertThat(context.getVariable("texto")).isEqualTo(
          "Estamos felizes em tê-lo(a) conosco. Para começar a usar o ASJ Catalog, confirme seu cadastro clicando no link abaixo.");
      assertThat(context.getVariable("linkConfirmacao")).isEqualTo(link);
    } else {
      assertThat(context.getVariable("token")).isEqualTo(token);
      assertThat(context.getVariable("linkRedefinicaoSenha")).isEqualTo(link);
    }
  }

  private void verifyEmailLog(String recipient, String content) {
    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(emailRepository).save(emailCaptor.capture());

    Email email = emailCaptor.getValue();
    assertThat(email.getSender()).isEqualTo("asjcatalog@gmail.com");
    assertThat(email.getRecipient()).isEqualTo(recipient);
    assertThat(email.getContent()).isEqualTo(content);
    assertThat(email.getStatus()).isEqualTo(EmailStatus.PENDING);
    assertThat(email.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
  }
}
