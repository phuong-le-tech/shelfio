package com.inventory.dto.request;

import com.inventory.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = PasswordConstraints.MIN_LENGTH, message = PasswordConstraints.MIN_LENGTH_MESSAGE)
    @Pattern(regexp = PasswordConstraints.LOWERCASE_PATTERN, message = PasswordConstraints.LOWERCASE_MESSAGE)
    @Pattern(regexp = PasswordConstraints.UPPERCASE_PATTERN, message = PasswordConstraints.UPPERCASE_MESSAGE)
    @Pattern(regexp = PasswordConstraints.DIGIT_PATTERN, message = PasswordConstraints.DIGIT_MESSAGE)
    @Pattern(regexp = PasswordConstraints.SPECIAL_CHAR_PATTERN, message = PasswordConstraints.SPECIAL_CHAR_MESSAGE)
    String password,

    Role role
) {}
