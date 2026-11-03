package com.albertsilva.dev.asjcatalog.service;

import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.OPERATOR_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ENCODED_PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.FIRST_NAME_FILTER;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.NON_EXISTING_ID;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.albertsilva.dev.asjcatalog.domain.user.Role;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.UserDetailsFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;
import com.albertsilva.dev.asjcatalog.mapper.user.UserMapper;
import com.albertsilva.dev.asjcatalog.repository.RoleRepository;
import com.albertsilva.dev.asjcatalog.repository.UserRepository;
import com.albertsilva.dev.asjcatalog.service.exception.ResourceNotFoundException;

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

  private Pageable pageable;
  private Page<User> page;
  private Page<UserResponse> responsePage;

  @BeforeEach
  void setUp() {
    pageable = PageRequest.of(0, 10);
    page = new PageImpl<>(List.of(UserFactory.createUser()));
    responsePage = new PageImpl<>(List.of(UserFactory.createUserResponse()), pageable, 1);
  }

  @Nested
  @DisplayName("Search Operations")
  class SearchOperations {

    @Test
    @DisplayName("search should return all users when firstName is empty")
    void searchShouldReturnAllUsersWhenFirstNameIsEmpty() {

      // Arrange
      when(repository.findAll(pageable)).thenReturn(page);
      when(userMapper.toResponsePage(page)).thenReturn(responsePage);

      // Act
      Page<UserResponse> result = service.search("", pageable);

      // Assert
      assertThat(result).isEqualTo(responsePage);
      verify(repository, times(1)).findAll(pageable);
      verify(repository, never()).findByFirstNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("search should return all users when firstName is null")
    void searchShouldReturnAllUsersWhenFirstNameIsNull() {

      // Arrange
      when(repository.findAll(pageable)).thenReturn(page);
      when(userMapper.toResponsePage(page)).thenReturn(responsePage);

      // Act
      Page<UserResponse> result = service.search(null, pageable);

      // Assert
      assertThat(result).isEqualTo(responsePage);
      verify(repository, times(1)).findAll(pageable);
      verify(repository, never()).findByFirstNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("search should return all users when firstName is blank")
    void searchShouldReturnAllUsersWhenFirstNameIsBlank() {

      // Arrange
      when(repository.findAll(pageable)).thenReturn(page);
      when(userMapper.toResponsePage(page)).thenReturn(responsePage);

      // Act
      Page<UserResponse> result = service.search("   ", pageable);

      // Assert
      assertThat(result).isEqualTo(responsePage);
      verify(repository, times(1)).findAll(pageable);
      verify(repository, never()).findByFirstNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("search should return users filtered by firstName when firstName has text")
    void searchShouldReturnUsersFilteredByFirstNameWhenFirstNameHasText() {

      // Arrange
      when(repository.findByFirstNameContainingIgnoreCase(FIRST_NAME_FILTER, pageable)).thenReturn(page);
      when(userMapper.toResponsePage(page)).thenReturn(responsePage);

      // Act
      Page<UserResponse> result = service.search(FIRST_NAME_FILTER, pageable);

      // Assert
      assertThat(result).isEqualTo(responsePage);
      verify(repository, times(1)).findByFirstNameContainingIgnoreCase(FIRST_NAME_FILTER, pageable);
      verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("search should trim firstName when firstName has surrounding spaces")
    void searchShouldTrimFirstNameWhenFirstNameHasSurroundingSpaces() {

      // Arrange
      when(repository.findByFirstNameContainingIgnoreCase(FIRST_NAME_FILTER, pageable)).thenReturn(page);
      when(userMapper.toResponsePage(page)).thenReturn(responsePage);

      // Act
      Page<UserResponse> result = service.search("  " + FIRST_NAME_FILTER + "  ", pageable);

      // Assert
      assertThat(result).isEqualTo(responsePage);
      verify(repository, times(1)).findByFirstNameContainingIgnoreCase(FIRST_NAME_FILTER, pageable);
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
      user.setId(EXISTING_ID);
      UserDetailsResponse expectedResponse = UserFactory.createUserDetailsResponse();

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(user));
      when(userMapper.toDetailsResponse(user)).thenReturn(expectedResponse);

      // Act
      UserDetailsResponse result = service.findById(EXISTING_ID);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      verify(repository, times(1)).findById(EXISTING_ID);
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when id does not exist")
    void findByIdShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.findById(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.user.notFound");

      verify(repository, times(1)).findById(NON_EXISTING_ID);
      verify(userMapper, never()).toDetailsResponse(any());
    }
  }

  /**
   * Testes de {@code UserService.create}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code roleIds} nulo ou vazio: o usuário é criado sem roles e não
   * consegue fazer login; o comportamento correto (exigir uma role ou usar uma
   * role padrão) ainda não foi decidido (#14).</li>
   * </ul>
   */
  @Nested
  @DisplayName("Create Operations")
  class CreateOperations {

    @Test
    @DisplayName("create should encode password, activate and save user when roles exist")
    void createShouldEncodePasswordActivateAndSaveUserWhenRolesExist() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();
      User user = UserFactory.createUser();
      user.deactivate();
      Set<Role> roles = Set.of(RoleFactory.createOperatorRole());
      UserResponse expectedResponse = UserFactory.createUserResponse();

      when(roleRepository.findAllById(request.roleIds())).thenReturn(List.copyOf(roles));
      when(userMapper.toEntity(request, roles)).thenReturn(user);
      when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
      when(repository.save(user)).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(expectedResponse);

      // Act
      UserResponse result = service.create(request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
      assertThat(user.isActive()).isTrue();
      verify(roleRepository, times(1)).findAllById(request.roleIds());
      verify(passwordEncoder, times(1)).encode(PASSWORD);
      verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("create should throw ResourceNotFoundException when any role does not exist")
    void createShouldThrowResourceNotFoundExceptionWhenAnyRoleDoesNotExist() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      when(roleRepository.findAllById(request.roleIds())).thenReturn(List.of());

      // Act & Assert
      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.role.ids.notFound");

      verify(userMapper, never()).toEntity(any(UserCreateRequest.class), any());
      verify(repository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("LoadUserByUsername Operations")
  class LoadUserByUsernameOperations {

    @Test
    @DisplayName("loadUserByUsername should return user with credentials and role when email exists")
    void loadUserByUsernameShouldReturnUserWithCredentialsAndRoleWhenEmailExists() {

      // Arrange
      when(repository.searchUserAndRolesByEmail(EXISTING_EMAIL))
          .thenReturn(UserDetailsFactory.createOperatorProjections(EXISTING_ID, EXISTING_EMAIL, true));

      // Act
      UserDetails result = service.loadUserByUsername(EXISTING_EMAIL);

      // Assert
      assertThat(((User) result).getId()).isEqualTo(EXISTING_ID);
      assertThat(result.getUsername()).isEqualTo(EXISTING_EMAIL);
      assertThat(result.getPassword()).isEqualTo(ENCODED_PASSWORD);
      assertThat(result.isEnabled()).isTrue();
      assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder(OPERATOR_AUTHORITY);
      verify(repository, times(1)).searchUserAndRolesByEmail(EXISTING_EMAIL);
    }

    @Test
    @DisplayName("loadUserByUsername should return one authority per row when user has many roles")
    void loadUserByUsernameShouldReturnOneAuthorityPerRowWhenUserHasManyRoles() {

      // Arrange
      when(repository.searchUserAndRolesByEmail(EXISTING_EMAIL))
          .thenReturn(UserDetailsFactory.createOperatorAndAdminProjections(EXISTING_ID, EXISTING_EMAIL));

      // Act
      UserDetails result = service.loadUserByUsername(EXISTING_EMAIL);

      // Assert
      assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder(OPERATOR_AUTHORITY, ADMIN_AUTHORITY);
    }

    @Test
    @DisplayName("loadUserByUsername should return disabled user when user is inactive")
    void loadUserByUsernameShouldReturnDisabledUserWhenUserIsInactive() {

      // Arrange
      when(repository.searchUserAndRolesByEmail(EXISTING_EMAIL))
          .thenReturn(UserDetailsFactory.createOperatorProjections(EXISTING_ID, EXISTING_EMAIL, false));

      // Act
      UserDetails result = service.loadUserByUsername(EXISTING_EMAIL);

      // Assert
      assertThat(result.getUsername()).isEqualTo(EXISTING_EMAIL);
      assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername should throw UsernameNotFoundException when email does not exist")
    void loadUserByUsernameShouldThrowUsernameNotFoundExceptionWhenEmailDoesNotExist() {

      // Arrange
      when(repository.searchUserAndRolesByEmail(NON_EXISTING_EMAIL)).thenReturn(List.of());

      // Act & Assert
      assertThatThrownBy(() -> service.loadUserByUsername(NON_EXISTING_EMAIL))
          .isInstanceOf(UsernameNotFoundException.class)
          .hasMessage("User not found with email: " + NON_EXISTING_EMAIL);
    }
  }

  /**
   * Testes de {@code UserService.update}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>{@code roleIds} vazio: todas as roles são removidas e o usuário perde
   * o acesso; a regra ainda não foi decidida (#14).</li>
   * </ul>
   */
  @Nested
  @DisplayName("Update Operations")
  class UpdateOperations {

    @Test
    @DisplayName("update should replace roles and encode password when roleIds and password are present")
    void updateShouldReplaceRolesAndEncodePasswordWhenRoleIdsAndPasswordArePresent() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();
      User user = UserFactory.createUser();
      user.setId(EXISTING_ID);
      user.addRole(RoleFactory.createAdminRole());
      Role operatorRole = RoleFactory.createOperatorRole();
      UserResponse expectedResponse = UserFactory.createUpdatedUserResponse();

      when(repository.getReferenceById(EXISTING_ID)).thenReturn(user);
      when(roleRepository.findAllById(request.roleIds())).thenReturn(List.of(operatorRole));
      when(passwordEncoder.encode(PASSWORD)).thenReturn(ENCODED_PASSWORD);
      when(repository.save(user)).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(expectedResponse);

      // Act
      UserResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(user.getRoles()).containsExactly(operatorRole);
      assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
      verify(userMapper, times(1)).updateEntity(request, user);
      verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("update should keep roles and password when roleIds and password are null")
    void updateShouldKeepRolesAndPasswordWhenRoleIdsAndPasswordAreNull() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequestWithoutRolesAndPassword();
      User user = UserFactory.createUser();
      user.setId(EXISTING_ID);
      Role adminRole = RoleFactory.createAdminRole();
      user.addRole(adminRole);
      UserResponse expectedResponse = UserFactory.createUpdatedUserResponse();

      when(repository.getReferenceById(EXISTING_ID)).thenReturn(user);
      when(repository.save(user)).thenReturn(user);
      when(userMapper.toResponse(user)).thenReturn(expectedResponse);

      // Act
      UserResponse result = service.update(EXISTING_ID, request);

      // Assert
      assertThat(result).isEqualTo(expectedResponse);
      assertThat(user.getRoles()).containsExactly(adminRole);
      assertThat(user.getPassword()).isEqualTo(PASSWORD);
      verify(roleRepository, never()).findAllById(any());
      verify(passwordEncoder, never()).encode(any());
      verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when any role does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenAnyRoleDoesNotExist() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();
      User user = UserFactory.createUser();
      user.setId(EXISTING_ID);

      when(repository.getReferenceById(EXISTING_ID)).thenReturn(user);
      when(roleRepository.findAllById(request.roleIds())).thenReturn(List.of());

      // Act & Assert
      assertThatThrownBy(() -> service.update(EXISTING_ID, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.role.ids.notFound");

      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw ResourceNotFoundException when id does not exist")
    void updateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();
      User reference = new User();

      // getReferenceById não consulta o banco: o proxy só lança
      // EntityNotFoundException quando é acessado, aqui pelo mapper.
      when(repository.getReferenceById(NON_EXISTING_ID)).thenReturn(reference);
      doThrow(EntityNotFoundException.class).when(userMapper).updateEntity(request, reference);

      // Act & Assert
      assertThatThrownBy(() -> service.update(NON_EXISTING_ID, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.user.notFound");

      verify(repository, never()).save(any());
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
      user.setId(EXISTING_ID);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(user));

      // Act
      service.delete(EXISTING_ID);

      // Assert
      verify(repository, times(1)).findById(EXISTING_ID);
      verify(repository, times(1)).delete(user);
    }

    @Test
    @DisplayName("delete should throw ResourceNotFoundException when id does not exist")
    void deleteShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.delete(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.user.notFound");

      verify(repository, never()).delete(any());
    }
  }

  @Nested
  @DisplayName("Activate Operations")
  class ActivateOperations {

    @Test
    @DisplayName("activate should set active to true when user is inactive")
    void activateShouldSetActiveToTrueWhenUserIsInactive() {

      // Arrange
      User user = UserFactory.createUser();
      user.setId(EXISTING_ID);
      user.deactivate();

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(user));

      // Act
      service.activate(EXISTING_ID);

      // Assert
      assertThat(user.isActive()).isTrue();
      verify(repository, times(1)).findById(EXISTING_ID);
    }

    @Test
    @DisplayName("activate should not change user when user is already active")
    void activateShouldNotChangeUserWhenUserIsAlreadyActive() {

      // Arrange
      // O spy é necessário porque o estado final (ativo) é o mesmo nos dois
      // caminhos; só o verify prova que o método saiu cedo.
      User user = spy(UserFactory.createUser());
      user.setId(EXISTING_ID);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(user));

      // Act
      service.activate(EXISTING_ID);

      // Assert
      assertThat(user.isActive()).isTrue();
      verify(user, never()).activate();
    }

    @Test
    @DisplayName("activate should throw ResourceNotFoundException when id does not exist")
    void activateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.activate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.user.notFound");

      verify(repository, times(1)).findById(NON_EXISTING_ID);
    }
  }

  @Nested
  @DisplayName("Deactivate Operations")
  class DeactivateOperations {

    @Test
    @DisplayName("deactivate should set active to false when user is active")
    void deactivateShouldSetActiveToFalseWhenUserIsActive() {

      // Arrange
      User user = UserFactory.createUser();
      user.setId(EXISTING_ID);
      user.activate();

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(user));

      // Act
      service.deactivate(EXISTING_ID);

      // Assert
      assertThat(user.isActive()).isFalse();
      verify(repository, times(1)).findById(EXISTING_ID);
    }

    @Test
    @DisplayName("deactivate should not change user when user is already inactive")
    void deactivateShouldNotChangeUserWhenUserIsAlreadyInactive() {

      // Arrange
      // O spy é necessário porque o estado final (inativo) é o mesmo nos dois
      // caminhos; só o verify prova que o método saiu cedo.
      User user = spy(UserFactory.createUser());
      user.setId(EXISTING_ID);
      user.setActive(false);

      when(repository.findById(EXISTING_ID)).thenReturn(Optional.of(user));

      // Act
      service.deactivate(EXISTING_ID);

      // Assert
      assertThat(user.isActive()).isFalse();
      verify(user, never()).deactivate();
    }

    @Test
    @DisplayName("deactivate should throw ResourceNotFoundException when id does not exist")
    void deactivateShouldThrowResourceNotFoundExceptionWhenIdDoesNotExist() {

      // Arrange
      when(repository.findById(NON_EXISTING_ID)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> service.deactivate(NON_EXISTING_ID))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("error.user.notFound");

      verify(repository, times(1)).findById(NON_EXISTING_ID);
    }
  }

}
