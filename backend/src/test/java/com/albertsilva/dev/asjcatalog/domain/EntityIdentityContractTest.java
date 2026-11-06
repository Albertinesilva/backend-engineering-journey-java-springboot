package com.albertsilva.dev.asjcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.albertsilva.dev.asjcatalog.domain.catalog.Category;
import com.albertsilva.dev.asjcatalog.domain.catalog.Product;
import com.albertsilva.dev.asjcatalog.domain.recovery.Email;
import com.albertsilva.dev.asjcatalog.domain.recovery.Token;
import com.albertsilva.dev.asjcatalog.domain.user.Role;
import com.albertsilva.dev.asjcatalog.domain.user.User;
import com.albertsilva.dev.asjcatalog.factory.CategoryFactory;
import com.albertsilva.dev.asjcatalog.factory.ProductFactory;
import com.albertsilva.dev.asjcatalog.factory.RoleFactory;
import com.albertsilva.dev.asjcatalog.factory.TokenFactory;
import com.albertsilva.dev.asjcatalog.factory.UserFactory;

/**
 * Contrato de identidade das seis entidades: {@code equals} e {@code hashCode}
 * comparam só o {@code id}. Cada caso parametrizado recebe uma função que cria
 * a entidade com o id informado, e o nome do caso mostra a entidade testada.
 *
 * <p>
 * <b>Cenários pendentes:</b>
 * </p>
 * <ul>
 * <li>Duas instâncias não salvas ({@code id} nulo nos dois lados): hoje são
 * consideradas iguais e têm o mesmo {@code hashCode}, e o {@code hashCode} muda
 * quando o id é atribuído (#51).</li>
 * </ul>
 */
@DisplayName("Tests for the identity contract of the entities")
class EntityIdentityContractTest {

  private static final Long FIRST_ID = 1L;
  private static final Long SECOND_ID = 2L;

  static Stream<Arguments> entities() {
    return Stream.of(
        entity("Category", id -> {
          Category category = CategoryFactory.createCategory();
          category.setId(id);
          return category;
        }),
        entity("Product", id -> {
          Product product = ProductFactory.createProduct();
          product.setId(id);
          return product;
        }),
        entity("User", id -> {
          User user = UserFactory.createUser();
          user.setId(id);
          return user;
        }),
        entity("Role", id -> new Role(id, RoleFactory.OPERATOR_AUTHORITY)),
        entity("Token", id -> {
          Token token = TokenFactory.createActivationToken(UserFactory.createUser());
          token.setId(id);
          return token;
        }),
        entity("Email", id -> {
          Email email = new Email();
          email.setId(id);
          return email;
        }));
  }

  private static Arguments entity(String name, Function<Long, Object> withId) {
    return Arguments.of(named(name, withId));
  }

  /** Descrição das asserções: o nome da entidade aparece na mensagem de falha do console. */
  private static String nameOf(Object entity) {
    return entity.getClass().getSimpleName();
  }

  @Nested
  @DisplayName("Equals and HashCode Operations")
  class EqualsAndHashCodeOperations {

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.domain.EntityIdentityContractTest#entities")
    @DisplayName("equals should return true when compared with itself")
    void equalsShouldReturnTrueWhenComparedWithItself(Function<Long, Object> withId) {

      // Arrange
      Object entity = withId.apply(FIRST_ID);

      // Act
      boolean result = entity.equals(entity);

      // Assert
      assertThat(result).as(nameOf(entity)).isTrue();
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.domain.EntityIdentityContractTest#entities")
    @DisplayName("equals should return true and hash codes should match when ids are equal")
    void equalsShouldReturnTrueAndHashCodesShouldMatchWhenIdsAreEqual(Function<Long, Object> withId) {

      // Arrange
      Object first = withId.apply(FIRST_ID);
      Object second = withId.apply(FIRST_ID);

      // Act
      boolean result = first.equals(second);

      // Assert
      assertThat(result).as(nameOf(first)).isTrue();
      assertThat(first.hashCode()).as(nameOf(first)).isEqualTo(second.hashCode());
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.domain.EntityIdentityContractTest#entities")
    @DisplayName("equals should return false when ids are different")
    void equalsShouldReturnFalseWhenIdsAreDifferent(Function<Long, Object> withId) {

      // Arrange
      Object first = withId.apply(FIRST_ID);
      Object second = withId.apply(SECOND_ID);

      // Act
      boolean result = first.equals(second);

      // Assert
      assertThat(result).as(nameOf(first)).isFalse();
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.domain.EntityIdentityContractTest#entities")
    @DisplayName("equals should return false when other is null")
    void equalsShouldReturnFalseWhenOtherIsNull(Function<Long, Object> withId) {

      // Arrange
      Object entity = withId.apply(FIRST_ID);

      // Act
      boolean result = entity.equals(null);

      // Assert
      assertThat(result).as(nameOf(entity)).isFalse();
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.domain.EntityIdentityContractTest#entities")
    @DisplayName("equals should return false when other is another type")
    void equalsShouldReturnFalseWhenOtherIsAnotherType(Function<Long, Object> withId) {

      // Arrange
      Object entity = withId.apply(FIRST_ID);

      // Act
      boolean result = entity.equals(new Object());

      // Assert
      assertThat(result).as(nameOf(entity)).isFalse();
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.domain.EntityIdentityContractTest#entities")
    @DisplayName("equals should return false when only this id is null")
    void equalsShouldReturnFalseWhenOnlyThisIdIsNull(Function<Long, Object> withId) {

      // Arrange
      Object unsaved = withId.apply(null);
      Object saved = withId.apply(FIRST_ID);

      // Act
      boolean result = unsaved.equals(saved);

      // Assert
      assertThat(result).as(nameOf(unsaved)).isFalse();
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("com.albertsilva.dev.asjcatalog.domain.EntityIdentityContractTest#entities")
    @DisplayName("equals should return false when only other id is null")
    void equalsShouldReturnFalseWhenOnlyOtherIdIsNull(Function<Long, Object> withId) {

      // Arrange
      Object saved = withId.apply(FIRST_ID);
      Object unsaved = withId.apply(null);

      // Act
      boolean result = saved.equals(unsaved);

      // Assert
      assertThat(result).as(nameOf(saved)).isFalse();
    }
  }
}
