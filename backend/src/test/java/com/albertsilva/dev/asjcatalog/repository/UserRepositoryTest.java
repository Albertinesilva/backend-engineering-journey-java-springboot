package com.albertsilva.dev.asjcatalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import com.albertsilva.dev.asjcatalog.domain.user.Role;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.projection.UserDetailsProjection;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests for UserRepository")
class UserRepositoryTest {

  private static final String READER_AUTHORITY = "ROLE_QWZX_READER";
  private static final String WRITER_AUTHORITY = "ROLE_QWZX_WRITER";

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  @Nested
  @DisplayName("Save Operations")
  class SaveOperations {

    @Test
    @DisplayName("save should generate id when user is new")
    void saveShouldGenerateIdWhenUserIsNew() {

      // Arrange
      long countBefore = userRepository.count();

      User user = UserFactory.createUser();
      user.setId(null);

      // Act
      User savedUser = userRepository.save(user);

      // Assert
      assertThat(savedUser.getId()).as("User id should be auto-generated").isNotNull().isPositive();

      assertThat(userRepository.count()).as("Repository count should increase by one").isEqualTo(countBefore + 1);
    }

    @Test
    @DisplayName("save should update user when user already exists")
    void saveShouldUpdateUserWhenUserAlreadyExists() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());
      String updatedFirstName = "Updated Name";

      // Act
      user.setFirstName(updatedFirstName);
      User updatedUser = userRepository.save(user);

