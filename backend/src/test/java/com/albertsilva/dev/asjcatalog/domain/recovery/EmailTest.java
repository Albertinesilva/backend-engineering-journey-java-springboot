package com.albertsilva.dev.asjcatalog.domain.recovery;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.albertsilva.dev.asjcatalog.dto.email.request.EmailRegisterRequest;

/**
 * Testes de {@code Email}. O {@code equals} e o {@code hashCode} ficam no
 * {@code EntityIdentityContractTest}.
 */
@DisplayName("Tests for Email")
class EmailTest {

  private static final String SENDER = "nao-responder@asjcatalog.com.br";
  private static final String CONTENT = "Confirmação de Cadastro";

  /**
   * Testes do construtor {@code Email(EmailRegisterRequest)}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code status}: o registro nasce sempre {@code PENDING} e nenhum código o
   * atualiza para {@code SENT} ou {@code ERROR} (#26).</li>
   * </ul>
   */
  @Nested
  @DisplayName("Constructor Operations")
  class ConstructorOperations {

    @Test
    @DisplayName("constructor should copy request data and set creation date when request is given")
    void constructorShouldCopyRequestDataAndSetCreationDateWhenRequestIsGiven() {

      // Arrange
      EmailRegisterRequest request = new EmailRegisterRequest(SENDER, EXISTING_EMAIL, CONTENT);
      Instant before = Instant.now();

      // Act
      Email email = new Email(request);
      Instant after = Instant.now();

      // Assert
      assertThat(email.getId()).isNull();
      assertThat(email.getSender()).isEqualTo(SENDER);
      assertThat(email.getRecipient()).isEqualTo(EXISTING_EMAIL);
      assertThat(email.getContent()).isEqualTo(CONTENT);
      assertThat(email.getCreatedAt()).isBetween(before, after);
    }
  }
}
