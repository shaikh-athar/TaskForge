package com.taskforge.tenant.service.impl;

import com.taskforge.common.exception.ApiException;
import com.taskforge.membership.tenant.model.TenantMembership;
import com.taskforge.membership.tenant.repository.TenantMembershipRepository;
import com.taskforge.tenant.dto.response.TenantResponse;
import com.taskforge.tenant.dto.response.TenantSummaryResponse;
import com.taskforge.tenant.model.Tenant;
import com.taskforge.tenant.repository.TenantRepository;
import com.taskforge.tenant.service.TenantService;
import com.taskforge.common.security.TenantSecurityService;
import com.taskforge.common.security.TenantSecurityService.Permission;
import com.taskforge.membership.tenant.model.TenantInvitation;
import com.taskforge.membership.tenant.repository.TenantInvitationRepository;
import com.taskforge.tenant.dto.response.TenantMemberResponse;
import com.taskforge.user.model.User;
import com.taskforge.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TenantServiceImpl implements TenantService {

        private final TenantRepository tenantRepository;
        private final TenantMembershipRepository membershipRepository;
        private final TenantSecurityService securityService;
        private final UserRepository userRepository;
        private final TenantInvitationRepository invitationRepository;
        private final com.taskforge.common.service.EmailService emailService;

        public TenantServiceImpl(
                        TenantRepository tenantRepository,
                        TenantMembershipRepository membershipRepository,
                        TenantSecurityService securityService,
                        UserRepository userRepository,
                        TenantInvitationRepository invitationRepository,
                        com.taskforge.common.service.EmailService emailService) {
                this.tenantRepository = tenantRepository;
                this.membershipRepository = membershipRepository;
                this.securityService = securityService;
                this.userRepository = userRepository;
                this.invitationRepository = invitationRepository;
                this.emailService = emailService;
        }

        @Override
        @Transactional
        public List<TenantResponse> getUserTenants(UUID userId) {
                // We don't need a specific tenant permission check here, as we are listing the
                // user's own tenants.
                // Logic: Find all memberships for this user, then extract the tenant info.

                // Note: For now, we will fetch all memberships and then get the tenant from
                // them.
                // Ideally, we would have a custom query in the repository.
                List<TenantMembership> memberships = membershipRepository.findAllByUserId(userId);

                return memberships.stream()
                                .map(membership -> {
                                        Tenant tenant = tenantRepository.findById(membership.getTenantId())
                                                        .orElseThrow(() -> new ApiException("TENANT_NOT_FOUND",
                                                                        "Tenant not found for membership"));
                                        return mapToResponse(tenant);
                                })
                                .collect(Collectors.toList());
        }

        private TenantResponse mapToResponse(Tenant tenant) {
                return new TenantResponse(
                                tenant.getId(),
                                tenant.getName(),
                                tenant.getCreatedAt().toInstant());
        }

        @Override
        @Transactional
        public TenantResponse createTenant(String name, UUID creatorUserId) {

                if (tenantRepository.existsByNameAndDeletedFalse(name)) {
                        throw new ApiException(
                                        "TENANT_ALREADY_EXISTS",
                                        "Tenant with this name already exists");
                }

                Tenant tenant = new Tenant();
                tenant.setName(name);
                Tenant saved = tenantRepository.save(tenant);

                TenantMembership membership = new TenantMembership();
                membership.setTenantId(saved.getId());
                membership.setUserId(creatorUserId);
                membership.setRole("OWNER");
                membershipRepository.save(membership);

                return new TenantResponse(
                                saved.getId(),
                                saved.getName(),
                                saved.getCreatedAt().toInstant());
        }

        @Override
        @Transactional
        public TenantResponse updateTenant(UUID tenantId, String name, UUID actorUserId) {

                // 1. Central Authorization Guard
                securityService.authorizeTenantAccess(tenantId, actorUserId, Permission.EDIT);

                // 2. Validate tenant exists
                Tenant tenant = tenantRepository.findByIdAndDeletedFalse(tenantId)
                                .orElseThrow(() -> new ApiException(
                                                "TENANT_NOT_FOUND",
                                                "Tenant not found"));

                // 3. Check for name conflict if name is changing
                if (!tenant.getName().equals(name) && tenantRepository.existsByNameAndDeletedFalse(name)) {
                        throw new ApiException(
                                        "TENANT_ALREADY_EXISTS",
                                        "Tenant with this name already exists");
                }

                tenant.setName(name);
                Tenant saved = tenantRepository.save(tenant);

                return new TenantResponse(
                                saved.getId(),
                                saved.getName(),
                                saved.getCreatedAt().toInstant());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TenantSummaryResponse> getMyTenants(UUID userId) {

                List<TenantMembership> memberships = membershipRepository.findByUserId(userId);

                if (memberships.isEmpty()) {
                        return List.of();
                }

                Map<UUID, Tenant> tenantMap = tenantRepository.findAllById(
                                memberships.stream()
                                                .map(TenantMembership::getTenantId)
                                                .toList())
                                .stream()
                                .collect(Collectors.toMap(Tenant::getId, t -> t));

                return memberships.stream()
                                .map(m -> {
                                        Tenant tenant = tenantMap.get(m.getTenantId());
                                        return new TenantSummaryResponse(
                                                        tenant.getId(),
                                                        tenant.getName(),
                                                        m.getRole());
                                })
                                .toList();
        }

        @Override
        @Transactional(readOnly = true)
        public TenantResponse getTenantProfile(UUID tenantId, UUID userId) {

                // 1. Central Authorization Guard
                securityService.authorizeTenantAccess(tenantId, userId, Permission.VIEW);

                Tenant tenant = tenantRepository.findByIdAndDeletedFalse(tenantId)
                                .orElseThrow(() -> new ApiException(
                                                "TENANT_NOT_FOUND",
                                                "Tenant does not exist"));

                return new TenantResponse(
                                tenant.getId(),
                                tenant.getName(),
                                tenant.getCreatedAt().toInstant());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TenantMemberResponse> getTenantMembers(UUID tenantId, UUID actorUserId) {

                // 1. Auth Guard (VIEW)
                securityService.authorizeTenantAccess(tenantId, actorUserId, Permission.VIEW);

                List<TenantMembership> memberships = membershipRepository.findByTenantId(tenantId);
                List<UUID> userIds = memberships.stream().map(TenantMembership::getUserId).toList();

                Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                                .collect(Collectors.toMap(User::getId, Function.identity()));

                return memberships.stream().map(m -> {
                        User user = userMap.get(m.getUserId());
                        return new TenantMemberResponse(
                                        m.getUserId(),
                                        user != null ? user.getDisplayName() : "Unknown",
                                        user != null ? user.getEmail() : "Unknown",
                                        m.getRole());
                }).toList();
        }

        @Override
        @Transactional
        public void inviteMember(UUID tenantId, String email, String role, UUID actorUserId) {

                // 1. Auth Guard (EDIT - Owner only)
                securityService.authorizeTenantAccess(tenantId, actorUserId, Permission.EDIT);

                // 2. Check if User exists
                userRepository.findByEmail(email).ifPresentOrElse(
                                user -> {
                                        // Check if already member
                                        if (membershipRepository.findByTenantIdAndUserId(tenantId, user.getId())
                                                        .isPresent()) {
                                                throw new ApiException("MEMBER_ALREADY_EXISTS",
                                                                "User is already a member");
                                        }
                                        // Add directly
                                        TenantMembership membership = new TenantMembership();
                                        membership.setTenantId(tenantId);
                                        membership.setUserId(user.getId());
                                        membership.setRole(role);
                                        membershipRepository.save(membership);

                                        // Send Email
                                        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
                                        emailService.sendMembershipAddedEmail(email, tenant.getName());
                                },
                                () -> {
                                        // 3. Create Invitation if user missing
                                        TenantInvitation invitation = new TenantInvitation();
                                        invitation.setTenantId(tenantId);
                                        invitation.setEmail(email);
                                        invitation.setRole(role);
                                        invitation.setToken(UUID.randomUUID().toString());
                                        invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
                                        invitationRepository.save(invitation);

                                        // Send Email
                                        // TODO: Use real frontend URL
                                        String inviteUrl = "http://localhost:3000/accept-invite?token="
                                                        + invitation.getToken();
                                        emailService.sendInvitationEmail(email, inviteUrl);

                                });
        }

        @Override
        @Transactional
        public UUID acceptInvitation(String token, UUID actorUserId, String email, String displayName) {
                TenantInvitation invitation = invitationRepository.findByToken(token)
                                .orElseThrow(() -> new ApiException("INVITATION_INVALID",
                                                "Invalid or expired invitation token"));

                // 2. Check Expiration
                if (invitation.getExpiresAt().isBefore(Instant.now())) {
                        invitationRepository.delete(invitation);
                        throw new ApiException("INVITATION_EXPIRED", "Invitation has expired");
                }

                // 2b. ensure User Exists (JIT Provisioning)
                // If the user registered via Keycloak but hasn't been synced to our DB yet,
                // we create them here.
                User actor = userRepository.findById(actorUserId).orElseGet(() -> {
                        User newUser = new User();
                        newUser.setId(actorUserId);
                        newUser.setEmail(email != null ? email : invitation.getEmail()); // Fallback to invite email if
                                                                                         // null
                        newUser.setDisplayName(displayName != null ? displayName : email);
                        return userRepository.save(newUser);
                });

                // 2c. Verify Email Match
                if (!actor.getEmail().equalsIgnoreCase(invitation.getEmail())) {
                        throw new ApiException("INVITATION_EMAIL_MISMATCH",
                                        "This invitation is for " + invitation.getEmail() + ". You are logged in as "
                                                        + actor.getEmail());
                }

                // 3. Check if user is already a member
                if (membershipRepository.findByTenantIdAndUserId(invitation.getTenantId(), actorUserId).isPresent()) {
                        invitationRepository.delete(invitation);
                        return invitation.getTenantId();
                }

                // 4. Create Membership
                TenantMembership membership = new TenantMembership();
                membership.setTenantId(invitation.getTenantId());
                membership.setUserId(actorUserId);
                membership.setRole(invitation.getRole());
                membershipRepository.save(membership);

                // 5. Delete Invitation
                invitationRepository.delete(invitation);

                return invitation.getTenantId();
        }

        @Override
        @Transactional
        public void removeMember(UUID tenantId, UUID userIdToRemove, UUID actorUserId) {
                // 1. Auth Guard (EDIT - Owner only)
                securityService.authorizeTenantAccess(tenantId, actorUserId, Permission.EDIT);

                if (userIdToRemove.equals(actorUserId)) {
                        throw new ApiException("CANNOT_REMOVE_SELF", "Cannot remove self",
                                        org.springframework.http.HttpStatus.BAD_REQUEST);
                }

                // Get User details for email
                User user = userRepository.findById(userIdToRemove)
                                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found",
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                // Get Tenant details for email
                Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

                membershipRepository.deleteByTenantIdAndUserId(tenantId, userIdToRemove);

                // Send Email
                emailService.sendMemberRemovedEmail(user.getEmail(), tenant.getName());
        }

        @Override
        @Transactional(readOnly = true)
        public List<TenantInvitation> getInvitations(UUID tenantId, UUID actorUserId) {
                // 1. Auth Guard (EDIT - Owner/Admin only)
                securityService.authorizeTenantAccess(tenantId, actorUserId, Permission.EDIT);

                // 2. Return all invitations for this tenant
                return invitationRepository.findByTenantId(tenantId);
        }

        @Override
        @Transactional(readOnly = true)
        public com.taskforge.tenant.dto.response.InvitationDetailsResponse getInvitationDetails(String token) {
                TenantInvitation invitation = invitationRepository.findByToken(token)
                                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND", "Invitation not found"));

                if (invitation.getExpiresAt().isBefore(Instant.now())) {
                        throw new ApiException("INVITATION_EXPIRED", "Invitation has expired");
                }

                Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                                .orElseThrow(() -> new ApiException("TENANT_NOT_FOUND", "Tenant not found"));

                // If inviterId exists, fetch details. Otherwise (if older invites), use
                // "Unknown".
                // Assuming inviterId was added later or might be null.
                String inviterName = "Someone";
                String inviterEmail = "";

                // TODO: Add inviter_id to tenant_invitations table to track this.
                // For now, we return empty/default.

                return com.taskforge.tenant.dto.response.InvitationDetailsResponse.builder()
                                .email(invitation.getEmail())
                                .role(invitation.getRole())
                                .tenantName(tenant.getName())
                                .inviterName(inviterName)
                                .inviterEmail(inviterEmail)
                                .build();
        }

        @Override
        @Transactional
        public void revokeInvitation(UUID tenantId, UUID invitationId, UUID actorUserId) {
                // 1. Auth Guard (EDIT - Owner only)
                securityService.authorizeTenantAccess(tenantId, actorUserId, Permission.EDIT);

                // 2. Find Invitation
                TenantInvitation invitation = invitationRepository.findById(invitationId)
                                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND", "Invitation not found",
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                // 3. Verify Tenant Match
                if (!invitation.getTenantId().equals(tenantId)) {
                        throw new ApiException("INVITATION_INVALID", "Invitation does not belong to this tenant",
                                        org.springframework.http.HttpStatus.BAD_REQUEST);
                }

                // 4. Get Tenant Name for Email
                Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();

                // 5. Delete Invitation
                invitationRepository.delete(invitation);

                // 6. Send Email
                emailService.sendInvitationRevokedEmail(invitation.getEmail(), tenant.getName());
        }

        @Override
        @Transactional
        public void revokeInvitation(UUID invitationId, UUID actorUserId) {
                // 1. Find Invitation first to get Tenant ID
                TenantInvitation invitation = invitationRepository.findById(invitationId)
                                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND", "Invitation not found",
                                                org.springframework.http.HttpStatus.NOT_FOUND));

                // 2. Delegate to the main method
                revokeInvitation(invitation.getTenantId(), invitationId, actorUserId);
        }
}
