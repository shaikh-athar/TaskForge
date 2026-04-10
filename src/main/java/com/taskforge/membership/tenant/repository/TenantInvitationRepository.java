package com.taskforge.membership.tenant.repository;

import com.taskforge.membership.tenant.model.TenantInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantInvitationRepository extends JpaRepository<TenantInvitation, UUID> {
    Optional<TenantInvitation> findByToken(String token);

    List<TenantInvitation> findByTenantId(UUID tenantId);

    void deleteByEmailAndTenantId(String email, UUID tenantId);
}
