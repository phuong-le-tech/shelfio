package com.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank(message = "Token is required")
    @Size(max = 512, message = "Token must not exceed 512 characters")
    String token,

    @NotBlank(message = "Password is required")
    @Size(min = PasswordConstraints.MIN_LENGTH, message = PasswordConstraints.MIN_LENGTH_MESSAGE)
    @Pattern(regexp = PasswordConstraints.LOWERCASE_PATTERN, message = PasswordConstraints.LOWERCASE_MESSAGE)
    @Pattern(regexp = PasswordConstraints.UPPERCASE_PATTERN, message = PasswordConstraints.UPPERCASE_MESSAGE)
    @Pattern(regexp = PasswordConstraints.DIGIT_PATTERN, message = PasswordConstraints.DIGIT_MESSAGE)
    @Pattern(regexp = PasswordConstraints.SPECIAL_CHAR_PATTERN, message = PasswordConstraints.SPECIAL_CHAR_MESSAGE)
    String password
) {}
