package com.taskforge.user.controller;

import com.taskforge.tenant.dto.response.TenantSummaryResponse;
import com.taskforge.tenant.service.TenantService;
import com.taskforge.user.dto.response.MeResponse;
import com.taskforge.user.model.User;
import com.taskforge.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserService userService;
    private final TenantService tenantService;


    public MeController(UserService userService, TenantService tenantService) {
        this.userService = userService;
        this.tenantService = tenantService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {

        User user = userService.getOrCreateUser(jwt);

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );
    }

    @GetMapping("/tenants")
    public List<TenantSummaryResponse> myTenants(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return tenantService.getMyTenants(userId);
    }
}
