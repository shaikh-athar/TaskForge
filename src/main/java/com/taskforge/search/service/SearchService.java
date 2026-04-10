package com.taskforge.search.service;

import com.taskforge.issue.model.Issue;
import com.taskforge.issue.repository.IssueRepository;
import com.taskforge.page.model.Page;
import com.taskforge.page.repository.PageRepository;
import com.taskforge.project.model.Project;
import com.taskforge.project.repository.ProjectRepository;
import com.taskforge.search.dto.*;
import com.taskforge.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final PageRepository pageRepository;

    private static final int MAX_RESULTS_PER_TYPE = 5;

    public SearchResultResponse search(String query) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is required for search");
        }

        Pageable pageable = PageRequest.of(0, MAX_RESULTS_PER_TYPE);

        List<IssueSearchResult> issues = issueRepository.searchByTenantAndQuery(tenantId, query, pageable)
                .stream()
                .map(this::mapIssue)
                .collect(Collectors.toList());

        List<ProjectSearchResult> projects = projectRepository.searchByTenantAndQuery(tenantId, query, pageable)
                .stream()
                .map(this::mapProject)
                .collect(Collectors.toList());

        List<PageSearchResult> pages = pageRepository.searchByTenantAndQuery(tenantId, query, pageable)
                .stream()
                .map(this::mapPage)
                .collect(Collectors.toList());

        return new SearchResultResponse(issues, projects, pages);
    }

    private IssueSearchResult mapIssue(Issue issue) {
        // Issue model doesn't have a key field - generate from type + ID prefix
        String key = issue.getType().name().substring(0, 1) + "-"
                + issue.getId().toString().substring(0, 8).toUpperCase();
        return new IssueSearchResult(
                issue.getId().toString(),
                key,
                issue.getTitle(),
                issue.getProjectId().toString(),
                issue.getStatus().name(),
                issue.getType().name());
    }

    private ProjectSearchResult mapProject(Project project) {
        return new ProjectSearchResult(
                project.getId().toString(),
                project.getKey(),
                project.getName(),
                project.getDescription());
    }

    private PageSearchResult mapPage(Page page) {
        return new PageSearchResult(
                page.getId().toString(),
                page.getTitle(),
                page.getProjectId().toString());
    }
}
