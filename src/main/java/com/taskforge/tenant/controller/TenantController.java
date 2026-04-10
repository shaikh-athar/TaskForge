package com.taskforge.tenant.controller;

import com.taskforge.tenant.dto.request.CreateTenantRequest;
import com.taskforge.tenant.dto.request.UpdateTenantRequest;
import com.taskforge.tenant.dto.response.TenantResponse;

import com.taskforge.tenant.service.TenantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public TenantResponse createTenant(
            @RequestBody CreateTenantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return tenantService.createTenant(request.getName(), userId);
    }

    @PutMapping("/{tenantId}")
    public TenantResponse updateTenant(
            @PathVariable UUID tenantId,
            @RequestBody UpdateTenantRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return tenantService.updateTenant(tenantId, request.getName(), userId);
    }

    @GetMapping("/{tenantId}")
    public TenantResponse getTenant(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return tenantService.getTenantProfile(tenantId, userId);
    }

    @GetMapping
    public java.util.List<com.taskforge.tenant.dto.response.TenantSummaryResponse> listTenants(
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return tenantService.getMyTenants(userId);
    }

    @GetMapping("/{tenantId}/invitations")
    public java.util.List<com.taskforge.membership.tenant.model.TenantInvitation> getInvitations(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return tenantService.getInvitations(tenantId, userId);
    }

}
