package com.albertsilva.dev.asjcatalog.mapper.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.albertsilva.dev.asjcatalog.domain.user.Role;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.dto.role.response.RoleResponse;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserCreateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserRegisterRequest;
import com.albertsilva.dev.asjcatalog.dto.user.request.UserUpdateRequest;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserDetailsResponse;
import com.albertsilva.dev.asjcatalog.dto.user.response.UserResponse;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;

@DisplayName("Tests for UserMapper")
class UserMapperTest {

  private final UserMapper mapper = new UserMapper();

  private Role operatorRole;
  private Role adminRole;

  @BeforeEach
  void setUp() {
    operatorRole = RoleFactory.createOperatorRole();
    adminRole = RoleFactory.createAdminRole();
  }

  /** Usuário com id e as roles OPERATOR e ADMIN. */
  private User userWithRoles() {
    User user = UserFactory.createUser();
    user.setId(UserFactory.EXISTING_ID);
    user.addRole(operatorRole);
    user.addRole(adminRole);
    return user;
  }

  private static RoleResponse roleResponse(Role role) {
    return new RoleResponse(role.getId(), role.getAuthority());
  }

  /**
   * Testes de {@code UserMapper.toEntity(UserCreateRequest, Set)}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>E-mail com espaços ou maiúsculas: é copiado sem normalização (#11).</li>
   * </ul>
   */
  @Nested
  @DisplayName("ToEntity (UserCreateRequest) Operations")
  class ToEntityFromCreateRequestOperations {

    @Test
    @DisplayName("toEntity should copy fields and roles when create request is given")
    void toEntityShouldCopyFieldsAndRolesWhenCreateRequestIsGiven() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      // Act
      User result = mapper.toEntity(request, Set.of(operatorRole));

      // Assert
      assertThat(result.getId()).isNull();
      assertThat(result.getFirstName()).isEqualTo(request.firstName());
      assertThat(result.getLastName()).isEqualTo(request.lastName());
      assertThat(result.getEmail()).isEqualTo(request.email());
      assertThat(result.getPassword()).isEqualTo(request.password());
      assertThat(result.getRoles()).containsExactly(operatorRole);
    }

