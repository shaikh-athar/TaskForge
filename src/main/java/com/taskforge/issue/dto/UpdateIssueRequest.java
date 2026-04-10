package com.taskforge.issue.dto;

import com.taskforge.issue.model.Issue.IssuePriority;
import com.taskforge.issue.model.Issue.IssueStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateIssueRequest {
    private String title;
    private String description;
    private IssueStatus status;
    private IssuePriority priority;
    private UUID assigneeId;

    private UUID sprintId;
    private UUID parentId;

    private String startDate;
    private String dueDate;
    private Boolean starred;
    private Double storyPoints;
}
