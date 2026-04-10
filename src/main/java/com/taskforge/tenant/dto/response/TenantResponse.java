package com.taskforge.tenant.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        Instant createdAt
) {}
