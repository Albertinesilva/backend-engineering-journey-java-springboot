package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(

    @NotBlank(message = "{user.password.resetToken.notBlank}") String token,

    @NotBlank(message = "{user.password.newPassword.notBlank}") @StrongPassword String password) {

}
