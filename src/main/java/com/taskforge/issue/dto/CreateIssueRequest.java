package com.taskforge.issue.dto;

import com.taskforge.issue.model.Issue.IssuePriority;
import com.taskforge.issue.model.Issue.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateIssueRequest {
    @NotNull
    private UUID projectId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private IssueType type;

    @NotNull
    private IssuePriority priority;

    private UUID assigneeId;

    private UUID sprintId;
    private UUID parentId;

    private String startDate;
    private String dueDate;
    private Double storyPoints;
}
