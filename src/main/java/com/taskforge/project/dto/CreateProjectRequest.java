package com.taskforge.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectRequest {
    @NotBlank
    private String key;

    @NotBlank
    private String name;

    private String description;
}
