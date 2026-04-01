package com.inventory.controller;

import com.inventory.dto.request.InviteRequest;
import com.inventory.dto.request.UpdateMemberRoleRequest;
import com.inventory.dto.request.WorkspaceRequest;
import com.inventory.dto.response.ActivityEventResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.dto.response.WorkspaceInvitationResponse;
import com.inventory.dto.response.WorkspaceMemberResponse;
import com.inventory.dto.response.WorkspaceResponse;
import com.inventory.enums.WorkspaceRole;
import com.inventory.model.Workspace;
import com.inventory.model.WorkspaceMember;
import com.inventory.security.SecurityUtils;
import com.inventory.service.IActivityService;
import com.inventory.service.IWorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final IWorkspaceService workspaceService;
    private final IActivityService activityService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces() {
        UUID currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.inventory.exception.UnauthorizedException("Not authenticated"));
        List<Workspace> workspaces = workspaceService.getMyWorkspaces();

        List<UUID> workspaceIds = workspaces.stream().map(Workspace::getId).toList();
        Map<UUID, Long> listCounts = workspaceService.batchCountListsByWorkspaceIds(workspaceIds);
        Map<UUID, Long> memberCounts = workspaceService.batchCountMembersByWorkspaceIds(workspaceIds);
        Map<UUID, WorkspaceRole> roles = workspaceService.batchFetchRoles(workspaceIds, currentUserId);

        List<WorkspaceResponse> responses = workspaces.stream()
                .map(w -> {
                    WorkspaceRole role = roles.getOrDefault(w.getId(), WorkspaceRole.VIEWER);
                    long listCount = listCounts.getOrDefault(w.getId(), 0L);
                    long memberCount = memberCounts.getOrDefault(w.getId(), 0L);
                    return WorkspaceResponse.fromEntity(w, role, memberCount, listCount);
                })
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspaceById(@PathVariable UUID id) {
        UUID currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.inventory.exception.UnauthorizedException("Not authenticated"));
        Workspace workspace = workspaceService.getWorkspaceById(id);
        long listCount = workspaceService.countListsByWorkspaceId(id);
        long memberCount = workspaceService.countMembersByWorkspaceId(id);
        WorkspaceRole role = workspaceService.findUserRole(id, currentUserId);
        return ResponseEntity.ok(WorkspaceResponse.fromEntity(workspace, role, memberCount, listCount));
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(@Valid @RequestBody WorkspaceRequest request) {
        Workspace workspace = workspaceService.createWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WorkspaceResponse.fromEntity(workspace, WorkspaceRole.OWNER, 1, 0));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable UUID id, @Valid @RequestBody WorkspaceRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.inventory.exception.UnauthorizedException("Not authenticated"));
        Workspace workspace = workspaceService.updateWorkspace(id, request);
        long listCount = workspaceService.countListsByWorkspaceId(id);
        long memberCount = workspaceService.countMembersByWorkspaceId(id);
        WorkspaceRole role = workspaceService.findUserRole(id, currentUserId);
        return ResponseEntity.ok(WorkspaceResponse.fromEntity(workspace, role, memberCount, listCount));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable UUID id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final java.util.Set<String> VALID_ENTITY_TYPES = java.util.Set.of("ITEM", "LIST", "MEMBER");

    @GetMapping("/{id}/activity")
    public ResponseEntity<PageResponse<ActivityEventResponse>> getActivityFeed(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType) {
        if (entityType != null && !VALID_ENTITY_TYPES.contains(entityType)) {
            return ResponseEntity.badRequest().build();
        }
        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        workspaceService.getWorkspaceById(id); // membership check
        Page<ActivityEventResponse> events = activityService.getActivity(
                id, null, entityType, null, null, PageRequest.of(page, cappedSize));
        return ResponseEntity.ok(PageResponse.from(events));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMemberResponse>> getMembers(@PathVariable UUID id) {
        List<WorkspaceMember> members = workspaceService.getMembers(id);
        List<WorkspaceMemberResponse> responses = members.stream()
                .map(WorkspaceMemberResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/invitations")
    public ResponseEntity<Void> inviteMember(
            @PathVariable UUID id, @Valid @RequestBody InviteRequest request) {
        workspaceService.inviteMember(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id, @PathVariable UUID userId) {
        workspaceService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable UUID id, @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        workspaceService.updateMemberRole(id, userId, request);
        return ResponseEntity.noContent().build();
    }

    // Token-based endpoints (for email links)
    @PostMapping("/invitations/{token}/accept")
    public ResponseEntity<Void> acceptInvitation(@PathVariable String token) {
        workspaceService.acceptInvitation(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/{token}/decline")
    public ResponseEntity<Void> declineInvitation(@PathVariable String token) {
        workspaceService.declineInvitation(token);
        return ResponseEntity.ok().build();
    }

    // ID-based endpoints (for in-app actions — no token exposure needed)
    @PostMapping("/invitations/id/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitationById(@PathVariable UUID invitationId) {
        workspaceService.acceptInvitationById(invitationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/id/{invitationId}/decline")
    public ResponseEntity<Void> declineInvitationById(@PathVariable UUID invitationId) {
        workspaceService.declineInvitationById(invitationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<List<WorkspaceInvitationResponse>> getPendingInvitations() {
        var invitations = workspaceService.getPendingInvitations();
        List<WorkspaceInvitationResponse> responses = invitations.stream()
                .map(WorkspaceInvitationResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
