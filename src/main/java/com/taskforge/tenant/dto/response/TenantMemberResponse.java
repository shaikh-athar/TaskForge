package com.taskforge.tenant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberResponse {
    private UUID userId;
    private String displayName;
    private String email;
    private String role;
}
