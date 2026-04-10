package com.taskforge.tenant.dto.response;

import java.util.UUID;

public record TenantSummaryResponse(
                UUID id,
                String name,
                String role) {
}
