package com.inventory.dto.request;

import com.inventory.dto.CustomFieldDefinition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ItemListRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 100, message = "Category must be at most 100 characters")
        String category,

        @Valid
        @Size(max = 20, message = "A list can have at most 20 custom fields")
        List<CustomFieldDefinition> customFieldDefinitions
) {}
