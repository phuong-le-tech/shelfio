package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.dto.response.ItemResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.service.IItemService;
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

import com.inventory.exception.RateLimitExceededException;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "name", "status", "stock");

    private final IItemService itemService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ApiRateLimiter uploadRateLimiter;

    public ItemController(IItemService itemService, ObjectMapper objectMapper, Validator validator,
                          @Qualifier("uploadRateLimiter") ApiRateLimiter uploadRateLimiter) {
        this.itemService = itemService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.uploadRateLimiter = uploadRateLimiter;
    }

    @GetMapping
    public PageResponse<ItemResponse> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @ModelAttribute @NonNull ItemSearchCriteria criteria) {

        size = Math.min(Math.max(size, 1), 100);
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Item> items = itemService.getAllItems(pageable, criteria);

        Page<ItemResponse> responsePage = items.map(ItemResponse::fromEntity);
        return PageResponse.from(responsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable @NonNull UUID id) {
        return itemService.getItemById(id)
                .map(item -> ResponseEntity.ok(ItemResponse.fromEntity(item)))
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getItemImage(@PathVariable @NonNull UUID id) {
        Item item = itemService.getItemById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        if (item.getImageData() == null || item.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Objects.requireNonNull(item.getContentType(), "Content type not found")))
                .body(item.getImageData());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponse> createItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("data") @NonNull String data,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        if (!uploadRateLimiter.tryAcquire("upload:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many upload requests. Please try again later.");
        }
        ItemRequest request = objectMapper.readValue(data, ItemRequest.class);
        Objects.requireNonNull(request, "Item request data must not be null");
        Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        Item item = itemService.createItem(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.fromEntity(item));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse updateItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @NonNull UUID id,
            @RequestParam("data") @NonNull String data,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        if (!uploadRateLimiter.tryAcquire("upload:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many upload requests. Please try again later.");
        }
        ItemRequest request = objectMapper.readValue(data, ItemRequest.class);
        Objects.requireNonNull(request, "Item request data must not be null");
        Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
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
