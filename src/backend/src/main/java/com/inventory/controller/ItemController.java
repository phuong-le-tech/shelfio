package com.inventory.controller;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.dto.response.ItemResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.model.Item;
import com.inventory.service.IItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private final IItemService itemService;

    public ItemController(IItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public PageResponse<ItemResponse> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @ModelAttribute @NonNull ItemSearchCriteria criteria) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Item> items = itemService.getAllItems(pageable, criteria);

        Page<ItemResponse> responsePage = items.map(ItemResponse::fromEntity);
        return PageResponse.from(responsePage);
    }

    @GetMapping("/{id}")
    public ItemResponse getItem(@PathVariable @NonNull UUID id) {
        return ItemResponse.fromEntity(itemService.getItemById(id));
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponse> createItem(
            @RequestPart("data") @Valid @NonNull ItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        Item item = itemService.createItem(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.fromEntity(item));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse updateItem(
            @PathVariable @NonNull UUID id,
            @RequestPart("data") @Valid @NonNull ItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        Item item = itemService.updateItem(id, request, image);
        return ItemResponse.fromEntity(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable @NonNull UUID id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public DashboardStats getDashboardStats() {
        return itemService.getDashboardStats();
    }
}
