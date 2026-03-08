package com.inventory.dto.request;

import com.inventory.enums.ItemStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ItemSearchCriteria(

        @Size(max = 255, message = "Search term must not exceed 255 characters")
        String search,
        UUID itemListId,
        ItemStatus status) {
}
