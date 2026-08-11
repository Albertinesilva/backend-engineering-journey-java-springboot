package com.albertsilva.dev.dscatalog.dto.user.request;

import com.albertsilva.dev.dscatalog.validation.user.annotation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(

    @NotBlank(message = "{user.password.blank}") 
    String currentPassword,

    @NotBlank(message = "{user.password.blank}") 
    @Size(min = 10, max = 72, message = "{user.password.length}")
    @StrongPassword 
    String newPassword,

    @NotBlank(message = "{user.password.blank}")
    @Size(min = 10, max = 72, message = "{user.password.length}") 
    String confirmPassword) {

}
