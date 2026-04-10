package com.taskforge.issue.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityItem {
    private UUID id;
    private ActivityType type;
    private OffsetDateTime createdAt;
    private UUID userId;
    private String userName;
    private String userAvatarUrl;
    private Object data;

    public enum ActivityType {
        HISTORY,
        COMMENT,
        WORKLOG
    }
}
