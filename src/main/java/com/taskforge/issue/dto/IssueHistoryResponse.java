package com.taskforge.issue.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class IssueHistoryResponse {
    private UUID id;
    private UUID issueId;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private String field;
    private String oldValue;
    private String newValue;
    private OffsetDateTime createdAt;
}
