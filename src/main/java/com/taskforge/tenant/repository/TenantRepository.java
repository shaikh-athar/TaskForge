package com.taskforge.tenant.repository;

import com.taskforge.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsByNameAndDeletedFalse(String name);
    Optional<Tenant> findByIdAndDeletedFalse(UUID tenantId);
}
