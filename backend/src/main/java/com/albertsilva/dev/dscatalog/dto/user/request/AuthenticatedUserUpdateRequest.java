package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.UniqueEmailForAuthenticatedUser;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticatedUserUpdateRequest(

  @NotBlank(message = "{user.firstName.notBlank}")
  @Size(max = 100, message = "{user.firstName.size}")
  String firstName,

  @NotBlank(message = "{user.lastName.notBlank}")
  @Size(max = 100, message = "{user.lastName.size}")
  String lastName,

  @NotBlank(message = "{user.email.notBlank}")
  @Email(message = "{user.email.invalid}")
  @Size(max = 255, message = "{user.email.size}")
  @UniqueEmailForAuthenticatedUser
  String email) {
}
