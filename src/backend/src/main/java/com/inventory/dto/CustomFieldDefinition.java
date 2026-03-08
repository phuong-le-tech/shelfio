package com.inventory.dto;

import com.inventory.enums.CustomFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomFieldDefinition(
        @NotBlank @Size(max = 50, message = "Field name must be at most 50 characters") String name,
        @NotBlank @Size(max = 100, message = "Field label must be at most 100 characters") String label,
        @NotNull CustomFieldType type,
        boolean required,
        int displayOrder
) {}
