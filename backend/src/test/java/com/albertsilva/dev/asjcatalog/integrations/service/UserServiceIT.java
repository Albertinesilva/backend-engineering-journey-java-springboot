package com.albertsilva.dev.asjcatalog.integrations.service;

import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ADMIN_USER_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.COUNT_TOTAL_USERS;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.OPERATOR_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;
import com.albertsilva.dev.asjcatalog.service.UserService;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

/**
 * Testes de integração do {@link UserService} com o banco H2 do perfil
 * {@code test}.
 *
 * <p>
 * Dependem do seed ({@code import.sql}): os dois usuários Albert
 * ({@code OPERATOR_USER_ID}) e Maria ({@code ADMIN_USER_ID}), o total
 * {@code COUNT_TOTAL_USERS} e os primeiros nomes esperados na ordenação (#21).
 * </p>
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("Integration tests for UserService")
class UserServiceIT {

  private static final String NOT_FOUND_KEY = "error.user.notFound";

  @Autowired
  private UserService service;

  @Autowired
  private UserRepository repository;

  private User saveUser(String firstName, String email) {
    return repository.saveAndFlush(new User(null, firstName, "Souza", email, "encoded-password", true));
  }

  @Nested
  @DisplayName("Search Operations")
  class SearchOperations {

    @Test
    @DisplayName("search should return first page of all users when firstName is empty")
    void searchShouldReturnFirstPageOfAllUsersWhenFirstNameIsEmpty() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(0, 10);

      // Act
      Page<UserResponse> result = service.search("", pageRequest);

      // Assert
      assertThat(result.getNumber()).isZero();
      assertThat(result.getSize()).isEqualTo(10);
      assertThat(result.getContent()).hasSize(COUNT_TOTAL_USERS.intValue());
      assertThat(result.getTotalElements()).isEqualTo(COUNT_TOTAL_USERS);
    }

    @Test
    @DisplayName("search should return empty page with the real total when page does not exist")
    void searchShouldReturnEmptyPageWithTheRealTotalWhenPageDoesNotExist() {

      // Arrange
      PageRequest pageRequest = PageRequest.of(50, 10);

      // Act
      Page<UserResponse> result = service.search("", pageRequest);

      // Assert
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getNumber()).isEqualTo(50);
      assertThat(result.getSize()).isEqualTo(10);
      assertThat(result.getTotalElements()).isEqualTo(COUNT_TOTAL_USERS);
    }

    @Test
    @DisplayName("search should return ordered page when sorting by firstName")
    void searchShouldReturnOrderedPageWhenSortingByFirstName() {

      // Arrange
      // "Aaron" recebe o maior id; sem ordenação por firstName ele viria por último.
      saveUser("Aaron", "aaron@gmail.com");
      PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

      // Act
      Page<UserResponse> result = service.search("", pageRequest);

      // Assert
      assertThat(result.getContent())
          .extracting(UserResponse::firstName)
          .containsExactly("Aaron", "Albert", "Maria");
    }

    @Test
    @DisplayName("search should return only matching users when firstName has text")
    void searchShouldReturnOnlyMatchingUsersWhenFirstNameHasText() {

      // Arrange
      User created = saveUser("Mariane", "mariane@gmail.com");
      saveUser("Pedro", "pedro.souza@gmail.com");
      PageRequest pageRequest = PageRequest.of(0, 10);

      // Act
      Page<UserResponse> result = service.search("maria", pageRequest);

      // Assert
      assertThat(result.getContent())
          .extracting(UserResponse::firstName)
          .contains(created.getFirstName())
          .allSatisfy(firstName -> assertThat(firstName).containsIgnoringCase("maria"));
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return user details when id exists")
    void findByIdShouldReturnUserDetailsWhenIdExists() {

      // Arrange
      User expected = repository.findById(EXISTING_ID).orElseThrow();

      // Act
      UserDetailsResponse result = service.findById(EXISTING_ID);

      // Assert
      assertThat(result)
          .extracting(UserDetailsResponse::id, UserDetailsResponse::firstName, UserDetailsResponse::email)
          .containsExactly(EXISTING_ID, expected.getFirstName(), expected.getEmail());
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
    void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThatThrownBy(() -> service.findById(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }

  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("create should persist user when request is valid")
    void createShouldPersistUserWhenRequestIsValid() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      // Act
      UserResponse result = service.create(request);

      // Assert
      assertThat(result.id()).isNotNull();
      assertThat(result)
          .extracting(UserResponse::firstName, UserResponse::lastName, UserResponse::email)
          .containsExactly(request.firstName(), request.lastName(), request.email());
      assertThat(repository.count()).isEqualTo(COUNT_TOTAL_USERS + 1);
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should persist new data when id exists")
    void updateShouldPersistNewDataWhenIdExists() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      // Act
      UserResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertThat(result.id()).isEqualTo(EXISTING_ID);
      assertThat(result.firstName()).isEqualTo(request.firstName());
      assertThat(repository.findById(EXISTING_ID).orElseThrow())
          .extracting(User::getFirstName, User::getLastName, User::getEmail)
          .containsExactly(request.firstName(), request.lastName(), request.email());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      // Act + Assert
      assertThatThrownBy(() -> service.update(NON_EXISTING_ID, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }

  @Nested
  @DisplayName("Activate Operations")
  class ActivateOperations {

    @Test
    @DisplayName("activate should set active to true when user is inactive")
    void activateShouldSetActiveToTrueWhenUserIsInactive() {

      // Arrange
      User user = repository.findById(OPERATOR_USER_ID).orElseThrow();
      user.deactivate();
      repository.saveAndFlush(user);

      // Act
      service.activate(OPERATOR_USER_ID);

      // Assert
      assertThat(repository.findById(OPERATOR_USER_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("activate should keep user active when user is already active")
    void activateShouldKeepUserActiveWhenUserIsAlreadyActive() {

      // Arrange
      assertThat(repository.findById(OPERATOR_USER_ID).orElseThrow().isActive()).isTrue();

      // Act
      service.activate(OPERATOR_USER_ID);

      // Assert
      assertThat(repository.findById(OPERATOR_USER_ID).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("activate should throw ResourceNotFoundException when id does not exist")
    void activateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThatThrownBy(() -> service.activate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("deactivate should set active to false when user is active")
    void deactivateShouldSetActiveToFalseWhenUserIsActive() {

      // Arrange
      assertThat(repository.findById(ADMIN_USER_ID).orElseThrow().isActive()).isTrue();

      // Act
      service.deactivate(ADMIN_USER_ID);

      // Assert
      assertThat(repository.findById(ADMIN_USER_ID).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("deactivate should throw ResourceNotFoundException when id does not exist")
    void deactivateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThatThrownBy(() -> service.deactivate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove user when id exists")
    void deleteShouldRemoveUserWhenIdExists() {

      // Act
      service.delete(ADMIN_USER_ID);

      // Assert
      assertThat(repository.count()).isEqualTo(COUNT_TOTAL_USERS - 1);
      assertThat(repository.existsById(ADMIN_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThatThrownBy(() -> service.delete(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage(NOT_FOUND_KEY);
    }
  }
}