      // Assert
      assertThat(updatedUser.getFirstName()).as("User first name should be updated").isEqualTo(updatedFirstName);
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return user when id exists")
    void findByIdShouldReturnUserWhenIdExists() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());

      // Act
      Optional<User> result = userRepository.findById(user.getId());

      // Assert
      assertThat(result).as("User should be found by id").isPresent();
      assertThat(result.get().getId()).as("User id should match").isEqualTo(user.getId());
    }

    @Test
    @DisplayName("findById should return empty when id does not exist")
    void findByIdShouldReturnEmptyWhenIdDoesNotExist() {

      // Arrange
      // No user with this id

      // Act
      Optional<User> result = userRepository.findById(9999L);

      // Assert
      assertThat(result).as("User should not be found").isEmpty();
    }
  }

  /**
   * Testes de {@code UserRepository.findByEmail}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>E-mail com maiúsculas diferentes das gravadas: a busca diferencia
   * maiúsculas e não encontra o usuário (#11).</li>
   * </ul>
   */
  @Nested
  @DisplayName("FindByEmail Operations")
  class FindByEmailOperations {

    @Test
    @DisplayName("findByEmail should return user when email exists")
    void findByEmailShouldReturnUserWhenEmailExists() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());

      // Act
      Optional<User> result = userRepository.findByEmail(user.getEmail());

      // Assert
      assertThat(result).as("User should be found by email").isPresent();
      assertThat(result.get().getEmail()).as("User email should match").isEqualTo(user.getEmail());
    }

    @Test
    @DisplayName("findByEmail should return empty when email does not exist")
    void findByEmailShouldReturnEmptyWhenEmailDoesNotExist() {

      // Arrange
      // No user with this email

      // Act
      Optional<User> result = userRepository.findByEmail(UserFactory.NON_EXISTING_EMAIL);

      // Assert
      assertThat(result).as("User should not be found").isEmpty();
    }
  }

  @Nested
  @DisplayName("FindByFirstNameContainingIgnoreCase Operations")
  class FindByFirstNameOperations {

    @Test
    @DisplayName("findByFirstNameContainingIgnoreCase should return users when first name contains term")
    void findByFirstNameContainingIgnoreCaseShouldReturnUsersWhenFirstNameContainsTerm() {

      // Arrange
      User user1 = UserFactory.createUser();
      user1.setFirstName("Eleanor");
      user1.setEmail("eleanor@gmail.com");
      userRepository.saveAndFlush(user1);

      User user2 = UserFactory.createUser();
      user2.setFirstName("Elenice");
      user2.setEmail("elenice@gmail.com");
      userRepository.saveAndFlush(user2);

      Pageable pageable = PageRequest.of(0, 10);

      // Act
      Page<User> result = userRepository.findByFirstNameContainingIgnoreCase("ele", pageable);

      // Assert
      assertThat(result.getContent()).extracting(User::getFirstName)
          .as("Should find both users whose first name contains the term")
          .containsExactlyInAnyOrder("Eleanor", "Elenice");
      assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("findByFirstNameContainingIgnoreCase should return empty page when no user matches")
    void findByFirstNameContainingIgnoreCaseShouldReturnEmptyPageWhenNoUserMatches() {

      // Arrange
      Pageable pageable = PageRequest.of(0, 10);

      // Act
      Page<User> result = userRepository.findByFirstNameContainingIgnoreCase("xyzabc", pageable);

      // Assert
      assertThat(result).as("Result should be empty").isEmpty();
    }

    @Test
    @DisplayName("findByFirstNameContainingIgnoreCase should find user when term differs in case")
    void findByFirstNameContainingIgnoreCaseShouldFindUserWhenTermDiffersInCase() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());

      Pageable pageable = PageRequest.of(0, 10);

      // Act
      Page<User> resultLowerCase = userRepository.findByFirstNameContainingIgnoreCase(UserFactory.FIRST_NAME_FILTER.toLowerCase(), pageable);
      Page<User> resultUpperCase = userRepository.findByFirstNameContainingIgnoreCase(UserFactory.FIRST_NAME_FILTER.toUpperCase(), pageable);

      // Assert
      assertThat(resultLowerCase.getContent()).extracting(User::getId).as("Should find user with lowercase search")
          .containsExactly(user.getId());
      assertThat(resultUpperCase.getContent()).extracting(User::getId).as("Should find user with uppercase search")
          .containsExactly(user.getId());
    }
  }

  @Nested
  @DisplayName("ExistsByEmailIgnoreCase Operations")
  class ExistsByEmailOperations {

    @Test
    @DisplayName("existsByEmailIgnoreCase should return true when email exists in another case")
    void existsByEmailIgnoreCaseShouldReturnTrueWhenEmailExistsInAnotherCase() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());

      // Act
      boolean result = userRepository.existsByEmailIgnoreCase(user.getEmail().toUpperCase());

      // Assert
      assertThat(result).as("Email should exist").isTrue();
    }

    @Test
    @DisplayName("existsByEmailIgnoreCase should return false when email does not exist")
    void existsByEmailIgnoreCaseShouldReturnFalseWhenEmailDoesNotExist() {

      // Arrange
      // No user with this email

      // Act
      boolean result = userRepository.existsByEmailIgnoreCase("nonexistent@gmail.com");

      // Assert
      assertThat(result).as("Email should not exist").isFalse();
    }
  }

  @Nested
  @DisplayName("ExistsByEmailIgnoreCaseAndIdNot Operations")
  class ExistsByEmailIgnoreCaseAndIdNotOperations {

    @Test
    @DisplayName("existsByEmailIgnoreCaseAndIdNot should return false when email belongs to the same user")
    void existsByEmailIgnoreCaseAndIdNotShouldReturnFalseWhenEmailBelongsToTheSameUser() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());

      // Act
      boolean result = userRepository.existsByEmailIgnoreCaseAndIdNot(user.getEmail(), user.getId());

      // Assert
      assertThat(result).as("Should return false for same user").isFalse();
    }

    @Test
    @DisplayName("existsByEmailIgnoreCaseAndIdNot should return true when email belongs to another user")
    void existsByEmailIgnoreCaseAndIdNotShouldReturnTrueWhenEmailBelongsToAnotherUser() {

      // Arrange
      User user1 = userRepository.saveAndFlush(UserFactory.createUser());

      User user2 = UserFactory.createUser();
      user2.setEmail("another@gmail.com");
      userRepository.saveAndFlush(user2);

      // Act
      boolean result = userRepository.existsByEmailIgnoreCaseAndIdNot(user2.getEmail(), user1.getId());

      // Assert
      assertThat(result).as("Should return true for different user").isTrue();
    }

    @Test
    @DisplayName("existsByEmailIgnoreCaseAndIdNot should return false when email does not exist")
    void existsByEmailIgnoreCaseAndIdNotShouldReturnFalseWhenEmailDoesNotExist() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());

      // Act
      boolean result = userRepository.existsByEmailIgnoreCaseAndIdNot("nonexistent@gmail.com", user.getId());

      // Assert
      assertThat(result).as("Should return false for non-existent email").isFalse();
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("deleteById should remove user when id exists")
    void deleteByIdShouldRemoveUserWhenIdExists() {

      // Arrange
      User user = userRepository.saveAndFlush(UserFactory.createUser());
      Long userId = user.getId();

      // Act
      userRepository.deleteById(userId);

      // Assert
      assertThat(userRepository.findById(userId)).as("User should be deleted").isEmpty();
    }
  }

  /**
   * Testes de {@code UserRepository.searchUserAndRolesByEmail}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>E-mail com maiúsculas diferentes das gravadas: a consulta compara com
   * {@code =} e não encontra o usuário (#11).</li>
   * <li>Usuário sem roles: o {@code INNER JOIN} não devolve nenhuma linha, e o
   * usuário não consegue fazer login (#14).</li>
   * </ul>
   */
  @Nested
  @DisplayName("SearchUserAndRolesByEmail Operations")
  class SearchUserAndRolesByEmailOperations {

    @Test
    @DisplayName("searchUserAndRolesByEmail should return one row per role with credentials and status")
    void searchUserAndRolesByEmailShouldReturnOneRowPerRoleWithCredentialsAndStatus() {

      // Arrange
      Role reader = entityManager.persist(new Role(null, READER_AUTHORITY));
      Role writer = entityManager.persist(new Role(null, WRITER_AUTHORITY));
      User user = UserFactory.createUser();
      user.addRole(reader);
      user.addRole(writer);
      user = entityManager.persistFlushFind(user);

      // Act
      List<UserDetailsProjection> result = userRepository.searchUserAndRolesByEmail(user.getEmail());

      // Assert
      assertThat(result).hasSize(2);
      assertThat(result).extracting(UserDetailsProjection::getAuthority)
          .containsExactlyInAnyOrder(READER_AUTHORITY, WRITER_AUTHORITY);
      assertThat(result).extracting(UserDetailsProjection::getRoleId)
          .containsExactlyInAnyOrder(reader.getId(), writer.getId());
      for (UserDetailsProjection row : result) {
        assertThat(row.getId()).isEqualTo(user.getId());
        assertThat(row.getUsername()).isEqualTo(user.getEmail());
        assertThat(row.getPassword()).isEqualTo(user.getPassword());
        assertThat(row.getActive()).isTrue();
      }
    }
  }

}
