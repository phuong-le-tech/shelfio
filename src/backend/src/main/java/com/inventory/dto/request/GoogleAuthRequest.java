package com.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleAuthRequest(
    @NotBlank(message = "Google credential is required")
    @Size(max = 4096, message = "Credential must not exceed 4096 characters")
    String credential
) {}
