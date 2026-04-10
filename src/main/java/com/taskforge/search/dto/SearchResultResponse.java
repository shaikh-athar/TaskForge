package com.taskforge.search.dto;

import java.util.List;

public record SearchResultResponse(
        List<IssueSearchResult> issues,
        List<ProjectSearchResult> projects,
        List<PageSearchResult> pages) {
}
