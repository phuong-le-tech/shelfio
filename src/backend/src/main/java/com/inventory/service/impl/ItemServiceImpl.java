package com.inventory.service.impl;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ItemStatus;
import com.inventory.exception.FileValidationException;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.repository.specification.ItemSpecification;
import com.inventory.security.SecurityUtils;
import com.inventory.service.CustomFieldValidator;
import com.inventory.service.IItemService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ItemServiceImpl implements IItemService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final ItemRepository itemRepository;
    private final ItemListRepository itemListRepository;
    private final CustomFieldValidator customFieldValidator;
    private final SecurityUtils securityUtils;

    public ItemServiceImpl(ItemRepository itemRepository, ItemListRepository itemListRepository,
                           CustomFieldValidator customFieldValidator, SecurityUtils securityUtils) {
        this.itemRepository = itemRepository;
        this.itemListRepository = itemListRepository;
        this.customFieldValidator = customFieldValidator;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Item> getAllItems(@NonNull Pageable pageable,
        @NonNull ItemSearchCriteria criteria) {
        UUID userId = null;
        if (!securityUtils.isAdmin()) {
            userId = securityUtils.getCurrentUserId()
                    .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        }
        return itemRepository.findAll(ItemSpecification.withCriteria(criteria, userId), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> getItemById(@NonNull UUID id) {
        return itemRepository.findById(id)
                .map(this::checkItemOwnership);
    }

    @Override
    @Transactional
    public Item createItem(@NonNull ItemRequest request, MultipartFile image) throws IOException {

        ItemList itemList = findListWithOwnershipCheck(
                Objects.requireNonNull(request.itemListId(), "Item list ID must not be null"));

        customFieldValidator.validate(itemList.getCustomFieldDefinitions(), request.customFieldValues());

        Item item = new Item();
        item.setName(request.name());
        item.setItemList(itemList);
        item.setStatus(request.status() != null ? request.status() : ItemStatus.IN_STOCK);
        item.setStock(request.stock() != null ? request.stock() : 0);
        item.setCustomFieldValues(request.customFieldValues());

        if (image != null && !image.isEmpty()) {
            byte[] imageBytes = image.getBytes();
            String detectedType = validateAndDetectContentType(imageBytes, image.getContentType());
            item.setImageData(imageBytes);
            item.setContentType(detectedType);
        }

        return itemRepository.save(item);
    }

    @Override
    @Transactional
    public Item updateItem(@NonNull UUID id, ItemRequest request, MultipartFile image) throws IOException {
        Item item = getItemById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));

        ItemList itemList = findListWithOwnershipCheck(
                Objects.requireNonNull(request.itemListId(), "Item list ID must not be null"));

        customFieldValidator.validate(itemList.getCustomFieldDefinitions(), request.customFieldValues());

        item.setName(request.name());
        item.setItemList(itemList);
        item.setStatus(request.status() != null ? request.status() : item.getStatus());
        item.setStock(request.stock() != null ? request.stock() : item.getStock());
        item.setCustomFieldValues(request.customFieldValues());

        if (image != null && !image.isEmpty()) {
            byte[] imageBytes = image.getBytes();
            String detectedType = validateAndDetectContentType(imageBytes, image.getContentType());
            item.setImageData(imageBytes);
            item.setContentType(detectedType);
        }

        return itemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteItem(@NonNull UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        checkItemOwnership(item);
        itemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        if (securityUtils.isAdmin()) {
            return buildStats(
                    itemRepository.count(),
                    itemRepository.sumStock(),
                    itemRepository.countLowStock(LOW_STOCK_THRESHOLD),
                    itemRepository.countOutOfStock(),
                    itemRepository.countByStatus(),
                    itemRepository.countByCategory(),
                    itemRepository.getListsOverview(),
                    itemRepository.findTop5ByOrderByUpdatedAtDesc());
        }
        UUID userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        return buildStats(
                itemRepository.countByUserId(userId),
                itemRepository.sumStockByUserId(userId),
                itemRepository.countLowStockByUserId(userId, LOW_STOCK_THRESHOLD),
                itemRepository.countOutOfStockByUserId(userId),
                itemRepository.countByStatusAndUserId(userId),
                itemRepository.countByCategoryAndUserId(userId),
                itemRepository.getListsOverviewByUserId(userId),
                itemRepository.findTop5ByUserIdOrderByUpdatedAtDesc(userId));
    }

    private DashboardStats buildStats(long totalItems, long totalQuantity, long lowStockCount, long outOfStockCount,
                                      List<Object[]> statusRows, List<Object[]> categoryRows,
                                      List<Object[]> listsOverviewRows, List<Item> recentItems) {
        Map<String, Long> statusCounts = statusRows.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? ((ItemStatus) row[0]).name() : "Unknown",
                        row -> (Long) row[1]));
        Map<String, Long> categoryCounts = categoryRows.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? (String) row[0] : "Uncategorized",
                        row -> (Long) row[1]));
        
        // Query returns: [listName (String), itemsCount (Long), totalQuantity (Long)]
        List<DashboardStats.ListOverviewDto> listsOverview = listsOverviewRows.stream()
                .map(row -> new DashboardStats.ListOverviewDto(
                        (String) row[0],
                        (Long) row[1],
                        (Long) row[2]))
                .toList();

        List<DashboardStats.RecentItemDto> recentlyUpdated = recentItems.stream()
                .map(item -> new DashboardStats.RecentItemDto(
                        item.getId().toString(),
                        item.getName(),
                        item.getItemList().getName(),
                        item.getCustomFieldValues() != null && item.getCustomFieldValues().containsKey("sku") 
                                ? item.getCustomFieldValues().get("sku").toString() : null,
                        item.getStock(),
                        item.getStatus().name(),
                        item.getUpdatedAt()))
                .toList();

        return new DashboardStats(totalItems, totalQuantity, lowStockCount, outOfStockCount,
                statusCounts, categoryCounts, listsOverview, recentlyUpdated);
    }

    private Item checkItemOwnership(Item item) {
        if (!securityUtils.isAdmin()) {
            UUID userId = securityUtils.getCurrentUserId()
                    .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
            if (!item.getItemList().getUser().getId().equals(userId)) {
                throw new ItemNotFoundException(item.getId());
            }
        }
        return item;
    }

    private ItemList findListWithOwnershipCheck(UUID listId) {
        ItemList itemList = itemListRepository.findById(listId)
                .orElseThrow(() -> new ItemListNotFoundException(listId));
        if (!securityUtils.isAdmin()) {
            UUID userId = securityUtils.getCurrentUserId()
                    .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
            if (!itemList.getUser().getId().equals(userId)) {
                throw new ItemListNotFoundException(listId);
            }
        }
        return itemList;
    }

    private String validateAndDetectContentType(byte[] data, String declaredContentType) {
        if (data.length > MAX_FILE_SIZE) {
            throw new FileValidationException("File size exceeds maximum allowed size of 10MB");
        }
        String detectedType = detectContentType(data);
        if (detectedType == null || !ALLOWED_CONTENT_TYPES.contains(detectedType)) {
            throw new FileValidationException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }
        if (declaredContentType != null && !declaredContentType.equals(detectedType)) {
            log.warn("Content-Type mismatch: declared={}, detected={}", declaredContentType, detectedType);
        }
        return detectedType;
    }

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC = {0x47, 0x49, 0x46, 0x38};
    private static final byte[] RIFF_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MAGIC = {0x57, 0x45, 0x42, 0x50};

    private String detectContentType(byte[] data) {
        if (data.length < 12) return null;
        if (startsWith(data, JPEG_MAGIC)) return "image/jpeg";
        if (startsWith(data, PNG_MAGIC)) return "image/png";
        if (startsWith(data, GIF_MAGIC)) return "image/gif";
        if (startsWith(data, RIFF_MAGIC) && Arrays.equals(Arrays.copyOfRange(data, 8, 12), WEBP_MAGIC)) {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
