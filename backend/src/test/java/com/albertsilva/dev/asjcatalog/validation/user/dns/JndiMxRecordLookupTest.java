package com.albertsilva.dev.asjcatalog.validation.user.dns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.naming.CommunicationException;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Tests for JndiMxRecordLookup")
@ExtendWith(MockitoExtension.class)
class JndiMxRecordLookupTest {

  private static final String DOMAIN = "gmail.com";
  private static final String[] MX_TYPE = { "MX" };

  @Mock
  private DirContext context;

  private JndiMxRecordLookup lookup;

  @BeforeEach
  void setUp() {
    lookup = new JndiMxRecordLookup(() -> context);
  }

  @Nested
  @DisplayName("hasMxRecord")
  class HasMxRecordTests {

    @Test
    @DisplayName("hasMxRecord should return true when domain has MX record")
    void hasMxRecordShouldReturnTrueWhenDomainHasMxRecord() throws NamingException {

      // Arrange
      when(context.getAttributes(eq(DOMAIN), eq(MX_TYPE)))
          .thenReturn(new BasicAttributes("MX", "10 mx.gmail.com"));

      // Act
      boolean result = lookup.hasMxRecord(DOMAIN);

      // Assert
      assertThat(result).isTrue();
      verify(context).close();
    }

    @Test
    @DisplayName("hasMxRecord should return false when domain has no MX record")
    void hasMxRecordShouldReturnFalseWhenDomainHasNoMxRecord() throws NamingException {

      // Arrange
      when(context.getAttributes(eq(DOMAIN), eq(MX_TYPE))).thenReturn(new BasicAttributes());

      // Act
      boolean result = lookup.hasMxRecord(DOMAIN);

      // Assert
      assertThat(result).isFalse();
      verify(context).close();
    }

    @Test
    @DisplayName("hasMxRecord should return false when MX record is empty")
    void hasMxRecordShouldReturnFalseWhenMxRecordIsEmpty() throws NamingException {

      // Arrange
      BasicAttributes attributes = new BasicAttributes();
      attributes.put(new BasicAttribute("MX"));

      when(context.getAttributes(eq(DOMAIN), eq(MX_TYPE))).thenReturn(attributes);

      // Act
      boolean result = lookup.hasMxRecord(DOMAIN);

      // Assert
      assertThat(result).isFalse();
      verify(context).close();
    }

    @Test
    @DisplayName("hasMxRecord should return false when lookup fails")
    void hasMxRecordShouldReturnFalseWhenLookupFails() throws NamingException {

      // Arrange
      when(context.getAttributes(eq(DOMAIN), any(String[].class)))
          .thenThrow(new CommunicationException("DNS indisponível"));

      // Act
      boolean result = lookup.hasMxRecord(DOMAIN);

      // Assert
      assertThat(result).isFalse();
      verify(context).close();
    }

    @Test
    @DisplayName("hasMxRecord should return false when context cannot be created")
    void hasMxRecordShouldReturnFalseWhenContextCannotBeCreated() {

      // Arrange
      JndiMxRecordLookup failingLookup = new JndiMxRecordLookup(() -> {
        throw new NamingException("Provedor indisponível");
      });

      // Act
      boolean result = failingLookup.hasMxRecord(DOMAIN);

      // Assert
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("hasMxRecord should ignore error when closing context")
    void hasMxRecordShouldIgnoreErrorWhenClosingContext() throws NamingException {

      // Arrange
      when(context.getAttributes(eq(DOMAIN), eq(MX_TYPE)))
          .thenReturn(new BasicAttributes("MX", "10 mx.gmail.com"));
      doThrow(new NamingException("Falha ao fechar")).when(context).close();

      // Act
      boolean result = lookup.hasMxRecord(DOMAIN);

      // Assert
      assertThat(result).isTrue();
      verify(context).close();
    }
  }
}
