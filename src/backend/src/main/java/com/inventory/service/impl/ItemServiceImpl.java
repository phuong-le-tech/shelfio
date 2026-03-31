package com.inventory.service.impl;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.request.ReorderItemsRequest;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ItemStatus;
import com.inventory.enums.WorkspaceRole;
import com.inventory.exception.FileValidationException;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.exception.WorkspaceAccessDeniedException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.WorkspaceMember;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.repository.specification.ItemSpecification;
import com.inventory.security.SecurityUtils;
import com.inventory.security.WorkspaceAccessUtils;
import com.inventory.enums.ActivityEventType;
import com.inventory.service.CustomFieldValidator;
import com.inventory.service.IActivityService;
import com.inventory.service.IItemService;
import com.inventory.service.ImageProcessingService;
import com.inventory.service.ImageStorageService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
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

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final ItemRepository itemRepository;
    private final ItemListRepository itemListRepository;
    private final CustomFieldValidator customFieldValidator;
    private final SecurityUtils securityUtils;
    private final WorkspaceAccessUtils workspaceAccessUtils;
    private final ImageStorageService imageStorageService;
    private final ImageProcessingService imageProcessingService;
    private final IActivityService activityService;

    public ItemServiceImpl(ItemRepository itemRepository, ItemListRepository itemListRepository,
                           CustomFieldValidator customFieldValidator, SecurityUtils securityUtils,
                           WorkspaceAccessUtils workspaceAccessUtils,
                           ImageStorageService imageStorageService, ImageProcessingService imageProcessingService,
                           IActivityService activityService) {
        this.itemRepository = itemRepository;
        this.itemListRepository = itemListRepository;
        this.customFieldValidator = customFieldValidator;
        this.securityUtils = securityUtils;
        this.workspaceAccessUtils = workspaceAccessUtils;
        this.imageStorageService = imageStorageService;
        this.imageProcessingService = imageProcessingService;
        this.activityService = activityService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Item> getAllItems(@NonNull Pageable pageable,
        @NonNull ItemSearchCriteria criteria) {
        if (securityUtils.isAdmin()) {
            return itemRepository.findAll(ItemSpecification.withCriteria(criteria, null), pageable);
        }
        List<UUID> workspaceIds = workspaceAccessUtils.getAccessibleWorkspaceIds();
        return itemRepository.findAll(ItemSpecification.withCriteria(criteria, null, workspaceIds), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> getItemById(@NonNull UUID id) {
        return itemRepository.findById(id)
                .map(this::checkItemOwnership);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Item> getItemByBarcode(@NonNull String barcode) {
        if (securityUtils.isAdmin()) {
            return itemRepository.findAll(ItemSpecification.withCriteria(
                    new ItemSearchCriteria(null, null, null, barcode), null),
                    org.springframework.data.domain.Pageable.ofSize(1))
                    .stream().findFirst();
        }
        List<UUID> workspaceIds = workspaceAccessUtils.getAccessibleWorkspaceIds();
        if (workspaceIds.isEmpty()) {
            return Optional.empty();
        }
        return itemRepository.findByBarcodeAndWorkspaceIds(barcode, workspaceIds);
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
        item.setStatus(request.status() != null ? request.status() : ItemStatus.AVAILABLE);
        item.setStock(request.stock() != null ? request.stock() : 0);
        item.setBarcode(request.barcode());
        item.setCustomFieldValues(request.customFieldValues());
        item.setPosition(itemRepository.findNextPositionForList(itemList.getId()));

        // Save first to get the generated ID
        Item savedItem = itemRepository.save(item);
        activityService.record(
                savedItem.getItemList().getWorkspace().getId(),
                ActivityEventType.ITEM_CREATED, "ITEM", savedItem.getId(), savedItem.getName());

        if (image != null && !image.isEmpty()) {
            byte[] imageBytes = image.getBytes();
            validateImageContent(imageBytes, image.getContentType());
            byte[] webpBytes = imageProcessingService.processToWebP(imageBytes);
            validateProcessedSize(webpBytes);
            String imageKey = "items/" + savedItem.getId() + "/" + UUID.randomUUID() + ".webp";
            imageStorageService.upload(imageKey, webpBytes, "image/webp");
            savedItem.setImageKey(imageKey);
            try {
                return itemRepository.save(savedItem);
            } catch (Exception e) {
                deleteImageQuietly(imageKey);
                throw e;
            }
        }

        return savedItem;
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
        item.setBarcode(request.barcode());
        item.setCustomFieldValues(request.customFieldValues());

        if (image != null && !image.isEmpty()) {
            byte[] imageBytes = image.getBytes();
            validateImageContent(imageBytes, image.getContentType());
            byte[] webpBytes = imageProcessingService.processToWebP(imageBytes);
            validateProcessedSize(webpBytes);
            String oldImageKey = item.getImageKey();
            String imageKey = "items/" + item.getId() + "/" + UUID.randomUUID() + ".webp";
            imageStorageService.upload(imageKey, webpBytes, "image/webp");
            item.setImageKey(imageKey);
            try {
                Item saved = itemRepository.save(item);
                if (oldImageKey != null) {
                    deleteImageQuietly(oldImageKey);
                }
                activityService.record(
                        saved.getItemList().getWorkspace().getId(),
                        ActivityEventType.ITEM_UPDATED, "ITEM", saved.getId(), saved.getName());
                return saved;
            } catch (Exception e) {
                deleteImageQuietly(imageKey);
                throw e;
            }
        }

        Item saved = itemRepository.save(item);
        activityService.record(
                saved.getItemList().getWorkspace().getId(),
                ActivityEventType.ITEM_UPDATED, "ITEM", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public void deleteItem(@NonNull UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        checkItemOwnership(item);

        // Verify EDITOR or OWNER role for write operations
        if (!securityUtils.isAdmin()) {
            WorkspaceMember member = workspaceAccessUtils.requireMembership(item.getItemList().getWorkspace().getId());
            if (member.getRole() == WorkspaceRole.VIEWER) {
                throw new WorkspaceAccessDeniedException("Viewers cannot delete items");
            }
        }

        String imageKey = item.getImageKey();
        activityService.record(
                item.getItemList().getWorkspace().getId(),
                ActivityEventType.ITEM_DELETED, "ITEM", item.getId(), item.getName());
        itemRepository.delete(item);
        if (imageKey != null) {
            deleteImageQuietly(imageKey);
        }
    }

    @Override
    @Transactional
    public void deleteItems(@NonNull List<UUID> ids) {
        for (UUID id : ids) {
            if (id == null) continue;
            try {
                deleteItem(id);
            } catch (com.inventory.exception.ItemNotFoundException e) {
                // Item already gone — skip silently
            }
        }
    }

    @Override
    @Transactional
    public void reorderItems(@NonNull ReorderItemsRequest request) {
        // Verify list ownership and write access
        findListWithOwnershipCheck(Objects.requireNonNull(request.getListId()));

        List<UUID> orderedIds = Objects.requireNonNull(request.getOrderedIds());
        if (orderedIds.isEmpty()) return;
        if (orderedIds.stream().distinct().count() != orderedIds.size()) {
            throw new IllegalArgumentException("Duplicate IDs in reorder request");
        }

        // Fetch all items for this list
        List<Item> items = itemRepository.findAllByItemListIdOrderByCreatedAtAsc(request.getListId());
        Map<UUID, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getId, i -> i));

        Set<UUID> orderedSet = new java.util.LinkedHashSet<>(orderedIds);

        // Assign positions for explicitly ordered items
        List<Item> toSave = new ArrayList<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID itemId = orderedIds.get(i);
            Item item = itemMap.get(itemId);
            if (item != null) {
                item.setPosition(i);
                toSave.add(item);
            }
        }

        // Re-index remaining items after the explicit block to close any gaps
        int nextPosition = orderedIds.size();
        for (Item item : items) {
            if (!orderedSet.contains(item.getId())) {
                item.setPosition(nextPosition++);
                toSave.add(item);
            }
        }

        itemRepository.saveAll(toSave);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        if (securityUtils.isAdmin()) {
            return buildStats(
                    itemRepository.count(),
                    itemRepository.sumStock(),
                    itemRepository.countByStatus(),
                    itemRepository.countByCategory(),
                    itemRepository.getListsOverview(),
                    itemRepository.findTop5ByOrderByUpdatedAtDesc());
        }
        List<UUID> workspaceIds = workspaceAccessUtils.getAccessibleWorkspaceIds();
        if (workspaceIds.isEmpty()) {
            return buildStats(0L, 0L, List.of(), List.of(), List.of(), List.of());
        }
        return buildStats(
                itemRepository.countByWorkspaceIds(workspaceIds),
                itemRepository.sumStockByWorkspaceIds(workspaceIds),
                itemRepository.countByStatusAndWorkspaceIds(workspaceIds),
                itemRepository.countByCategoryAndWorkspaceIds(workspaceIds),
                itemRepository.getListsOverviewByWorkspaceIds(workspaceIds),
                itemRepository.findTop5ByWorkspaceIdsOrderByUpdatedAtDesc(workspaceIds));
    }

    private DashboardStats buildStats(long totalItems, long totalQuantity,
                                      List<Object[]> statusRows, List<Object[]> categoryRows,
                                      List<Object[]> listsOverviewRows, List<Item> recentItems) {
        Map<String, Long> statusCounts = statusRows.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? ((ItemStatus) row[0]).name() : "Unknown",
                        row -> (Long) row[1]));

        long toVerifyCount = statusCounts.getOrDefault("TO_VERIFY", 0L);
        long needsAttentionCount = statusCounts.getOrDefault("NEEDS_MAINTENANCE", 0L)
                + statusCounts.getOrDefault("DAMAGED", 0L);

        Map<String, Long> categoryCounts = categoryRows.stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? (String) row[0] : "Uncategorized",
                        row -> (Long) row[1]));

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
                        item.getItemList().getId().toString(),
                        item.getItemList().getName(),
                        item.getStock(),
                        item.getStatus().name(),
                        item.getUpdatedAt()))
                .toList();

        return new DashboardStats(totalItems, totalQuantity, toVerifyCount, needsAttentionCount,
                statusCounts, categoryCounts, listsOverview, recentlyUpdated);
    }

    private Item checkItemOwnership(Item item) {
        if (!securityUtils.isAdmin()) {
            List<UUID> workspaceIds = workspaceAccessUtils.getAccessibleWorkspaceIds();
            UUID itemWorkspaceId = item.getItemList().getWorkspace().getId();
            if (!workspaceIds.contains(itemWorkspaceId)) {
                throw new ItemNotFoundException(item.getId());
            }
        }
        return item;
    }

    private ItemList findListWithOwnershipCheck(UUID listId) {
        ItemList itemList = itemListRepository.findByIdWithLock(listId)
                .orElseThrow(() -> new ItemListNotFoundException(listId));
        if (!securityUtils.isAdmin()) {
            // Verify workspace membership
            WorkspaceMember member = workspaceAccessUtils.requireMembership(itemList.getWorkspace().getId());
            // Verify write access
            if (member.getRole() == WorkspaceRole.VIEWER) {
                throw new WorkspaceAccessDeniedException("Viewers cannot create or update items");
            }
        }
        return itemList;
    }

    private void validateProcessedSize(byte[] data) {
        if (data.length > MAX_FILE_SIZE) {
            throw new FileValidationException("Processed image exceeds maximum allowed size of 10MB");
        }
    }

    private void deleteImageQuietly(String key) {
        try {
            imageStorageService.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete image from storage: {}", key, e);
        }
    }

    private void validateImageContent(byte[] data, String declaredContentType) {
        if (data.length > MAX_FILE_SIZE) {
            throw new FileValidationException("File size exceeds maximum allowed size of 10MB");
        }
        String detectedType = com.inventory.util.ImageContentValidator.detectContentType(data);
        if (detectedType == null || !ALLOWED_CONTENT_TYPES.contains(detectedType)) {
            throw new FileValidationException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }
        if (declaredContentType != null && !declaredContentType.equals(detectedType)) {
            log.warn("Content-Type mismatch: declared={}, detected={}", declaredContentType, detectedType);
        }
    }
}
