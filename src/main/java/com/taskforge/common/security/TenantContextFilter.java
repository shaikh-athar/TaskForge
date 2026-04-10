package com.taskforge.common.security;

import com.taskforge.common.tenant.TenantContext;
import com.taskforge.membership.tenant.repository.TenantMembershipRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantMembershipRepository membershipRepository;

    public TenantContextFilter(TenantMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.debug(">>> Entering TenantContextFilter for [{} {}]", method, path);

        if (isExcludedPath(path, method)) {
            log.debug("Skipping tenant validation for excluded path: {}", path);
            chain.doFilter(request, response);
            return;
        }

        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            log.debug(">>> Attempting to authenticate [{}]", auth);
            if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
                log.warn("No JWT found in SecurityContext for path: {}. Proceeding without tenant context.", path);
                chain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(jwt.getSubject());
            UUID tenantId = null;
            boolean skipDbCheck = false;

            if (jwt.hasClaim("tenant_id")) {
                String tenantIdClaim = jwt.getClaimAsString("tenant_id");
                if (tenantIdClaim != null && !tenantIdClaim.isBlank()) {
                    tenantId = UUID.fromString(tenantIdClaim);
                    skipDbCheck = true;
                    log.debug("Tenant ID [{}] resolved from JWT claim for User [{}] (DB Check Skipped)", tenantId,
                            userId);
                }
            }

            if (tenantId == null) {
                String tenantHeader = request.getHeader("X-Tenant-Id");
                if (tenantHeader != null && !tenantHeader.isBlank()) {
                    tenantId = UUID.fromString(tenantHeader);
                    log.debug("Tenant ID [{}] resolved from X-Tenant-Id header for User [{}]", tenantId, userId);
                }
            }

            if (tenantId != null) {
                if (!skipDbCheck) {
                    boolean isMember = membershipRepository
                            .findByTenantIdAndUserId(tenantId, userId)
                            .isPresent();

                    if (!isMember) {
                        log.error("Access Denied: User [{}] is NOT a member of Tenant [{}]", userId, tenantId);
                        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN,
                                "User not member of tenant");
                        return;
                    }
                }

                log.info("Context Set: User [{}] successfully verified for Tenant [{}]", userId, tenantId);
                TenantContext.set(tenantId);
                request.setAttribute("tenantId", tenantId);
            } else {
                log.info("No Tenant ID provided in request for User [{}]", userId);
            }

            chain.doFilter(request, response);

        } catch (IllegalArgumentException e) {
            log.error("UUID parsing failed: {}", e.getMessage());
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid Tenant or User ID format");
        } catch (Exception e) {
            log.error("Unexpected error in TenantContextFilter: ", e);
            chain.doFilter(request, response);
        } finally {
            log.debug("<<< Exiting TenantContextFilter. Clearing context.");
            TenantContext.clear();
        }
    }

    private boolean isExcludedPath(String path, String method) {
        return (path.equals("/api/v1/tenants") && "GET".equalsIgnoreCase(method)) ||
                path.startsWith("/api/v1/invitations");
    }
}