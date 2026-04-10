package com.taskforge.membership.project.repository;

import com.taskforge.membership.project.model.ProjectMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectMembershipRepository extends JpaRepository<ProjectMembership, UUID> {
    Optional<ProjectMembership> findByTenantIdAndProjectIdAndUserId(UUID tenantId, UUID projectId, UUID userId);

    java.util.List<ProjectMembership> findByTenantIdAndProjectId(UUID tenantId, UUID projectId);

    java.util.List<ProjectMembership> findAllByUserId(UUID userId);

    Optional<ProjectMembership> findByUserIdAndProjectId(UUID userId, UUID projectId);
}
