package com.inventory.dto.request;

import com.inventory.enums.ItemStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record ItemRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @NotNull(message = "List ID is required")
        UUID itemListId,

        ItemStatus status,

        @Min(value = 0, message = "Stock must not be negative")
        Integer stock,

        Map<String, Object> customFieldValues
) {}
