package com.taskforge.issue.repository;

import com.taskforge.issue.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IssueRepository extends JpaRepository<Issue, UUID> {
    List<Issue> findAllByTenantId(UUID tenantId);

    List<Issue> findAllByTenantIdAndProjectId(UUID tenantId, UUID projectId);

    Optional<Issue> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndId(UUID tenantId, UUID id);

    List<Issue> findAllByTenantIdAndProjectIdOrderBySortOrderAsc(UUID tenantId, UUID projectId);

    List<Issue> findAllByTenantIdAndSprintIdOrderBySortOrderAsc(UUID tenantId, UUID sprintId);

    List<Issue> findAllByTenantIdAndProjectIdAndSprintIdIsNullOrderBySortOrderAsc(UUID tenantId, UUID projectId);

    List<Issue> findAllByTenantIdAndAssigneeId(UUID tenantId, UUID assigneeId);

    List<Issue> findTop10ByTenantIdAndUpdatedByOrderByUpdatedAtDesc(UUID tenantId, UUID updatedBy);

    List<Issue> findAllByTenantIdAndStarredTrue(UUID tenantId);

    @Query("SELECT i FROM Issue i WHERE i.tenantId = :tenantId AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Issue> searchByTenantAndQuery(@Param("tenantId") UUID tenantId,
            @Param("query") String query,
            Pageable pageable);
}
