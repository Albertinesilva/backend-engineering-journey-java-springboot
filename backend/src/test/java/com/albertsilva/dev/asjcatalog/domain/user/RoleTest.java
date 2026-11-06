package com.albertsilva.dev.asjcatalog.domain.user;

import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_AUTHORITY;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Testes de {@code Role}. O {@code equals} e o {@code hashCode} ficam no
 * {@code EntityIdentityContractTest}.
 */
@DisplayName("Tests for Role")
class RoleTest {

  @Nested
  @DisplayName("Constructor Operations")
  class ConstructorOperations {

    @Test
    @DisplayName("constructor should expose id and authority as granted authority when values are given")
    void constructorShouldExposeIdAndAuthorityAsGrantedAuthorityWhenValuesAreGiven() {

      // Act
      Role role = new Role(ADMIN_ROLE_ID, ADMIN_AUTHORITY);

      // Assert
      assertThat(role.getId()).isEqualTo(ADMIN_ROLE_ID);
      assertThat(role).isInstanceOf(GrantedAuthority.class);
      assertThat(role.getAuthority()).isEqualTo(ADMIN_AUTHORITY);
    }
  }
}
