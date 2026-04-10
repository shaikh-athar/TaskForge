package com.taskforge.issue.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class WorkLogResponse {
    private UUID id;
    private UUID issueId;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private Integer timeSpentMinutes;
    private OffsetDateTime startedAt;
    private String description;
    private OffsetDateTime createdAt;
}
