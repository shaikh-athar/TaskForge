package com.taskforge.issue.repository;

import com.taskforge.issue.model.IssueHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IssueHistoryRepository extends JpaRepository<IssueHistory, UUID> {
    List<IssueHistory> findByIssueIdOrderByCreatedAtDesc(UUID issueId);

    List<IssueHistory> findAllByIssueIdIn(List<UUID> issueIds);
}
