package com.taskforge.project.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ProjectResponse {
    private UUID id;
    private UUID tenantId;
    private String key;
    private String name;
    private String description;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
    private String boardConfig;
}
