package com.albertsilva.dev.asjcatalog.validation.role.validator;

import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.ADMIN_ROLE_ID;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.NON_EXISTING_ROLE_ID;
import static com.albertsilva.dev.asjcatalog.factory.RoleFactory.OPERATOR_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.albertsilva.dev.asjcatalog.repository.RoleRepository;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;

@DisplayName("Tests for ValidRolesValidator")
@ExtendWith(MockitoExtension.class)
class ValidRolesValidatorTest {

  private static final String INVALID_ROLE_MESSAGE = "{role.invalid}";

  @InjectMocks
  private ValidRolesValidator validator;

  @Mock
  private RoleRepository repository;

  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintViolationBuilder violationBuilder;

  @Nested
  @DisplayName("IsValid Operations")
  class IsValidOperations {

    @Test
    @DisplayName("isValid should return true when all role ids exist")
    void isValidShouldReturnTrueWhenAllRoleIdsExist() {

      // Arrange
      when(repository.existsById(OPERATOR_ROLE_ID)).thenReturn(true);
      when(repository.existsById(ADMIN_ROLE_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(Set.of(OPERATOR_ROLE_ID, ADMIN_ROLE_ID), context);

      // Assert
      assertThat(result).isTrue();
      verify(repository).existsById(OPERATOR_ROLE_ID);
      verify(repository).existsById(ADMIN_ROLE_ID);
      verifyNoInteractions(context);
    }

    @Test
    @DisplayName("isValid should return true when single role id exists")
    void isValidShouldReturnTrueWhenSingleRoleIdExists() {

      // Arrange
      when(repository.existsById(OPERATOR_ROLE_ID)).thenReturn(true);

      // Act
      boolean result = validator.isValid(Set.of(OPERATOR_ROLE_ID), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(context);
    }

    // #14: aceitar roleIds nulo segue a regra de que validadores tratam null como válido;
    // se usuário sem roles é permitido ainda está em aberto na issue. Mantido sem reforço.
    @Test
    @DisplayName("isValid should return true when role ids are null")
    void isValidShouldReturnTrueWhenRoleIdsAreNull() {

      // Act
      boolean result = validator.isValid(null, context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    // #14: com o conjunto vazio, a obrigatoriedade ficaria com um @NotEmpty no DTO; se
    // usuário sem roles é permitido ainda está em aberto na issue. Mantido sem reforço.
    @Test
    @DisplayName("isValid should return true when role ids are empty")
    void isValidShouldReturnTrueWhenRoleIdsAreEmpty() {

      // Act
      boolean result = validator.isValid(Set.of(), context);

      // Assert
      assertThat(result).isTrue();
      verifyNoInteractions(repository, context);
    }

    @Test
    @DisplayName("isValid should return false when any role id does not exist")
    void isValidShouldReturnFalseWhenAnyRoleIdDoesNotExist() {

      // Arrange
      when(context.buildConstraintViolationWithTemplate(INVALID_ROLE_MESSAGE)).thenReturn(violationBuilder);
      when(repository.existsById(NON_EXISTING_ROLE_ID)).thenReturn(false);

      // Act
      boolean result = validator.isValid(Set.of(NON_EXISTING_ROLE_ID), context);

      // Assert
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(violationBuilder).addConstraintViolation();
    }
  }
}
