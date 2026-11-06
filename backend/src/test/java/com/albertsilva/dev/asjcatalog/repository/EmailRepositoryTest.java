package com.albertsilva.dev.asjcatalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.albertsilva.dev.asjcatalog.domain.recovery.Email;
import com.albertsilva.dev.asjcatalog.domain.recovery.enums.EmailStatus;
import com.albertsilva.dev.asjcatalog.dto.email.request.EmailRegisterRequest;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests for EmailRepository")
class EmailRepositoryTest {

  @Autowired
  private EmailRepository emailRepository;

  /**
   * Testes de {@code EmailRepository.save}.
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
  @DisplayName("Save Operations")
  class SaveOperations {

    @Test
    @DisplayName("save should generate id and creation date when email is new")
    void saveShouldGenerateIdAndCreationDateWhenEmailIsNew() {
      // Arrange
      long countBefore = emailRepository.count();
      Email email = createEmail();

      // Act
      Email savedEmail = emailRepository.saveAndFlush(email);

      // Assert
      assertThat(savedEmail.getId()).isNotNull().isPositive();
      assertThat(savedEmail.getCreatedAt()).isNotNull();
      assertThat(emailRepository.count()).isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("save should update fields when email already exists")
    void saveShouldUpdateFieldsWhenEmailAlreadyExists() {
      // Arrange
      Email email = emailRepository.saveAndFlush(createEmail());
      email.setStatus(EmailStatus.SENT);
      email.setContent("Updated content");

      // Act
      Email updatedEmail = emailRepository.saveAndFlush(email);

      // Assert
      assertThat(updatedEmail.getStatus()).isEqualTo(EmailStatus.SENT);
      assertThat(updatedEmail.getContent()).isEqualTo("Updated content");
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return email when id exists")
    void findByIdShouldReturnEmailWhenIdExists() {
      // Arrange
      Email savedEmail = emailRepository.saveAndFlush(createEmail());

      // Act
      Optional<Email> result = emailRepository.findById(savedEmail.getId());

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().getRecipient()).isEqualTo("recipient@example.com");
      assertThat(result.get().getSender()).isEqualTo("sender@example.com");
    }

    @Test
    @DisplayName("findById should return empty when id does not exist")
    void findByIdShouldReturnEmptyWhenIdDoesNotExist() {
      // Act
      Optional<Email> result = emailRepository.findById(999999L);

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("FindAll Operations")
  class FindAllOperations {

    @Test
    @DisplayName("findAll should return every persisted email")
    void findAllShouldReturnEveryPersistedEmail() {
      // Arrange
      emailRepository.saveAndFlush(createEmail("first@example.com", "First content"));
      emailRepository.saveAndFlush(createEmail("second@example.com", "Second content"));

      // Act
      List<Email> result = emailRepository.findAll();

      // Assert
      assertThat(result).extracting(Email::getRecipient)
          .contains("first@example.com", "second@example.com");
    }

    @Test
    @DisplayName("findAll should return empty list when no email is persisted")
    void findAllShouldReturnEmptyListWhenNoEmailIsPersisted() {
      // Act
      List<Email> result = emailRepository.findAll();

      // Assert
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("deleteById should remove email when id exists")
    void deleteByIdShouldRemoveEmailWhenIdExists() {
      // Arrange
      Email savedEmail = emailRepository.saveAndFlush(createEmail());
      Long emailId = savedEmail.getId();

      // Act
      emailRepository.deleteById(emailId);
      emailRepository.flush();

      // Assert
      assertThat(emailRepository.existsById(emailId)).isFalse();
    }

  }

  private Email createEmail() {
    return createEmail("recipient@example.com", "Email content");
  }

  private Email createEmail(String recipient, String content) {
    return new Email(new EmailRegisterRequest("sender@example.com", recipient, content));
  }
}
