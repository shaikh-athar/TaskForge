package com.taskforge.issue.service;

import com.taskforge.common.exception.ApiException;
import com.taskforge.common.tenant.TenantContext;
import com.taskforge.issue.dto.ActivityItem;
import com.taskforge.issue.dto.CreateIssueRequest;
import com.taskforge.issue.dto.IssueHistoryResponse;
import com.taskforge.issue.dto.IssueResponse;
import com.taskforge.issue.dto.UpdateIssueRequest;
import com.taskforge.issue.model.Comment;
import com.taskforge.issue.model.Issue;
import com.taskforge.issue.model.Issue.IssueStatus;
import com.taskforge.issue.model.IssueHistory;
import com.taskforge.issue.repository.IssueHistoryRepository;
import com.taskforge.issue.repository.IssueRepository;
import com.taskforge.project.model.Project;
import com.taskforge.project.repository.ProjectRepository;
import com.taskforge.sprint.model.Sprint;
import com.taskforge.sprint.repository.SprintRepository;
import com.taskforge.common.security.TenantSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.taskforge.issue.dto.MoveIssueRequest;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final com.taskforge.membership.project.repository.ProjectMembershipRepository projectMembershipRepository;
    private final com.taskforge.membership.tenant.repository.TenantMembershipRepository tenantMembershipRepository;
    private final com.taskforge.notification.service.NotificationService notificationService;
    private final TenantSecurityService securityService;
    private final com.taskforge.user.repository.UserRepository userRepository;
    private final IssueHistoryRepository issueHistoryRepository;
    private final com.taskforge.issue.repository.CommentRepository commentRepository;
    private final com.taskforge.issue.repository.WorkLogRepository workLogRepository;

    @Transactional
    public IssueResponse createIssue(CreateIssueRequest request) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        Project project = projectRepository.findByTenantIdAndId(tenantId, request.getProjectId())
                .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));

        Issue issue = new Issue();
        issue.setTenantId(tenantId);
        issue.setProjectId(project.getId());
        issue.setType(request.getType());
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setStatus(IssueStatus.TODO);
        issue.setPriority(request.getPriority());
        issue.setAssigneeId(request.getAssigneeId());
        issue.setStoryPoints(request.getStoryPoints());

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new ApiException("SPRINT_NOT_FOUND", "Sprint not found"));
            if (!sprint.getProjectId().equals(project.getId())) {
                throw new ApiException("INVALID_SPRINT", "Sprint does not belong to this project");
            }
            issue.setSprintId(request.getSprintId());
        }

        if (request.getParentId() != null) {
            issue.setParentId(request.getParentId());
        }

        if (request.getStartDate() != null) {
            issue.setStartDate(java.time.OffsetDateTime.parse(request.getStartDate()));
        }
        if (request.getDueDate() != null) {
            issue.setDueDate(java.time.OffsetDateTime.parse(request.getDueDate()));
        }
        issue.setReporterId(userId);
        issue.setStarred(false);

        issue.setCreatedBy(userId);
        issue.setUpdatedBy(userId);

        issue = issueRepository.save(issue);

        // Record Creation History
        saveHistory(issue, userId, "issue", null, "created");

        if (issue.getAssigneeId() != null && !issue.getAssigneeId().equals(userId)) {
            notificationService.createNotification(
                    issue.getAssigneeId().toString(),
                    com.taskforge.notification.model.NotificationType.ISSUE_ASSIGNED,
                    "You have been assigned to issue " + issue.getTitle(),
                    issue.getId().toString());
        }

        return mapToResponse(issue);
    }

    @Transactional
    public IssueResponse updateIssue(UUID issueId, UpdateIssueRequest request) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        Issue issue = issueRepository.findByTenantIdAndId(tenantId, issueId)
                .orElseThrow(() -> new ApiException("ISSUE_NOT_FOUND", "Issue not found"));

        UUID oldAssigneeId = issue.getAssigneeId();

        // Track History
        if (request.getTitle() != null && !Objects.equals(issue.getTitle(), request.getTitle())) {
            saveHistory(issue, userId, "title", issue.getTitle(), request.getTitle());
            issue.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !Objects.equals(issue.getDescription(), request.getDescription())) {
            // Usually don't track full description history in simple table, but valid for
            // now
            // saveHistory(issue, userId, "description", "...", "...");
            issue.setDescription(request.getDescription());
        }
        if (request.getStatus() != null && issue.getStatus() != request.getStatus()) {
            validateTransition(issue.getStatus(), request.getStatus(), tenantId, issue.getProjectId(), userId);
            saveHistory(issue, userId, "status", issue.getStatus().toString(), request.getStatus().toString());
            issue.setStatus(request.getStatus());
        }
        if (request.getPriority() != null && issue.getPriority() != request.getPriority()) {
            saveHistory(issue, userId, "priority", issue.getPriority().toString(), request.getPriority().toString());
            issue.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null && !Objects.equals(issue.getAssigneeId(), request.getAssigneeId())) {
            String oldAssignee = issue.getAssigneeId() != null ? issue.getAssigneeId().toString() : "Unassigned";
            String newAssignee = request.getAssigneeId().toString();
            saveHistory(issue, userId, "assignee", oldAssignee, newAssignee);
            issue.setAssigneeId(request.getAssigneeId());
        }
        if (request.getStoryPoints() != null && !Objects.equals(issue.getStoryPoints(), request.getStoryPoints())) {
            String oldPoints = issue.getStoryPoints() != null ? issue.getStoryPoints().toString() : null;
            saveHistory(issue, userId, "story_points", oldPoints, request.getStoryPoints().toString());
            issue.setStoryPoints(request.getStoryPoints());
        }

        if (request.getStartDate() != null)
            issue.setStartDate(java.time.OffsetDateTime.parse(request.getStartDate()));
        if (request.getDueDate() != null)
            issue.setDueDate(java.time.OffsetDateTime.parse(request.getDueDate()));

        if (request.getSprintId() != null) {
            if (!Objects.equals(issue.getSprintId(), request.getSprintId())) {
                Sprint sprint = sprintRepository.findById(request.getSprintId())
                        .orElseThrow(() -> new ApiException("SPRINT_NOT_FOUND", "Sprint not found"));
                if (!sprint.getProjectId().equals(issue.getProjectId())) {
                    throw new ApiException("INVALID_SPRINT", "Sprint does not belong to this project");
                }
                saveHistory(issue, userId, "sprint",
                        issue.getSprintId() != null ? issue.getSprintId().toString() : null,
                        request.getSprintId().toString());
                issue.setSprintId(request.getSprintId());
            }
        }

        if (request.getParentId() != null) {
            issue.setParentId(request.getParentId());
        }

        if (request.getStarred() != null) {
            issue.setStarred(request.getStarred());
        }

        issue.setUpdatedBy(userId);
        issue = issueRepository.save(issue);

        // Notify New Assignee
        if (issue.getAssigneeId() != null && !issue.getAssigneeId().equals(oldAssigneeId)
                && !issue.getAssigneeId().equals(userId)) {
            notificationService.createNotification(
                    issue.getAssigneeId().toString(),
                    com.taskforge.notification.model.NotificationType.ISSUE_ASSIGNED,
                    "You have been assigned to issue " + issue.getTitle(),
                    issue.getId().toString());
        }

        return mapToResponse(issue);
    }

    private void saveHistory(Issue issue, UUID userId, String field, String oldValue, String newValue) {
        IssueHistory history = new IssueHistory();
        history.setIssueId(issue.getId());
        history.setUserId(userId);
        history.setField(field);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        issueHistoryRepository.save(history);
    }

    @Transactional
    public void deleteIssue(UUID issueId) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        Issue issue = issueRepository.findByTenantIdAndId(tenantId, issueId)
                .orElseThrow(() -> new ApiException("ISSUE_NOT_FOUND", "Issue not found"));

        issue.setDeleted(true);
        issue.setUpdatedBy(userId);
        issueRepository.save(issue);
    }

    @Transactional
    public IssueResponse moveIssue(UUID issueId, MoveIssueRequest request) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }

        Issue issue = issueRepository.findByTenantIdAndId(tenantId, issueId)
                .orElseThrow(() -> new ApiException("ISSUE_NOT_FOUND", "Issue not found"));

        List<Issue> siblings;
        if (request.getSprintId() != null) {
            siblings = issueRepository.findAllByTenantIdAndSprintIdOrderBySortOrderAsc(tenantId, request.getSprintId());
        } else {
            siblings = issueRepository.findAllByTenantIdAndProjectIdAndSprintIdIsNullOrderBySortOrderAsc(tenantId,
                    issue.getProjectId());
        }

        siblings = siblings.stream()
                .filter(i -> !i.getId().equals(issueId))
                .collect(Collectors.toList());

        double newOrder;
        int index = request.getTargetIndex();
        if (index < 0)
            index = 0;
        if (index > siblings.size())
            index = siblings.size();

        if (siblings.isEmpty()) {
            newOrder = 1000.0;
        } else if (index == 0) {
            Double next = siblings.get(0).getSortOrder();
            newOrder = (next != null ? next : 0.0) / 2.0;
        } else if (index == siblings.size()) {
            Double prev = siblings.get(siblings.size() - 1).getSortOrder();
            newOrder = (prev != null ? prev : 0.0) + 1000.0;
        } else {
            Double prev = siblings.get(index - 1).getSortOrder();
            Double next = siblings.get(index).getSortOrder();
            newOrder = ((prev != null ? prev : 0.0) + (next != null ? next : 0.0)) / 2.0;
        }

        issue.setSprintId(request.getSprintId());
        issue.setSortOrder(newOrder);

        return mapToResponse(issueRepository.save(issue));
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getProjectIssues(String projectIdOrKey) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }

        UUID projectId;
        try {
            projectId = UUID.fromString(projectIdOrKey);
            if (!projectRepository.existsByTenantIdAndId(tenantId, projectId)) {
                if (projectRepository.existsByTenantIdAndKey(tenantId, projectIdOrKey)) {
                    projectId = projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey).get().getId();
                } else {
                    throw new ApiException("PROJECT_NOT_FOUND", "Project not found");
                }
            }
        } catch (IllegalArgumentException e) {
            Project project = projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey)
                    .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
            projectId = project.getId();
        }

        UUID userId = getCurrentUserId();
        securityService.authorizeProjectAccess(tenantId, projectId, userId,
                com.taskforge.common.security.TenantSecurityService.Permission.VIEW);

        return issueRepository.findAllByTenantIdAndProjectId(tenantId, projectId)
                .stream()
                .filter(i -> !i.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getAllIssues() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }

        return issueRepository.findAllByTenantId(tenantId).stream()
                .filter(i -> !i.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IssueResponse getIssue(UUID issueId) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        Issue issue = issueRepository.findByTenantIdAndId(tenantId, issueId)
                .orElseThrow(() -> new ApiException("ISSUE_NOT_FOUND", "Issue not found"));

        return mapToResponse(issue);
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getMyIssues() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();
        return issueRepository.findAllByTenantIdAndAssigneeId(tenantId, userId).stream()
                .filter(i -> !i.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getRecentWork() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();
        return issueRepository.findTop10ByTenantIdAndUpdatedByOrderByUpdatedAtDesc(tenantId, userId).stream()
                .filter(i -> !i.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueResponse> getStarredIssues() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        return issueRepository.findAllByTenantIdAndStarredTrue(tenantId).stream()
                .filter(i -> !i.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ActivityItem> getActivity(UUID issueId) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        if (!issueRepository.existsByTenantIdAndId(tenantId, issueId)) {
            throw new ApiException("ISSUE_NOT_FOUND", "Issue not found");
        }

        List<com.taskforge.issue.dto.ActivityItem> activity = new java.util.ArrayList<>();

        activity.addAll(issueHistoryRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(this::mapHistoryToActivity)
                .toList());

        activity.addAll(commentRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(this::mapCommentToActivity)
                .toList());

        activity.addAll(workLogRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(this::mapWorkLogToActivity)
                .toList());

        activity.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return activity;
    }

    private com.taskforge.issue.dto.ActivityItem mapHistoryToActivity(IssueHistory history) {
        IssueHistoryResponse response = mapHistoryToResponse(history);

        if ("story_points".equals(response.getField())) {
            response.setField("Story Points");
        } else if (response.getField() != null) {
            response.setField(org.springframework.util.StringUtils.capitalize(response.getField().replace("_", " ")));
        }

        return com.taskforge.issue.dto.ActivityItem.builder()
                .id(history.getId())
                .type(com.taskforge.issue.dto.ActivityItem.ActivityType.HISTORY)
                .createdAt(history.getCreatedAt())
                .userId(history.getUserId())
                .userName(response.getUserName())
                .userAvatarUrl(response.getUserAvatarUrl())
                .data(response)
                .build();
    }

    private ActivityItem mapCommentToActivity(Comment comment) {
        com.taskforge.issue.dto.CommentResponse response = new com.taskforge.issue.dto.CommentResponse();
        response.setId(comment.getId());
        response.setIssueId(comment.getIssueId());
        response.setText(comment.getText());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        response.setUserId(comment.getUserId());

        userRepository.findById(comment.getUserId()).ifPresent(user -> {
            response.setUserName(user.getDisplayName());
            response.setUserAvatarUrl(user.getAvatarUrl());
        });

        return com.taskforge.issue.dto.ActivityItem.builder()
                .id(comment.getId())
                .type(com.taskforge.issue.dto.ActivityItem.ActivityType.COMMENT)
                .createdAt(comment.getCreatedAt())
                .userId(comment.getUserId())
                .userName(response.getUserName())
                .userAvatarUrl(response.getUserAvatarUrl())
                .data(response)
                .build();
    }

    private com.taskforge.issue.dto.ActivityItem mapWorkLogToActivity(com.taskforge.issue.model.WorkLog workLog) {
        com.taskforge.issue.dto.WorkLogResponse response = new com.taskforge.issue.dto.WorkLogResponse();
        response.setId(workLog.getId());
        response.setIssueId(workLog.getIssueId());
        response.setUserId(workLog.getUserId());
        response.setDescription(workLog.getDescription());
        response.setCreatedAt(workLog.getCreatedAt());
        response.setTimeSpentMinutes(workLog.getTimeSpentMinutes());
        response.setStartedAt(workLog.getStartedAt());

        userRepository.findById(workLog.getUserId()).ifPresent(user -> {
            response.setUserName(user.getDisplayName());
            response.setUserAvatarUrl(user.getAvatarUrl());
        });

        return com.taskforge.issue.dto.ActivityItem.builder()
                .id(workLog.getId())
                .type(com.taskforge.issue.dto.ActivityItem.ActivityType.WORKLOG)
                .createdAt(workLog.getCreatedAt())
                .userId(workLog.getUserId())
                .userName(response.getUserName())
                .userAvatarUrl(response.getUserAvatarUrl())
                .data(response)
                .build();
    }

    @Transactional(readOnly = true)
    public List<IssueHistoryResponse> getHistory(UUID issueId) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        if (!issueRepository.existsByTenantIdAndId(tenantId, issueId)) {
            throw new ApiException("ISSUE_NOT_FOUND", "Issue not found");
        }
        return issueHistoryRepository.findByIssueIdOrderByCreatedAtDesc(issueId).stream()
                .map(this::mapHistoryToResponse)
                .collect(Collectors.toList());
    }

    private IssueHistoryResponse mapHistoryToResponse(IssueHistory history) {
        IssueHistoryResponse response = new IssueHistoryResponse();
        response.setId(history.getId());
        response.setIssueId(history.getIssueId());
        response.setUserId(history.getUserId());
        response.setField(history.getField());
        response.setOldValue(history.getOldValue());
        response.setNewValue(history.getNewValue());
        response.setCreatedAt(history.getCreatedAt());

        userRepository.findById(history.getUserId()).ifPresent(user -> {
            response.setUserName(user.getDisplayName());
            response.setUserAvatarUrl(user.getAvatarUrl());
        });

        return response;
    }

    private IssueResponse mapToResponse(Issue issue) {
        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setTenantId(issue.getTenantId());
        response.setProjectId(issue.getProjectId());
        response.setType(issue.getType());
        response.setTitle(issue.getTitle());
        response.setDescription(issue.getDescription());
        response.setStatus(issue.getStatus());
        response.setPriority(issue.getPriority());
        response.setAssigneeId(issue.getAssigneeId());
        if (issue.getAssigneeId() != null) {
            userRepository.findById(issue.getAssigneeId()).ifPresent(user -> {
                response.setAssigneeName(user.getDisplayName());
                response.setAssigneeAvatarUrl(user.getAvatarUrl());
            });
        }
        response.setReporterId(issue.getReporterId());
        response.setSprintId(issue.getSprintId());
        response.setParentId(issue.getParentId());
        response.setStartDate(issue.getStartDate());
        response.setDueDate(issue.getDueDate());
        response.setCreatedAt(issue.getCreatedAt());
        response.setCreatedBy(issue.getCreatedBy());
        response.setUpdatedAt(issue.getUpdatedAt());
        response.setUpdatedBy(issue.getUpdatedBy());
        response.setSortOrder(issue.getSortOrder());
        response.setStarred(issue.getStarred());
        response.setStoryPoints(issue.getStoryPoints());
        return response;
    }

    private void validateTransition(IssueStatus currentStatus, IssueStatus newStatus, UUID tenantId, UUID projectId,
            UUID userId) {
        if (currentStatus == IssueStatus.IN_TESTING && newStatus == IssueStatus.DONE) {
            boolean isTenantAdmin = tenantMembershipRepository.findByTenantIdAndUserId(tenantId, userId)
                    .map(tm -> "OWNER".equals(tm.getRole()) || "ADMIN".equals(tm.getRole()))
                    .orElse(false);

            if (isTenantAdmin) {
                return;
            }

            com.taskforge.membership.project.model.ProjectMembership membership = projectMembershipRepository
                    .findByTenantIdAndProjectIdAndUserId(tenantId, projectId, userId)
                    .orElseThrow(() -> new ApiException("ACCESS_DENIED", "User is not a member of this project"));

            com.taskforge.membership.project.model.ProjectMembership.ProjectRole role = membership.getRole();
            if (role != com.taskforge.membership.project.model.ProjectMembership.ProjectRole.QA &&
                    role != com.taskforge.membership.project.model.ProjectMembership.ProjectRole.MANAGER) {

                throw new ApiException("WORKFLOW_VIOLATION", "Only QA can move issues from Testing to Done");
            }
        }
    }

    private UUID getCurrentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
