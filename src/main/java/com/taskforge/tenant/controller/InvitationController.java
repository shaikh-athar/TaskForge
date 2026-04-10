package com.taskforge.tenant.controller;

import com.taskforge.tenant.dto.request.AcceptInvitationRequest;
import com.taskforge.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final TenantService tenantService;

    @PostMapping("/accept")
    public ResponseEntity<?> acceptInvitation(
            @RequestBody @Valid AcceptInvitationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        UUID tenantId = tenantService.acceptInvitation(request.getToken(), userId, email, name);

        return ResponseEntity.ok(Map.of("tenantId", tenantId));
    }

    @org.springframework.web.bind.annotation.GetMapping("/{token}")
    public ResponseEntity<?> getInvitationDetails(@org.springframework.web.bind.annotation.PathVariable String token) {
        return ResponseEntity.ok(tenantService.getInvitationDetails(token));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{invitationId}")
    public ResponseEntity<?> revokeInvitation(
            @org.springframework.web.bind.annotation.PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        tenantService.revokeInvitation(invitationId, userId);
        return ResponseEntity.noContent().build();
    }
}
