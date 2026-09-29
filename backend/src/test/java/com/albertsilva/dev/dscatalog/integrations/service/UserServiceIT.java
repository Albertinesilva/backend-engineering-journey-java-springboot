package com.albertsilva.dev.dscatalog.integrations.service;

import static com.albertsilva.dev.dscatalog.factory.UserFactory.COUNT_TOTAL_USERS;
import static com.albertsilva.dev.dscatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.UserFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.dscatalog.factory.UserFactory.OPERATOR_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Assertions;
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

import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.service.UserService;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("UserService Integration Tests")
class UserServiceIT {

  @Autowired
  private UserService service;

  @Autowired
  private UserRepository repository;

  @Nested
  @DisplayName("Read Operations")
  class ReadOperations {

    @Nested
    @DisplayName("Search Operations")
    class SearchOperations {

      @Test
      @DisplayName("search should return paged users when firstName is empty")
      void searchShouldReturnPagedUsersWhenFirstNameIsEmpty() {

        // Arrange
        String firstName = "";
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Act
        Page<UserResponse> result = service.search(firstName, pageRequest);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(0, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(COUNT_TOTAL_USERS, result.getTotalElements());
      }

      @Test
      @DisplayName("search should return empty page when page does not exist")
      void searchShouldReturnEmptyPageWhenPageDoesNotExist() {

        // Arrange
        String firstName = "";
        PageRequest pageRequest = PageRequest.of(50, 10);

        // Act
        Page<UserResponse> result = service.search(firstName, pageRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(50, result.getNumber());
        assertEquals(10, result.getSize());
        assertEquals(COUNT_TOTAL_USERS, result.getTotalElements());
      }

      @Test
      @DisplayName("search should return ordered page when sorting by firstName")
      void searchShouldReturnOrderedPageWhenSortingByFirstName() {

        // Arrange
        // "Aaron" recebe o maior id; sem ordenação por firstName ele viria por último.
        repository.saveAndFlush(new User(null, "Aaron", "Souza", "aaron@gmail.com", "encoded-password", true));
        String firstName = "";
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("firstName"));

        // Act
        Page<UserResponse> result = service.search(firstName, pageRequest);

        // Assert
        List<String> firstNames = result.getContent().stream().map(UserResponse::firstName).toList();
        assertEquals(List.of("Aaron", "Albert", "Maria"), firstNames);
      }

      @Test
      @DisplayName("search should return filtered users when firstName exists")
      void searchShouldReturnFilteredUsersWhenFirstNameExists() {

        // Arrange
        String firstName = "maria";
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Act
        Page<UserResponse> result = service.search(firstName, pageRequest);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());

        result.getContent().forEach(user -> assertTrue(user.firstName().toLowerCase().contains(firstName)));
      }
    }

    @Nested
    @DisplayName("FindById Operations")
    class FindByIdOperations {

      @Test
      @DisplayName("findById should return user details when id exists")
      void findByIdShouldReturnUserDetailsWhenIdExists() {

        // Act
        UserDetailsResponse result = service.findById(EXISTING_ID);

        // Assert
        assertNotNull(result);
        assertEquals(EXISTING_ID, result.id());
      }

      @Test
      @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
      void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> service.findById(NON_EXISTING_ID));
      }
    }
  }

  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("create should persist user when valid data")
    void createShouldPersistUserWhenValidData() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      // Act
      UserResponse result = service.create(request);

      // Assert
      assertNotNull(result);
      assertNotNull(result.id());
      assertEquals(COUNT_TOTAL_USERS + 1, repository.count());
    }
  }

  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should update user when id exists")
    void updateShouldUpdateUserWhenIdExists() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      // Act
      UserResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertNotNull(result);
      assertEquals(EXISTING_ID, result.id());
      assertEquals(request.firstName(), result.firstName());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.update(NON_EXISTING_ID, request));
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
      assertTrue(repository.findById(OPERATOR_USER_ID).orElseThrow().isActive());
    }

    @Test
    @DisplayName("activate should keep an already active user active")
    void activateShouldKeepUserActiveWhenUserIsAlreadyActive() {

      // Arrange
      assertTrue(repository.findById(OPERATOR_USER_ID).orElseThrow().isActive());

      // Act
      service.activate(OPERATOR_USER_ID);

      // Assert
      assertTrue(repository.findById(OPERATOR_USER_ID).orElseThrow().isActive());
    }

    @Test
    @DisplayName("activate should throw ResourceNotFoundException when id does not exist")
    void activateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.activate(NON_EXISTING_ID));
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("deactivate should set active to false when user exists")
    void deactivateShouldSetActiveToFalseWhenUserExists() {

      // Arrange
      Long userId = 2L;

      // Act
      service.deactivate(userId);

      // Assert
      assertFalse(repository.findById(userId).get().isActive());
    }

    @Test
    @DisplayName("deactivate should throw ResourceNotFoundException when id does not exist")
    void deactivateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.deactivate(NON_EXISTING_ID));
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove user when id exists")
    void deleteShouldRemoveUserWhenIdExists() {

      // Arrange
      Long userId = 2L;

      // Act
      service.delete(userId);

      // Assert
      Assertions.assertEquals(COUNT_TOTAL_USERS - 1, repository.count());
      assertFalse(repository.existsById(userId));
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Act + Assert
      assertThrows(ResourceNotFoundException.class, () -> service.delete(NON_EXISTING_ID));
    }
  }

}
