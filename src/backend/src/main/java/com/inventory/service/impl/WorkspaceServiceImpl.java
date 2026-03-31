package com.inventory.service.impl;

import com.inventory.dto.request.InviteRequest;
import com.inventory.dto.request.UpdateMemberRoleRequest;
import com.inventory.dto.request.WorkspaceRequest;
import com.inventory.enums.InvitationStatus;
import com.inventory.enums.Role;
import com.inventory.enums.WorkspaceRole;
import com.inventory.exception.*;
import com.inventory.model.User;
import com.inventory.model.Workspace;
import com.inventory.model.WorkspaceInvitation;
import com.inventory.model.WorkspaceMember;
import com.inventory.repository.*;
import com.inventory.repository.ItemListRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.enums.ActivityEventType;
import com.inventory.service.EmailSender;
import com.inventory.service.IActivityService;
import com.inventory.service.IWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements IWorkspaceService {

    private static final int INVITATION_EXPIRY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final ItemListRepository itemListRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final EmailSender emailSender;
    private final IActivityService activityService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private UUID requireCurrentUserId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workspace> getMyWorkspaces() {
        UUID userId = requireCurrentUserId();
        return workspaceRepository.findByMembersUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Workspace getWorkspaceById(UUID id) {
        if (securityUtils.isAdmin()) {
            return workspaceRepository.findById(id)
                    .orElseThrow(() -> new WorkspaceNotFoundException(id));
        }
        UUID userId = requireCurrentUserId();
        return workspaceRepository.findByIdAndMembersUserId(id, userId)
                .orElseThrow(() -> new WorkspaceNotFoundException(id));
    }

    @Override
    @Transactional
    public Workspace createWorkspace(WorkspaceRequest request) {
        UUID userId = requireCurrentUserId();
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Free users can only have their default workspace
        if (user.getRole() == Role.USER) {
            throw new WorkspaceLimitExceededException(
                    "Free plan limited to 1 workspace. Upgrade to Premium to create additional workspaces.");
        }

        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspace.setOwner(user);
        workspace.setDefault(false);
        workspace = workspaceRepository.save(workspace);

        // Add creator as OWNER member
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        log.info("User {} created workspace {}", userId, workspace.getId());
        return workspace;
    }

    @Override
    @Transactional
    public Workspace updateWorkspace(UUID id, WorkspaceRequest request) {
        Workspace workspace = getWorkspaceById(id);
        requireOwnerRole(workspace);

        workspace.setName(request.name());
        return workspaceRepository.save(workspace);
    }

    @Override
    @Transactional
    public void deleteWorkspace(UUID id) {
        Workspace workspace = getWorkspaceById(id);
        requireOwnerRole(workspace);

        if (workspace.isDefault()) {
            throw new WorkspaceAccessDeniedException("Cannot delete the default workspace");
        }

        log.info("User {} deleting workspace {} ({})", requireCurrentUserId(), id, workspace.getName());
        workspaceRepository.delete(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMember> getMembers(UUID workspaceId) {
        // Any member can view the member list
        getWorkspaceById(workspaceId);
        return workspaceMemberRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional
    public void inviteMember(UUID workspaceId, InviteRequest request) {
        Workspace workspace = getWorkspaceById(workspaceId);
        requireOwnerRole(workspace);

        // Cannot invite as OWNER
        if (request.role() == WorkspaceRole.OWNER) {
            throw new WorkspaceAccessDeniedException("Cannot invite as OWNER");
        }

        // Check if already a member
        User existingUser = userRepository.findByEmail(request.email()).orElse(null);
        if (existingUser != null && workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, existingUser.getId())) {
            throw new WorkspaceAccessDeniedException("User is already a member of this workspace");
        }

        // Check for existing pending invitation
        if (workspaceInvitationRepository.existsByWorkspaceIdAndEmailAndStatus(
                workspaceId, request.email(), InvitationStatus.PENDING)) {
            throw new WorkspaceAccessDeniedException("An invitation is already pending for this email");
        }

        // Remove any previously declined/expired invitation to allow re-invitation
        // (unique constraint on workspace_id + email would otherwise block the insert)
        workspaceInvitationRepository.deleteByWorkspaceIdAndEmailAndStatusNot(
                workspaceId, request.email(), InvitationStatus.PENDING);

        UUID userId = requireCurrentUserId();
        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String token = generateToken();

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspace(workspace);
        invitation.setEmail(request.email());
        invitation.setRole(request.role());
        invitation.setToken(token);
        invitation.setInvitedBy(inviter);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(INVITATION_EXPIRY_DAYS));
        workspaceInvitationRepository.save(invitation);

        // Send invitation email
        String inviteUrl = frontendUrl + "/workspaces/invitations/" + token;
        String subject = "Invitation à rejoindre l'espace de travail \"" + workspace.getName() + "\"";
        String html = """
                <p>Vous avez été invité(e) à rejoindre l'espace de travail <strong>%s</strong> sur Shelfio.</p>
                <p>Rôle : <strong>%s</strong></p>
                <p><a href="%s">Accepter l'invitation</a></p>
                <p>Cette invitation expire dans %d jours.</p>
                """.formatted(workspace.getName(), request.role().name(), inviteUrl, INVITATION_EXPIRY_DAYS);
        emailSender.send(request.email(), subject, html);

        log.info("User {} invited {} to workspace {} as {}", userId, request.email(), workspaceId, request.role());
    }

    @Override
    @Transactional
    public void acceptInvitation(String token) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid invitation token"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotFoundException("Invitation is no longer pending");
        }

        processAcceptInvitation(invitation);
        log.info("User {} accepted invitation to workspace {}", requireCurrentUserId(), invitation.getWorkspace().getId());
    }

    @Override
    @Transactional
    public void declineInvitation(String token) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid invitation token"));
        processDeclineInvitation(invitation);
        log.info("User {} declined invitation {}", requireCurrentUserId(), invitation.getId());
    }

    @Override
    @Transactional
    public void acceptInvitationById(UUID invitationId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid invitation"));
        processAcceptInvitation(invitation);
        log.info("User {} accepted invitation {} to workspace {}", requireCurrentUserId(), invitationId, invitation.getWorkspace().getId());
    }

    @Override
    @Transactional
    public void declineInvitationById(UUID invitationId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvitationNotFoundException("Invalid invitation"));
        processDeclineInvitation(invitation);
        log.info("User {} declined invitation {}", requireCurrentUserId(), invitationId);
    }

    private void processAcceptInvitation(WorkspaceInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotFoundException("Invitation is no longer pending");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            workspaceInvitationRepository.save(invitation);
            throw new InvitationNotFoundException("Invitation has expired");
        }

        UUID userId = requireCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new WorkspaceAccessDeniedException("This invitation was sent to a different email address");
        }

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(invitation.getWorkspace().getId(), userId)) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            workspaceInvitationRepository.save(invitation);
            return;
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(invitation.getWorkspace());
        member.setUser(user);
        member.setRole(invitation.getRole());
        workspaceMemberRepository.save(member);
        activityService.record(invitation.getWorkspace().getId(),
                ActivityEventType.MEMBER_ADDED, "MEMBER", user.getId(), user.getEmail());

        invitation.setStatus(InvitationStatus.ACCEPTED);
        workspaceInvitationRepository.save(invitation);
    }

    private void processDeclineInvitation(WorkspaceInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationNotFoundException("Invitation is no longer pending");
        }

        UUID userId = requireCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new WorkspaceAccessDeniedException("This invitation was sent to a different email address");
        }

        invitation.setStatus(InvitationStatus.DECLINED);
        workspaceInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void removeMember(UUID workspaceId, UUID userId) {
        Workspace workspace = getWorkspaceById(workspaceId);
        requireOwnerRole(workspace);

        // Cannot remove the owner
        if (workspace.getOwner().getId().equals(userId)) {
            throw new WorkspaceAccessDeniedException("Cannot remove the workspace owner");
        }

        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new UserNotFoundException(userId);
        }

        UUID currentUserId = requireCurrentUserId();
        workspaceMemberRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId);
        activityService.record(workspaceId, ActivityEventType.MEMBER_REMOVED, "MEMBER", userId, null);
        log.info("User {} removed user {} from workspace {}", currentUserId, userId, workspaceId);
    }

    @Override
    @Transactional
    public void updateMemberRole(UUID workspaceId, UUID userId,
                                  UpdateMemberRoleRequest request) {
        Workspace workspace = getWorkspaceById(workspaceId);
        requireOwnerRole(workspace);

        // Cannot change owner's role
        if (workspace.getOwner().getId().equals(userId)) {
            throw new WorkspaceAccessDeniedException("Cannot change the workspace owner's role");
        }

        // Cannot set role to OWNER
        if (request.role() == WorkspaceRole.OWNER) {
            throw new WorkspaceAccessDeniedException("Cannot assign OWNER role");
        }

        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));

        member.setRole(request.role());
        workspaceMemberRepository.save(member);
        activityService.record(workspaceId, ActivityEventType.MEMBER_ROLE_CHANGED, "MEMBER", userId, null);

        log.info("User {} updated role of user {} in workspace {} to {}",
                requireCurrentUserId(), userId, workspaceId, request.role());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceInvitation> getPendingInvitations() {
        UUID userId = requireCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return workspaceInvitationRepository.findByEmailAndStatus(user.getEmail(), InvitationStatus.PENDING);
    }

    @Override
    @Transactional
    public Workspace createDefaultWorkspace(User user) {
        Workspace workspace = new Workspace();
        workspace.setName("Personnel");
        workspace.setOwner(user);
        workspace.setDefault(true);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        log.info("Created default workspace for user {}", user.getId());
        return workspace;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> batchCountListsByWorkspaceIds(List<UUID> workspaceIds) {
        if (workspaceIds.isEmpty()) {
            return Map.of();
        }
        return itemListRepository.countListsByWorkspaceIds(workspaceIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> batchCountMembersByWorkspaceIds(List<UUID> workspaceIds) {
        if (workspaceIds.isEmpty()) {
            return Map.of();
        }
        return workspaceMemberRepository.countMembersByWorkspaceIds(workspaceIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, WorkspaceRole> batchFetchRoles(List<UUID> workspaceIds, UUID userId) {
        return workspaceMemberRepository.findByWorkspaceIdInAndUserId(workspaceIds, userId)
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getWorkspace().getId(),
                        WorkspaceMember::getRole
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceRole findUserRole(UUID workspaceId, UUID userId) {
        if (userId == null) {
            return WorkspaceRole.VIEWER;
        }
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole)
                .orElse(WorkspaceRole.VIEWER);
    }

    @Override
    @Transactional(readOnly = true)
    public long countListsByWorkspaceId(UUID workspaceId) {
        return itemListRepository.countByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMembersByWorkspaceId(UUID workspaceId) {
        return workspaceMemberRepository.countByWorkspaceId(workspaceId);
    }

    private void requireOwnerRole(Workspace workspace) {
        if (securityUtils.isAdmin()) {
            return;
        }
        UUID userId = requireCurrentUserId();
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspace.getId(), userId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspace.getId()));
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new WorkspaceAccessDeniedException("Only the workspace owner can perform this action");
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
