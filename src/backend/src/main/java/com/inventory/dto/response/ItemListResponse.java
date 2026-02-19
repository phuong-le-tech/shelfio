package com.inventory.dto.response;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.model.ItemList;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ItemListResponse(
        UUID id,
        String name,
        String description,
        String category,
        int itemCount,
        List<ItemResponse> items,
        List<CustomFieldDefinition> customFieldDefinitions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ItemListResponse fromEntity(ItemList itemList) {
        int count = 0;
        List<ItemResponse> itemResponses = List.of();

        if (itemList.getItems() != null && Hibernate.isInitialized(itemList.getItems())) {
            count = itemList.getItems().size();
            itemResponses = itemList.getItems().stream().map(ItemResponse::fromEntity).toList();
        }

        return new ItemListResponse(
                itemList.getId(),
                itemList.getName(),
                itemList.getDescription(),
                itemList.getCategory(),
                count,
                itemResponses,
                itemList.getCustomFieldDefinitions() != null
                        ? itemList.getCustomFieldDefinitions()
                        : List.of(),
                itemList.getCreatedAt(),
                itemList.getUpdatedAt()
        );
    }

    public static ItemListResponse fromEntityWithoutItems(ItemList itemList) {
        return new ItemListResponse(
                itemList.getId(),
                itemList.getName(),
                itemList.getDescription(),
                itemList.getCategory(),
                0,
                List.of(),
                itemList.getCustomFieldDefinitions() != null
                        ? itemList.getCustomFieldDefinitions()
                        : List.of(),
                itemList.getCreatedAt(),
                itemList.getUpdatedAt()
        );
    }
}
