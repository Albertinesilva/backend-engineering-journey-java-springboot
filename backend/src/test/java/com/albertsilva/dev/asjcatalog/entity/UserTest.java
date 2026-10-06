package com.albertsilva.dev.asjcatalog.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.albertsilva.dev.asjcatalog.factory.UserFactory;

@DisplayName("Tests for User Entity")
public class UserTest {

  @Test
  @DisplayName("User should instantiate correctly")
  void userShouldInstantiateCorrectly() {

    // Arrange
    User user = UserFactory.createUser();

    // Act
    // Apenas instanciação

    // Assert
    Assertions.assertNotNull(user);
    Assertions.assertNotNull(user.getFirstName());
    Assertions.assertNotNull(user.getLastName());
    Assertions.assertNotNull(user.getEmail());
    Assertions.assertNotNull(user.getPassword());
  }

  @Test
  @DisplayName("User should add roles correctly")
  void userShouldAddRolesCorrectly() {

    // Arrange
    User user = UserFactory.createUser();
    Role role = new Role(1L, "ROLE_OPERATOR");

    // Act
    user.addRole(role);

    // Assert
    Assertions.assertFalse(user.getRoles().isEmpty());
    Assertions.assertTrue(user.getRoles().contains(role));
  }

  @Test
  @DisplayName("User equals should return true for same id")
  void userEqualsShouldReturnTrueWhenIdsAreEqual() {

    // Arrange
    User user1 = new User();
    user1.setId(1L);

    User user2 = new User();
    user2.setId(1L);

    // Act
    boolean result = user1.equals(user2);

    // Assert
    Assertions.assertTrue(result);
  }

  @Test
  @DisplayName("User equals should return false for different ids")
  void userEqualsShouldReturnFalseWhenIdsAreDifferent() {

    // Arrange
    User user1 = new User();
    user1.setId(1L);

    User user2 = new User();
    user2.setId(2L);

    // Act
    boolean result = user1.equals(user2);

    // Assert
    Assertions.assertFalse(result);
  }

  @Test
  @DisplayName("User hashCode should be based on id")
  void userHashCodeShouldBeBasedOnId() {

    // Arrange
    User user = new User();
    user.setId(1L);

    int expected = 31 * 1 + Long.valueOf(1L).hashCode();

    // Act
    int hashCode = user.hashCode();

    // Assert
    Assertions.assertEquals(expected, hashCode);
  }

  @Test
  @DisplayName("User getUsername should return email")
  void userGetUsernameShouldReturnEmail() {

    // Arrange
    User user = UserFactory.createUser();
    String expectedEmail = user.getEmail();

    // Act
    String username = user.getUsername();

    // Assert
    Assertions.assertEquals(expectedEmail, username);
  }

  @Test
  @DisplayName("User getAuthorities should return roles")
  void userGetAuthoritiesShouldReturnRoles() {

    // Arrange
    User user = UserFactory.createUser();
    Role role = new Role(1L, "ROLE_OPERATOR");
    user.addRole(role);

    // Act
    var authorities = user.getAuthorities();

    // Assert
    Assertions.assertNotNull(authorities);
    Assertions.assertFalse(authorities.isEmpty());
    Assertions.assertTrue(authorities.contains(role));
  }

  @Test
  @DisplayName("User hasRole should return true when role exists")
  void userHasRoleShouldReturnTrueWhenRoleExists() {

    // Arrange
    User user = UserFactory.createUser();
    Role role = new Role(1L, "ROLE_OPERATOR");
    user.addRole(role);

    // Act
    boolean hasRole = user.hasRole("ROLE_OPERATOR");

    // Assert
    Assertions.assertTrue(hasRole);
  }

  @Test
  @DisplayName("User hasRole should return false when role does not exist")
  void userHasRoleShouldReturnFalseWhenRoleDoesNotExist() {

    // Arrange
    User user = UserFactory.createUser();

    // Act
    boolean hasRole = user.hasRole("ROLE_ADMIN");

    // Assert
    Assertions.assertFalse(hasRole);
  }

  @Test
  @DisplayName("User isActive should return true when active is true")
  void userIsActiveShouldReturnTrueWhenActiveIsTrue() {

    // Arrange
    User user = UserFactory.createUser();

    // Act
    boolean isActive = user.isActive();

    // Assert
    Assertions.assertTrue(isActive);
  }

}
