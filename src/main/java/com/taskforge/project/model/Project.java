package com.taskforge.project.model;

import com.taskforge.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "projects", uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "key" }))
public class Project extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "board_config", columnDefinition = "TEXT")
    private String boardConfig;
}
