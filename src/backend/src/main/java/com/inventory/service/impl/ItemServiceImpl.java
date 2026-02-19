package com.inventory.service.impl;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ItemStatus;
import com.inventory.exception.FileValidationException;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.repository.specification.ItemSpecification;
import com.inventory.service.CustomFieldValidator;
import com.inventory.service.IItemService;

import org.springframework.lang.NonNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements IItemService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final ItemRepository itemRepository;
    private final ItemListRepository itemListRepository;
    private final CustomFieldValidator customFieldValidator;

    public ItemServiceImpl(ItemRepository itemRepository, ItemListRepository itemListRepository,
                           CustomFieldValidator customFieldValidator) {
        this.itemRepository = itemRepository;
        this.itemListRepository = itemListRepository;
        this.customFieldValidator = customFieldValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Item> getAllItems(@NonNull Pageable pageable,
        @NonNull ItemSearchCriteria criteria) {
        return itemRepository.findAll(ItemSpecification.withCriteria(criteria), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Item getItemById(@NonNull UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @Override
    @Transactional
    public Item createItem(@NonNull ItemRequest request, MultipartFile image) throws IOException {
        ItemList itemList = itemListRepository.findById(request.itemListId())
                .orElseThrow(() -> new ItemListNotFoundException(request.itemListId()));

        customFieldValidator.validate(itemList.getCustomFieldDefinitions(), request.customFieldValues());

        Item item = new Item();
        item.setName(request.name());
        item.setItemList(itemList);
        item.setStatus(request.status() != null ? request.status() : ItemStatus.TO_PREPARE);
        item.setStock(request.stock() != null ? request.stock() : 0);
        item.setCustomFieldValues(request.customFieldValues());

        if (image != null && !image.isEmpty()) {
            validateImage(image);
            item.setImageData(image.getBytes());
            item.setContentType(image.getContentType());
        }

        return itemRepository.save(item);
    }

    @Override
    @Transactional
    public Item updateItem(@NonNull UUID id, ItemRequest request, MultipartFile image) throws IOException {
        Item item = getItemById(id);

        ItemList itemList = itemListRepository.findById(request.itemListId())
                .orElseThrow(() -> new ItemListNotFoundException(request.itemListId()));

        customFieldValidator.validate(itemList.getCustomFieldDefinitions(), request.customFieldValues());

        item.setName(request.name());
        item.setItemList(itemList);
        item.setStatus(request.status() != null ? request.status() : item.getStatus());
        item.setStock(request.stock() != null ? request.stock() : item.getStock());
        item.setCustomFieldValues(request.customFieldValues());

        if (image != null && !image.isEmpty()) {
            validateImage(image);
            item.setImageData(image.getBytes());
            item.setContentType(image.getContentType());
        }

        return itemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteItem(@NonNull UUID id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException(id);
        }
        itemRepository.deleteById(id);
    }

    @Override
    public DashboardStats getDashboardStats() {
        long totalItems = itemRepository.count();

        Map<String, Long> statusCounts = itemRepository.countByStatus().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? ((ItemStatus) row[0]).name() : "Unknown",
                        row -> (Long) row[1]));

        Map<String, Long> categoryCounts = itemRepository.countByCategory().stream()
                .collect(Collectors.toMap(
                        row -> row[0] != null ? (String) row[0] : "Uncategorized",
                        row -> (Long) row[1]));

        return new DashboardStats(totalItems, statusCounts, categoryCounts);
    }

    private void validateImage(MultipartFile image) {
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new FileValidationException("File size exceeds maximum allowed size of 10MB");
        }
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileValidationException("Invalid file type. Allowed types: JPEG, PNG, GIF, WebP");
        }
    }
}
