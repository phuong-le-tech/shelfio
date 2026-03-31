package com.inventory.controller;

import com.inventory.dto.request.ItemListRequest;
import com.inventory.dto.response.CsvExportResult;
import com.inventory.dto.response.ItemListResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.exception.RateLimitExceededException;
import com.inventory.model.ItemList;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.IItemListService;
import com.inventory.service.ImageStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/lists")
public class ItemListController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "name", "category");

    private final IItemListService itemListService;
    private final ImageStorageService imageStorageService;
    private final ApiRateLimiter duplicateRateLimiter;

    public ItemListController(IItemListService itemListService,
                              ImageStorageService imageStorageService,
                              @Qualifier("duplicateRateLimiter") ApiRateLimiter duplicateRateLimiter) {
        this.itemListService = itemListService;
        this.imageStorageService = imageStorageService;
        this.duplicateRateLimiter = duplicateRateLimiter;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ItemListResponse>> getAllLists(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) UUID workspaceId,
            @RequestParam(required = false) @jakarta.validation.constraints.Size(max = 255) String search
    ) {
        size = Math.min(Math.max(size, 1), 100);
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ItemList> listsPage = itemListService.getAllLists(pageable, workspaceId, search);
        Page<ItemListResponse> responsePage = listsPage.map(ItemListResponse::fromEntityWithoutItems);

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemListResponse> getListById(@PathVariable @NonNull UUID id) {
        ItemList itemList = itemListService.getListById(id);
        return ResponseEntity.ok(ItemListResponse.fromEntity(itemList, imageStorageService));
    }

    @PostMapping
    public ResponseEntity<ItemListResponse> createList(@Valid @RequestBody @NonNull ItemListRequest request) {
        ItemList itemList = itemListService.createList(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItemListResponse.fromEntity(itemList, imageStorageService));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ItemListResponse> updateList(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull ItemListRequest request
    ) {
        ItemList itemList = itemListService.updateList(id, request);
        return ResponseEntity.ok(ItemListResponse.fromEntity(itemList, imageStorageService));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteList(@PathVariable @NonNull UUID id) {
        itemListService.deleteList(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ItemListResponse> duplicateList(
            @PathVariable @NonNull UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!duplicateRateLimiter.tryAcquire("duplicate:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many duplication requests. Please try again later.");
        }
        ItemList duplicated = itemListService.duplicateList(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ItemListResponse.fromEntityWithoutItems(duplicated));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportList(@PathVariable @NonNull UUID id) {
        CsvExportResult result = itemListService.exportListAsCsv(id);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentLength(result.content().length);
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        headers.setPragma("no-cache");

        return ResponseEntity.ok().headers(headers).body(result.content());
    }
}
