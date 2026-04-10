package com.taskforge.user.dto.response;

import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl
) {}
