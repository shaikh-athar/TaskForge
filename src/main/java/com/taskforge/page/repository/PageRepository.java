package com.taskforge.page.repository;

import com.taskforge.page.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PageRepository extends JpaRepository<Page, UUID> {
    List<Page> findAllByProjectId(UUID projectId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Page p WHERE p.tenantId = :tenantId AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Page> searchByTenantAndQuery(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId,
            @org.springframework.data.repository.query.Param("query") String query,
            org.springframework.data.domain.Pageable pageable);
}
