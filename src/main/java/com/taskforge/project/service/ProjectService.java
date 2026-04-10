package com.taskforge.project.service;

import com.taskforge.common.exception.ApiException;
import com.taskforge.common.tenant.TenantContext;
import com.taskforge.issue.model.Issue;
import com.taskforge.issue.model.IssueHistory;
import com.taskforge.project.dto.CreateProjectRequest;
import com.taskforge.project.dto.ProjectResponse;
import com.taskforge.project.dto.response.ProjectMemberResponse;
import com.taskforge.project.dto.UpdateProjectRequest;
import com.taskforge.project.model.Project;
import com.taskforge.project.repository.ProjectRepository;
import com.taskforge.membership.project.repository.ProjectMembershipRepository;
import com.taskforge.membership.tenant.repository.TenantMembershipRepository;
import com.taskforge.membership.tenant.model.TenantMembership;
import com.taskforge.user.repository.UserRepository;
import com.taskforge.user.model.User;
import com.taskforge.membership.project.model.ProjectMembership;
import com.taskforge.project.dto.ProjectInsightsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.retry.annotation.Backoff;
import com.taskforge.project.dto.ProjectInsightsResponse.TimeInStatusMetric;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final UserRepository userRepository;
    private final com.taskforge.issue.repository.IssueRepository issueRepository;
    private final com.taskforge.issue.repository.IssueHistoryRepository issueHistoryRepository;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        if (projectRepository.existsByTenantIdAndKey(tenantId, request.getKey())) {
            throw new ApiException("PROJECT_KEY_EXISTS", "Project key already exists");
        }

        Project project = new Project();
        project.setTenantId(tenantId);
        project.setKey(request.getKey());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setCreatedBy(userId);
        project.setUpdatedBy(userId);

        project = projectRepository.save(project);

        // Auto-add creator as MANAGER
        ProjectMembership pm = new ProjectMembership();
        pm.setTenantId(tenantId);
        pm.setProjectId(project.getId());
        pm.setUserId(userId);
        pm.setRole(ProjectMembership.ProjectRole.MANAGER);
        projectMembershipRepository.save(pm);

        return mapToResponse(project);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            TransactionSystemException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    @Transactional
    public ProjectResponse updateProject(String projectIdOrKey, UpdateProjectRequest request) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        Project project = resolveProject(tenantId, projectIdOrKey);

        if (request.getKey() != null && !request.getKey().equals(project.getKey())) {
            if (projectRepository.existsByTenantIdAndKey(tenantId, request.getKey())) {
                throw new ApiException("PROJECT_KEY_EXISTS", "Project key already exists");
            }
            project.setKey(request.getKey());
        }

        if (request.getName() != null)
            project.setName(request.getName());
        if (request.getDescription() != null)
            project.setDescription(request.getDescription());
        if (request.getBoardConfig() != null)
            project.setBoardConfig(request.getBoardConfig());

        project.setUpdatedBy(userId);
        project = projectRepository.save(project);

        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(String projectIdOrKey) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        Project project = resolveProject(tenantId, projectIdOrKey);

        project.setDeleted(true);
        project.setUpdatedBy(userId);
        projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(String projectIdOrKey) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }

        Project project;
        try {
            // Try as UUID
            UUID projectId = UUID.fromString(projectIdOrKey);
            project = projectRepository.findByTenantIdAndId(tenantId, projectId)
                    .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
        } catch (IllegalArgumentException e) {
            // Treat as Key
            project = projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey)
                    .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
        }

        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "projects", key = "#projectId")
    public ProjectResponse getProject(UUID projectId) {
        return getProject(projectId.toString());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects() {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }
        UUID userId = getCurrentUserId();

        TenantMembership tenantMembership = tenantMembershipRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new ApiException("TENANT_ACCESS_DENIED", "You are not a member of this tenant"));

        List<Project> projects;

        if ("OWNER".equals(tenantMembership.getRole()) || "ADMIN".equals(tenantMembership.getRole())) {
            projects = projectRepository.findAll().stream()
                    .filter(p -> p.getTenantId().equals(tenantId) && !p.isDeleted())
                    .collect(Collectors.toList());
        } else {
            List<UUID> assignedProjectIds = projectMembershipRepository.findAllByUserId(userId).stream()
                    .map(ProjectMembership::getProjectId)
                    .collect(Collectors.toList());

            projects = projectRepository.findAll().stream()
                    .filter(p -> p.getTenantId().equals(tenantId) && !p.isDeleted())
                    .filter(p -> assignedProjectIds.contains(p.getId()))
                    .collect(Collectors.toList());
        }

        return projects.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(String projectIdOrKey) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }

        Project project = resolveProject(tenantId, projectIdOrKey);
        UUID projectId = project.getId();

        List<ProjectMembership> memberships = projectMembershipRepository
                .findByTenantIdAndProjectId(tenantId, projectId);

        if (memberships.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = memberships.stream()
                .map(ProjectMembership::getUserId)
                .collect(Collectors.toList());

        java.util.Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, java.util.function.Function.identity()));

        return memberships.stream()
                .map(m -> {
                    User user = userMap.get(m.getUserId());
                    return new ProjectMemberResponse(
                            m.getUserId(),
                            user != null ? user.getDisplayName() : "Unknown",
                            user != null ? user.getEmail() : "Unknown",
                            m.getRole());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void addMember(String projectIdOrKey, String email,
            ProjectMembership.ProjectRole role) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new ApiException("TENANT_CONTEXT_MISSING", "X-Tenant-Id header is required");
        }

        Project project = resolveProject(tenantId, projectIdOrKey);
        UUID projectId = project.getId();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User with this email not found"));

        tenantMembershipRepository.findByTenantIdAndUserId(tenantId, user.getId())
                .orElseGet(() -> {
                    TenantMembership tm = new TenantMembership();
                    tm.setTenantId(tenantId);
                    tm.setUserId(user.getId());
                    tm.setRole("RESTRICTED");
                    return tenantMembershipRepository.save(tm);
                });

        projectMembershipRepository.findByTenantIdAndProjectIdAndUserId(tenantId, projectId, user.getId())
                .ifPresentOrElse(
                        membership -> {
                            membership.setRole(role);
                            projectMembershipRepository.save(membership);
                        },
                        () -> {
                            ProjectMembership pm = new ProjectMembership();
                            pm.setTenantId(tenantId);
                            pm.setProjectId(projectId);
                            pm.setUserId(user.getId());
                            pm.setRole(role);
                            projectMembershipRepository.save(pm);
                        });
    }

    @Transactional(readOnly = true)
    public ProjectInsightsResponse getInsights(String projectIdOrKey) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        Project project = resolveProject(tenantId, projectIdOrKey);
        List<Issue> issues = issueRepository
                .findAllByTenantIdAndProjectId(tenantId, project.getId())
                .stream().filter(i -> !i.isDeleted()).collect(Collectors.toList());

        ProjectInsightsResponse response = new ProjectInsightsResponse();

        long highPriorityCount = issues.stream()
                .filter(i -> i.getPriority() == Issue.IssuePriority.HIGH ||
                        i.getPriority() == Issue.IssuePriority.URGENT ||
                        i.getPriority() == Issue.IssuePriority.CRITICAL ||
                        i.getPriority() == Issue.IssuePriority.BLOCKER)
                .count();
        response.setHighPriorityCount((int) highPriorityCount);

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        long overdueCount = issues.stream().filter(i -> i.getDueDate() != null &&
                i.getDueDate().isBefore(now) &&
                i.getStatus() != Issue.IssueStatus.DONE &&
                i.getStatus() != Issue.IssueStatus.RESOLVED &&
                i.getStatus() != Issue.IssueStatus.CLOSED).count();
        response.setOverdueCount((int) overdueCount);

        if (issues.isEmpty()) {
            response.setTimeInStatus(List.of());
            return response;
        }

        List<UUID> issueIds = issues.stream().map(Issue::getId).collect(Collectors.toList());
        List<IssueHistory> histories = issueHistoryRepository.findAllByIssueIdIn(issueIds);

        // Map<IssueId, List<History>>
        java.util.Map<UUID, List<IssueHistory>> historyMap = histories.stream()
                .filter(h -> "status".equals(h.getField()))
                .collect(Collectors.groupingBy(IssueHistory::getIssueId));

        // Map<Status, List<DurationInSeconds>>
        java.util.Map<String, java.util.List<Long>> statusDurations = new java.util.HashMap<>();

        for (Issue issue : issues) {
            List<IssueHistory> issueHistory = historyMap.getOrDefault(issue.getId(),
                    new java.util.ArrayList<>());
            issueHistory.sort(java.util.Comparator.comparing(IssueHistory::getCreatedAt));

            java.time.OffsetDateTime lastTime = issue.getCreatedAt();
            // Assuming initial status is TODO, or infer from first history
            String currentStatus = "TODO";
            if (!issueHistory.isEmpty()) {
                currentStatus = issueHistory.get(0).getOldValue();
                if (currentStatus == null)
                    currentStatus = "TODO";
            }

            for (IssueHistory h : issueHistory) {
                java.time.Duration duration = java.time.Duration.between(lastTime, h.getCreatedAt());
                statusDurations.computeIfAbsent(currentStatus.toUpperCase(), k -> new java.util.ArrayList<>())
                        .add(duration.getSeconds());

                lastTime = h.getCreatedAt();
                currentStatus = h.getNewValue() != null ? h.getNewValue() : currentStatus;
            }

            if (currentStatus != null) {
                java.time.Duration duration = java.time.Duration.between(lastTime, now);
                statusDurations.computeIfAbsent(currentStatus.toUpperCase(), k -> new java.util.ArrayList<>())
                        .add(duration.getSeconds());
            }
        }

        List<TimeInStatusMetric> metrics = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, java.util.List<Long>> entry : statusDurations.entrySet()) {
            double avgSeconds = entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0);
            TimeInStatusMetric metric = new TimeInStatusMetric();
            metric.setStatus(entry.getKey());
            metric.setAvgDays(Math.round((avgSeconds / 86400.0) * 10.0) / 10.0); 
            metrics.add(metric);
        }

        metrics.sort(java.util.Comparator
                .comparing(TimeInStatusMetric::getStatus));
        response.setTimeInStatus(metrics);

        return response;
    }

    private ProjectResponse mapToResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setTenantId(project.getTenantId());
        response.setKey(project.getKey());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setCreatedAt(project.getCreatedAt());
        response.setCreatedBy(project.getCreatedBy());
        response.setUpdatedAt(project.getUpdatedAt());
        response.setUpdatedBy(project.getUpdatedBy());
        response.setBoardConfig(project.getBoardConfig());
        return response;
    }

    private UUID getCurrentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }

    private Project resolveProject(UUID tenantId, String projectIdOrKey) {
        try {
            UUID projectId = UUID.fromString(projectIdOrKey);
            return projectRepository.findByTenantIdAndId(tenantId, projectId)
                    .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
        } catch (IllegalArgumentException e) {
            return projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey)
                    .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
        }
    }
}
