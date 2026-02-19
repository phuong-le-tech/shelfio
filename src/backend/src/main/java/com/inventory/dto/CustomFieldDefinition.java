package com.inventory.dto;

import com.inventory.enums.CustomFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CustomFieldDefinition(
        @NotBlank String name,
        @NotBlank String label,
        @NotNull CustomFieldType type,
        boolean required,
        int displayOrder
) {}
