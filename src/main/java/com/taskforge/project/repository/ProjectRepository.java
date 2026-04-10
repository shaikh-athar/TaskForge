package com.taskforge.project.repository;

import com.taskforge.project.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndKey(UUID tenantId, String key);

    Optional<Project> findByTenantIdAndKey(UUID tenantId, String key);

    boolean existsByTenantIdAndId(UUID tenantId, UUID id);

    java.util.List<Project> findAllByTenantId(UUID tenantId);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.key) LIKE LOWER(CONCAT('%', :query, '%')))")
    java.util.List<Project> searchByTenantAndQuery(
            @Param("tenantId") UUID tenantId,
            @Param("query") String query,
            Pageable pageable);
}
