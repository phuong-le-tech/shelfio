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
        String barcode,
        String imageUrl,
        Map<String, Object> customFieldValues,
        Integer position,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ItemResponse fromEntity(Item item, ImageStorageService imageStorageService) {
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getItemList() != null ? item.getItemList().getId() : null,
                item.getStatus() != null ? item.getStatus().name() : null,
                item.getStock(),
                item.getBarcode(),
                item.getImageKey() != null
                        ? imageStorageService.getPresignedUrl(item.getImageKey())
                        : null,
                item.getCustomFieldValues() != null
                        ? item.getCustomFieldValues()
                        : Map.of(),
                item.getPosition(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
