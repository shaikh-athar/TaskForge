package com.taskforge.tenant.controller;

import com.taskforge.tenant.dto.request.InviteMemberRequest;
import com.taskforge.tenant.dto.response.TenantMemberResponse;
import com.taskforge.tenant.service.TenantService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/members")
public class TenantMemberController {

    private final TenantService tenantService;

    public TenantMemberController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<TenantMemberResponse> getMembers(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return tenantService.getTenantMembers(tenantId, userId);
    }

    @PostMapping
    public void inviteMember(
            @PathVariable UUID tenantId,
            @RequestBody @jakarta.validation.Valid InviteMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        tenantService.inviteMember(tenantId, request.getEmail(), request.getRole(), userId);
    }

    @DeleteMapping("/{userIdToRemove}")
    public void removeMember(
            @PathVariable UUID tenantId,
            @PathVariable UUID userIdToRemove,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        tenantService.removeMember(tenantId, userIdToRemove, userId);
    }
}
