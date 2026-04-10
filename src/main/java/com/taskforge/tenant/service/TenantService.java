package com.taskforge.tenant.service;

import com.taskforge.tenant.dto.response.TenantMemberResponse;

import com.taskforge.tenant.dto.response.TenantResponse;
import com.taskforge.tenant.dto.response.TenantSummaryResponse;
import com.taskforge.tenant.model.Tenant;

import java.util.List;
import java.util.UUID;

public interface TenantService {

    TenantResponse createTenant(String name, UUID creatorUserId);

    TenantResponse updateTenant(UUID tenantId, String name, UUID actorUserId);

    List<TenantSummaryResponse> getMyTenants(UUID userId);

    TenantResponse getTenantProfile(UUID tenantId, UUID userId);

    List<TenantMemberResponse> getTenantMembers(UUID tenantId, UUID actorUserId);

    void inviteMember(UUID tenantId, String email, String role, UUID actorUserId);

    void removeMember(UUID tenantId, UUID userIdToRemove, UUID actorUserId);

    List<TenantResponse> getUserTenants(UUID userId);

    UUID acceptInvitation(String token, UUID actorUserId, String email, String displayName);

    com.taskforge.tenant.dto.response.InvitationDetailsResponse getInvitationDetails(String token);

    List<com.taskforge.membership.tenant.model.TenantInvitation> getInvitations(UUID tenantId, UUID actorUserId);

    void revokeInvitation(UUID tenantId, UUID invitationId, UUID actorUserId);

    void revokeInvitation(UUID invitationId, UUID actorUserId);
}
