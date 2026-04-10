package com.taskforge.issue.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CommentResponse {
    private UUID id;
    private UUID issueId;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private String text;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
