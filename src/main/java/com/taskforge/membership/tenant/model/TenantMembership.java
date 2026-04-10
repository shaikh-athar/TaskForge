package com.taskforge.membership.tenant.model;

import com.taskforge.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tenant_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id"}))
public class TenantMembership extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role", nullable = false)
    private String role; // OWNER, ADMIN, MEMBER
}
