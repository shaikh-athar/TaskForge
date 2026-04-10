package com.taskforge.search.dto;

public record IssueSearchResult(
        String id,
        String key,
        String summary,
        String projectId,
        String status,
        String type) {
}
