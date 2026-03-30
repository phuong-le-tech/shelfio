package com.inventory.service;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Validated
public interface IItemService {

    // Read operations
    Page<Item> getAllItems(@NonNull Pageable pageable, @NonNull ItemSearchCriteria criteria);

    Optional<Item> getItemById(@NonNull UUID id);

    Optional<Item> getItemByBarcode(@NonNull String barcode);

    DashboardStats getDashboardStats();

    // Write operations
    Item createItem(@NonNull ItemRequest request, MultipartFile image) throws IOException;

    Item updateItem(@NonNull UUID id, ItemRequest request, MultipartFile image) throws IOException;

    void deleteItem(@NonNull UUID id);

    void deleteItems(@NonNull List<UUID> ids);
}
