package com.inventory.dto.response;

import com.inventory.model.Item;
import com.inventory.service.ImageStorageService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;


public record ItemResponse(
        UUID id,
        String name,
        UUID itemListId,
        String status,
        Integer stock,
        String imageUrl,
        Map<String, Object> customFieldValues,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ItemResponse fromEntity(Item item, ImageStorageService imageStorageService) {
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getItemList() != null ? item.getItemList().getId() : null,
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getStock(),
                item.getImageKey() != null
                        ? imageStorageService.getPresignedUrl(item.getImageKey())
                        : (item.getImageData() != null
                                ? "/api/v1/items/" + item.getId() + "/image"
                                : null),
                item.getCustomFieldValues() != null
                        ? item.getCustomFieldValues()
                        : Map.of(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
