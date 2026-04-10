package com.taskforge.issue.service;

import com.taskforge.common.exception.ApiException;
import com.taskforge.common.tenant.TenantContext;
import com.taskforge.issue.dto.WorkLogResponse;
import com.taskforge.issue.model.WorkLog;
import com.taskforge.issue.repository.IssueRepository;
import com.taskforge.issue.repository.WorkLogRepository;
import com.taskforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkLogResponse logWork(UUID issueId, Integer timeSpentMinutes, OffsetDateTime startedAt,
            String description) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        if (!issueRepository.existsByTenantIdAndId(tenantId, issueId)) {
            throw new ApiException("ISSUE_NOT_FOUND", "Issue not found");
        }

        WorkLog workLog = new WorkLog();
        workLog.setIssueId(issueId);
        workLog.setUserId(getCurrentUserId());
        workLog.setTimeSpentMinutes(timeSpentMinutes);
        workLog.setStartedAt(startedAt != null ? startedAt : OffsetDateTime.now());
        workLog.setDescription(description);
        workLog.setCreatedBy(workLog.getUserId());
        workLog.setUpdatedBy(workLog.getUserId());

        workLog = workLogRepository.save(workLog);
        return mapToResponse(workLog);
    }

    @Transactional(readOnly = true)
    public List<WorkLogResponse> getWorkLogs(UUID issueId) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        if (!issueRepository.existsByTenantIdAndId(tenantId, issueId)) {
            throw new ApiException("ISSUE_NOT_FOUND", "Issue not found");
        }

        return workLogRepository.findByIssueIdOrderByStartedAtDesc(issueId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private WorkLogResponse mapToResponse(WorkLog workLog) {
        WorkLogResponse response = new WorkLogResponse();
        response.setId(workLog.getId());
        response.setIssueId(workLog.getIssueId());
        response.setUserId(workLog.getUserId());
        response.setTimeSpentMinutes(workLog.getTimeSpentMinutes());
        response.setStartedAt(workLog.getStartedAt());
        response.setDescription(workLog.getDescription());
        response.setCreatedAt(workLog.getCreatedAt());

        userRepository.findById(workLog.getUserId()).ifPresent(user -> {
            response.setUserName(user.getDisplayName());
            response.setUserAvatarUrl(user.getAvatarUrl());
        });

        return response;
    }

    private UUID getCurrentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
