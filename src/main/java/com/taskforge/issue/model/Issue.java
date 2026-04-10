package com.taskforge.issue.model;

import com.taskforge.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "issues")
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private IssueType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private IssuePriority priority;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "start_date")
    private java.time.OffsetDateTime startDate;

    @Column(name = "due_date")
    private java.time.OffsetDateTime dueDate;

    @Column(name = "sort_order")
    private Double sortOrder;

    @Column(name = "starred", nullable = false)
    private Boolean starred = false;

    @Column(name = "story_points")
    private Double storyPoints;

    public enum IssueType {
        EPIC, STORY, TASK, BUG, SUB_TASK
    }

    public enum IssueStatus {
        BACKLOG, TODO, IN_PROGRESS, IN_REVIEW, IN_TESTING, DONE, BLOCKED, CLOSED, REOPENED, RESOLVED
    }

    public enum IssuePriority {
        BLOCKER, CRITICAL, URGENT, HIGH, MEDIUM, LOW, LOWEST
    }
}