    // A regra sobre usuário sem roles é da #14 e fica no service; o teste só confere
    // que o mapper não falha com roles nulas.
    @Test
    @DisplayName("toEntity should create user with empty roles when roles are null")
    void toEntityShouldCreateUserWithEmptyRolesWhenRolesAreNull() {

      // Arrange
      UserCreateRequest request = UserFactory.createUserCreateRequest();

      // Act
      User result = mapper.toEntity(request, null);

      // Assert
      assertThat(result.getEmail()).isEqualTo(request.email());
      assertThat(result.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("toEntity should return null when create request is null")
    void toEntityShouldReturnNullWhenCreateRequestIsNull() {

      // Act
      User result = mapper.toEntity((UserCreateRequest) null, Set.of(operatorRole));

      // Assert
      assertThat(result).isNull();
    }
  }

  /**
   * Testes de {@code UserMapper.toEntity(UserRegisterRequest, Set)}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>E-mail com espaços ou maiúsculas: é copiado sem normalização (#11).</li>
   * </ul>
   */
  @Nested
  @DisplayName("ToEntity (UserRegisterRequest) Operations")
  class ToEntityFromRegisterRequestOperations {

    @Test
    @DisplayName("toEntity should copy fields and roles when register request is given")
    void toEntityShouldCopyFieldsAndRolesWhenRegisterRequestIsGiven() {

      // Arrange
      UserRegisterRequest request = UserFactory.createUserRegisterRequest();

      // Act
      User result = mapper.toEntity(request, Set.of(operatorRole));

      // Assert
      assertThat(result.getId()).isNull();
      assertThat(result.getFirstName()).isEqualTo(request.firstName());
      assertThat(result.getLastName()).isEqualTo(request.lastName());
      assertThat(result.getEmail()).isEqualTo(request.email());
      assertThat(result.getPassword()).isEqualTo(request.password());
      assertThat(result.getRoles()).containsExactly(operatorRole);
    }

    // A regra sobre usuário sem roles é da #14 e fica no service; o teste só confere
    // que o mapper não falha com roles nulas.
    @Test
    @DisplayName("toEntity should create user with empty roles when roles are null")
    void toEntityShouldCreateUserWithEmptyRolesWhenRolesAreNull() {

      // Arrange
      UserRegisterRequest request = UserFactory.createUserRegisterRequest();

      // Act
      User result = mapper.toEntity(request, null);

      // Assert
      assertThat(result.getEmail()).isEqualTo(request.email());
      assertThat(result.getRoles()).isEmpty();
    }

    @Test
    @DisplayName("toEntity should return null when register request is null")
    void toEntityShouldReturnNullWhenRegisterRequestIsNull() {

      // Act
      User result = mapper.toEntity((UserRegisterRequest) null, Set.of(operatorRole));

      // Assert
      assertThat(result).isNull();
    }
  }

  /**
   * Testes de {@code UserMapper.updateEntity}.
   *
   * <p>
   * <b>Cenários pendentes:</b>
   * </p>
   * <ul>
   * <li>E-mail com espaços ou maiúsculas: é copiado sem normalização (#11).</li>
   * </ul>
   */
  @Nested
  @DisplayName("UpdateEntity Operations")
  class UpdateEntityOperations {

    @Test
    @DisplayName("updateEntity should replace name and email when request is given")
    void updateEntityShouldReplaceNameAndEmailWhenRequestIsGiven() {

      // Arrange
      UserUpdateRequest request = UserFactory.createUserUpdateRequest();
      User user = userWithRoles();
      String originalPassword = user.getPassword();

      // Act
      mapper.updateEntity(request, user);

      // Assert
      assertThat(user.getId()).isEqualTo(UserFactory.EXISTING_ID);
      assertThat(user.getFirstName()).isEqualTo(request.firstName());
      assertThat(user.getLastName()).isEqualTo(request.lastName());
      assertThat(user.getEmail()).isEqualTo(request.email());
      assertThat(user.getPassword()).isEqualTo(originalPassword);
      assertThat(user.getRoles()).containsExactlyInAnyOrder(operatorRole, adminRole);
    }
  }

  @Nested
  @DisplayName("ToResponse Operations")
  class ToResponseOperations {

    @Test
    @DisplayName("toResponse should map fields and roles when entity is given")
    void toResponseShouldMapFieldsAndRolesWhenEntityIsGiven() {

      // Arrange
      User user = userWithRoles();

      // Act
      UserResponse result = mapper.toResponse(user);

      // Assert
      assertThat(result.id()).isEqualTo(user.getId());
      assertThat(result.firstName()).isEqualTo(user.getFirstName());
      assertThat(result.lastName()).isEqualTo(user.getLastName());
      assertThat(result.email()).isEqualTo(user.getEmail());
      assertThat(result.roles()).containsExactlyInAnyOrder(roleResponse(operatorRole), roleResponse(adminRole));
    }

    @Test
    @DisplayName("toResponse should return null when entity is null")
    void toResponseShouldReturnNullWhenEntityIsNull() {

      // Act
      UserResponse result = mapper.toResponse(null);

      // Assert
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("ToDetailsResponse Operations")
  class ToDetailsResponseOperations {

    @Test
    @DisplayName("toDetailsResponse should map fields, roles and status when entity is given")
    void toDetailsResponseShouldMapFieldsRolesAndStatusWhenEntityIsGiven() {

      // Arrange
      User user = userWithRoles();
      user.deactivate();

      // Act
      UserDetailsResponse result = mapper.toDetailsResponse(user);

      // Assert
      assertThat(result.id()).isEqualTo(user.getId());
      assertThat(result.firstName()).isEqualTo(user.getFirstName());
      assertThat(result.lastName()).isEqualTo(user.getLastName());
      assertThat(result.email()).isEqualTo(user.getEmail());
      assertThat(result.roles()).containsExactlyInAnyOrder(roleResponse(operatorRole), roleResponse(adminRole));
      assertThat(result.active()).isFalse();
    }

    @Test
    @DisplayName("toDetailsResponse should return null when entity is null")
    void toDetailsResponseShouldReturnNullWhenEntityIsNull() {

      // Act
      UserDetailsResponse result = mapper.toDetailsResponse(null);

      // Assert
      assertThat(result).isNull();
    }
  }

  @Nested
  @DisplayName("ToResponsePage Operations")
  class ToResponsePageOperations {

    @Test
    @DisplayName("toResponsePage should map every user and keep pagination when page is given")
    void toResponsePageShouldMapEveryUserAndKeepPaginationWhenPageIsGiven() {

      // Arrange
      User user = userWithRoles();
      PageRequest pageable = PageRequest.of(0, 5);
      Page<User> page = new PageImpl<>(List.of(user), pageable, 1);

      // Act
      Page<UserResponse> result = mapper.toResponsePage(page);

      // Assert
      assertThat(result.getContent()).extracting(UserResponse::id).containsExactly(user.getId());
      assertThat(result.getPageable()).isEqualTo(pageable);
      assertThat(result.getTotalElements()).isEqualTo(1);
    }
  }
}
