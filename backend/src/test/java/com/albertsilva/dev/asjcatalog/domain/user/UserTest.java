package com.albertsilva.dev.asjcatalog.domain.user;

import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.OPERATOR_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.ENCODED_PASSWORD;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_EMAIL;
import static com.albertsilva.dev.asjcatalog.factory.UserFactory.EXISTING_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;

/**
 * Testes de {@code User}. O {@code equals} e o {@code hashCode} ficam no
 * {@code EntityIdentityContractTest}.
 */
@DisplayName("Tests for User")
class UserTest {

  private static final String FIRST_NAME = "Maria";
  private static final String LAST_NAME = "Silva";

  private static User userWithOperatorRole() {
    User user = UserFactory.createUser();
    user.addRole(RoleFactory.createOperatorRole());
    return user;
  }

  @Nested
  @DisplayName("Constructor Operations")
  class ConstructorOperations {

    @Test
    @DisplayName("constructor should set all fields when values are given")
    void constructorShouldSetAllFieldsWhenValuesAreGiven() {

      // Act
      User user = new User(EXISTING_ID, FIRST_NAME, LAST_NAME, EXISTING_EMAIL, ENCODED_PASSWORD, false);

      // Assert
      assertThat(user.getId()).isEqualTo(EXISTING_ID);
      assertThat(user.getFirstName()).isEqualTo(FIRST_NAME);
      assertThat(user.getLastName()).isEqualTo(LAST_NAME);
      assertThat(user.getEmail()).isEqualTo(EXISTING_EMAIL);
      assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
      assertThat(user.isActive()).isFalse();
      assertThat(user.getRoles()).isEmpty();
    }
  }

  @Nested
  @DisplayName("GetUsername Operations")
  class GetUsernameOperations {

    @Test
    @DisplayName("getUsername should return email")
    void getUsernameShouldReturnEmail() {

      // Arrange
      User user = UserFactory.createUser();

      // Act
      String result = user.getUsername();

      // Assert
      assertThat(result).isEqualTo(user.getEmail());
    }
  }

  @Nested
  @DisplayName("GetAuthorities Operations")
  class GetAuthoritiesOperations {

    @Test
    @DisplayName("getAuthorities should return exactly the user roles")
    void getAuthoritiesShouldReturnExactlyTheUserRoles() {

      // Arrange
      User user = userWithOperatorRole();
      user.addRole(RoleFactory.createAdminRole());

      // Act
      var result = user.getAuthorities();

      // Assert
      assertThat(result).extracting(GrantedAuthority::getAuthority)
          .containsExactlyInAnyOrder(OPERATOR_AUTHORITY, ADMIN_AUTHORITY);
    }
  }

  @Nested
  @DisplayName("HasRole Operations")
  class HasRoleOperations {

    @Test
    @DisplayName("hasRole should return true when user has the role")
    void hasRoleShouldReturnTrueWhenUserHasTheRole() {

      // Arrange
      User user = userWithOperatorRole();

      // Act
      boolean result = user.hasRole(OPERATOR_AUTHORITY);

      // Assert
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasRole should return false when user does not have the role")
    void hasRoleShouldReturnFalseWhenUserDoesNotHaveTheRole() {

      // Arrange
      User user = userWithOperatorRole();

      // Act
      boolean result = user.hasRole(ADMIN_AUTHORITY);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasRole should return false when role name is null")
    void hasRoleShouldReturnFalseWhenRoleNameIsNull() {

      // Arrange
      User user = userWithOperatorRole();

      // Act
      boolean result = user.hasRole(null);

      // Assert
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Activate and Deactivate Operations")
  class ActivateAndDeactivateOperations {

    @Test
    @DisplayName("activate should make user enabled")
    void activateShouldMakeUserEnabled() {

      // Arrange
      User user = UserFactory.createUser();
      user.deactivate();

      // Act
      user.activate();

      // Assert
      assertThat(user.isActive()).isTrue();
      assertThat(user.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("deactivate should make user disabled")
    void deactivateShouldMakeUserDisabled() {

      // Arrange
      User user = UserFactory.createUser();

      // Act
      user.deactivate();

      // Assert
      assertThat(user.isActive()).isFalse();
      assertThat(user.isEnabled()).isFalse();
    }
  }

  @Nested
  @DisplayName("Account Status Operations")
  class AccountStatusOperations {

    // Contrato do UserDetails registrado no SECURITY-CONTRACT (seção 7.1): o modelo
    // não tem bloqueio nem expiração de conta ou de senha.
    @Test
    @DisplayName("account status flags should always be true")
    void accountStatusFlagsShouldAlwaysBeTrue() {

      // Arrange
      User user = UserFactory.createUser();

      // Act & Assert
      assertThat(user.isAccountNonExpired()).isTrue();
      assertThat(user.isAccountNonLocked()).isTrue();
      assertThat(user.isCredentialsNonExpired()).isTrue();
    }
  }
}
