package com.inventory.controller;

import com.inventory.dto.request.CreateUserRequest;
import com.inventory.dto.request.UpdateRoleRequest;
import com.inventory.dto.response.PageResponse;
import com.inventory.dto.response.UserResponse;
import com.inventory.model.User;
import com.inventory.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "email", "role");

    private final IUserService userService;

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
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

        Page<User> usersPage = userService.getAllUsers(pageable);
        Page<UserResponse> responsePage = usersPage.map(UserResponse::fromEntity);

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody @NonNull CreateUserRequest request) {
        User savedUser = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserResponse.fromEntity(savedUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable @NonNull UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody @NonNull UpdateRoleRequest request
    ) {
        User savedUser = userService.updateUserRole(id, request.role());
        return ResponseEntity.ok(UserResponse.fromEntity(savedUser));
    }
}
