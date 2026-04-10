package com.taskforge.membership.project.model;

import com.taskforge.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "project_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenant_id", "project_id", "user_id" })
})
public class ProjectMembership extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ProjectRole role;

    public enum ProjectRole {
        MANAGER,
        CONTRIBUTOR,
        VIEWER,
        QA
    }
}
