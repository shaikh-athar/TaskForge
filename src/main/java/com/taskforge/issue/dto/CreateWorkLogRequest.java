package com.taskforge.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CreateWorkLogRequest {
    @NotNull
    private Integer timeSpentMinutes;

    private OffsetDateTime startedAt;

    private String description;
}
