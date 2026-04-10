package com.taskforge.common.security;

import com.taskforge.common.exception.ApiException;
import com.taskforge.membership.tenant.model.TenantMembership;
import com.taskforge.membership.tenant.repository.TenantMembershipRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantSecurityService {

    private final TenantMembershipRepository membershipRepository;
    private final com.taskforge.membership.project.repository.ProjectMembershipRepository projectMembershipRepository;

    public TenantSecurityService(
            TenantMembershipRepository membershipRepository,
            com.taskforge.membership.project.repository.ProjectMembershipRepository projectMembershipRepository) {
        this.membershipRepository = membershipRepository;
        this.projectMembershipRepository = projectMembershipRepository;
    }

    public enum Permission {
        VIEW,
        EDIT
    }

    /**
     * Central Authorization Guard
     * 
     * @param tenantId   Target Tenant
     * @param userId     Actor
     * @param permission Required Permission
     */
    public void authorizeTenantAccess(UUID tenantId, UUID userId, Permission permission) {
        TenantMembership membership = membershipRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ApiException(
                        "TENANT_ACCESS_DENIED",
                        "You are not a member of this tenant",
                        org.springframework.http.HttpStatus.FORBIDDEN));

        if (permission == Permission.EDIT &&
                !("OWNER".equals(membership.getRole()) || "ADMIN".equals(membership.getRole()))) {
            throw new ApiException(
                    "TENANT_PERMISSION_DENIED",
                    "This action requires ADMIN or OWNER privileges",
                    org.springframework.http.HttpStatus.FORBIDDEN);
        }

    }

    /**
     * Project Authorization Guard
     *
     * @param projectId  Target Project
     * @param userId     Actor
     * @param permission Required Permission
     */
    /**
     * Project Authorization Guard
     *
     * @param tenantId   Context Tenant
     * @param projectId  Target Project
     * @param userId     Actor
     * @param permission Required Permission
     */
    public void authorizeProjectAccess(UUID tenantId, UUID projectId, UUID userId, Permission permission) {

        TenantMembership tenantMembership = membershipRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ApiException(
                        "TENANT_ACCESS_DENIED",
                        "You are not a member of this tenant",
                        org.springframework.http.HttpStatus.FORBIDDEN));

        if ("OWNER".equals(tenantMembership.getRole()) || "ADMIN".equals(tenantMembership.getRole())) {
            return;
        }

        com.taskforge.membership.project.model.ProjectMembership membership = projectMembershipRepository
                .findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException(
                        "PROJECT_ACCESS_DENIED",
                        "You are not a member of this project",
                        org.springframework.http.HttpStatus.FORBIDDEN));

        if (permission == Permission.EDIT) {
            if (membership.getRole() != com.taskforge.membership.project.model.ProjectMembership.ProjectRole.MANAGER) {
                throw new ApiException("PROJECT_PERMISSION_DENIED", "Only Project Managers can edit project settings",
                        org.springframework.http.HttpStatus.FORBIDDEN);
            }
        }
    }

}
