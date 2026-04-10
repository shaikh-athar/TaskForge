package com.taskforge.project.dto.response;

import com.taskforge.membership.project.model.ProjectMembership.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private ProjectRole role;
}
