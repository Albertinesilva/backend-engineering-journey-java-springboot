package com.albertsilva.dev.dscatalog.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.albertsilva.dev.dscatalog.domain.user.Role;
import com.albertsilva.dev.dscatalog.domain.user.User;
import com.albertsilva.dev.dscatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.dscatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.dscatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.dscatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.dscatalog.factory.UserFactory;
import com.albertsilva.dev.dscatalog.mapper.user.UserMapper;
import com.albertsilva.dev.dscatalog.repository.RoleRepository;
import com.albertsilva.dev.dscatalog.repository.UserRepository;
import com.albertsilva.dev.dscatalog.service.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

@DisplayName("Tests for UserService")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @InjectMocks
  private UserService service;

  @Mock
  private UserRepository repository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  private Long existingId;
  private Long nonExistingId;
  private Pageable pageable;
  private PageImpl<User> page;

  @BeforeEach
  void setUp() {
    existingId = 1L;
    nonExistingId = 1000L;
    pageable = PageRequest.of(0, 10);
    page = new PageImpl<>(List.of(UserFactory.createUser()));
  }

  @Nested
  @DisplayName("Search Operations")
  class SearchOperations {

    @Test
    @DisplayName("search should return paged users when firstName is empty")
    void searchShouldReturnPagedUsersWhenFirstNameIsEmpty() {

      // Arrange
      when(repository.findAll(pageable)).thenReturn(page);
      when(userMapper.toResponsePage(page)).thenReturn(
          new PageImpl<>(List.of(UserFactory.createUserResponse()), PageRequest.of(0, 10), 1));

      // Act
      Page<UserResponse> result = service.search("", pageable);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertFalse(result.isEmpty());
      verify(repository, times(1)).findAll(pageable);
    }

    @Test
    @DisplayName("search should return paged users filtered by firstName")
    void searchShouldReturnPagedUsersFilteredByFirstName() {

      // Arrange
      when(repository.findByFirstNameContainingIgnoreCase("João", pageable)).thenReturn(page);
      when(userMapper.toResponsePage(page)).thenReturn(
          new PageImpl<>(List.of(UserFactory.createUserResponse()), PageRequest.of(0, 10), 1));

      // Act
      Page<UserResponse> result = service.search("João", pageable);

      // Assert
      Assertions.assertNotNull(result);
      Assertions.assertFalse(result.isEmpty());
      verify(repository, times(1)).findByFirstNameContainingIgnoreCase("João", pageable);
    }
  }

  @Nested
  @DisplayName("FindById Operations")
  class FindByIdOperations {

    @Test
    @DisplayName("findById should return user when id exists")
    void findByIdShouldReturnUserWhenIdExists() {

      // Arrange
      User user = UserFactory.createUser();
      user.setId(existingId);

      when(repository.findById(existingId)).thenReturn(Optional.of(user));
      when(userMapper.toDetailsResponse(user)).thenReturn(UserFactory.createUserDetailsResponse());

      // Act
      UserDetailsResponse result = service.findById(existingId);

      // Assert
      Assertions.assertNotNull(result);
      verify(repository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
    void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act & Assert
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.findById(nonExistingId));

      Assertions.assertEquals("error.user.notFound", exception.getMessage());
      verify(repository, times(1)).findById(nonExistingId);
    }
  }

  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("create should persist user when UserCreateRequest is valid")
    void createShouldPersistUserWhenUserCreateRequestIsValid() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();
      User user = UserFactory.createUser();
      user.setId(existingId);

      Set<Role> roles = new HashSet<>();
      roles.add(new Role(1L, "ROLE_OPERATOR"));

      when(roleRepository.findAllById(request.roleIds())).thenReturn(new ArrayList<>(roles));
      when(userMapper.toEntity(request, roles)).thenReturn(user);
      when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");
      when(repository.save(user)).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(UserFactory.createUserResponse());

      // Act
      UserResponse result = service.create(request);

      // Assert
      Assertions.assertNotNull(result);
      verify(roleRepository, times(1)).findAllById(request.roleIds());
      verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("create should throw ResourceNotFoundException when roles not found")
    void createShouldThrowResourceNotFoundExceptionWhenRolesNotFound() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      when(roleRepository.findAllById(request.roleIds())).thenReturn(new ArrayList<>());

      // Act & Assert
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.create(request));

      Assertions.assertNotNull(exception);
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
      User user = UserFactory.createUser();
      user.setId(existingId);

      Set<Role> roles = new HashSet<>();
      roles.add(new Role(1L, "ROLE_OPERATOR"));

      when(repository.getReferenceById(existingId)).thenReturn(user);
      when(roleRepository.findAllById(request.roleIds())).thenReturn(new ArrayList<>(roles));
      when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");
      when(repository.save(user)).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(UserFactory.createUpdatedUserResponse());

      // Act
      UserResponse result = service.update(existingId, request);

      // Assert
      Assertions.assertNotNull(result);
      verify(repository, times(1)).getReferenceById(existingId);
      verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();

      when(repository.getReferenceById(nonExistingId)).thenThrow(EntityNotFoundException.class);

      // Act & Assert
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.update(nonExistingId, request));

      Assertions.assertEquals("error.user.notFound", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Delete Operations")
  class DeleteOperations {

    @Test
    @DisplayName("delete should remove user when id exists")
    void deleteShouldRemoveUserWhenIdExists() {

      // Arrange
      User user = UserFactory.createUser();
      user.setId(existingId);

      when(repository.findById(existingId)).thenReturn(Optional.of(user));

      // Act
      assertDoesNotThrow(() -> service.delete(existingId));

      // Assert
      verify(repository, times(1)).findById(existingId);
      verify(repository, times(1)).delete(user);
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act & Assert
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.delete(nonExistingId));

      Assertions.assertEquals("error.user.notFound", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Activate Operations")
  class ActivateOperations {

    @Test
    @DisplayName("activate should set active to true when user exists")
    void activateShouldSetActiveToTrueWhenUserExists() {

      // Arrange
      User user = UserFactory.createUser();
      user.setId(existingId);
      user.deactivate();

      when(repository.findById(existingId)).thenReturn(Optional.of(user));

      // Act
      assertDoesNotThrow(() -> service.activate(existingId));

      // Assert
      Assertions.assertTrue(user.isActive());
      verify(repository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("activate should throw ResourceNotFoundException when id does not exist")
    void activateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act & Assert
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.activate(nonExistingId));

      Assertions.assertEquals("error.user.notFound", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("deactivate should set active to false when user exists")
    void deactivateShouldSetActiveToFalseWhenUserExists() {

      // Arrange
      User user = UserFactory.createUser();
      user.setId(existingId);
      user.activate();

      when(repository.findById(existingId)).thenReturn(Optional.of(user));

      // Act
      assertDoesNotThrow(() -> service.deactivate(existingId));

      // Assert
      Assertions.assertFalse(user.isActive());
      verify(repository, times(1)).findById(existingId);
    }

    @Test
    @DisplayName("deactivate should throw ResourceNotFoundException when id does not exist")
    void deactivateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(nonExistingId)).thenReturn(Optional.empty());

      // Act & Assert
      ResourceNotFoundException exception = Assertions.assertThrows(ResourceNotFoundException.class,
          () -> service.deactivate(nonExistingId));

      Assertions.assertEquals("error.user.notFound", exception.getMessage());
    }
  }

}
