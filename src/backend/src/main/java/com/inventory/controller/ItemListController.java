package com.inventory.controller;

import com.inventory.dto.request.ItemListRequest;
import com.inventory.dto.response.ItemListResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.model.ItemList;
import com.inventory.service.IItemListService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lists")
public class ItemListController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "name", "category");

    private final IItemListService itemListService;

    public ItemListController(IItemListService itemListService) {
        this.itemListService = itemListService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ItemListResponse>> getAllLists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        size = Math.min(Math.max(size, 1), 100);
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ItemList> listsPage = itemListService.getAllLists(pageable);
        Page<ItemListResponse> responsePage = listsPage.map(ItemListResponse::fromEntityWithoutItems);

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemListResponse> getListById(@PathVariable @NonNull UUID id) {
        ItemList itemList = itemListService.getListById(id);
        return ResponseEntity.ok(ItemListResponse.fromEntity(itemList));
    }

    @PostMapping
    public ResponseEntity<ItemListResponse> createList(@Valid @RequestBody @NonNull ItemListRequest request) {
        ItemList itemList = itemListService.createList(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItemListResponse.fromEntity(itemList));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ItemListResponse> updateList(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull ItemListRequest request
    ) {
        ItemList itemList = itemListService.updateList(id, request);
        return ResponseEntity.ok(ItemListResponse.fromEntity(itemList));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteList(@PathVariable @NonNull UUID id) {
        itemListService.deleteList(id);
        return ResponseEntity.noContent().build();
    }
}
