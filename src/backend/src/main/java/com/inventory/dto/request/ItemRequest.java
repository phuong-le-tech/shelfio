package com.inventory.dto.request;

import com.inventory.enums.ItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record ItemRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "List ID is required")
        UUID itemListId,

        ItemStatus status,

        Integer stock,

        Map<String, Object> customFieldValues
) {}
