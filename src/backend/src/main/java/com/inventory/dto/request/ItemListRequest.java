package com.inventory.dto.request;

import com.inventory.dto.CustomFieldDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ItemListRequest(

        @NotBlank(message = "Name is required")
        String name,

        String description,

        String category,

        @Valid List<CustomFieldDefinition> customFieldDefinitions
) {}
