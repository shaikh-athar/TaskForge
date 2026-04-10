package com.taskforge.sprint.service;

import com.taskforge.sprint.model.Sprint;
import com.taskforge.sprint.repository.SprintRepository;
import com.taskforge.project.repository.ProjectRepository;
import com.taskforge.project.model.Project;
import com.taskforge.common.tenant.TenantContext;
import com.taskforge.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public Sprint createSprint(UUID tenantId, String projectIdOrKey, Sprint sprint) {
        UUID projectId;
        try {
            projectId = UUID.fromString(projectIdOrKey);
            if (!projectRepository.existsByTenantIdAndId(tenantId, projectId)) {
                Project project = projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey)
                        .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
                projectId = project.getId();
            }
        } catch (IllegalArgumentException e) {
            Project project = projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey)
                    .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
            projectId = project.getId();
        }

        sprint.setTenantId(tenantId);
        sprint.setProjectId(projectId);
        return sprintRepository.save(sprint);
    }

    @Transactional(readOnly = true)
    public List<Sprint> getSprintsByProject(String projectIdOrKey) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

        UUID projectId;
        try {
            projectId = UUID.fromString(projectIdOrKey);
            // Verify if exists by ID, if not try Key
            if (!projectRepository.existsByTenantIdAndId(tenantId, projectId)) {
                Project project = projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey)
                        .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
                projectId = project.getId();
            }
        } catch (IllegalArgumentException e) {
            Project project = projectRepository.findByTenantIdAndKey(tenantId, projectIdOrKey)
                    .orElseThrow(() -> new ApiException("PROJECT_NOT_FOUND", "Project not found"));
            projectId = project.getId();
        }

        return sprintRepository.findAllByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Sprint getSprint(UUID sprintId) {
        return sprintRepository.findById(sprintId)
                .orElseThrow(() -> new IllegalArgumentException("Sprint not found"));
    }

    @Transactional
    public Sprint updateSprint(UUID sprintId, Sprint updatedSprint) {
        Sprint existingSprint = getSprint(sprintId);

        if (updatedSprint.getName() != null) {
            existingSprint.setName(updatedSprint.getName());
        }
        if (updatedSprint.getGoal() != null) {
            existingSprint.setGoal(updatedSprint.getGoal());
        }
        if (updatedSprint.getStartDate() != null) {
            existingSprint.setStartDate(updatedSprint.getStartDate());
        }
        if (updatedSprint.getEndDate() != null) {
            existingSprint.setEndDate(updatedSprint.getEndDate());
        }
        if (updatedSprint.getStatus() != null) {
            existingSprint.setStatus(updatedSprint.getStatus());
        }

        return sprintRepository.save(existingSprint);
    }

    @Transactional
    public void deleteSprint(UUID sprintId) {
        sprintRepository.deleteById(sprintId);
    }
}
