package com.taskforge.project.dto;

import lombok.Data;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateProjectRequest {
    private String name;
    private String description;
    private String key;
    private String boardConfig;
}
