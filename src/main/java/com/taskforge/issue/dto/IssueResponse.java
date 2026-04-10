package com.taskforge.issue.dto;

import com.taskforge.issue.model.Issue.IssuePriority;
import com.taskforge.issue.model.Issue.IssueStatus;
import com.taskforge.issue.model.Issue.IssueType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class IssueResponse {
    private UUID id;
    private UUID tenantId;
    private UUID projectId;
    private IssueType type;
    private String title;
    private String description;
    private IssueStatus status;
    private IssuePriority priority;
    private UUID assigneeId;
    private String assigneeName;
    private String assigneeAvatarUrl;
    private UUID reporterId;
    private UUID sprintId;
    private UUID parentId;
    private java.time.OffsetDateTime startDate;
    private java.time.OffsetDateTime dueDate;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
    private Double sortOrder;
    private Boolean starred;
    private Double storyPoints;
}
