package com.albertsilva.dev.dscatalog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.albertsilva.dev.dscatalog.domain.user.Role;

@DataJpaTest
@DisplayName("RoleRepository Tests")
class RoleRepositoryTest {

  @Autowired
  private RoleRepository roleRepository;

  @Nested
  @DisplayName("FindByAuthority Operations")
  class FindByAuthorityOperations {

    @Test
    @DisplayName("should return role when authority exists")
    void shouldReturnRoleWhenAuthorityExists() {
      // Arrange
      Role savedRole = roleRepository.saveAndFlush(new Role(null, "ROLE_TEST"));

      // Act
      Optional<Role> result = roleRepository.findByAuthority("ROLE_TEST");

      // Assert
      assertThat(result).isPresent();
      assertThat(result.get().getId()).isEqualTo(savedRole.getId());
      assertThat(result.get().getAuthority()).isEqualTo("ROLE_TEST");
    }

    @Test
    @DisplayName("should return empty when authority does not exist")
    void shouldReturnEmptyWhenAuthorityDoesNotExist() {
      // Act
      Optional<Role> result = roleRepository.findByAuthority("ROLE_MISSING");

      // Assert
      assertThat(result).isEmpty();
    }
  }
}
