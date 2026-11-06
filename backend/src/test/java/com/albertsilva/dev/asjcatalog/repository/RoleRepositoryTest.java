package com.albertsilva.dev.asjcatalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.albertsilva.dev.asjcatalog.domain.user.Role;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests for RoleRepository")
class RoleRepositoryTest {

  private static final String TEST_AUTHORITY = "ROLE_TEST";
  private static final String MISSING_AUTHORITY = "ROLE_MISSING";

  @Autowired
  private RoleRepository roleRepository;

  @Nested
  @DisplayName("FindByAuthority Operations")
  class FindByAuthorityOperations {

    @Test
    @DisplayName("findByAuthority should return role when authority exists")
    void findByAuthorityShouldReturnRoleWhenAuthorityExists() {
      // Arrange
      Role savedRole = roleRepository.saveAndFlush(new Role(null, TEST_AUTHORITY));

      // Act
      Optional<Role> result = roleRepository.findByAuthority(TEST_AUTHORITY);

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(savedRole.getId());
      assertThat(result.get().getAuthority()).isEqualTo(TEST_AUTHORITY);
    }

    @Test
    @DisplayName("findByAuthority should return empty when authority does not exist")
    void findByAuthorityShouldReturnEmptyWhenAuthorityDoesNotExist() {
      // Act
      Optional<Role> result = roleRepository.findByAuthority(MISSING_AUTHORITY);

      // Assert
      assertThat(result).isEmpty();
    }
  }
}
