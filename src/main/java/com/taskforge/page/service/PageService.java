package com.taskforge.page.service;

import com.taskforge.page.dto.UpdatePageRequest;
import com.taskforge.page.model.Page;
import com.taskforge.page.repository.PageRepository;
import com.taskforge.project.repository.ProjectRepository;
import com.taskforge.project.model.Project;
import com.taskforge.common.tenant.TenantContext;
import com.taskforge.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public Page createPage(UUID tenantId, String projectIdOrKey, Page page) {
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

        page.setTenantId(tenantId);
        page.setProjectId(projectId);
        return pageRepository.save(page);
    }

    @Transactional(readOnly = true)
    public List<Page> getPagesByProject(String projectIdOrKey) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null)
            throw new ApiException("TENANT_CONTEXT_MISSING", "Tenant context missing");

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

        return pageRepository.findAllByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public Page getPage(UUID pageId) {
        return pageRepository.findById(pageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found"));
    }

    @Transactional
    public Page updatePage(UUID pageId, UpdatePageRequest request) {
        Page existingPage = getPage(pageId);

        if (request.getTitle() != null) {
            existingPage.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            existingPage.setContent(request.getContent());
        }
        if (request.getIcon() != null) {
            existingPage.setIcon(request.getIcon());
        }

        if (request.getParentId() != null) {
            existingPage.setParentId(request.getParentId());
        }

        return pageRepository.save(existingPage);
    }

    @Transactional
    public void deletePage(UUID pageId) {
        pageRepository.deleteById(pageId);
    }
}
